package br.gabriel.cochilo.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.gabriel.cochilo.CochiloApp
import br.gabriel.cochilo.MainActivity
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.Reminders
import br.gabriel.cochilo.domain.ageWeeks
import br.gabriel.cochilo.ui.Format
import java.time.ZoneId

/**
 * Window reminders (docs/notifications.md): 30 and 15 minutes before the next sleep window opens,
 * a heads-up to start the routine, with one button, Start routine. The timing rules are
 * `domain/Reminders.kt`; this posts them.
 *
 * Its own channel (default importance, so it can be heard; the person can change that), apart
 * from the quiet live notification. The alarm is inexact-but-allowed-while-idle: no special
 * permission. When it fires, the prediction is read again, so a reminder never speaks for a
 * window that has moved or a baby already asleep.
 */
object SleepReminders {
    const val CHANNEL = "reminders"
    const val ID = 2

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ch = NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.reminder_channel_meta)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private data class State(val prediction: Predictor.Result, val due: Reminders.Due?)

    /** The prediction and the next reminder, or null when reminders are off or can't post. */
    private suspend fun state(context: Context, now: Long): State? {
        val app = context.applicationContext as CochiloApp
        val baby = app.db.baby().get() ?: return null
        if (!app.prefs.reminders.value || !LiveNotifications.canPost(context)) return null
        val zone = ZoneId.systemDefault()
        val p = Predictor(zone).predict(app.db.events().all(), ageWeeks(baby.birthDate, baby.dueDate, now), now)
        return State(p, Reminders.next(p, now, app.prefs.reminderLeads.value, zone, app.prefs.remindersSent))
    }

    /** Keeps the alarm on the next reminder (or none), and takes a reminder away once the baby sleeps. */
    suspend fun refresh(context: Context) {
        val now = System.currentTimeMillis()
        val s = state(context, now)
        if (s == null || s.prediction !is Predictor.Result.Awake) NotificationManagerCompat.from(context).cancel(ID)
        val due = s?.due
        if (due == null) cancelAlarm(context) else schedule(context, maxOf(due.at, now + Reminders.EARLY))
    }

    /** The alarm went off: send the reminder if, read again now, it's still due. */
    @android.annotation.SuppressLint("MissingPermission") // canPost() in state()
    suspend fun fire(context: Context) {
        val app = context.applicationContext as CochiloApp
        val now = System.currentTimeMillis()
        val due = state(context, now)?.due ?: return
        if (!Reminders.isDue(due, now)) return
        createChannel(context)
        val f = Format(context, ZoneId.systemDefault(), now)
        val bedtime = due.next == Predictor.Next.BEDTIME
        val title = context.getString(if (bedtime) R.string.reminder_bed_title else R.string.reminder_nap_title, due.lead)
        val text = context.getString(R.string.reminder_text, f.time(due.windowStart), f.time(due.windowEnd))
        val routine = if (bedtime) Kinds.BEDTIME_ROUTINE else Kinds.NAP_ROUTINE
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.gg_ic_moon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            // Gone by itself once the window has closed.
            .setTimeoutAfter((due.windowEnd - now).coerceAtLeast(Reminders.EARLY))
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 2, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .addAction(0, context.getString(R.string.reminder_start_routine), routineIntent(context, routine))
            // The routine's music, one tap from the heads-up (0.60.0), when a link is saved.
            .apply {
                app.prefs.routineMusic.value.firstOrNull()?.let {
                    addAction(0, context.getString(R.string.music_play_short), br.gabriel.cochilo.music.MusicOpener.pending(context, it))
                }
            }
            .build()
        NotificationManagerCompat.from(context).notify(ID, n)
        app.prefs.remindersSent = (app.prefs.remindersSent + Reminders.Sent(due.lead, due.windowStart)).takeLast(SENT_KEPT)
    }

    private const val SENT_KEPT = 8

    private fun routineIntent(context: Context, kind: String): PendingIntent = PendingIntent.getBroadcast(
        context, 3,
        Intent(context, NotificationActions::class.java).setAction(NotificationActions.START_ROUTINE).putExtra(NotificationActions.EXTRA_ID, kind),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(
        context, 4, Intent(context, NotificationActions::class.java).setAction(NotificationActions.REMIND),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Wakes the phone near [at], even in Doze, without the exact-alarm permission. */
    private fun schedule(context: Context, at: Long) {
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent(context))
    }

    private fun cancelAlarm(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
    }
}
