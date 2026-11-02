package br.gabriel.cochilo.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Baby
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.domain.Changes
import br.gabriel.cochilo.domain.Milk
import br.gabriel.cochilo.domain.Places
import br.gabriel.cochilo.domain.Stats
import br.gabriel.cochilo.domain.ageWeeks
import br.gabriel.cochilo.domain.isNightSleep
import br.gabriel.cochilo.domain.localDate
import br.gabriel.cochilo.domain.startOfDay
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.screens.changeText
import br.gabriel.cochilo.ui.theme.CTokens
import java.io.OutputStream
import java.time.ZoneId
import java.util.Locale

/**
 * The printable report (docs/report.md): one A4 page the parent makes on the phone and shares with
 * the paediatrician or consultant: the last 7 days' averages, what changed, where naps happened,
 * and the last 14 days as bars. From the parent's own log; it says it is not a medical record.
 * [build] reads the log; [draw] paints the page (the screenshot test paints the same page).
 */
object Report {
    const val PAGE_W = 595
    const val PAGE_H = 842
    private const val MARGIN = 48f
    private const val DAYS = 14

    data class Bar(val from: Float, val to: Float, val color: Int, val band: Band)
    enum class Band { FULL, TOP, BOTTOM }

    data class Model(
        val title: String,
        val subtitle: String,
        val brand: String,
        val averagesCaption: String,
        val averages: List<Pair<String, String>>,
        val changesCaption: String,
        val changes: List<Pair<String, String>>,
        val placesCaption: String,
        val places: List<Pair<String, String>>,
        val rhythmCaption: String,
        val days: List<Pair<String, List<Bar>>>,
        val legend: List<Pair<String, Int>>,
        val footer: String,
    )

    fun build(context: Context, baby: Baby?, log: List<Event>, now: Long, zone: ZoneId): Model {
        val f = Format(context, zone, now)
        val p = CTokens.Light
        val today = localDate(now, zone)
        val stats = Stats(zone)
        val avg = stats.averages(stats.days(log, today.minusDays(1), 7, now))
        val name = baby?.name?.ifBlank { null } ?: context.getString(R.string.app_name)
        val weeks = baby?.let { ageWeeks(it.birthDate, it.dueDate, now) }
        val first = today.minusDays((DAYS - 1).toLong())
        fun s(id: Int, vararg a: Any) = if (a.isEmpty()) context.getString(id) else context.getString(id, *a)

        val averages = if (avg == null) emptyList() else buildList {
            add(s(R.string.total_sleep) to f.durationMin(avg.totalSleepMin))
            add(s(R.string.night_sleep) to f.durationMin(avg.nightSleepMin))
            add(s(R.string.naps) to s(R.string.naps_value, avg.naps, f.durationMin(avg.napSleepMin)))
            add(s(R.string.longest_stretch) to f.durationMin(avg.longestStretchMin))
            add(s(R.string.night_wakings) to String.format(Locale.getDefault(), "%.1f", avg.nightWakings))
            add(s(R.string.feeds) to s(R.string.per_day, avg.feeds))
            add(s(R.string.diapers) to s(R.string.per_day, avg.diapers))
            add(s(R.string.daylight) to f.durationMin(avg.daylightMin))
            val (day, night) = Milk.pumped(log, zone, now)
            if (day + night > 0) add(s(R.string.pumped_week) to s(R.string.pumped_split, day, night))
        }

        val days = (0 until DAYS).map { i ->
            val d = first.plusDays(i.toLong())
            val from = startOfDay(d, zone)
            val to = startOfDay(d.plusDays(1), zone)
            val span = (to - from).toFloat()
            fun frac(t: Long) = ((t.coerceIn(from, to) - from) / span)
            val bars = log.filter { it.start < to && (it.end ?: now) > from }.mapNotNull { e ->
                val end = if (e.type.hasDuration) (e.end ?: now) else e.start + 15 * 60_000L
                val (color, band) = when (e.type) {
                    EventType.SLEEP -> (if (e.isNightSleep(zone)) p.eventNight else p.eventNap) to Band.FULL
                    EventType.FEED -> p.eventFeed to Band.TOP
                    EventType.PUMP -> p.eventPump to Band.TOP
                    EventType.DIAPER -> p.eventDiaper to Band.BOTTOM
                    EventType.DAYLIGHT -> p.eventDaylight to Band.BOTTOM
                    else -> return@mapNotNull null
                }
                Bar(frac(e.start), frac(end), color.toArgb(), band)
            }
            f.dayAbsolute(d) to bars
        }

        return Model(
            title = s(R.string.report_title, name),
            subtitle = listOfNotNull(weeks?.let { s(R.string.status_weeks, it) }, s(R.string.report_period, f.dayAbsolute(first), f.dayAbsolute(today))).joinToString(" · "),
            brand = s(R.string.brand_line),
            averagesCaption = s(R.string.caption_week),
            averages = averages,
            changesCaption = s(R.string.caption_changes),
            changes = Changes.detect(log, today, now, stats).map { changeText(context, it, f) },
            placesCaption = s(R.string.caption_places),
            places = Places.napsByPlace(log, zone, now).map { (k, n) -> (f.kindName(k) ?: k) to s(R.string.naps_count, n) },
            rhythmCaption = s(R.string.report_rhythm, DAYS),
            days = days,
            legend = listOf(
                s(R.string.sleep_night) to p.eventNight.toArgb(), s(R.string.sleep_nap) to p.eventNap.toArgb(),
                s(R.string.type_feed) to p.eventFeed.toArgb(), s(R.string.type_pump) to p.eventPump.toArgb(),
                s(R.string.type_diaper) to p.eventDiaper.toArgb(), s(R.string.type_daylight) to p.eventDaylight.toArgb(),
            ),
            footer = s(R.string.report_footer, f.dayAbsolute(today) + " " + f.time(now)),
        )
    }

