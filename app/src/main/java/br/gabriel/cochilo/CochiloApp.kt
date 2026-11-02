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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        // The live notification and the widget follow the log: any entry saved, edited or deleted (in the app,
        // from an import, or from the notification's own buttons) updates it.
        scope.launch {
            combine(db.events().observeAll(), db.baby().observe(), prefs.liveNotification) { _, _, _ -> }
                .debounce(300)
                .collect {
                    LiveNotifications.refresh(this@CochiloApp)
                    // The home-screen widget follows the log the same way (docs/widget.md).
                    br.gabriel.cochilo.widget.CochiloWidget.refresh(this@CochiloApp)
                }
        }
    }

    /** After the person answers the notification permission. */
    fun refreshNotifications() {
        scope.launch { LiveNotifications.refresh(this@CochiloApp) }
    }
}
