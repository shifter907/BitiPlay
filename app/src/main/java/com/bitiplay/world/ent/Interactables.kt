package com.bitiplay.world.ent

import android.graphics.Canvas
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.shade
import com.bitiplay.world.art.withAlpha
import com.bitiplay.world.audio.Sfx
import com.bitiplay.world.audio.Snd
import com.bitiplay.world.core.TAU
import com.bitiplay.world.core.clamp01
import com.bitiplay.world.core.rnd
import com.bitiplay.world.engine.Entity
import com.bitiplay.world.engine.Scene
import com.bitiplay.world.engine.Z
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// =====================================================================  PLANT

enum class PlantKind(val label: String, val produce: ItemKind?, val fullH: Float) {
    FLOWERS("flowers", ItemKind.FLOWER, 150f),
    TOMATO("tomato plant", ItemKind.TOMATO, 190f),
    CORN("corn", ItemKind.CORN, 260f),
    CARROTS("carrots", ItemKind.CARROT, 130f),
    SUNFLOWER("sunflower", ItemKind.FLOWER, 300f),
    LETTUCE("lettuce", ItemKind.LETTUCE, 120f),
    PUMPKIN_VINE("pumpkin vine", ItemKind.PUMPKIN, 150f),
    SAPLING("little tree", ItemKind.APPLE, 300f),
    BUSH("bush", null, 160f)
}

/** Water it to make it grow; pick the fruit when it is ready. */
class Plant(val kind: PlantKind, startX: Float, startStage: Int = 0) : Entity() {

    var stage = startStage
    private var pop = 0f
    private var wet = 0f
    private val seed = (startX.toInt() * 17) and 0xFFF

    val ripe: Boolean get() = stage >= 3 && kind.produce != null

    init {
        x = startX
        z = Z.MAIN
        tappable = true
        hitW = 150f
        hitH = kind.fullH + 60f
        hitCY = -kind.fullH * 0.5f
        useRange = 140f
        cullPad = 300f
    }

    override fun approachOffset(): Float = 78f

    override fun update(dt: Float, scene: Scene) {
        if (pop > 0f) pop -= dt * 2.2f
        if (wet > 0f) wet -= dt * 0.35f
    }

    override fun caption(ch: Character): String = when {
        ripe -> "pick the ${kind.produce?.label}"
        ch.hasTool(ItemKind.WATERING_CAN) -> "water the ${kind.label}"
        else -> kind.label
    }

    override fun onUse(ch: Character, scene: Scene): Boolean {
        ch.faceToward(scene, x)
        if (ripe && ch.carried == null) {
            stage = 1
            pop = 1f
            val it = scene.add(Item(kind.produce!!, x, -kind.fullH * 0.55f))
            scene.after(0.05f) { ch.pickUp(it, scene) }
            scene.fx.sparkles(x, -kind.fullH * 0.6f, level, 7, C.YELLOW)
            Sfx.play(Snd.POP)
            return true
        }
        if (ch.hasTool(ItemKind.WATERING_CAN)) {
            ch.startAct(Act.WATER, 1.0f)
            Sfx.play(Snd.SPLASH)
            for (i in 0 until 3) {
                scene.after(0.15f + i * 0.2f) {
                    scene.fx.spawn(
                        com.bitiplay.world.fx.Fx.SPLASH,
                        x + ch.facing * -10f, -110f, level, 5, C.SEA_LIGHT, 9f, 130f, 900f, 0.55f, 1.2f
                    )
                }
            }
            scene.after(0.75f) {
                wet = 1f
                if (stage < 3) {
                    stage++
                    pop = 1f
                    scene.fx.sparkles(x, -kind.fullH * 0.5f, level, 6, C.GREEN_DARK)
                    Sfx.play(Snd.CHIME)
                }
            }
            return true
        }
        pop = 0.7f
        scene.fx.leaves(x, -kind.fullH * 0.5f, level, 3)
        Sfx.play(Snd.POP)
        return true
    }

