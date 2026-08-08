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
import kotlin.math.sin

enum class PushKind(
    val label: String,
    val capacity: Int,
    val offset: Float,
    val loadY: Float,
    val loadSpread: Float
) {
    WHEELBARROW("wheelbarrow", 3, 132f, -104f, 44f),
    SHOPPING_CART("cart", 4, 128f, -136f, 40f),
    LUGGAGE_CART("luggage cart", 3, 138f, -118f, 46f),
    WAGON("wagon", 3, 130f, -96f, 46f),
    ZOO_CART("feed cart", 3, 134f, -110f, 44f)
}

/**
 * A cart the character walks behind. Tap to grab, tap again to let go; tap
 * while carrying something to load it in, tap while holding nothing to take
 * the top item back out.
 */
class Pushable(val kind: PushKind, startX: Float, val paint: Int = defaultPaint(kind)) : Entity() {

    var holder: Character? = null
    val contents = ArrayList<Item>()

    private var wheelRot = 0f
    private var lastX = 0f

    val pushOffset: Float get() = kind.offset

    init {
        x = startX
        lastX = startX
        z = Z.MAIN
        tappable = true
        hitW = 210f
        hitH = 190f
        hitCY = -95f
        useRange = 175f
        cullPad = 340f
    }

    override fun approachOffset(): Float = 130f

    override fun update(dt: Float, scene: Scene) {
        val d = scene.delta(lastX, x)
        if (dt > 0f) wheelRot += d / 34f
        lastX = x
        for (i in contents.indices) {
            val it = contents[i]
            it.x = x
            it.y = kind.loadY
            it.level = level
        }
    }

    override fun caption(ch: Character): String = when {
        ch.carried != null && contents.size < kind.capacity -> "load the ${kind.label}"
        ch.pushing === this && contents.isNotEmpty() -> "take one out"
        ch.pushing === this -> "let go"
        else -> "push the ${kind.label}"
    }

    override fun onUse(ch: Character, scene: Scene): Boolean {
        val held = ch.carried
        if (held != null && contents.size < kind.capacity) {
            val it = ch.surrenderCarried() ?: return false
            contents.add(it)
            ch.startAct(Act.REACH, 0.3f)
            Sfx.play(Snd.DROP)
            scene.fx.sparkles(x, kind.loadY, level, 4, C.WHITE)
            return true
        }
        if (ch.pushing === this) {
            if (contents.isNotEmpty()) {
                val it = contents.removeAt(contents.size - 1)
                it.stored = false
                it.visible = true
                it.place(scene.wrapX(x), it.restY)
                ch.pickUp(it, scene)
                return true
            }
            ch.releasePush()
            Sfx.play(Snd.DROP)
            return true
        }
        ch.grab(this)
        return true
    }

    // ------------------------------------------------------------------- art

    private fun wheel(c: Canvas, cx: Float, cy: Float, r: Float, hub: Int) {
        D.circle(c, cx, cy, r, C.BLACK)
        D.circle(c, cx, cy, r * 0.5f, hub)
        for (i in 0 until 3) {
            val a = wheelRot + i * TAU / 3f
            D.line(c, cx, cy, cx + cos(a) * r * 0.44f, cy + sin(a) * r * 0.44f, shade(hub, 0.7f), 4f)
        }
    }

    private fun drawContents(c: Canvas) {
        for (i in contents.indices) {
            val it = contents[i]
            val col = i % 2
            val row = i / 2
            val ox = (col - 0.5f) * kind.loadSpread
            val oy = -row * 40f
            c.save()
            c.translate(ox, kind.loadY + oy)
            c.scale(0.82f, 0.82f)
            ItemArt.draw(c, it.kind, t)
            c.restore()
        }
    }

