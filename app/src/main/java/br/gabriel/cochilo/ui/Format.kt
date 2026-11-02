package br.gabriel.cochilo.ui

import android.content.Context
import android.text.format.DateFormat
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Dates and durations the way the system's content rules say (docs/content.md "Dates"). */
class Format(
    private val context: Context,
    private val zone: ZoneId = ZoneId.systemDefault(),
    /** The app's "now", so Today and Yesterday agree with the rest of the screen. */
    private val now: Long = System.currentTimeMillis(),
) {
    private val locale: Locale get() = context.resources.configuration.locales[0]
    private val timeFmt: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale)

    fun time(ms: Long): String = timeFmt.format(Instant.ofEpochMilli(ms).atZone(zone))

    /** "1h 20m", "35m", "0m". */
    fun duration(ms: Long): String {
        val total = (ms / 60_000).coerceAtLeast(0)
        val h = total / 60
        val m = total % 60
        // Under a minute reads "<1m", never "0m": a feed that just began isn't nothing.
        return when {
            h > 0 -> context.getString(R.string.duration_hm, h, m)
            m == 0L -> context.getString(R.string.duration_under_m)
            else -> context.getString(R.string.duration_m, m)
        }
    }

    fun durationMin(min: Int): String = duration(min * 60_000L)

    /** "Today", "Yesterday", "Fri 9 Oct", "Fri 9 Oct 2025". Never ISO. */
    fun day(date: LocalDate, today: LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()): String = when (date) {
        today -> context.getString(R.string.today)
        today.minusDays(1) -> context.getString(R.string.yesterday)
        else -> {
            val pattern = if (date.year == today.year) "EEE d MMM" else "EEE d MMM yyyy"
            DateTimeFormatter.ofPattern(pattern, locale).format(date).replace(".", "")
        }
    }

    /** "Yesterday 15:32" under a week, then "Fri 9 Oct 15:32". */
    fun dayTime(ms: Long): String {
        val d = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        return day(d) + " " + time(ms)
    }

    fun typeName(t: EventType): String = context.getString(
        when (t) {
            EventType.SLEEP -> R.string.type_sleep
            EventType.FEED -> R.string.type_feed
            EventType.PUMP -> R.string.type_pump
            EventType.DIAPER -> R.string.type_diaper
            EventType.DAYLIGHT -> R.string.type_daylight
            EventType.ROUTINE -> R.string.type_routine
            EventType.NOTE -> R.string.type_note
            EventType.MEDICINE -> R.string.type_medicine
        }
    )

    fun kindName(kind: String?): String? = when (kind) {
        Kinds.BREAST_LEFT -> context.getString(R.string.kind_breast_left)
        Kinds.BREAST_RIGHT -> context.getString(R.string.kind_breast_right)
        Kinds.BOTTLE -> context.getString(R.string.kind_bottle)
        Kinds.SOLIDS -> context.getString(R.string.kind_solids)
        Kinds.WET -> context.getString(R.string.kind_wet)
        Kinds.DIRTY -> context.getString(R.string.kind_dirty)
        Kinds.BOTH -> context.getString(R.string.kind_both)
        Kinds.NAP_ROUTINE -> context.getString(R.string.kind_nap_routine)
        Kinds.BEDTIME_ROUTINE -> context.getString(R.string.kind_bedtime_routine)
        Kinds.OUTDOORS -> context.getString(R.string.kind_outdoors)
        Kinds.WINDOW -> context.getString(R.string.kind_window)
        else -> null
    }
}
