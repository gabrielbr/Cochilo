package br.gabriel.cochilo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import br.gabriel.cochilo.CochiloApp
import br.gabriel.cochilo.MainActivity
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.WidgetState
import br.gabriel.cochilo.domain.widgetState
import br.gabriel.cochilo.notify.NotificationActions
import br.gabriel.cochilo.ui.Format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * The home-screen widget (docs/widget.md): Start sleep / Woke up and the two breasts, one tap each
 * without opening the app. The breast used last has the Last mark (an outline and a dot in the feed
 * colour); while a breastfeed runs, its side is lit and reads Finish, and the other side switches. The
 * buttons go to [NotificationActions], so they do exactly what the app's buttons do (Entries).
 * It reads only the phone's own log; nothing leaves the phone.
 */
class CochiloWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try { refresh(context) } finally { pending.finish() }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Redraws every Cochilo widget on the home screen from the log. */
        suspend fun refresh(context: Context) {
            val app = context.applicationContext as CochiloApp
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CochiloWidget::class.java))
            if (ids.isEmpty()) return
            val state = widgetState(app.db.events().all(), app.db.baby().get() != null)
            val views = views(context, state, System.currentTimeMillis())
            manager.updateAppWidget(ids, views)
        }

        fun views(context: Context, s: WidgetState, now: Long): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_cochilo).also { bind(context, s, now, RemoteTarget(it)) }

        /**
         * Fills the widget's layout for [s]: what every field says and does. Written once against
         * [WidgetTarget], so the screenshot test fills the same layout the home screen shows.
         */
        fun bind(context: Context, s: WidgetState, now: Long, t: WidgetTarget) {
            val f = Format(context, ZoneId.systemDefault(), now)
            t.click(R.id.status) { openApp(context) }
            if (!s.hasBaby) {
                // Nothing to log against yet: the widget says so and opens the app.
                t.text(R.id.title, context.getString(R.string.app_name))
                t.visible(R.id.clock, false)
                t.visible(R.id.empty, true)
                t.visible(R.id.buttons, false)
                t.click(R.id.root) { openApp(context) }
                return
            }

            // Asleep or awake, since when, and a clock that ticks by itself.
            val since = s.since
            t.text(
                R.id.title,
                when {
                    since == null -> context.getString(R.string.app_name)
                    s.asleep -> context.getString(R.string.widget_asleep, f.time(since))
                    else -> context.getString(R.string.widget_awake, f.time(since))
                },
            )
            t.clock(R.id.clock, since?.let { now - it })

            // The one mint action.
            val sleepLabel = context.getString(when {
                s.asleep -> R.string.action_woke_up
                s.nightWaking -> R.string.action_back_to_sleep
                else -> R.string.action_start_sleep
            })
            t.text(R.id.sleep_label, sleepLabel)
            t.describe(R.id.sleep, sleepLabel)
            t.click(R.id.sleep) { action(context, if (s.asleep) NotificationActions.WOKE_UP else NotificationActions.START_SLEEP) }

            // The two breasts. The one used last has the Last mark. While a breastfeed runs, its side
            // is lit and reads Finish (one tap ends it); the other side switches to it (one tap).
            val feed = ContextCompat.getColor(context, R.color.widget_feed)
            breast(context, t, R.id.left, Kinds.BREAST_LEFT, R.string.slide_left, NotificationActions.FEED_LEFT, s, feed, f)
            breast(context, t, R.id.right, Kinds.BREAST_RIGHT, R.string.slide_right, NotificationActions.FEED_RIGHT, s, feed, f)
        }

        /** One breast's button: start (or switch to) that side; Finish while it runs; the Last mark if it was last. */
        private fun breast(
            context: Context, t: WidgetTarget, id: Int, kind: String, label: Int, act: String,
            s: WidgetState, feed: Int, f: Format,
        ) {
            val name = context.getString(label)
            val side = f.kindName(kind) ?: name
            val running = s.feeding
            if (running != null && running.kind == kind) {
                val finish = context.getString(R.string.action_finish)
                t.text(id, finish)
                t.background(id, R.drawable.widget_btn_feeding)
                t.describe(id, context.getString(R.string.widget_finish_side, side.lowercase(), f.time(running.start)))
                t.click(id) { action(context, NotificationActions.FINISH, running.id) }
                return
            }
            val last = running == null && kind == s.lastBreast
            val text: CharSequence = if (last) SpannableString("$DOT $name").apply {
                setSpan(ForegroundColorSpan(feed), 0, DOT.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else name
            t.text(id, text)
            t.background(id, if (last) R.drawable.widget_btn_last else R.drawable.widget_btn)
            val spoken = when {
                running != null -> context.getString(R.string.slide_switch, side)
                last -> context.getString(R.string.slide_last_spoken, context.getString(R.string.a11y_log, side))
                else -> context.getString(R.string.a11y_log, side)
            }
            t.describe(id, spoken)
            t.click(id) { action(context, act) }
        }

        private fun action(context: Context, act: String, id: String? = null): PendingIntent {
            val i = Intent(context, NotificationActions::class.java).setAction(act)
            if (id != null) i.putExtra(NotificationActions.EXTRA_ID, id)
            // A distinct request code per action (and running entry), so the extras aren't shared.
            val code = (act + (id ?: "")).hashCode()
            return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /** The Last mark's dot, drawn in the feed colour. */
        private const val DOT = "●"
    }
}

/** What [CochiloWidget.bind] sets on the widget's layout: RemoteViews on the home screen, views in tests. */
interface WidgetTarget {
    fun text(id: Int, text: CharSequence)
    fun visible(id: Int, shown: Boolean)
    fun background(id: Int, res: Int)
    fun describe(id: Int, words: String)
    /** The running clock, [elapsed] ms in; null hides it. */
    fun clock(id: Int, elapsed: Long?)
    /** What a tap does; built only on the home screen (a test never taps). */
    fun click(id: Int, intent: () -> PendingIntent)
}

private class RemoteTarget(val v: RemoteViews) : WidgetTarget {
    override fun text(id: Int, text: CharSequence) = v.setTextViewText(id, text)
    override fun visible(id: Int, shown: Boolean) = v.setViewVisibility(id, if (shown) View.VISIBLE else View.GONE)
    override fun background(id: Int, res: Int) = v.setInt(id, "setBackgroundResource", res)
    override fun describe(id: Int, words: String) = v.setContentDescription(id, words)
    override fun clock(id: Int, elapsed: Long?) {
        if (elapsed == null) { visible(id, false); return }
        v.setChronometer(id, SystemClock.elapsedRealtime() - elapsed, null, true)
        visible(id, true)
    }
    override fun click(id: Int, intent: () -> PendingIntent) = v.setOnClickPendingIntent(id, intent())
}