    override fun draw(c: Canvas) {
        D.shadow(c, 0f, 0f, 105f, 17f)
        when (kind) {
            PushKind.WHEELBARROW -> {
                wheel(c, -74f, -30f, 30f, C.METAL)
                D.shape(c, paint) { p ->
                    p.moveTo(-72f, -128f); p.lineTo(74f, -104f)
                    p.lineTo(52f, -46f); p.lineTo(-52f, -60f)
                }
                D.stroke(c, C.METAL_DARK, 9f) { p ->
                    p.moveTo(-70f, -60f); p.lineTo(-38f, -14f)
                    p.moveTo(66f, -74f); p.lineTo(102f, -108f)
                    p.moveTo(48f, -52f); p.lineTo(96f, -102f)
                }
                D.circle(c, 104f, -110f, 11f, C.WOOD_DARK)
                drawContents(c)
            }
            PushKind.SHOPPING_CART -> {
                wheel(c, -60f, -16f, 16f, C.METAL)
                wheel(c, 54f, -16f, 16f, C.METAL)
                D.shape(c, paint) { p ->
                    p.moveTo(-72f, -168f); p.lineTo(70f, -148f)
                    p.lineTo(54f, -56f); p.lineTo(-56f, -56f)
                }
                D.stroke(c, shade(paint, 0.7f), 5f) { p ->
                    for (i in 0 until 5) {
                        val fx = -64f + i * 30f
                        p.moveTo(fx, -160f); p.lineTo(fx + 4f, -58f)
                    }
                    p.moveTo(-70f, -140f); p.lineTo(66f, -122f)
                    p.moveTo(-66f, -104f); p.lineTo(62f, -92f)
                }
                D.stroke(c, C.METAL_DARK, 8f) { p ->
                    p.moveTo(-70f, -166f); p.lineTo(-104f, -178f)
                }
                D.capsule(c, -104f, -176f, -104f, -152f, 11f, C.RED)
                drawContents(c)
            }
            PushKind.LUGGAGE_CART -> {
                wheel(c, -62f, -18f, 18f, C.METAL)
                wheel(c, 58f, -18f, 18f, C.METAL)
                D.rectC(c, 0f, -66f, 190f, 22f, paint, 8f)
                D.capsule(c, -86f, -76f, -86f, -180f, 10f, C.METAL_DARK)
                D.capsule(c, 84f, -76f, 84f, -180f, 10f, C.METAL_DARK)
                D.capsule(c, -92f, -178f, 90f, -178f, 11f, C.METAL_DARK)
                drawContents(c)
            }
            PushKind.WAGON -> {
                wheel(c, -58f, -22f, 22f, C.RED)
                wheel(c, 58f, -22f, 22f, C.RED)
                D.rectC(c, 0f, -78f, 180f, 62f, paint, 10f)
                D.rectC(c, 0f, -106f, 180f, 14f, shade(paint, 0.82f), 6f)
                D.stroke(c, C.METAL_DARK, 9f) { p ->
                    p.moveTo(88f, -84f); p.lineTo(140f, -136f)
                }
                D.circle(c, 144f, -140f, 12f, C.WOOD_DARK)
                drawContents(c)
            }
            PushKind.ZOO_CART -> {
                wheel(c, -54f, -20f, 20f, C.LEAF_DARK)
                wheel(c, 54f, -20f, 20f, C.LEAF_DARK)
                D.rectC(c, 0f, -74f, 176f, 58f, paint, 8f)
                D.rectC(c, 0f, -110f, 176f, 16f, shade(paint, 0.8f), 6f)
                // Wheat glyph rather than a word, which would mirror when pushed left.
                D.capsule(c, 0f, -56f, 0f, -92f, 6f, C.YELLOW)
                D.oval(c, -12f, -80f, 12f, 6f, C.YELLOW_DARK)
                D.oval(c, 12f, -72f, 12f, 6f, C.YELLOW_DARK)
                D.oval(c, -12f, -66f, 12f, 6f, C.YELLOW_DARK)
                D.stroke(c, C.METAL_DARK, 9f) { p ->
                    p.moveTo(-84f, -84f); p.lineTo(-128f, -140f)
                }
                D.circle(c, -132f, -144f, 12f, C.WOOD_DARK)
                drawContents(c)
            }
        }
    }

    companion object {
        fun defaultPaint(kind: PushKind): Int = when (kind) {
            PushKind.WHEELBARROW -> C.ORANGE
            PushKind.SHOPPING_CART -> C.METAL
            PushKind.LUGGAGE_CART -> C.YELLOW_DARK
            PushKind.WAGON -> C.RED
            PushKind.ZOO_CART -> C.GREEN_DARK
        }
    }
}
