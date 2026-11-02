package br.gabriel.cochilo.ui

import android.content.Context
import android.graphics.Canvas
import android.view.View
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import br.gabriel.cochilo.data.Baby
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.report.Report
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** The printable report's page (docs/report.md), painted by the same function the PDF uses. */
class ReportSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 0.1)

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 11, 2)
    private val now = today.atTime(15, 0).atZone(zone).toInstant().toEpochMilli()
    private fun at(d: LocalDate, h: Int, m: Int = 0) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    private val log = buildList {
        for (back in 0..21) {
            val d = today.minusDays(back.toLong())
            val night = if (back <= 7) 10.5 else 9.0
            val start = at(d.minusDays(1), 20)
            add(Event(type = EventType.SLEEP, start = start, end = start + (night * 3_600_000).toLong(), kind = Kinds.CRIB))
            if (back > 0) {
                add(Event(type = EventType.SLEEP, start = at(d, 10), end = at(d, 11), kind = if (back % 2 == 0) Kinds.SLING else Kinds.CRIB))
                add(Event(type = EventType.SLEEP, start = at(d, 14), end = at(d, 15), kind = Kinds.ARMS))
                for (h in listOf(7, 11, 15, 19)) add(Event(type = EventType.FEED, start = at(d, h), end = at(d, h, 20), kind = Kinds.BREAST_LEFT))
                add(Event(type = EventType.DIAPER, start = at(d, 8), kind = Kinds.WET))
                add(Event(type = EventType.DAYLIGHT, start = at(d, 12), end = at(d, 12, 40), kind = Kinds.OUTDOORS))
            }
        }
    }

    private class Page(context: Context, private val m: Report.Model) : View(context) {
        override fun onDraw(canvas: Canvas) {
            val k = width / Report.PAGE_W.toFloat()
            canvas.scale(k, k)
            Report.draw(canvas, m, context)
        }
    }

    @Test fun report_page_light() {
        val ctx = paparazzi.context
        val baby = Baby(name = "Dante", birthDate = at(today.minusWeeks(16), 0))
        val m = Report.build(ctx, baby, log, now, zone)
        paparazzi.snapshot(Page(ctx, m))
    }
}
