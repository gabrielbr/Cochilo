package br.gabriel.cochilo

import android.app.Application
import br.gabriel.cochilo.data.CochiloDb
import br.gabriel.cochilo.data.Prefs
import br.gabriel.cochilo.notify.LiveNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class CochiloApp : Application() {
    val db: CochiloDb by lazy { CochiloDb.create(this) }
    val prefs: Prefs by lazy { Prefs(this) }
    /** Partner sync on the home Wi-Fi (0.47.0, docs/sync.md); the play build has no link. */
    val sync: br.gabriel.cochilo.sync.PartnerSync by lazy {
        br.gabriel.cochilo.sync.PartnerSync(this, db, br.gabriel.cochilo.sync.SyncLinks.create(this)).also {
            it.onUnpaired = { br.gabriel.cochilo.sync.SyncBackground.update(this) }
        }
    }
    /** Ask Cochilo's saved chats (0.40.0): on the phone only, not in backups. */
    val chats: br.gabriel.cochilo.data.ChatStore by lazy { br.gabriel.cochilo.data.ChatStore(java.io.File(filesDir, "chats.json")) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        // The live notification and the widget follow the log: any entry saved, edited or deleted (in the app,
        // from an import, or from the notification's own buttons) updates it.
        scope.launch {
            combine(db.events().observeAll(), db.baby().observe(), prefs.liveNotification, prefs.reminders, prefs.reminderLeads) { _, _, _, _, _ -> }
                .debounce(300)
                .collect {
                    // Each step on its own (0.57.1): this loop lives as long as the process (days,
                    // with sync kept on when closed), and one failure used to end it for good,
                    // leaving the notification frozen on an old "Asleep since…" while the app
                    // itself was right.
                    step { LiveNotifications.refresh(this@CochiloApp) }
                    // The home-screen widget follows the log the same way (docs/widget.md).
                    step { br.gabriel.cochilo.widget.CochiloWidget.refresh(this@CochiloApp) }
                    step { br.gabriel.cochilo.tile.SleepTile.refresh(this@CochiloApp) }
                    // And the watch's tile, on a watch connected directly (0.53.0, docs/wear.md).
                    // Bounded, so a watch link that never answers can't hold the loop.
                    step { kotlinx.coroutines.withTimeoutOrNull(15_000) { br.gabriel.cochilo.wear.WearBridge.push(this@CochiloApp) } }
                }
        }
    }

    /** Runs one update; a failure is logged and the next update still runs. */
    private suspend fun step(block: suspend () -> Unit) {
        try { block() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
            android.util.Log.w("Cochilo", "update failed", e)
        }
    }

    /** After the person answers the notification permission, and each time Cochilo comes on screen. */
    fun refreshNotifications() {
        scope.launch { LiveNotifications.refresh(this@CochiloApp) }
    }
}
