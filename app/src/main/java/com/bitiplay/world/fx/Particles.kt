package com.bitiplay.world.fx

import android.graphics.Canvas
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.withAlphaF
import com.bitiplay.world.core.TAU
import com.bitiplay.world.core.clamp01
import com.bitiplay.world.core.rnd
import kotlin.math.cos
import kotlin.math.sin

enum class Fx { HEART, SPARKLE, SMOKE, SPLASH, DUST, CONFETTI, BUBBLE, NOTE, STAR, CRUMB, LEAF, ZZZ }

class Particle {
    var active = false
    var kind = Fx.SPARKLE
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var grav = 0f
    var life = 0f
    var maxLife = 1f
    var size = 12f
    var color = C.WHITE
    var rot = 0f
    var vrot = 0f
    var level = 0
    var drag = 0f

    val t: Float get() = clamp01(1f - life / maxLife)

    fun draw(c: Canvas) {
        val fade = if (life < 0.28f) life / 0.28f else 1f
        val a = clamp01(fade)
        when (kind) {
            Fx.HEART -> {
                val s = size * (0.7f + 0.5f * t)
                val col = withAlphaF(color, a)
                D.circle(c, -s * 0.32f, -s * 0.25f, s * 0.42f, col)
                D.circle(c, s * 0.32f, -s * 0.25f, s * 0.42f, col)
                D.tri(c, -s * 0.72f, -s * 0.05f, s * 0.72f, -s * 0.05f, 0f, s * 0.85f, col)
            }
            Fx.SPARKLE -> {
                val s = size * (1f - t * 0.35f)
                D.star(c, 0f, 0f, s, s * 0.38f, 4, withAlphaF(color, a), rot)
            }
            Fx.STAR -> D.star(c, 0f, 0f, size, size * 0.45f, 5, withAlphaF(color, a), rot)
            Fx.SMOKE -> {
                val s = size * (0.6f + t * 1.5f)
                D.circle(c, 0f, 0f, s, withAlphaF(color, a * 0.5f))
            }
            Fx.SPLASH -> {
                val s = size * (1f - t * 0.4f)
                D.oval(c, 0f, 0f, s * 0.55f, s, withAlphaF(color, a * 0.9f))
            }
            Fx.BUBBLE -> {
                val s = size * (0.8f + t * 0.3f)
                D.circleStroke(c, 0f, 0f, s, withAlphaF(color, a * 0.85f), s * 0.22f)
            }
            Fx.DUST -> {
                val s = size * (0.5f + t * 1.1f)
                D.circle(c, 0f, 0f, s, withAlphaF(color, a * 0.45f))
            }
            Fx.CRUMB -> D.rectC(c, 0f, 0f, size, size, withAlphaF(color, a), size * 0.3f)
            Fx.CONFETTI -> {
                c.save()
                c.rotate(rot * 57.29578f)
                D.rectC(c, 0f, 0f, size * 1.6f, size * 0.8f, withAlphaF(color, a), size * 0.2f)
                c.restore()
            }
            Fx.LEAF -> {
                c.save()
                c.rotate(rot * 57.29578f)
                D.oval(c, 0f, 0f, size, size * 0.5f, withAlphaF(color, a))
                c.restore()
            }
            Fx.NOTE -> {
                val col = withAlphaF(color, a)
                D.oval(c, -size * 0.25f, size * 0.35f, size * 0.45f, size * 0.34f, col)
                D.rectC(c, size * 0.17f, -size * 0.1f, size * 0.16f, size * 1.0f, col, size * 0.08f)
                D.rectC(c, size * 0.42f, -size * 0.55f, size * 0.6f, size * 0.2f, col, size * 0.08f)
            }
            Fx.ZZZ -> {
                val col = withAlphaF(color, a)
                val s = size
                D.stroke(c, col, s * 0.22f) { p ->
                    p.moveTo(-s * 0.5f, -s * 0.5f); p.lineTo(s * 0.5f, -s * 0.5f)
                    p.lineTo(-s * 0.5f, s * 0.5f); p.lineTo(s * 0.5f, s * 0.5f)
                }
            }
        }
    }
}

/** Fixed-capacity, allocation-free particle pool. */
class Particles(capacity: Int = 260) {

    val pool = Array(capacity) { Particle() }
    private var cursor = 0