    override fun draw(c: Canvas) {
        val grow = when (stage) {
            0 -> 0.22f
            1 -> 0.45f
            2 -> 0.72f
            else -> 1f
        }
        val squash = 1f + (if (pop > 0f) sin(pop * TAU * 0.5f) * 0.16f else 0f)
        val sway = sin(t * 1.3f + seed) * 0.035f

        // soil
        D.arcFill(c, 0f, 0f, 54f, 22f, 180f, 180f, if (wet > 0.05f) shade(C.DIRT, 0.78f) else C.DIRT)
        D.oval(c, 0f, -1f, 54f, 8f, shade(C.DIRT, 0.86f))

        c.save()
        c.rotate(sway * 57.29578f)
        c.scale(1f / squash, squash)

        val h = kind.fullH * grow
        when (kind) {
            PlantKind.FLOWERS -> {
                for (i in -1..1) {
                    val sx = i * 30f
                    D.capsule(c, sx, 0f, sx, -h, 7f, C.LEAF_DARK)
                    D.oval(c, sx + 16f, -h * 0.55f, 16f, 7f, C.LEAF)
                    if (stage >= 2) {
                        val col = when (i) {
                            -1 -> C.PINK
                            0 -> C.YELLOW
                            else -> C.PURPLE
                        }
                        for (k in 0 until 5) {
                            val a = k * TAU / 5f
                            D.circle(c, sx + cos(a) * 15f, -h + sin(a) * 15f, 11f, col)
                        }
                        D.circle(c, sx, -h, 9f, C.YELLOW_DARK)
                    }
                }
            }
            PlantKind.TOMATO -> {
                D.capsule(c, 0f, 0f, 0f, -h, 9f, C.LEAF_DARK)
                D.oval(c, -26f, -h * 0.5f, 24f, 11f, C.LEAF)
                D.oval(c, 26f, -h * 0.72f, 24f, 11f, C.LEAF)
                D.circle(c, 0f, -h - 12f, 26f, C.LEAF)
                if (stage >= 3) {
                    D.circle(c, -20f, -h * 0.62f, 15f, C.RED)
                    D.circle(c, 22f, -h * 0.82f, 15f, C.RED)
                }
            }
            PlantKind.CORN -> {
                D.capsule(c, 0f, 0f, 0f, -h, 12f, C.LEAF_DARK)
                for (i in 0 until 4) {
                    val ly = -h * (0.28f + i * 0.2f)
                    val s = if (i % 2 == 0) 1f else -1f
                    D.stroke(c, C.LEAF, 12f) { p ->
                        p.moveTo(0f, ly); p.quadTo(s * 52f, ly - 22f, s * 78f, ly + 8f)
                    }
                }
                if (stage >= 3) {
                    D.oval(c, 20f, -h * 0.66f, 15f, 34f, C.YELLOW)
                    D.oval(c, 20f, -h * 0.66f, 8f, 34f, shade(C.YELLOW, 0.9f))
                }
                D.oval(c, 0f, -h - 6f, 10f, 20f, C.YELLOW_DARK)
            }
            PlantKind.CARROTS -> {
                for (i in -1..1) {
                    val sx = i * 34f
                    for (k in -1..1) {
                        D.capsule(c, sx, -4f, sx + k * 16f, -h, 6f, C.LEAF)
                    }
                    if (stage >= 3) D.tri(c, sx - 10f, -6f, sx + 10f, -6f, sx, 22f, C.ORANGE)
                }
            }
            PlantKind.SUNFLOWER -> {
                D.capsule(c, 0f, 0f, 0f, -h, 13f, C.LEAF_DARK)
                D.stroke(c, C.LEAF, 14f) { p ->
                    p.moveTo(0f, -h * 0.45f); p.quadTo(46f, -h * 0.55f, 62f, -h * 0.35f)
                }
                if (stage >= 2) {
                    for (k in 0 until 10) {
                        val a = k * TAU / 10f
                        D.oval(c, cos(a) * 34f, -h + sin(a) * 34f, 18f, 11f, C.YELLOW)
                    }
                    D.circle(c, 0f, -h, 26f, 0xFF6B4A2B.toInt())
                }
            }
            PlantKind.LETTUCE -> {
                for (i in -1..1) {
                    val sx = i * 40f
                    D.circle(c, sx, -h * 0.5f, h * 0.42f, C.LEAF_DARK)
                    D.circle(c, sx - h * 0.16f, -h * 0.6f, h * 0.28f, C.LEAF)
                    D.circle(c, sx + h * 0.16f, -h * 0.6f, h * 0.28f, C.LEAF_LIGHT)
                }
            }
            PlantKind.PUMPKIN_VINE -> {
                D.stroke(c, C.LEAF_DARK, 9f) { p ->
                    p.moveTo(-70f, -8f); p.quadTo(0f, -h, 70f, -10f)
                }
                D.circle(c, -34f, -h * 0.55f, 22f, C.LEAF)
                D.circle(c, 40f, -h * 0.45f, 20f, C.LEAF)
                if (stage >= 3) {
                    D.oval(c, 6f, -26f, 34f, 27f, C.ORANGE)
                    D.rectC(c, 6f, -54f, 9f, 16f, C.LEAF_DARK, 3f)
                }
            }
            PlantKind.SAPLING -> {
                D.capsule(c, 0f, 0f, 0f, -h * 0.62f, 15f * grow + 5f, C.TRUNK)
                D.circle(c, -26f * grow, -h * 0.72f, 40f * grow + 8f, C.LEAF_DARK)
                D.circle(c, 28f * grow, -h * 0.68f, 36f * grow + 8f, C.LEAF)
                D.circle(c, 0f, -h * 0.9f, 42f * grow + 8f, C.LEAF_LIGHT)
                if (stage >= 3) {
                    D.circle(c, -30f, -h * 0.7f, 12f, C.RED)
                    D.circle(c, 26f, -h * 0.82f, 12f, C.RED)
                }
            }
            PlantKind.BUSH -> {
                D.circle(c, -30f * grow, -h * 0.45f, 38f * grow + 6f, C.LEAF_DARK)
                D.circle(c, 30f * grow, -h * 0.45f, 34f * grow + 6f, C.LEAF)
                D.circle(c, 0f, -h * 0.68f, 40f * grow + 6f, C.LEAF_LIGHT)
            }
        }
        c.restore()

        if (ripe) {
            val a = (sin(t * 3f) * 0.5f + 0.5f)
            D.circle(c, 0f, -kind.fullH - 34f, 7f + a * 3f, withAlpha(C.YELLOW, 190))
        }
    }
}

