package br.gabriel.cochilo.notify

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import br.gabriel.cochilo.domain.BarPart
import br.gabriel.cochilo.domain.LiveStretch
import br.gabriel.cochilo.domain.fraction
import br.gabriel.cochilo.domain.segments
import br.gabriel.cochilo.ui.kit.eventColor
import br.gabriel.cochilo.ui.theme.CTokens
import java.time.ZoneId

/**
 * Draws the live notification's timeline bar, the same language as the Day tab: the stretch's
 * pieces in their colours (night or nap while asleep; awake in muted and the nap window in the
 * brand accent while awake), solid for the time already lived and faded for what's to come, the
 * entries logged during it as dots in their timeline colours, and a tick at now.
 * A bitmap in a custom view, so it shows the same on every Android version.
 */
object TimelineBar {
    /** Opacity of the part still to come. */
    private const val AHEAD_ALPHA = 0.32f

    fun draw(s: LiveStretch, c: CTokens.Palette, zone: ZoneId, now: Long, widthPx: Int, heightPx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val track = h / 2f // the bar is half the image's height; dots overhang it
        val top = (h - track) / 2f
        val radius = track / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val nowX = s.fraction(now, now) * w

        // Clip to a rounded track so pieces join cleanly.
        val save = canvas.save()
        val clip = android.graphics.Path().apply { addRoundRect(RectF(0f, top, w, top + track), radius, radius, android.graphics.Path.Direction.CW) }
        canvas.clipPath(clip)
        paint.color = c.line.toArgb()
        canvas.drawRect(0f, top, w, top + track, paint)
        for (seg in s.segments(now)) {
            val col = when (seg.part) {
                BarPart.NIGHT -> c.eventNight
                BarPart.NAP -> c.eventNap
                BarPart.AWAKE -> c.muted
                BarPart.WINDOW -> c.mint
            }
            val x0 = seg.from * w
            val x1 = seg.to * w
            // Lived: solid. Ahead: faded.
            if (nowX > x0) { paint.color = col.toArgb(); canvas.drawRect(x0, top, minOf(x1, nowX), top + track, paint) }
            if (x1 > nowX) { paint.color = col.copy(alpha = AHEAD_ALPHA).toArgb(); canvas.drawRect(maxOf(x0, nowX), top, x1, top + track, paint) }
        }
        canvas.restoreToCount(save)

        // Entries during the stretch: dots in their timeline colours, ringed in the page colour.
        val dot = h / 2f * 0.9f
        for (e in s.points) {
            val x = (s.fraction(e.start, now) * w).coerceIn(dot, w - dot)
            paint.color = c.page.toArgb(); canvas.drawCircle(x, h / 2f, dot, paint)
            paint.color = c.eventColor(e, zone).toArgb(); canvas.drawCircle(x, h / 2f, dot * 0.7f, paint)
        }
        // Now: a short ink tick, unless the stretch is over its expected end.
        if (nowX < w - 1f) {
            paint.color = c.ink.toArgb()
            val tick = maxOf(2f, h / 8f)
            canvas.drawRect(nowX - tick / 2f, 0f, nowX + tick / 2f, h, paint)
        }
        return bmp
    }

}
