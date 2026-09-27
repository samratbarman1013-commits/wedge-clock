package com.samrat.wedgeclock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/**
 * Wedge Clock: a big slanted LED-style clock where every digit segment is a
 * pointed wedge, seconds are shown as a 60-wedge progress bar, and the date
 * sits underneath. Unlit segments stay faintly visible, like a real LED panel.
 */
class WedgeClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val onPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ON_COLOR }
    private val offPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = OFF_COLOR }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT_COLOR
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        letterSpacing = 0.08f
    }
    private val path = Path()

    private val is24h = DateFormat.is24HourFormat(context)
    private val dateFmt = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    private val ampmFmt = SimpleDateFormat("a", Locale.getDefault())

    private var glowScale = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            invalidate()
            val now = System.currentTimeMillis()
            handler.postAtTime(this, SystemClock.uptimeMillis() + (250 - now % 250))
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(ticker)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(ticker)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        var hour = cal.get(if (is24h) Calendar.HOUR_OF_DAY else Calendar.HOUR)
        if (!is24h && hour == 0) hour = 12
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        val frac = (now % 1000L) / 1000f

        val timeUnits = 4f + 2f * COLON_W + 5f * GAP
        val s = min(w * 0.94f / (timeUnits + LEAN), h / 4.3f)
        if (s < 1f) return

        if (s != glowScale) {
            onPaint.setShadowLayer(s * 0.14f, 0f, 0f, GLOW_COLOR)
            glowScale = s
        }

        val timeW = timeUnits * s
        val dateSize = 0.52f * s
        val barH = 0.36f * s
        val stackH = 2f * s + 0.45f * s + barH + 0.5f * s + dateSize * 1.15f
        val top = ((h - stackH) / 2f).coerceAtLeast(h * 0.02f)
        val timeLeft = (w - timeW) / 2f

        // ---- time: HH:MM in wedge digits ----
        var x = timeLeft
        val hs = String.format(Locale.US, "%02d", hour)
        val ms = String.format(Locale.US, "%02d", minute)
        drawDigit(canvas, hs[0] - '0', x, top, s); x += (1f + GAP) * s
        drawDigit(canvas, hs[1] - '0', x, top, s); x += (1f + GAP) * s
        drawColon(canvas, x, top, s, (now % 1000L) < 550); x += (COLON_W + GAP) * s
        drawDigit(canvas, ms[0] - '0', x, top, s); x += (1f + GAP) * s
        drawDigit(canvas, ms[1] - '0', x, top, s)

        // ---- seconds: 60-wedge progress bar ----
        val barY = top + 2f * s + 0.45f * s
        drawSecondsBar(canvas, timeLeft, barY, timeW, s, second, frac)

        // ---- date (and AM/PM on 12-hour devices) ----
        textPaint.textSize = dateSize
        val dateText = if (is24h) {
            dateFmt.format(now)
        } else {
            dateFmt.format(now) + "  \u00B7  " + ampmFmt.format(now)
        }
        val baseline = barY + barH + 0.5f * s + dateSize * 0.9f
        canvas.drawText(dateText, w / 2f, baseline, textPaint)
    }

    private fun drawDigit(c: Canvas, d: Int, x: Float, y: Float, s: Float) {
        c.save()
        c.translate(x, y)
        c.skew(SKEW, 0f)
        val t = THICK * s
        val half = t / 2f
        val x1 = half * 1.15f
        val x2 = s - half * 1.15f
        val hTop = half
        val hMid = s
        val hBot = 2f * s - half
        val v1t = half * 1.15f
        val v2t = s - half * 1.15f
        val v1b = s + half * 1.15f
        val v2b = 2f * s - half * 1.15f
        val on = DIGIT_SEGS[d]
        for (seg in "ABCDEFG") {
            val p = if (on.indexOf(seg) >= 0) onPaint else offPaint
            when (seg) {
                'A' -> { hSeg(x1, x2, hTop, t); c.drawPath(path, p) }
                'G' -> { hSeg(x1, x2, hMid, t); c.drawPath(path, p) }
                'D' -> { hSeg(x1, x2, hBot, t); c.drawPath(path, p) }
                'F' -> { vSeg(half, v1t, v2t, t); c.drawPath(path, p) }
                'B' -> { vSeg(s - half, v1t, v2t, t); c.drawPath(path, p) }
                'E' -> { vSeg(half, v1b, v2b, t); c.drawPath(path, p) }
                'C' -> { vSeg(s - half, v1b, v2b, t); c.drawPath(path, p) }
            }
        }
        c.restore()
    }

    private fun hSeg(x1: Float, x2: Float, yc: Float, t: Float) {
        val e = t * 0.30f
        path.reset()
        path.moveTo(x1, yc)
        path.lineTo(x1 + e, yc - t / 2f)
        path.lineTo(x2 - e, yc - t / 2f)
        path.lineTo(x2, yc)
        path.lineTo(x2 - e, yc + t / 2f)
        path.lineTo(x1 + e, yc + t / 2f)
        path.close()
    }

    private fun vSeg(xc: Float, y1: Float, y2: Float, t: Float) {
        val e = t * 0.30f
        path.reset()
        path.moveTo(xc, y1)
        path.lineTo(xc + t / 2f, y1 + e)
        path.lineTo(xc + t / 2f, y2 - e)
        path.lineTo(xc, y2)
        path.lineTo(xc - t / 2f, y2 - e)
        path.lineTo(xc - t / 2f, y1 + e)
        path.close()
    }

    private fun drawColon(c: Canvas, x: Float, y: Float, s: Float, on: Boolean) {
        c.save()
        c.translate(x, y)
        c.skew(SKEW, 0f)
        val r = THICK * s * 0.85f
        val cx = COLON_W * s / 2f
        val p = if (on) onPaint else offPaint
        diamond(cx, 0.66f * s, r)
        c.drawPath(path, p)
        diamond(cx, 1.34f * s, r)
        c.drawPath(path, p)
        c.restore()
    }

    private fun diamond(cx: Float, cy: Float, r: Float) {
        path.reset()
        path.moveTo(cx, cy - r)
        path.lineTo(cx + r, cy)
        path.lineTo(cx, cy + r)
        path.lineTo(cx - r, cy)
        path.close()
    }

    private fun drawSecondsBar(
        c: Canvas, x: Float, y: Float, barW: Float, s: Float, second: Int, frac: Float
    ) {
        c.save()
        c.translate(x, y)
        c.skew(SKEW, 0f)
        val tw = barW / 60f
        val gap = (tw * 0.28f).coerceAtLeast(1f)
        val progress = second + frac
        for (i in 0 until 60) {
            val fill = (progress - i).coerceIn(0f, 1f)
            val tickH = if (i % 5 == 0) 0.36f * s else 0.26f * s
            val tx = i * tw
            val wTick = tw - gap
            barRect(tx, 0f, wTick, tickH)
            c.drawPath(path, offPaint)
            if (fill > 0f) {
                barRect(tx, 0f, wTick * fill, tickH)
                c.drawPath(path, onPaint)
            }
        }
        c.restore()
    }

    private fun barRect(x: Float, y: Float, w: Float, h: Float) {
        path.reset()
        path.moveTo(x, y)
        path.lineTo(x + w, y)
        path.lineTo(x + w, y + h)
        path.lineTo(x, y + h)
        path.close()
    }

    companion object {
        private const val COLON_W = 0.40f   // colon cell width in digit widths
        private const val GAP = 0.15f       // spacing between digits
        private const val THICK = 0.24f      // segment thickness in digit widths
        private const val SKEW = -0.10f      // italic lean of every wedge
        private const val LEAN = 0.22f       // horizontal overhang of the lean
        private val DIGIT_SEGS = arrayOf(
            "ABCDEF", "BC", "ABGED", "ABGCD", "FGBC",
            "AFGCD", "AFGEDC", "ABC", "ABCDEFG", "ABCDFG"
        )
        private val ON_COLOR = 0xFFFFB300.toInt()
        private val OFF_COLOR = 0x26FFB300
        private val TEXT_COLOR = 0xFFD9DEE7.toInt()
        private val GLOW_COLOR = 0x59FFB300
    }
}