// ==================================================================  DIG SPOT

/** A patch of loose earth. Dig it out - sometimes something is buried. */
class DigSpot(startX: Float, val buried: ItemKind? = null) : Entity() {

    var dug = false
    private var digging = false
    private var open = 0f

    init {
        x = startX
        z = Z.DECAL
        tappable = true
        hitW = 150f
        hitH = 110f
        hitCY = -30f
        useRange = 130f
        cullPad = 200f
    }

    override fun approachOffset(): Float = 70f

    override fun update(dt: Float, scene: Scene) {
        if (dug && open < 1f) open = min(1f, open + dt * 2.6f)
    }

    override fun caption(ch: Character): String =
        if (dug) "a hole" else if (ch.hasTool(ItemKind.SHOVEL)) "dig here" else "dig with your paws"

    override fun onUse(ch: Character, scene: Scene): Boolean {
        if (dug || digging) return false
        digging = true
        ch.faceToward(scene, x)
        val fast = ch.hasTool(ItemKind.SHOVEL)
        val dur = if (fast) 1.0f else 1.5f
        ch.startAct(Act.DIG, dur)
        val shots = if (fast) 4 else 5
        for (i in 0 until shots) {
            scene.after(0.18f + i * (dur / shots)) {
                scene.fx.dirt(x, -18f, level, 7)
                Sfx.play(Snd.DIG)
            }
        }
        scene.after(dur) {
            dug = true
            digging = false
            scene.fx.dirt(x, -24f, level, 12)
            if (buried != null) {
                val it = scene.add(Item(buried, x, -60f))
                it.launch(rnd(-0.4f, 0.4f), 0.7f)
                scene.fx.sparkles(x, -70f, level, 10, C.YELLOW)
                Sfx.play(Snd.CHIME)
            }
        }
        return true
    }

    override fun draw(c: Canvas) {
        if (!dug) {
            D.arcFill(c, 0f, 4f, 52f, 26f, 180f, 180f, C.DIRT)
            D.arcFill(c, -14f, 4f, 26f, 15f, 180f, 180f, shade(C.DIRT, 1.12f))
            D.circle(c, 20f, -6f, 5f, shade(C.DIRT, 0.8f))
            D.circle(c, -4f, -13f, 4f, shade(C.DIRT, 0.8f))
        } else {
            D.oval(c, 0f, -2f, 56f * open, 20f * open, C.DIRT_DARK)
            D.oval(c, 0f, -5f, 46f * open, 14f * open, 0xFF4A3323.toInt())
            D.arcFill(c, -52f, 2f, 26f, 13f, 180f, 180f, C.DIRT)
            D.arcFill(c, 54f, 2f, 24f, 12f, 180f, 180f, C.DIRT)
        }
    }
}

// ======================================================================  TENT

/** Four taps to pitch it, then you can duck inside. */
class Tent(startX: Float, val paint: Int = C.RED) : Entity() {

    var stage = 0
    private var pop = 0f

