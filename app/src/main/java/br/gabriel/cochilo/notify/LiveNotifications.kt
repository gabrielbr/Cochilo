package br.gabriel.cochilo.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.gabriel.cochilo.CochiloApp
import br.gabriel.cochilo.MainActivity
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.domain.LiveStretch
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.ageWeeks
import br.gabriel.cochilo.domain.liveStretch
import br.gabriel.cochilo.domain.otherBreast
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.theme.CTokens
import java.time.ZoneId

/**
 * The live notification (docs/notifications.md), in the spirit of Napper's: while there is a log,
 * one quiet, ongoing notification says whether the baby is asleep or awake, since when, and what
 * comes next, with a timeline bar for the stretch (TimelineBar) and the one action that ends it. Each running
 * timer (a feed, pumping, a walk) gets its own, with Finish (and Switch side for a breastfeed).
 *
 * Nothing here talks to the network: it reads the phone's own log and posts to the phone.
 */
object LiveNotifications {
    const val CHANNEL = "live"
    private const val ID_STRETCH = 1
    private const val TIMER_BASE = 100
    /** How often the bar moves while something is live. The header's chronometer ticks by itself. */
    private const val TICK_MS = 5 * 60_000L
    /** The notification's side margins and icon column, so the bar's bitmap is about its real width. */
    private const val BAR_INSET_DP = 96

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ch = NotificationChannel(CHANNEL, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.notif_channel_meta)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Reads the log and brings every live notification up to date (posts, updates or clears). */
    suspend fun refresh(context: Context) {
        // Window reminders have their own setting and channel; they follow the log the same way.
        SleepReminders.refresh(context)
        val app = context.applicationContext as CochiloApp
        val nm = NotificationManagerCompat.from(context)
        val baby = app.db.baby().get()
        if (!app.prefs.liveNotification.value || baby == null || !canPost(context)) {
            clearAll(context)
            return
        }
        createChannel(context)
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val events = app.db.events().all()
        val prediction = Predictor(zone).predict(events, ageWeeks(baby.birthDate, baby.dueDate, now), now)
        val f = Format(context, zone, now)
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val palette = if (dark) CTokens.Dark else CTokens.Light

        val stretch = liveStretch(events, prediction, zone, now)
        val waking = br.gabriel.cochilo.domain.Nights.openNight(events, now)?.takeIf { stretch?.asleep == false }
        if (stretch != null) post(nm) { stretchNotification(context, stretch, prediction, f, palette, zone, now, waking) }
        else nm.cancel(ID_STRETCH)

        val timers = events.filter { it.running && it.type != EventType.SLEEP }
        val keep = timers.map { timerId(it) }.toSet()
        for (e in timers) post(nm) { timerNotification(context, e, f, palette, zone) }
        // Clear timers that have ended.
        nm.activeNotifications.map { it.id }.filter { it >= TIMER_BASE && it !in keep }.forEach { nm.cancel(it) }

        if (stretch != null || timers.isNotEmpty()) scheduleTick(context) else cancelTick(context)
    }

    fun clearAll(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        // Everything live; a window reminder (its own setting) stays until it times out.
        nm.activeNotifications.map { it.id }.filter { it != SleepReminders.ID }.forEach { nm.cancel(it) }
        cancelTick(context)
    }

    @android.annotation.SuppressLint("MissingPermission") // checked in canPost()
    private inline fun post(nm: NotificationManagerCompat, build: () -> Pair<Int, android.app.Notification>) {
        val (id, n) = build()
        nm.notify(id, n)
    }

    private fun timerId(e: Event) = TIMER_BASE + (e.id.hashCode() and 0x7fff)

