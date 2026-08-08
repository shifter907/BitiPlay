package com.bitiplay.world.ent

import android.graphics.Canvas
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.shade
import com.bitiplay.world.audio.Sfx
import com.bitiplay.world.audio.Snd
import com.bitiplay.world.core.TAU
import com.bitiplay.world.engine.Entity
import com.bitiplay.world.engine.Scene
import com.bitiplay.world.engine.Z
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * A rideable carousel. Characters bind to a horse and orbit with it; several
 * can ride at once, and you can switch between them while they go round.
 *
 * Riders are drawn by the ride itself rather than by the renderer, so they
 * interleave correctly with the horses, the centre mast and the canopy.
 */
class Carousel(px: Float, py: Float, private val slots: Int = 6) : Entity() {

    var spin = 0f
        private set
    private var boost = 0f
    private val riders = arrayOfNulls<Character>(slots)

    init {
        x = px
        y = py
        z = Z.MAIN
        tappable = true
        hitW = 620f
        hitH = 620f
        hitCY = -300f
        useRange = 330f
        cullPad = 700f
    }

    override fun approachOffset(): Float = 300f

    override fun standDepth(): Float = 150f

    val full: Boolean get() = riders.all { it != null }

    fun release(ch: Character) {
        for (i in riders.indices) if (riders[i] === ch) riders[i] = null
    }

    override fun caption(ch: Character): String =
        if (ch.carousel === this) "hop off" else if (full) "all full!" else "ride the carousel"

    override fun onUse(ch: Character, scene: Scene): Boolean {
        if (ch.carousel === this) {
            ch.dismountCarousel(scene)
            return true
        }
        var slot = -1
        for (i in riders.indices) if (riders[i] == null) {
            slot = i
            break
        }
        if (slot < 0) {
            Sfx.play(Snd.UI)
            return false
        }
        riders[slot] = ch
        ch.mountCarousel(this, slot)
        boost = 1.1f
        scene.fx.notes(x, y - 460f, level, 4)
        scene.fx.confetti(x, y - 420f, level, 12)
        Sfx.play(Snd.HAPPY)
        return true
    }

    override fun update(dt: Float, scene: Scene) {
        spin += dt * (0.4f + boost)
        boost = max(0f, boost - dt * 0.22f)
        for (i in riders.indices) {
            val r = riders[i] ?: continue
            if (r.dead || r.carousel !== this) {
                riders[i] = null
                continue
            }
            // Parked at the hub so the camera does not swing with the orbit.
            r.x = x
            r.y = y
            r.level = level
        }
    }

    // ------------------------------------------------------------------- art

    override fun draw(c: Canvas) {
        val rx = 230f
        val ry = 56f
        val platY = -44f
        val domeY = -470f

        D.shadow(c, 0f, 0f, 300f, 46f)

        // stepped base
        D.oval(c, 0f, 14f, rx + 46f, ry + 22f, shade(C.SIDEWALK, 0.78f))
        D.oval(c, 0f, -6f, rx + 30f, ry + 14f, shade(C.CREAM, 0.86f))
        D.oval(c, 0f, platY, rx + 16f, ry + 6f, C.CREAM)

        // rotating floor pinwheel
        for (i in 0 until 16) {
            val a0 = spin + i * TAU / 16f
            val a1 = spin + (i + 1) * TAU / 16f
            D.shape(c, if (i % 2 == 0) shade(C.RED, 1.22f) else C.CREAM) { p ->
                p.moveTo(0f, platY)
                p.lineTo(cos(a0) * rx, platY + sin(a0) * ry)
                p.lineTo(cos(a1) * rx, platY + sin(a1) * ry)
            }
        }
        D.ovalStroke(c, 0f, platY, rx, ry, C.YELLOW_DARK, 6f)

        // back half, centre mast, then near half
        for (pass in 0 until 2) {
            for (i in 0 until slots) {
                val a = spin + i * TAU / slots
                val depth = sin(a)
                val isBack = depth < 0f
                if ((pass == 0) != isBack) continue
                drawMount(c, i, a, rx, ry, platY, domeY)
            }
            if (pass == 0) {
                D.capsule(c, 0f, platY, 0f, domeY, 26f, C.YELLOW_DARK)
                D.capsule(c, -7f, platY, -7f, domeY, 11f, C.YELLOW)
                for (k in 0 until 6) {
                    D.oval(c, 0f, platY - 56f - k * 70f, 31f, 8f, shade(C.RED, 1.1f))
                }
            }
        }

        // scalloped canopy
        for (i in 0 until 14) {
            val a0 = 180f + i * (180f / 14f)
            D.arcFill(c, 0f, domeY, rx + 46f, 132f, a0, 180f / 14f,
                if (i % 2 == 0) C.RED else C.CREAM)
        }
        for (i in 0 until 14) {
            val t = (i + 0.5f) / 14f
            val sx = -(rx + 46f) + t * 2f * (rx + 46f)
            D.circle(c, sx, domeY, 19f, if (i % 2 == 0) C.RED else C.CREAM)
        }
        D.ovalStroke(c, 0f, domeY, rx + 46f, 22f, C.YELLOW_DARK, 7f)

        for (i in 0 until 15) {
            val t = i / 14f
            val lx = -(rx + 40f) + t * 2f * (rx + 40f)
            val on = ((i + (spin * 2.4f).toInt()) % 3) != 0
            D.circle(c, lx, domeY - 20f, 8f, if (on) C.YELLOW else shade(C.YELLOW, 0.55f))
        }

        D.capsule(c, 0f, domeY - 128f, 0f, domeY - 188f, 11f, C.YELLOW_DARK)
        D.circle(c, 0f, domeY - 198f, 20f, C.YELLOW)
        D.stroke(c, C.RED, 5f) { p ->
            p.moveTo(2f, domeY - 216f); p.lineTo(2f, domeY - 262f)
        }
        D.shape(c, C.RED) { p ->
            val fy = domeY - 260f
            p.moveTo(4f, fy)
            p.quadTo(40f, fy + 8f, 74f, fy + 3f)
            p.lineTo(74f, fy + 33f)
            p.quadTo(40f, fy + 38f, 4f, fy + 30f)
        }
    }

