package br.gabriel.cochilo.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.coroutines.resume

/**
 * The family build's link (docs/sync.md): Android's network service discovery (mDNS, the way a
 * phone finds a printer) on the home Wi-Fi, and a socket between the two phones. It needs only the
 * INTERNET permission this build already has; nothing leaves the local network, and what crosses
 * it is sealed (SyncSession). Nothing here touches the baby's data: it only moves bytes.
 */
object SyncLinks {
    fun create(context: Context): SyncLink = NsdLink(context.applicationContext)
}

private const val TYPE = "_cochilo._tcp."
private const val CONNECT_MS = 5_000
/** Pairing waits for two parents to compare numbers; a sync is quick. */
private const val PAIR_TIMEOUT_MS = 180_000
private const val SYNC_TIMEOUT_MS = 30_000

private class NsdLink(private val context: Context) : SyncLink {
    override val available = true
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    /** This run's token: a phone never lists itself. */
    private val token = UUID.randomUUID().toString().take(8)

    override fun offer(mode: SyncLink.Mode, name: String, pairId: String?, onConnection: (Wire) -> Unit): java.io.Closeable {
        val server = ServerSocket(0)
        val timeout = if (mode == SyncLink.Mode.PAIR) PAIR_TIMEOUT_MS else SYNC_TIMEOUT_MS
        thread(name = "cochilo-sync-accept", isDaemon = true) {
            while (!server.isClosed) {
                val s = runCatching { server.accept() }.getOrNull() ?: break
                thread(name = "cochilo-sync-conn", isDaemon = true) {
                    s.use { socket ->
                        socket.soTimeout = timeout
                        val wire = Wire(socket.getInputStream(), socket.getOutputStream())
                        runCatching { onConnection(wire) }
                        wire.close()
                    }
                }
            }
        }
        val info = NsdServiceInfo().apply {
            serviceName = "Cochilo-$token-${mode.name.lowercase()}"
            serviceType = TYPE
            port = server.localPort
            setAttribute("m", mode.name)
            setAttribute("n", name.take(MAX_NAME))
            setAttribute("t", token)
            if (pairId != null) setAttribute("id", pairId)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(i: NsdServiceInfo) {}
            override fun onRegistrationFailed(i: NsdServiceInfo, code: Int) {}
            override fun onServiceUnregistered(i: NsdServiceInfo) {}
            override fun onUnregistrationFailed(i: NsdServiceInfo, code: Int) {}
        }
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        return java.io.Closeable {
            runCatching { nsd.unregisterService(listener) }
            runCatching { server.close() }
        }
    }

    override fun browse(mode: SyncLink.Mode, pairId: String?): Flow<List<Peer>> = callbackFlow {
        val found = LinkedHashMap<String, Peer>()
        val toResolve = Channel<NsdServiceInfo>(Channel.UNLIMITED)
        // One resolve at a time: older Android refuses a second while one runs.
        val resolver = CoroutineScope(Dispatchers.IO).launch {
            for (info in toResolve) {
                val r = resolve(info) ?: continue
                val a = r.attributes
                fun attr(k: String) = a[k]?.let { String(it) }
                val host = r.host?.hostAddress ?: continue
                if (attr("m") != mode.name || attr("t") == token) continue
                if (pairId != null && attr("id") != pairId) continue
                found[info.serviceName] = Peer(attr("n") ?: "", host, r.port, attr("t") ?: "")
                trySend(found.values.toList())
            }
        }
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) {}
            override fun onDiscoveryStopped(type: String) {}
            override fun onStartDiscoveryFailed(type: String, code: Int) { close() }
            override fun onStopDiscoveryFailed(type: String, code: Int) {}
            override fun onServiceFound(info: NsdServiceInfo) {
                if (info.serviceName.startsWith("Cochilo-") && !info.serviceName.contains(token)) toResolve.trySend(info)
            }
            override fun onServiceLost(info: NsdServiceInfo) {
                if (found.remove(info.serviceName) != null) trySend(found.values.toList())
            }
        }
        trySend(emptyList())
        nsd.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose {
            runCatching { nsd.stopServiceDiscovery(listener) }
            toResolve.close()
            resolver.cancel()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun resolve(info: NsdServiceInfo): NsdServiceInfo? = suspendCancellableCoroutine { c ->
        runCatching {
            nsd.resolveService(info, object : NsdManager.ResolveListener {
                override fun onResolveFailed(i: NsdServiceInfo, code: Int) { if (c.isActive) c.resume(null) }
                override fun onServiceResolved(i: NsdServiceInfo) { if (c.isActive) c.resume(i) }
            })
        }.onFailure { if (c.isActive) c.resume(null) }
    }

    override fun connect(peer: Peer): Wire {
        val s = Socket()
        s.connect(InetSocketAddress(peer.host, peer.port), CONNECT_MS)
        s.soTimeout = PAIR_TIMEOUT_MS
        return SocketWire(s)
    }
}

/** A Wire that closes its socket when closed. */
private fun SocketWire(s: Socket): Wire = object : Wire(s.getInputStream(), s.getOutputStream()) {
    override fun close() { super.close(); runCatching { s.close() } }
}

private const val MAX_NAME = 40