    private fun stretchNotification(
        context: Context, s: LiveStretch, p: Predictor.Result, f: Format, c: CTokens.Palette, zone: ZoneId, now: Long,
        /** A night waking (docs/nights.md): the night's last sleep before it. */
        waking: Event? = null,
    ): Pair<Int, android.app.Notification> {
        val title: String
        val text: String
        val action: NotificationCompat.Action
        var second: NotificationCompat.Action? = null
        if (waking != null) {
            // Awake in the night: no next-nap guess, two buttons, back to sleep first.
            title = context.getString(R.string.panel_awake_night)
            text = context.getString(R.string.panel_since, f.time(waking.end ?: s.since))
            action = NotificationCompat.Action(0, context.getString(R.string.action_back_to_sleep), actionIntent(context, NotificationActions.START_SLEEP))
            second = NotificationCompat.Action(0, context.getString(R.string.action_up_for_day), actionIntent(context, NotificationActions.UP_FOR_DAY))
        } else if (s.asleep) {
            title = context.getString(if (s.isNight) R.string.sleep_night else R.string.sleep_nap)
            text = if (s.expectedEnd != null) context.getString(R.string.notif_asleep_expected, f.time(s.since), f.time(s.expectedEnd))
            else context.getString(R.string.panel_since, f.time(s.since))
            action = NotificationCompat.Action(0, context.getString(R.string.action_woke_up), actionIntent(context, NotificationActions.WOKE_UP))
        } else {
            val a = p as Predictor.Result.Awake
            title = context.getString(R.string.notif_awake_title)
            // In the hour before bedtime the line adds the wind-down (research §2.3): dim the lights.
            val windDown = a.next == Predictor.Next.BEDTIME && a.at - now in 0..60 * 60_000L
            text = context.getString(
                when {
                    windDown -> R.string.notif_wind_down
                    a.next == Predictor.Next.BEDTIME -> R.string.notif_awake_bedtime
                    else -> R.string.notif_awake_nap
                },
                f.time(s.since), f.time(a.at),
            )
            action = NotificationCompat.Action(0, context.getString(R.string.action_start_sleep), actionIntent(context, NotificationActions.START_SLEEP))
        }
        // Title, line and the timeline bar in a custom view; Android adds the header (with the
        // running chronometer) and the button. Title and text are also set plainly, for watches,
        // the lock screen's public version and screen readers.
        val dm = context.resources.displayMetrics
        val barW = (dm.widthPixels - BAR_INSET_DP * dm.density).toInt()
        val barH = context.resources.getDimensionPixelSize(R.dimen.notif_bar_height)
        val view = RemoteViews(context.packageName, R.layout.notif_live).apply {
            setTextViewText(R.id.title, title)
            setTextViewText(R.id.text, text)
            setImageViewBitmap(R.id.bar, TimelineBar.draw(s, c, zone, now, barW, barH))
        }
        val n = base(context, c)
            .setContentTitle(title)
            .setContentText(text)
            .setWhen(s.since).setUsesChronometer(true).setShowWhen(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(view)
            .setCustomBigContentView(view)
            .addAction(action)
            .apply { second?.let { addAction(it) } }
            .build()
        return ID_STRETCH to n
    }

    private fun timerNotification(context: Context, e: Event, f: Format, c: CTokens.Palette, zone: ZoneId): Pair<Int, android.app.Notification> {
        val label = listOfNotNull(f.typeName(e.type), f.kindName(e.kind)).joinToString(" · ")
        // The status-bar icon is the entry's own (drop, bottle, pump, sun…), so timers tell apart.
        val b = base(context, c, br.gabriel.cochilo.ui.kit.entryIcon(e))
            .setContentTitle(label)
            .setContentText(context.getString(R.string.panel_since, f.time(e.start)))
            .setWhen(e.start).setUsesChronometer(true).setShowWhen(true)
        otherBreast(e.kind)?.takeIf { e.type == EventType.FEED }?.let { other ->
            b.addAction(0, context.getString(R.string.slide_switch, f.kindName(other) ?: other), actionIntent(context, NotificationActions.SWITCH, e.id))
        }
        b.addAction(0, context.getString(R.string.action_finish), actionIntent(context, NotificationActions.FINISH, e.id))
        return timerId(e) to b.build()
    }

    private fun base(context: Context, c: CTokens.Palette, icon: Int = R.drawable.ic_notification) = NotificationCompat.Builder(context, CHANNEL)
        .setSmallIcon(icon)
        .setColor(c.mint.toArgb())
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(
            PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        )

    private fun actionIntent(context: Context, action: String, id: String? = null): PendingIntent {
        val i = Intent(context, NotificationActions::class.java).setAction(action).putExtra(NotificationActions.EXTRA_ID, id)
        return PendingIntent.getBroadcast(context, (action + id).hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun tickIntent(context: Context) = PendingIntent.getBroadcast(
        context, 1, Intent(context, NotificationActions::class.java).setAction(NotificationActions.TICK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** An inexact alarm (no special permission) that moves the bar every few minutes. */
    private fun scheduleTick(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + TICK_MS, tickIntent(context))
    }

    private fun cancelTick(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(tickIntent(context))
    }
}
