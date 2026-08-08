package com.bitiplay.world.scenes

import android.graphics.Canvas
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.shade
import com.bitiplay.world.art.withAlpha
import com.bitiplay.world.core.TAU
import com.bitiplay.world.core.wobble
import kotlin.math.cos
import kotlin.math.sin

/** Reusable scenery pieces. Each draws with its base at the origin. */
object Art {

    fun cloud(c: Canvas, s: Float, alpha: Int = 255) {
        val col = withAlpha(C.CLOUD, alpha)
        D.circle(c, -s * 0.7f, 0f, s * 0.55f, col)
        D.circle(c, 0f, -s * 0.28f, s * 0.78f, col)
        D.circle(c, s * 0.75f, 0f, s * 0.6f, col)
        D.rect(c, -s * 0.75f, -s * 0.08f, s * 1.55f, s * 0.6f, col, s * 0.3f)
        D.circle(c, s * 0.1f, s * 0.16f, s * 0.42f, withAlpha(C.CLOUD_SHADE, alpha))
    }

    fun sun(c: Canvas, r: Float, t: Float) {
        val pulse = 1f + sin(t * 0.7f) * 0.03f
        for (i in 0 until 12) {
            val a = i * TAU / 12f + t * 0.12f
            D.capsule(
                c, cos(a) * r * 1.25f, sin(a) * r * 1.25f,
                cos(a) * r * 1.62f * pulse, sin(a) * r * 1.62f * pulse,
                r * 0.13f, withAlpha(C.SUN, 150)
            )
        }
        D.circle(c, 0f, 0f, r * pulse, C.SUN)
        D.circle(c, 0f, 0f, r * 0.82f * pulse, shade(C.SUN, 1.12f))
    }

    fun moonlessStars(c: Canvas, w: Float, h: Float, seed: Int) {
        for (i in 0 until 26) {
            val a = (i * 97 + seed) % 1000 / 1000f
            val b = (i * 53 + seed) % 700 / 700f
            D.circle(c, a * w, b * h, 2.5f, withAlpha(C.WHITE, 190))
        }
    }

    fun hill(c: Canvas, w: Float, h: Float, col: Int) {
        D.shape(c, col) { p ->
            p.moveTo(-w * 0.5f, 0f)
            p.quadTo(-w * 0.22f, -h, 0f, -h)
            p.quadTo(w * 0.22f, -h, w * 0.5f, 0f)
        }
    }

    fun tree(c: Canvas, seed: Int, t: Float, h: Float, leaf: Int = C.LEAF, trunk: Int = C.TRUNK) {
        val sway = wobble(seed, t * 0.35f) * 0.02f
        D.capsule(c, 0f, 0f, 0f, -h * 0.5f, h * 0.09f, trunk)
        D.capsule(c, 0f, -h * 0.34f, -h * 0.16f, -h * 0.48f, h * 0.045f, trunk)
        c.save()
        c.rotate(sway * 57.29578f, 0f, -h * 0.5f)
        D.circle(c, -h * 0.2f, -h * 0.6f, h * 0.24f, shade(leaf, 0.86f))
        D.circle(c, h * 0.2f, -h * 0.62f, h * 0.23f, leaf)
        D.circle(c, 0f, -h * 0.78f, h * 0.27f, shade(leaf, 1.1f))
        D.circle(c, -h * 0.08f, -h * 0.64f, h * 0.2f, leaf)
        c.restore()
    }

    fun pine(c: Canvas, seed: Int, t: Float, h: Float) {
        val sway = wobble(seed, t * 0.3f) * 0.015f
        D.capsule(c, 0f, 0f, 0f, -h * 0.25f, h * 0.07f, C.WOOD_DARK)
        c.save()
        c.rotate(sway * 57.29578f, 0f, 0f)
        for (i in 0 until 3) {
            val yy = -h * (0.22f + i * 0.24f)
            val ww = h * (0.34f - i * 0.08f)
            D.tri(c, -ww, yy, ww, yy, 0f, yy - h * 0.36f, shade(C.LEAF_DARK, 1f + i * 0.08f))
        }
        c.restore()
    }

    fun palm(c: Canvas, seed: Int, t: Float, h: Float) {
        val sway = wobble(seed, t * 0.5f) * 0.05f
        D.stroke(c, C.WOOD, h * 0.075f) { p ->
            p.moveTo(0f, 0f)
            p.quadTo(h * 0.1f, -h * 0.55f, h * 0.2f + sway * h, -h)
        }
        val tx = h * 0.2f + sway * h
        for (i in 0 until 6) {
            val a = -TAU * 0.5f + i * (TAU * 0.5f / 5f)
            val ex = tx + cos(a) * h * 0.42f
            val ey = -h + sin(a) * h * 0.3f
            D.stroke(c, if (i % 2 == 0) C.LEAF_DARK else C.LEAF, h * 0.055f) { p ->
                p.moveTo(tx, -h)
                p.quadTo((tx + ex) * 0.5f, ey - h * 0.16f, ex, ey)
            }
        }
        D.circle(c, tx - h * 0.06f, -h * 0.94f, h * 0.05f, C.WOOD_DARK)
        D.circle(c, tx + h * 0.07f, -h * 0.9f, h * 0.05f, C.WOOD_DARK)
    }

