package br.gabriel.cochilo.wear

import android.content.Context
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.tasks.await

/**
 * The watch's side (0.53.0, docs/wear.md). It keeps the last state the phone sent, and sends the
 * tile's taps to the phone, which does the logging. The log itself stays on the phone.
 */
object Watch {
    private fun prefs(c: Context) = c.getSharedPreferences("cochilo-watch", Context.MODE_PRIVATE)

    fun state(c: Context): WearWire.State? = prefs(c).getString("state", null)?.let { WearWire.decode(it.toByteArray()) }

    fun save(c: Context, s: WearWire.State) {
        prefs(c).edit().putString("state", String(WearWire.encode(s))).apply()
    }

    /** Sends [path] to the phone, connected directly only. False when no phone is near. */
    suspend fun send(c: Context, path: String, data: ByteArray = ByteArray(0)): Boolean = runCatching {
        val phones = Wearable.getNodeClient(c).connectedNodes.await().filter { it.isNearby }
        phones.forEach { Wearable.getMessageClient(c).sendMessage(it.id, path, data).await() }
        phones.isNotEmpty()
    }.getOrDefault(false)
}

/** The phone's state, as it changes: saved, and the tile redrawn. */
class WatchListener : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WearWire.STATE) return
        val s = WearWire.decode(event.data) ?: return
        Watch.save(this, s)
        TileService.getUpdater(this).requestUpdate(CochiloTile::class.java)
    }
}