    init {
        x = startX
        z = Z.MAIN
        tappable = true
        hitW = 300f
        hitH = 230f
        hitCY = -110f
        useRange = 180f
        cullPad = 400f
    }

    override fun approachOffset(): Float = 150f

    override fun update(dt: Float, scene: Scene) {
        if (pop > 0f) pop -= dt * 2.4f
    }

    override fun caption(ch: Character): String = when (stage) {
        0 -> "unpack the tent"
        1 -> "raise the poles"
        2 -> "pull the canvas over"
        else -> "go inside"
    }

    override fun onUse(ch: Character, scene: Scene): Boolean {
        ch.faceToward(scene, x)
        if (stage < 3) {
            ch.startAct(Act.BUILD, 0.8f)
            scene.after(0.55f) {
                stage++
                pop = 1f
                Sfx.play(Snd.BUILD)
                scene.fx.dust(x, -10f, level, 5)
                if (stage == 3) {
                    Sfx.play(Snd.HAPPY)
                    scene.fx.confetti(x, -220f, level, 16)
                }
            }
            return true
        }
        // Duck inside for a moment.
        ch.stop()
        ch.visible = false
        scene.fx.spawn(com.bitiplay.world.fx.Fx.ZZZ, x, -170f, level, 3, C.WHITE, 14f, 40f, -30f, 1.6f, 0.9f)
        Sfx.play(Snd.PAGE)
        scene.after(1.7f) {
            ch.visible = true
            ch.x = scene.wrapX(x + 150f)
            ch.facing = 1
            scene.fx.sparkles(ch.x, -120f, level, 6, C.WHITE)
        }
        return true
    }

    override fun draw(c: Canvas) {
        val squash = 1f + (if (pop > 0f) sin(pop * TAU * 0.5f) * 0.12f else 0f)
        D.shadow(c, 0f, 0f, 150f, 22f)
        c.save()
        c.scale(1f / squash, squash)
        when (stage) {
            0 -> {
                D.rectC(c, 0f, -30f, 150f, 56f, paint, 26f)
                D.rectC(c, -40f, -30f, 16f, 58f, shade(paint, 0.8f), 6f)
                D.rectC(c, 40f, -30f, 16f, 58f, shade(paint, 0.8f), 6f)
                D.capsule(c, -66f, -56f, 66f, -56f, 8f, C.WOOD_DARK)
            }
            1 -> {
                D.capsule(c, -110f, -6f, 0f, -200f, 9f, C.WOOD)
                D.capsule(c, 110f, -6f, 0f, -200f, 9f, C.WOOD)
                D.capsule(c, -130f, -6f, -104f, -60f, 6f, C.METAL_DARK)
                D.capsule(c, 130f, -6f, 104f, -60f, 6f, C.METAL_DARK)
                D.rectC(c, 0f, -26f, 130f, 44f, paint, 20f)
            }
            2 -> {
                D.capsule(c, -110f, -6f, 0f, -200f, 9f, C.WOOD)
                D.capsule(c, 110f, -6f, 0f, -200f, 9f, C.WOOD)
                D.shape(c, paint) { p ->
                    p.moveTo(-118f, -4f); p.lineTo(0f, -128f); p.lineTo(118f, -4f)
                }
            }
            else -> {
                D.shape(c, shade(paint, 0.82f)) { p ->
                    p.moveTo(-140f, -4f); p.lineTo(0f, -208f); p.lineTo(58f, -208f)
                    p.lineTo(-84f, -4f)
                }
                D.shape(c, paint) { p ->
                    p.moveTo(-84f, -4f); p.lineTo(58f, -208f); p.lineTo(150f, -4f)
                }
                D.shape(c, 0xFF3A2A22.toInt()) { p ->
                    p.moveTo(-34f, -4f); p.lineTo(24f, -128f); p.lineTo(78f, -4f)
                }
                D.shape(c, shade(paint, 1.12f)) { p ->
                    p.moveTo(24f, -128f); p.lineTo(78f, -4f); p.lineTo(112f, -4f); p.lineTo(46f, -132f)
                }
                D.capsule(c, 58f, -212f, 58f, -246f, 6f, C.WOOD)
                D.tri(c, 58f, -246f, 58f, -222f, 104f, -234f, C.YELLOW)
                D.capsule(c, -140f, -4f, -168f, -8f, 5f, C.METAL_DARK)
                D.capsule(c, 150f, -4f, 178f, -8f, 5f, C.METAL_DARK)
            }
        }
        c.restore()
    }
}

