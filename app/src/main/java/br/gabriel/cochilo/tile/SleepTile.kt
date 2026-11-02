package br.gabriel.cochilo.tile

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import br.gabriel.cochilo.CochiloApp
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Entries
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.notify.LiveNotifications
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.widget.CochiloWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * The quick-settings tile (docs/flows.md): Start sleep / Woke up from the notification shade, one
 * tap, from any screen, without unlocking to the app. Active (lit) while the baby sleeps; the
 * subtitle says since when. It goes through Entries, like every other button.
 */
class SleepTile : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { render() }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val app = applicationContext as CochiloApp
            if (app.db.baby().get() == null) return@launch
            val entries = Entries(app.db.events())
            if (asleep(app)) entries.wakeUp() else entries.startSleep()
            render()
            LiveNotifications.refresh(app)
            CochiloWidget.refresh(app)
        }
    }

    private suspend fun asleep(app: CochiloApp) = app.db.events().all().any { it.type == EventType.SLEEP && it.end == null }

    private suspend fun render() {
        val app = applicationContext as CochiloApp
        val tile = qsTile ?: return
        val sleep = app.db.events().all().filter { it.type == EventType.SLEEP && it.end == null }.maxByOrNull { it.start }
        val f = Format(this, ZoneId.systemDefault())
        tile.icon = Icon.createWithResource(this, R.drawable.gg_ic_moon)
        tile.label = getString(if (sleep != null) R.string.action_woke_up else R.string.action_start_sleep)
        tile.state = when {
            app.db.baby().get() == null -> Tile.STATE_UNAVAILABLE
            sleep != null -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = sleep?.let { getString(R.string.tile_asleep_since, f.time(it.start)) } ?: getString(R.string.app_name)
        }
        tile.updateTile()
    }

    companion object {
        /** Ask the system to refresh the tile (after the log changes anywhere). */
        fun refresh(context: Context) {
            runCatching { requestListeningState(context, ComponentName(context, SleepTile::class.java)) }
        }
    }
}
