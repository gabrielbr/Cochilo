package br.gabriel.cochilo.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import br.gabriel.cochilo.CochiloApp
import br.gabriel.cochilo.notify.LiveNotifications

/**
 * Keep in sync with Cochilo closed (0.52.0, docs/sync.md). A foreground service that keeps partner
 * sync answering on the home Wi-Fi. It wears the live notification Cochilo already shows (the same
 * id), so it adds no notification of its own. Off until the parent turns it on in Settings › Sync.
 */
object SyncBackground {
    @Volatile var running = false
        internal set

    /** Starts or stops the service to match the switch. Call it while Cochilo is on screen. */
    fun update(context: Context) {
        val app = context.applicationContext as CochiloApp
        val want = app.sync.wantsBackground
        val intent = Intent(context, SyncService::class.java)
        if (want && !running) runCatching { ContextCompat.startForegroundService(context, intent) }
        if (!want && running) context.stopService(intent)
    }
}

class SyncService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as CochiloApp
        LiveNotifications.createChannel(this)
        // The live notification's id, shown at once with a short line; the next refresh puts the
        // live content back in it.
        val n = LiveNotifications.keepNotification(this, app.sync.state.value.partner)
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        runCatching { ServiceCompat.startForeground(this, LiveNotifications.ID_STRETCH, n, type) }
            .onFailure { stopSelf(); return START_NOT_STICKY }
        SyncBackground.running = true
        app.sync.start()
        app.refreshNotifications()
        return START_STICKY
    }

    override fun onDestroy() {
        SyncBackground.running = false
        (application as CochiloApp).sync.stopUnlessOnScreen()
        super.onDestroy()
    }
}
