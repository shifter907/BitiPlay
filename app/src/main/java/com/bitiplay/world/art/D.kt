package com.bitiplay.world.art

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.bitiplay.world.core.TAU
import kotlin.math.cos
import kotlin.math.sin

/**
 * Allocation-free drawing primitives. Everything in the game is drawn with these -
 * there are no bitmap assets, so art scales cleanly to any screen density.
 *
 * All helpers take centre-or-corner coordinates in the caller's local space; the
 * renderer has already applied the camera transform to the [Canvas].
 */
object D {

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val rf = RectF()

    @PublishedApi
    internal val pathPool = Array(12) { Path() }

    @PublishedApi
    internal var pathDepth = 0

    // ------------------------------------------------------------------ paints

    private fun fillPaint(color: Int): Paint {
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = color
        return paint
    }

    private fun strokePaint(color: Int, w: Float, roundCap: Boolean = true): Paint {
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = w
        paint.strokeCap = if (roundCap) Paint.Cap.ROUND else Paint.Cap.BUTT
        paint.strokeJoin = Paint.Join.ROUND
        return paint
    }

    // ------------------------------------------------------------- rectangles

    /** Rectangle from its top-left corner. */
    fun rect(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int, r: Float = 0f) {
        val p = fillPaint(color)
        if (r > 0f) {
            rf.set(x, y, x + w, y + h)
            c.drawRoundRect(rf, r, r, p)
        } else {
            c.drawRect(x, y, x + w, y + h, p)
        }
    }

    /** Rectangle centred on (cx, cy). */
    fun rectC(c: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, r: Float = 0f) {
        rect(c, cx - w * 0.5f, cy - h * 0.5f, w, h, color, r)
    }

    fun rectStroke(
        c: Canvas, x: Float, y: Float, w: Float, h: Float,
        color: Int, sw: Float, r: Float = 0f
    ) {
        val p = strokePaint(color, sw)
        if (r > 0f) {
            rf.set(x, y, x + w, y + h)
            c.drawRoundRect(rf, r, r, p)
        } else {
            c.drawRect(x, y, x + w, y + h, p)
        }
    }

    fun rectShader(c: Canvas, x: Float, y: Float, w: Float, h: Float, sh: Shader) {
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFFFFFF.toInt()
        paint.shader = sh
        c.drawRect(x, y, x + w, y + h, paint)
        paint.shader = null
    }

    // ----------------------------------------------------------------- rounds

    fun circle(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        c.drawCircle(cx, cy, r, fillPaint(color))
    }

    fun circleStroke(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, sw: Float) {
        c.drawCircle(cx, cy, r, strokePaint(color, sw))
    }

    fun oval(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, color: Int) {
        rf.set(cx - rx, cy - ry, cx + rx, cy + ry)
        c.drawOval(rf, fillPaint(color))
    }

    fun ovalStroke(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, color: Int, sw: Float) {
        rf.set(cx - rx, cy - ry, cx + rx, cy + ry)
        c.drawOval(rf, strokePaint(color, sw))
    }

    /** Soft contact shadow used under characters, props and vehicles. */
    fun shadow(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float = rx * 0.28f, alpha: Int = 44) {
        oval(c, cx, cy, rx, ry, withAlpha(0xFF000000.toInt(), alpha))
    }

    // ------------------------------------------------------------------- arcs

    fun arcFill(
        c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float,
        startDeg: Float, sweepDeg: Float, color: Int, useCenter: Boolean = true
    ) {
        rf.set(cx - rx, cy - ry, cx + rx, cy + ry)
        c.drawArc(rf, startDeg, sweepDeg, useCenter, fillPaint(color))
    }

    fun arcLine(
        c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float,
        startDeg: Float, sweepDeg: Float, color: Int, sw: Float
    ) {
        rf.set(cx - rx, cy - ry, cx + rx, cy + ry)
        c.drawArc(rf, startDeg, sweepDeg, false, strokePaint(color, sw))
    }

    // ------------------------------------------------------------------ lines

    fun line(
        c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float,
        color: Int, w: Float, roundCap: Boolean = true
    ) {
        c.drawLine(x1, y1, x2, y2, strokePaint(color, w, roundCap))
    }

    /** Thick line with round caps - the workhorse for limbs, ropes and poles. */
    fun capsule(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, thick: Float, color: Int) {
        line(c, x1, y1, x2, y2, color, thick, true)
    }