    fun bushClump(c: Canvas, seed: Int, w: Float, h: Float, col: Int = C.LEAF_DARK) {
        D.circle(c, -w * 0.3f, -h * 0.4f, h * 0.52f, col)
        D.circle(c, w * 0.32f, -h * 0.36f, h * 0.46f, shade(col, 1.1f))
        D.circle(c, 0f, -h * 0.6f, h * 0.55f, shade(col, 1.2f))
    }

    fun tuft(c: Canvas, h: Float, col: Int) {
        D.stroke(c, col, h * 0.22f) { p ->
            p.moveTo(-h * 0.35f, 0f); p.lineTo(-h * 0.15f, -h)
            p.moveTo(0f, 0f); p.lineTo(h * 0.06f, -h * 1.2f)
            p.moveTo(h * 0.35f, 0f); p.lineTo(h * 0.2f, -h * 0.9f)
        }
    }

    fun flowerPatch(c: Canvas, seed: Int) {
        val cols = intArrayOf(C.PINK, C.YELLOW, C.PURPLE, C.WHITE)
        // Callers pass repeating-tile indices, which are negative left of the
        // world origin, so normalise before indexing or sizing.
        val s = seed.mod(1000)
        for (i in 0 until 3) {
            val sx = (i - 1) * 26f + s.mod(7) - 3f
            val hh = 30f + (s + i * 13).mod(14)
            D.capsule(c, sx, 0f, sx, -hh, 4f, C.LEAF_DARK)
            D.circle(c, sx, -hh - 5f, 9f, cols[(s + i).mod(cols.size)])
            D.circle(c, sx, -hh - 5f, 4f, C.YELLOW_DARK)
        }
    }

    fun rockCluster(c: Canvas, seed: Int, s: Float) {
        D.shape(c, C.METAL_DARK) { p ->
            p.moveTo(-s, 0f); p.lineTo(-s * 0.6f, -s * 0.85f)
            p.lineTo(s * 0.2f, -s); p.lineTo(s, 0f)
        }
        D.shape(c, shade(C.METAL_DARK, 1.22f)) { p ->
            p.moveTo(-s * 0.6f, -s * 0.85f); p.lineTo(s * 0.2f, -s); p.lineTo(-s * 0.1f, -s * 0.5f)
        }
        D.oval(c, s * 0.9f, -s * 0.18f, s * 0.5f, s * 0.28f, shade(C.METAL_DARK, 1.1f))
    }

    /** Simple pitched-roof house with door and windows. */
    fun house(c: Canvas, w: Float, h: Float, wall: Int, roof: Int, seed: Int) {
        D.rect(c, -w * 0.5f, -h, w, h, wall, 8f)
        D.rect(c, -w * 0.5f, -h * 0.06f, w, h * 0.06f, shade(wall, 0.82f))
        D.shape(c, roof) { p ->
            p.moveTo(-w * 0.62f, -h)
            p.lineTo(0f, -h - h * 0.42f)
            p.lineTo(w * 0.62f, -h)
        }
        D.rect(c, -w * 0.62f, -h - 10f, w * 1.24f, 14f, shade(roof, 0.82f), 6f)
        // door
        val dw = w * 0.22f
        D.rect(c, -dw * 0.5f + w * 0.12f, -h * 0.52f, dw, h * 0.52f, shade(wall, 0.7f), 6f)
        D.circle(c, w * 0.12f + dw * 0.3f, -h * 0.26f, 5f, C.YELLOW)
        // windows
        val ww = w * 0.2f
        D.rect(c, -w * 0.34f, -h * 0.78f, ww, ww, C.GLASS, 5f)
        D.rectStroke(c, -w * 0.34f, -h * 0.78f, ww, ww, C.WHITE, 5f, 5f)
        if (seed % 2 == 0) {
            D.rect(c, w * 0.16f, -h * 0.78f, ww, ww, C.GLASS, 5f)
            D.rectStroke(c, w * 0.16f, -h * 0.78f, ww, ww, C.WHITE, 5f, 5f)
        }
        // chimney
        D.rect(c, -w * 0.36f, -h - h * 0.5f, w * 0.12f, h * 0.24f, shade(roof, 0.75f), 4f)
    }