    private fun next(): Particle {
        // Prefer a free slot; if the pool is saturated, recycle round-robin.
        for (i in pool.indices) {
            val idx = (cursor + i) % pool.size
            if (!pool[idx].active) {
                cursor = (idx + 1) % pool.size
                return pool[idx]
            }
        }
        val p = pool[cursor]
        cursor = (cursor + 1) % pool.size
        return p
    }

    fun clear() {
        for (p in pool) p.active = false
    }

    fun spawn(
        kind: Fx, x: Float, y: Float, level: Int, count: Int,
        color: Int = C.WHITE, size: Float = 12f, speed: Float = 120f,
        grav: Float = 300f, life: Float = 0.9f, spread: Float = TAU, dir: Float = -TAU * 0.25f
    ) {
        repeat(count) {
            val p = next()
            p.active = true
            p.kind = kind
            p.x = x
            p.y = y
            p.level = level
            val a = dir + rnd(-spread * 0.5f, spread * 0.5f)
            val sp = speed * rnd(0.55f, 1.35f)
            p.vx = cos(a) * sp
            p.vy = sin(a) * sp
            p.grav = grav
            p.maxLife = life * rnd(0.75f, 1.25f)
            p.life = p.maxLife
            p.size = size * rnd(0.75f, 1.3f)
            p.color = color
            p.rot = rnd(0f, TAU)
            p.vrot = rnd(-6f, 6f)
            p.drag = 0.6f
        }
    }

    /** Hearts floating up out of a happy animal or character. */
    fun hearts(x: Float, y: Float, level: Int, count: Int = 4) =
        spawn(Fx.HEART, x, y, level, count, C.PINK, 16f, 90f, -40f, 1.2f, 1.1f)

    fun sparkles(x: Float, y: Float, level: Int, count: Int = 8, color: Int = C.YELLOW) =
        spawn(Fx.SPARKLE, x, y, level, count, color, 13f, 190f, 130f, 0.7f)

    fun smoke(x: Float, y: Float, level: Int, count: Int = 2) =
        spawn(Fx.SMOKE, x, y, level, count, 0xFFBFBFBF.toInt(), 15f, 42f, -55f, 1.5f, 0.7f)

    fun splash(x: Float, y: Float, level: Int, count: Int = 10, color: Int = C.SEA_LIGHT) =
        spawn(Fx.SPLASH, x, y, level, count, color, 12f, 230f, 700f, 0.7f, 2.0f)

    fun dust(x: Float, y: Float, level: Int, count: Int = 5) =
        spawn(Fx.DUST, x, y, level, count, 0xFFCBB18C.toInt(), 12f, 90f, -20f, 0.6f, 1.6f)

    fun dirt(x: Float, y: Float, level: Int, count: Int = 9) =
        spawn(Fx.CRUMB, x, y, level, count, C.DIRT_DARK, 11f, 250f, 900f, 0.8f, 1.6f)

    fun confetti(x: Float, y: Float, level: Int, count: Int = 22) {
        val cols = intArrayOf(C.RED, C.YELLOW, C.GREEN, C.BLUE, C.PINK, C.PURPLE)
        repeat(count) { i ->
            spawn(
                Fx.CONFETTI, x, y, level, 1, cols[i % cols.size],
                11f, 330f, 520f, 1.6f, 1.5f
            )
        }
    }

    fun bubbles(x: Float, y: Float, level: Int, count: Int = 5) =
        spawn(Fx.BUBBLE, x, y, level, count, C.FOAM, 10f, 60f, -90f, 1.4f, 1.0f)

    fun notes(x: Float, y: Float, level: Int, count: Int = 3) =
        spawn(Fx.NOTE, x, y, level, count, C.PURPLE, 15f, 80f, -50f, 1.4f, 1.0f)

    fun crumbs(x: Float, y: Float, level: Int, color: Int, count: Int = 7) =
        spawn(Fx.CRUMB, x, y, level, count, color, 10f, 190f, 800f, 0.7f, 2.0f)

    fun leaves(x: Float, y: Float, level: Int, count: Int = 6) =
        spawn(Fx.LEAF, x, y, level, count, C.LEAF, 12f, 70f, 130f, 1.8f, 2.2f)

    fun update(dt: Float) {
        for (p in pool) {
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f) {
                p.active = false
                continue
            }
            p.vy += p.grav * dt
            val d = 1f - p.drag * dt
            p.vx *= d
            p.vy *= d
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rot += p.vrot * dt
        }
    }

    inline fun forEachActive(action: (Particle) -> Unit) {
        for (p in pool) if (p.active) action(p)
    }
}