// =====================================================================  GRILL

/** Put raw food on the grate, wait for the sizzle, take it off cooked. */
class Grill(startX: Float) : Entity() {

    private class Slot {
        var kind: ItemKind? = null
        var cook = 0f
        var done = false
    }

    private val slots = Array(3) { Slot() }
    private var smokeTimer = 0f
    private var sizzleTimer = 0f

    init {
        x = startX
        z = Z.MAIN
        tappable = true
        hitW = 250f
        hitH = 240f
        hitCY = -120f
        useRange = 180f
        cullPad = 320f
    }

    override fun approachOffset(): Float = 130f

    private fun firstDone(): Int {
        for (i in slots.indices) if (slots[i].done) return i
        return -1
    }

    private fun firstFree(): Int {
        for (i in slots.indices) if (slots[i].kind == null) return i
        return -1
    }

    private val busyCooking: Boolean
        get() = slots.any { it.kind != null && !it.done }

    override fun update(dt: Float, scene: Scene) {
        var cooking = false
        for (s in slots) {
            val k = s.kind ?: continue
            if (s.done) continue
            cooking = true
            s.cook += dt
            if (s.cook >= COOK_TIME) {
                s.done = true
                s.kind = k.cooked ?: k
                scene.fx.sparkles(x, -172f, level, 8, C.YELLOW)
                Sfx.play(Snd.CHIME)
            }
        }
        if (cooking) {
            smokeTimer -= dt
            if (smokeTimer <= 0f) {
                smokeTimer = 0.22f
                scene.fx.smoke(x + rnd(-60f, 60f), -190f, level, 1)
            }
            sizzleTimer -= dt
            if (sizzleTimer <= 0f) {
                sizzleTimer = 1.1f
                Sfx.play(Snd.SIZZLE)
            }
        }
    }

    override fun caption(ch: Character): String {
        val held = ch.carried
        if (held != null && held.kind.cooked != null) return "grill the ${held.kind.label}"
        if (firstDone() >= 0) return "take it off the grill"
        if (busyCooking) return "cooking..."
        return "grill"
    }

    override fun onUse(ch: Character, scene: Scene): Boolean {
        ch.faceToward(scene, x)
        val held = ch.carried
        if (held != null && held.kind.cooked != null) {
            val slot = firstFree()
            if (slot < 0) return false
            val kind = held.kind
            ch.consumeCarried(scene)
            ch.startAct(Act.COOK, 0.6f)
            slots[slot].kind = kind
            slots[slot].cook = 0f
            slots[slot].done = false
            Sfx.play(Snd.SIZZLE)
            scene.fx.smoke(x, -186f, level, 3)
            return true
        }
        val done = firstDone()
        if (done >= 0 && ch.carried == null) {
            val kind = slots[done].kind!!
            slots[done].kind = null
            slots[done].done = false
            slots[done].cook = 0f
            ch.startAct(Act.COOK, 0.4f)
            val it = scene.add(Item(kind, x, -190f))
            scene.after(0.05f) { ch.pickUp(it, scene) }
            return true
        }
        scene.fx.smoke(x, -186f, level, 2)
        Sfx.play(Snd.POP)
        return true
    }

    override fun draw(c: Canvas) {
        D.shadow(c, 0f, 0f, 116f, 18f)
        // legs
        D.capsule(c, -78f, -6f, -50f, -140f, 10f, C.METAL_DARK)
        D.capsule(c, 78f, -6f, 50f, -140f, 10f, C.METAL_DARK)
        D.capsule(c, -74f, -66f, 74f, -66f, 7f, C.METAL_DARK)
        // basin
        D.shape(c, C.METAL_DARK) { p ->
            p.moveTo(-108f, -150f); p.lineTo(108f, -150f)
            p.lineTo(94f, -196f); p.lineTo(-94f, -196f)
        }
        D.rectC(c, 0f, -152f, 224f, 16f, shade(C.METAL_DARK, 1.2f), 8f)
        // coals
        for (i in -2..2) {
            val glow = 0.5f + 0.5f * sin(t * 3f + i)
            D.circle(c, i * 32f, -168f, 12f, if (busyCooking) C.ORANGE else 0xFF5A5A5A.toInt())
            if (busyCooking) D.circle(c, i * 32f, -168f, 6f * glow + 3f, C.YELLOW)
        }
        // grate
        for (i in -3..3) D.line(c, i * 26f, -206f, i * 26f, -186f, C.METAL, 5f)
        D.capsule(c, -100f, -196f, 100f, -196f, 7f, C.METAL)
        D.capsule(c, -100f, -186f, 100f, -186f, 7f, C.METAL)
        // lid, propped open behind
        D.arcFill(c, -8f, -200f, 116f, 74f, 190f, 130f, C.RED)
        D.circle(c, -108f, -232f, 11f, C.METAL_DARK)
        // food
        for (i in slots.indices) {
            val k = slots[i].kind ?: continue
            c.save()
            c.translate((i - 1) * 66f, -216f)
            c.scale(0.8f, 0.8f)
            ItemArt.draw(c, k, t)
            c.restore()
            if (slots[i].done) {
                val a = sin(t * 4f) * 0.5f + 0.5f
                D.circle(c, (i - 1) * 66f, -262f, 6f + a * 3f, withAlpha(C.YELLOW, 200))
            }
        }
    }