    /** One pole, its horse, and whoever is sitting on it. */
    private fun drawMount(
        c: Canvas, slot: Int, a: Float,
        rx: Float, ry: Float, platY: Float, domeY: Float
    ) {
        val depth = sin(a)
        val hx = cos(a) * rx * 0.84f
        val hy = platY + depth * ry * 0.84f
        val s = 0.86f + depth * 0.14f
        val bob = sin(spin * 2.4f + slot * 1.9f) * 22f
        // Horses face the way they are travelling: dx/da is -sin(a).
        val dir = if (depth > 0f) -1f else 1f

        D.capsule(c, hx, hy + 12f, hx, domeY + 36f, 8f * s, C.YELLOW_DARK)
        D.circle(c, hx, domeY + 40f, 10f * s, C.YELLOW)

        val yy = hy + bob - 12f
        c.save()
        c.translate(hx, yy)
        c.scale(dir * s, s)
        horse(c, spin * 3f + slot)
        val rider = riders[slot]
        if (rider != null) {
            c.save()
            c.translate(4f, -62f)
            c.scale(0.62f, 0.62f)
            rider.drawMounted(c)
            c.restore()
        }
        c.restore()
    }

    /** Carousel horse, drawn facing +x with its hooves on the origin line. */
    private fun horse(c: Canvas, gait: Float) {
        val kick = sin(gait) * 8f
        D.stroke(c, C.CREAM, 12f) { p ->
            p.moveTo(-48f, -56f); p.quadTo(-80f, -44f, -74f, -6f)
        }
        D.capsule(c, -26f, -36f, -36f + kick, 2f, 12f, shade(C.OFF_WHITE, 0.84f))
        D.capsule(c, 24f, -36f, 34f - kick, 2f, 12f, shade(C.OFF_WHITE, 0.84f))
        D.oval(c, 0f, -52f, 52f, 32f, C.OFF_WHITE)
        D.capsule(c, -20f, -36f, -30f - kick, 4f, 13f, C.OFF_WHITE)
        D.capsule(c, 28f, -36f, 38f + kick, 4f, 13f, C.OFF_WHITE)
        D.shape(c, C.RED) { p ->
            p.moveTo(-22f, -72f); p.lineTo(26f, -72f)
            p.lineTo(22f, -50f); p.lineTo(-18f, -50f)
        }
        D.capsule(c, -20f, -61f, 24f, -61f, 6f, C.YELLOW)
        D.capsule(c, 30f, -62f, 52f, -104f, 21f, C.OFF_WHITE)
        D.circle(c, 58f, -110f, 22f, C.OFF_WHITE)
        D.oval(c, 74f, -104f, 13f, 10f, shade(C.OFF_WHITE, 0.9f))
        D.tri(c, 46f, -126f, 60f, -126f, 54f, -146f, C.OFF_WHITE)
        D.circle(c, 67f, -117f, 4.5f, C.BLACK)
        D.stroke(c, C.PINK, 10f) { p ->
            p.moveTo(40f, -124f); p.quadTo(22f, -92f, 14f, -64f)
        }
        D.capsule(c, 52f, -104f, 72f, -100f, 4f, C.YELLOW_DARK)
    }
}