    /** City tower with lit windows. */
    fun tower(c: Canvas, w: Float, h: Float, col: Int, seed: Int, t: Float) {
        D.rect(c, -w * 0.5f, -h, w, h, col, 6f)
        D.rect(c, -w * 0.5f, -h, w * 0.16f, h, shade(col, 1.14f))
        val cols = ((w - 30f) / 42f).toInt().coerceAtLeast(1)
        val rows = ((h - 60f) / 58f).toInt().coerceAtLeast(1)
        for (r in 0 until rows) {
            for (q in 0 until cols) {
                val lx = -w * 0.5f + 22f + q * 42f
                val ly = -h + 34f + r * 58f
                val on = ((r * 7 + q * 13 + seed) % 5) != 0
                val flick = if (((r + q + seed) % 11) == 0) (sin(t * 2f + r + q) > 0f) else true
                D.rect(
                    c, lx, ly, 26f, 34f,
                    if (on && flick) withAlpha(C.YELLOW, 220) else shade(col, 0.72f), 4f
                )
            }
        }
        D.rect(c, -w * 0.54f, -h - 16f, w * 1.08f, 18f, shade(col, 0.78f), 5f)
    }

    fun lamppost(c: Canvas, h: Float) {
        D.capsule(c, 0f, 0f, 0f, -h, 9f, C.METAL_DARK)
        D.oval(c, 0f, -6f, 22f, 8f, C.METAL_DARK)
        D.stroke(c, C.METAL_DARK, 8f) { p ->
            p.moveTo(0f, -h); p.quadTo(0f, -h - 26f, 34f, -h - 26f)
        }
        D.shape(c, C.METAL_DARK) { p ->
            p.moveTo(18f, -h - 26f); p.lineTo(50f, -h - 26f)
            p.lineTo(42f, -h - 2f); p.lineTo(26f, -h - 2f)
        }
        D.oval(c, 34f, -h - 6f, 14f, 7f, withAlpha(C.YELLOW, 230))
    }

    fun bench(c: Canvas, w: Float = 150f) {
        D.capsule(c, -w * 0.38f, 0f, -w * 0.38f, -46f, 10f, C.METAL_DARK)
        D.capsule(c, w * 0.38f, 0f, w * 0.38f, -46f, 10f, C.METAL_DARK)
        D.rect(c, -w * 0.5f, -58f, w, 16f, C.WOOD, 7f)
        D.rect(c, -w * 0.5f, -96f, w, 14f, C.WOOD, 6f)
        D.rect(c, -w * 0.5f, -120f, w, 14f, C.WOOD, 6f)
        D.capsule(c, -w * 0.42f, -58f, -w * 0.42f, -124f, 8f, C.METAL_DARK)
        D.capsule(c, w * 0.42f, -58f, w * 0.42f, -124f, 8f, C.METAL_DARK)
    }

    /** Picket fence section centred on the origin. */
    fun fence(c: Canvas, w: Float, h: Float, col: Int = C.OFF_WHITE) {
        D.rect(c, -w * 0.5f, -h * 0.72f, w, h * 0.14f, col, 4f)
        D.rect(c, -w * 0.5f, -h * 0.36f, w, h * 0.14f, col, 4f)
        var i = 0
        val step = 34f
        var px = -w * 0.5f + 8f
        while (px < w * 0.5f) {
            D.rect(c, px, -h, 18f, h, shade(col, if (i % 2 == 0) 1f else 0.96f), 4f)
            D.tri(c, px, -h, px + 18f, -h, px + 9f, -h - 16f, col)
            px += step
            i++
        }
    }

    fun awning(c: Canvas, w: Float, y: Float, a: Int, b: Int) {
        val n = 6
        val seg = w / n
        for (i in 0 until n) {
            val x0 = -w * 0.5f + i * seg
            D.shape(c, if (i % 2 == 0) a else b) { p ->
                p.moveTo(x0, y); p.lineTo(x0 + seg, y)
                p.lineTo(x0 + seg, y + 40f); p.lineTo(x0 + seg * 0.5f, y + 56f)
                p.lineTo(x0, y + 40f)
            }
        }
        D.rect(c, -w * 0.5f, y - 8f, w, 12f, C.WOOD_DARK, 5f)
    }

    fun signBoard(c: Canvas, text: String, w: Float, h: Float, col: Int, textCol: Int = C.WHITE) {
        D.rect(c, -w * 0.5f, -h * 0.5f, w, h, shade(col, 0.75f), 12f)
        D.rect(c, -w * 0.5f + 6f, -h * 0.5f + 6f, w - 12f, h - 12f, col, 9f)
        D.label(c, text, 0f, h * 0.5f - h * 0.34f, h * 0.46f, textCol)
    }

    /** Water band with a moving surface, drawn in camera-relative space. */
    fun water(c: Canvas, left: Float, width: Float, top: Float, depth: Float, t: Float, deep: Int, light: Int) {
        D.rect(c, left, top, width, depth, deep)
        D.rect(c, left, top, width, depth * 0.28f, light)
        var i = 0
        var px = left
        while (px < left + width) {
            val yy = top + 10f + sin(t * 1.6f + i * 0.7f) * 5f
            D.capsule(c, px + 14f, yy, px + 54f, yy, 7f, withAlpha(C.FOAM, 150))
            px += 120f
            i++
        }
    }
}
