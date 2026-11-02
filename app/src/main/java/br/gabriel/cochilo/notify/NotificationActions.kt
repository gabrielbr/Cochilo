package br.gabriel.cochilo.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.gabriel.cochilo.CochiloApp
import br.gabriel.cochilo.data.Entries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The notification's buttons, and its tick. Not exported: only the app's own PendingIntents reach it.
 * Each button does what the same button on Today does (Entries), then the notifications update.
 */
class NotificationActions : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as CochiloApp
        val entries = Entries(app.db.events())
        val id = intent.getStringExtra(EXTRA_ID)
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    START_SLEEP -> entries.startSleep()
                    WOKE_UP -> entries.wakeUp()
                    FINISH -> id?.let { entries.finish(it) }
                    SWITCH -> id?.let { entries.switchSide(it) }
                    TICK -> Unit
                }
                LiveNotifications.refresh(app)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val START_SLEEP = "br.gabriel.cochilo.START_SLEEP"
        const val WOKE_UP = "br.gabriel.cochilo.WOKE_UP"
        const val FINISH = "br.gabriel.cochilo.FINISH"
        const val SWITCH = "br.gabriel.cochilo.SWITCH"
        const val TICK = "br.gabriel.cochilo.TICK"
        const val EXTRA_ID = "id"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