    companion object {
        const val COOK_TIME = 4.5f
    }
}

// =================================================================  DISPENSER

/**
 * Anything that hands you an item: a fruit tree, a shop shelf, an ice-cream
 * stand, a ticket booth. Art is supplied by the scene.
 */
class Dispenser(
    px: Float,
    val gives: ItemKind,
    val captionText: String,
    layer: Int = Z.MAIN,
    val drawer: (Canvas, Dispenser) -> Unit
) : Entity() {

    var pop = 0f
    private var cooldown = 0f

    init {
        x = px
        z = layer
        tappable = true
        hitW = 200f
        hitH = 260f
        hitCY = -130f
        useRange = 180f
        cullPad = 400f
    }

    fun sized(w: Float, h: Float, cy: Float = -h * 0.5f): Dispenser {
        hitW = w
        hitH = h
        hitCY = cy
        cullPad = w * 0.75f + 260f
        return this
    }

    override fun approachOffset(): Float = 110f

    override fun update(dt: Float, scene: Scene) {
        if (pop > 0f) pop -= dt * 2.6f
        if (cooldown > 0f) cooldown -= dt
    }

    override fun caption(ch: Character): String = captionText

    override fun onUse(ch: Character, scene: Scene): Boolean {
        if (cooldown > 0f) return false
        cooldown = 0.35f
        ch.faceToward(scene, x)
        ch.startAct(Act.REACH, 0.35f)
        pop = 1f
        val it = scene.add(Item(gives, x, -150f))
        scene.after(0.08f) { ch.pickUp(it, scene) }
        scene.fx.sparkles(x, -160f, level, 6, C.WHITE)
        Sfx.play(Snd.POP)
        return true
    }

    override fun draw(c: Canvas) {
        val s = 1f + (if (pop > 0f) sin(pop * TAU * 0.5f) * 0.06f else 0f)
        c.save()
        c.scale(1f / s, s)
        drawer(c, this)
        c.restore()
    }
}

// ====================================================================  STAIRS

/** Moves the active character between decks on the cruise ship. */
class Stairs(px: Float, val toLevel: Int, val label: String) : Entity() {

    init {
        x = px
        z = Z.BACK
        tappable = true
        hitW = 190f
        hitH = 260f
        hitCY = -130f
        useRange = 170f
        cullPad = 300f
    }

    override fun approachOffset(): Float = 90f

    override fun caption(ch: Character): String = label

    override fun onUse(ch: Character, scene: Scene): Boolean {
        ch.stop()
        ch.startAct(Act.REACH, 0.3f)
        Sfx.play(Snd.STEP)
        scene.after(0.18f) {
            ch.level = toLevel
            ch.x = scene.wrapX(x)
            scene.fx.sparkles(ch.x, -120f, toLevel, 6, C.WHITE)
        }
        return true
    }

    override fun draw(c: Canvas) {
        val up = toLevel > level
        D.rectC(c, 0f, -110f, 150f, 220f, shade(C.METAL, 0.9f), 10f)
        for (i in 0 until 5) {
            val sy = -30f - i * 38f
            D.rectC(c, -50f + i * 22f, sy, 90f, 16f, C.METAL, 5f)
        }
        D.stroke(c, C.YELLOW, 8f) { p ->
            p.moveTo(-72f, -34f); p.lineTo(56f, -216f)
        }
        val a = sin(t * 3f) * 6f
        val ay = if (up) -232f + a else -20f - a
        D.tri(
            c, 0f, ay + (if (up) -18f else 18f),
            -16f, ay + (if (up) 10f else -10f),
            16f, ay + (if (up) 10f else -10f), C.YELLOW
        )
    }
}
