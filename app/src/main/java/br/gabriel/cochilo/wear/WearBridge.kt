package br.gabriel.cochilo.wear

import android.content.Context
import br.gabriel.cochilo.CochiloApp
import br.gabriel.cochilo.data.Entries
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.widgetState
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.tasks.await

/**
 * The phone's side of the watch tile (0.53.0, docs/wear.md): it tells a watch connected directly
 * (never through the cloud) whether the baby is asleep, and does what the tile's buttons ask,
 * through [Entries], like every other button. Without a watch, or without Google's wearable
 * services, it does nothing.
 */
object WearBridge {
    suspend fun push(context: Context) {
        val app = context.applicationContext as CochiloApp
        runCatching {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await().filter { it.isNearby }
            if (nodes.isEmpty()) return
            val bytes = WearWire.encode(state(app))
            val messages = Wearable.getMessageClient(context)
            nodes.forEach { messages.sendMessage(it.id, WearWire.STATE, bytes).await() }
        }
    }

    private suspend fun state(app: CochiloApp): WearWire.State {
        val w = widgetState(app.db.events().all(), app.db.baby().get() != null)
        return WearWire.State(w.hasBaby, w.asleep, w.since, w.feeding?.kind, w.lastBreast, w.nightWaking)
    }

    suspend fun act(app: CochiloApp, a: WearWire.Action) {
        if (app.db.baby().get() == null) return
        val entries = Entries(app.db.events())
        when (a) {
            WearWire.Action.START_SLEEP -> entries.startSleep()
            WearWire.Action.WOKE_UP -> entries.wakeUp()
            WearWire.Action.FEED_LEFT -> entries.startBreastfeed(Kinds.BREAST_LEFT)
            WearWire.Action.FEED_RIGHT -> entries.startBreastfeed(Kinds.BREAST_RIGHT)
            WearWire.Action.FINISH_FEED -> app.db.events().all()
                .filter { it.type == EventType.FEED && it.end == null }.maxByOrNull { it.start }
                ?.let { entries.finish(it.id) }
        }
    }
}

/** Messages from the watch. Exported, as Google's wearable services require; it reads only Cochilo's paths. */
class WearListener : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        val app = applicationContext as CochiloApp
        kotlinx.coroutines.runBlocking {
            when (event.path) {
                WearWire.ACTION -> WearWire.action(event.data)?.let { WearBridge.act(app, it) }
                WearWire.ASK -> Unit
                else -> return@runBlocking
            }
            // An action changes the log, which updates everything (CochiloApp); an ask just wants the state.
            WearBridge.push(app)
        }
    }
}