    /** Writes the model as a one-page PDF. */
    fun write(context: Context, m: Model, out: OutputStream) {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        draw(page.canvas, m, context)
        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }

    /** Paints the page in PDF points (A4 portrait), in the design system's light theme. */
    fun draw(canvas: Canvas, m: Model, context: Context) {
        val p = CTokens.Light
        fun font(id: Int) = runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: Typeface.DEFAULT
        val regular = font(R.font.libre_franklin_regular)
        val semibold = font(R.font.libre_franklin_semibold)
        val bold = font(R.font.libre_franklin_bold)
        fun paint(face: Typeface, size: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = face; textSize = size; this.color = color }
        val ink = p.ink.toArgb()
        val muted = p.muted.toArgb()
        val line = p.line.toArgb()

        canvas.drawColor(p.surface.toArgb())
        var y = MARGIN + 20f
        canvas.drawText(m.title, MARGIN, y, paint(bold, 22f, ink))
        val brandPaint = paint(regular, 9f, muted)
        canvas.drawText(m.brand, PAGE_W - MARGIN - brandPaint.measureText(m.brand), MARGIN + 6f, brandPaint)
        y += 18f
        canvas.drawText(m.subtitle, MARGIN, y, paint(regular, 11f, muted))
        y += 26f

        fun caption(text: String) {
            canvas.drawText(text, MARGIN, y, paint(semibold, 11f, muted))
            y += 8f
        }
        fun rows(items: List<Pair<String, String>>) {
            val label = paint(regular, 11f, ink)
            val value = paint(semibold, 11f, ink)
            for ((l, v) in items) {
                y += 16f
                canvas.drawText(l, MARGIN, y, label)
                canvas.drawText(v, PAGE_W - MARGIN - value.measureText(v), y, value)
                y += 5f
                canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, Paint().apply { color = line; strokeWidth = 0.5f })
            }
            y += 20f
        }

        if (m.averages.isNotEmpty()) { caption(m.averagesCaption); rows(m.averages) }
        if (m.changes.isNotEmpty()) { caption(m.changesCaption); rows(m.changes) }
        if (m.places.isNotEmpty()) { caption(m.placesCaption); rows(m.places) }

        // The rhythm: one bar per day, midnight to midnight.
        caption(m.rhythmCaption)
        y += 8f
        val labelW = 70f
        val left = MARGIN + labelW
        val width = PAGE_W - MARGIN - left
        val rowH = 11f
        val dayPaint = paint(regular, 8.5f, muted)
        val track = Paint().apply { color = line }
        for ((label, bars) in m.days) {
            canvas.drawText(label, MARGIN, y + rowH - 2f, dayPaint)
            canvas.drawRoundRect(RectF(left, y, left + width, y + rowH), 2f, 2f, track)
            for (b in bars.sortedBy { it.band != Band.FULL }) {
                val (top, bottom) = when (b.band) {
                    Band.FULL -> y to y + rowH
                    Band.TOP -> y to y + rowH / 3f
                    Band.BOTTOM -> y + rowH * 2f / 3f to y + rowH
                }
                canvas.drawRect(left + width * b.from, top, left + width * maxOf(b.to, b.from + 0.004f), bottom, Paint().apply { color = b.color })
            }
            y += rowH + 3f
        }
        val hourPaint = paint(regular, 8f, muted)
        for (h in listOf(0, 6, 12, 18, 24)) {
            val t = "%02d".format(h)
            val x = left + width * h / 24f - hourPaint.measureText(t) / 2f
            canvas.drawText(t, x, y + 9f, hourPaint)
        }
        y += 24f
        var x = MARGIN
        val legendPaint = paint(regular, 9f, ink)
        for ((name, color) in m.legend) {
            canvas.drawRoundRect(RectF(x, y - 7f, x + 8f, y + 1f), 2f, 2f, Paint().apply { this.color = color })
            canvas.drawText(name, x + 12f, y, legendPaint)
            x += 12f + legendPaint.measureText(name) + 14f
        }

        canvas.drawText(m.footer, MARGIN, PAGE_H - MARGIN, paint(regular, 8.5f, muted))
    }
}
