package br.gabriel.cochilo.sync

import android.content.Context
import android.provider.Settings
import android.util.Base64
import br.gabriel.cochilo.data.CochiloDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Partner sync (0.47.0, docs/sync.md): two parents' phones keep one log, over the home Wi-Fi, with
 * no server and no account.
 * - **Pairing, once:** both open Settings › Pair a phone; one taps the other's name; both show
 *   the same six digits; both tap "Same number".
 * - **Sync:** while Cochilo is open on both, every change goes across within seconds, and opening
 *   the app catches up.
 * Each phone keeps its whole log; a sync sends both logs both ways and merges them ([SyncMerge]).
 */
class PartnerSync(context: Context, private val db: CochiloDb, private val link: SyncLink) {
    private val sp = context.getSharedPreferences("cochilo-sync", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private val myName: String = runCatching { Settings.Global.getString(context.contentResolver, "device_name") }.getOrNull()
        ?.takeIf { it.isNotBlank() } ?: android.os.Build.MODEL

    val available get() = link.available

    data class State(val partner: String?, val lastSyncedAt: Long?, val syncing: Boolean = false, val unreachable: Boolean = false)

    private val _state = MutableStateFlow(State(sp.getString(PARTNER, null), sp.getLong(LAST, 0).takeIf { it > 0 }))
    val state: StateFlow<State> = _state

    /** What a finished sync brought, for the snackbar. */
    data class Synced(val partner: String, val added: Int, val updated: Int, val removed: Int)
    private val _synced = MutableSharedFlow<Synced>(extraBufferCapacity = 4)
    val synced: SharedFlow<Synced> = _synced

    private fun key(): ByteArray? = sp.getString(KEY, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    // ---------------------------------------------------------------- syncing

    private var serving: java.io.Closeable? = null
    private var watching: Job? = null
    private var lastPeer: Peer? = null

    /** Cochilo came to the front: answer the partner, catch up, and send changes as they happen. */
    @OptIn(FlowPreview::class)
    fun start() {
        val k = key() ?: return
        if (!link.available || serving != null) return
        serving = link.offer(SyncLink.Mode.SYNC, myName, SyncCrypto.pairId(k)) { wire -> serve(wire, k) }
        scope.launch { syncNow() }
        watching = scope.launch {
            db.events().observeAll().drop(1).debounce(CHANGE_DEBOUNCE_MS).collect { syncNow(quiet = true) }
        }
    }

    /** Cochilo went to the back: stop answering. */
    fun stop() {
        serving?.close(); serving = null
        watching?.cancel(); watching = null
    }

    // The lock covers only writing the merge, never a conversation: both phones may reach out to
    // each other at the same moment, and each must still answer the other.
    private fun serve(wire: Wire, k: ByteArray) = runBlocking {
        val remote = SyncSession.asServer(wire, k) { runBlocking { snapshot() } }
        apply(remote)
    }

    /** Finds the partner on the Wi-Fi and syncs both ways. [quiet]: no "not found" state after a change. */
    suspend fun syncNow(quiet: Boolean = false) {
        val k = key() ?: return
        if (!link.available) return
        _state.value = _state.value.copy(syncing = true)
        val ok = runCatching {
            val peer = withContext(Dispatchers.IO) { lastPeer?.takeIf { canReach(it) } }
                ?: withTimeoutOrNull(FIND_MS) { link.browse(SyncLink.Mode.SYNC, SyncCrypto.pairId(k)).first { it.isNotEmpty() } }?.first()
                ?: return@runCatching false
            lastPeer = peer
            val remote = withContext(Dispatchers.IO) { link.connect(peer).use { SyncSession.asClient(it, k) { runBlocking { snapshot() } } } }
            apply(remote)
            true
        }.getOrDefault(false)
        _state.value = _state.value.copy(syncing = false, unreachable = !ok && !quiet)
    }

    private fun canReach(p: Peer) = runCatching { link.connect(p).close(); true }.getOrDefault(false)

    private suspend fun snapshot() = SyncData(baby = db.baby().get(), events = db.events().all(), deletions = db.events().deletions())

    private suspend fun apply(remote: SyncData) = lock.withLock {
        val c = SyncMerge.changes(snapshot(), remote)
        if (c.upsert.isNotEmpty()) db.events().upsertAll(c.upsert)
        if (c.delete.isNotEmpty()) db.events().deleteIds(c.delete)
        if (c.deletions.isNotEmpty()) db.events().rememberAll(c.deletions)
        c.baby?.let { db.baby().upsert(it) }
        val now = System.currentTimeMillis()
        sp.edit().putLong(LAST, now).apply()
        _state.value = _state.value.copy(lastSyncedAt = now, unreachable = false)
        val partner = _state.value.partner ?: ""
        if (c.added + c.updated + c.removed > 0) _synced.tryEmit(Synced(partner, c.added, c.updated, c.removed))
    }

    // ---------------------------------------------------------------- pairing

    sealed interface Pairing {
        data object Idle : Pairing
        /** Looking for phones that also opened "Pair a phone". */
        data class Looking(val phones: List<Peer>) : Pairing
        /** Both phones show [number]; the parent says whether the partner's phone shows it too. */
        data class Compare(val number: String, val partner: String, val waitingForPartner: Boolean = false) : Pairing
        data class Done(val partner: String) : Pairing
        data object Failed : Pairing
    }

    private val _pairing = MutableStateFlow<Pairing>(Pairing.Idle)
    val pairing: StateFlow<Pairing> = _pairing
    private var pairJob: Job? = null
    private var offering: java.io.Closeable? = null
    private var answer: CompletableFuture<Boolean>? = null
    @Volatile private var busyPairing = false

    /** Settings › Pair a phone: offer this phone and list the others doing the same. */
    fun startPairing() {
        if (!link.available) return
        cancelPairing()
        _pairing.value = Pairing.Looking(emptyList())
        offering = link.offer(SyncLink.Mode.PAIR, myName, null) { wire -> if (!busyPairing) pairOver(wire, guest = false) }
        pairJob = scope.launch {
            link.browse(SyncLink.Mode.PAIR, null).collect { phones -> if (_pairing.value is Pairing.Looking) _pairing.value = Pairing.Looking(phones) }
        }
    }

    /** The parent tapped the other phone's name. */
    fun pick(peer: Peer) {
        pairJob?.cancel()
        scope.launch { runCatching { link.connect(peer).use { pairOver(it, guest = true) } }.onFailure { _pairing.value = Pairing.Failed } }
    }

    private fun pairOver(wire: Wire, guest: Boolean) {
        busyPairing = true
        pairJob?.cancel()
        val result = runCatching {
            br.gabriel.cochilo.sync.Pairing.run(wire, myName, guest) { number, partner ->
                val f = CompletableFuture<Boolean>()
                answer = f
                _pairing.value = Pairing.Compare(number, partner)
                f.get(PAIR_ANSWER_MINUTES, TimeUnit.MINUTES).also { yes -> if (yes) _pairing.value = Pairing.Compare(number, partner, waitingForPartner = true) }
            }
        }.getOrNull()
        busyPairing = false
        offering?.close(); offering = null
        if (result == null) { _pairing.value = Pairing.Failed; return }
        sp.edit().putString(KEY, Base64.encodeToString(result.key, Base64.NO_WRAP)).putString(PARTNER, result.partnerName).apply()
        _state.value = State(result.partnerName, null)
        _pairing.value = Pairing.Done(result.partnerName)
        scope.launch { syncNow() }
    }

    /** "Same number" (true) or "Not the same" (false). */
    fun answer(same: Boolean) { answer?.complete(same) }

    fun cancelPairing() {
        answer?.complete(false); answer = null
        pairJob?.cancel(); pairJob = null
        offering?.close(); offering = null
        if (_pairing.value !is Pairing.Done) _pairing.value = Pairing.Idle
    }

    fun pairingSeen() { _pairing.value = Pairing.Idle }

    /** Settings › Unpair: forget the key. Both phones keep the log they have. */
    fun unpair() {
        stop()
        sp.edit().clear().apply()
        lastPeer = null
        _state.value = State(null, null)
    }

    companion object {
        private const val KEY = "key"
        private const val PARTNER = "partner"
        private const val LAST = "last"
        /** A change goes across this long after the last tap, so a burst of taps is one sync. */
        const val CHANGE_DEBOUNCE_MS = 3_000L
        /** How long to look for the partner on the Wi-Fi before saying it isn't there. */
        const val FIND_MS = 8_000L
        const val PAIR_ANSWER_MINUTES = 3L
    }
}