    // --------------------------------------------------------------- polygons

    fun tri(
        c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int
    ) {
        shape(c, color) { p ->
            p.moveTo(x1, y1); p.lineTo(x2, y2); p.lineTo(x3, y3)
        }
    }

    fun quad(
        c: Canvas,
        x1: Float, y1: Float, x2: Float, y2: Float,
        x3: Float, y3: Float, x4: Float, y4: Float,
        color: Int
    ) {
        shape(c, color) { p ->
            p.moveTo(x1, y1); p.lineTo(x2, y2); p.lineTo(x3, y3); p.lineTo(x4, y4)
        }
    }

    fun star(
        c: Canvas, cx: Float, cy: Float, rOut: Float, rIn: Float,
        points: Int, color: Int, rot: Float = 0f
    ) {
        shape(c, color) { p ->
            val step = TAU / (points * 2)
            for (i in 0 until points * 2) {
                val r = if (i % 2 == 0) rOut else rIn
                val a = rot - TAU * 0.25f + i * step
                val px = cx + cos(a) * r
                val py = cy + sin(a) * r
                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
        }
    }

    /** Regular polygon, flat-top when [rot] is 0. */
    fun ngon(c: Canvas, cx: Float, cy: Float, r: Float, n: Int, color: Int, rot: Float = 0f) {
        shape(c, color) { p ->
            for (i in 0 until n) {
                val a = rot - TAU * 0.25f + i * TAU / n
                val px = cx + cos(a) * r
                val py = cy + sin(a) * r
                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
        }
    }

    // ------------------------------------------------------- custom path work

    @PublishedApi
    internal fun acquire(): Path {
        val p = pathPool[if (pathDepth < pathPool.size) pathDepth else pathPool.size - 1]
        pathDepth++
        p.reset()
        return p
    }

    @PublishedApi
    internal fun release() {
        if (pathDepth > 0) pathDepth--
    }

    @PublishedApi
    internal fun drawPathFill(c: Canvas, p: Path, color: Int) {
        c.drawPath(p, fillPaint(color))
    }

    @PublishedApi
    internal fun drawPathStroke(c: Canvas, p: Path, color: Int, w: Float) {
        c.drawPath(p, strokePaint(color, w))
    }

    /** Builds and fills an arbitrary closed shape without allocating. */
    inline fun shape(c: Canvas, color: Int, block: (Path) -> Unit) {
        val p = acquire()
        try {
            block(p)
            p.close()
            drawPathFill(c, p, color)
        } finally {
            release()
        }
    }

    /** Builds and strokes an arbitrary open path without allocating. */
    inline fun stroke(c: Canvas, color: Int, w: Float, block: (Path) -> Unit) {
        val p = acquire()
        try {
            block(p)
            drawPathStroke(c, p, color, w)
        } finally {
            release()
        }
    }

    // ------------------------------------------------------------------- text

    fun label(
        c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int,
        align: Paint.Align = Paint.Align.CENTER
    ) {
        textPaint.textSize = size
        textPaint.color = color
        textPaint.textAlign = align
        textPaint.style = Paint.Style.FILL
        c.drawText(s, x, y, textPaint)
    }

    fun measure(s: String, size: Float): Float {
        textPaint.textSize = size
        return textPaint.measureText(s)
    }

    // ------------------------------------------------------------- characters

    /** Standard cartoon eye with a highlight; [open] 0 draws a closed blink line. */
    fun eye(c: Canvas, x: Float, y: Float, r: Float, open: Float, color: Int = C.BLACK) {
        if (open < 0.12f) {
            line(c, x - r, y, x + r, y, color, r * 0.55f)
            return
        }
        oval(c, x, y, r, r * open, color)
        if (open > 0.5f) {
            circle(c, x + r * 0.3f, y - r * 0.35f * open, r * 0.3f, withAlpha(C.WHITE, 235))
        }
    }

    /** Rosy cheek blush. */
    fun blush(c: Canvas, x: Float, y: Float, r: Float) {
        oval(c, x, y, r, r * 0.62f, withAlpha(C.PINK, 130))
    }

    /** Simple smile arc. */
    fun smile(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int = C.BLACK, sw: Float = 4f) {
        arcLine(c, x, y - h * 0.5f, w * 0.5f, h, 20f, 140f, color, sw)
    }
}
