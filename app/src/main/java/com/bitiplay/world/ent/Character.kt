package com.bitiplay.world.ent

import android.graphics.Canvas
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.shade
import com.bitiplay.world.art.withAlpha
import com.bitiplay.world.audio.Sfx
import com.bitiplay.world.audio.Snd
import com.bitiplay.world.core.TAU
import com.bitiplay.world.core.approach
import com.bitiplay.world.core.clamp
import com.bitiplay.world.core.clamp01
import com.bitiplay.world.core.rnd
import com.bitiplay.world.core.sgn
import com.bitiplay.world.engine.Entity
import com.bitiplay.world.engine.Scene
import com.bitiplay.world.engine.Z
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

enum class Ear { ROUND, POINTY, LONG, FLOPPY, TINY, NONE }
enum class Tail { PUFF, LONG, BUSHY, SHORT, NONE }
enum class Snout { CAT, DOG, ROUND, BEAK }

class CharSpec(
    val name: String,
    val body: Int,
    val belly: Int,
    val ear: Ear,
    val tail: Tail,
    val snout: Snout,
    val inner: Int = C.PINK
)

/** What the character is currently miming. */
enum class Act { NONE, REACH, WATER, DIG, FEED, PET, COOK, BUILD, CHEER }

/** The six playable friends. Every scene gets its own copy of each. */
object Roster {
    val all = listOf(
        CharSpec("Mimi", 0xFFF39B4C.toInt(), 0xFFFBE3C2.toInt(), Ear.POINTY, Tail.LONG, Snout.CAT),
        CharSpec("Rufus", 0xFF7E93AE.toInt(), 0xFFE9EEF4.toInt(), Ear.FLOPPY, Tail.SHORT, Snout.DOG),
        CharSpec("Poppy", 0xFFF2C6D6.toInt(), 0xFFFFFFFF.toInt(), Ear.LONG, Tail.PUFF, Snout.ROUND),
        CharSpec("Bo", 0xFF9C6B49.toInt(), 0xFFEBD3B4.toInt(), Ear.ROUND, Tail.NONE, Snout.ROUND),
        CharSpec("Kit", 0xFFE2703A.toInt(), 0xFFFBEFE0.toInt(), Ear.POINTY, Tail.BUSHY, Snout.CAT),
        CharSpec("Pip", 0xFF49BFC4.toInt(), 0xFFFFF0B8.toInt(), Ear.NONE, Tail.SHORT, Snout.BEAK)
    )
}

open class Character(val spec: CharSpec, val slot: Int) : Entity() {

    /** False for wandering background folk, which never join the switcher. */
    open val playable: Boolean get() = true

    var speed = 305f

    var targetX: Float? = null
    protected var arrive: ((Character) -> Unit)? = null

    /**
     * How far toward the viewer the character stands, 0 (back) to [DEPTH_BAND]
     * (front). Drives both the world y and a slight size change.
     */
    var depth = 0f
    private var targetDepth: Float? = null

    /** Set while walking off to the switcher wheel. */
    var leaving = false
        private set
    private var leaveT = 0f

    var carried: Item? = null
    var riding: Vehicle? = null
    var pushing: Pushable? = null

    /** Set while bound to a carousel horse; the ride owns our position and drawing. */
    var carousel: Carousel? = null
        private set
    private var carouselSlot = -1
    private var seatedOverride = false

    var act = Act.NONE
        private set
    private var actT = 0f
    private var actDur = 0f

    private var walkT = 0f
    private var moving = false
    private var blink = 0f
    private var blinkTimer = rnd(1.5f, 5f)
    private var joy = 0f

    val busy: Boolean get() = act != Act.NONE && act != Act.REACH

    init {
        z = Z.MAIN
        tappable = true
        hitW = 168f
        hitH = 218f
        hitCY = -106f
        useRange = 150f
        cullPad = 300f
        // Loose items and props sitting near a character win the tap.
        pickBias = 70f
    }

    // ----------------------------------------------------------------- orders

    /** [wy] is a depth in world units; null keeps the current depth. */
    fun goTo(scene: Scene, wx: Float, wy: Float? = null, then: ((Character) -> Unit)? = null) {
        if (wy != null) targetDepth = clamp(wy, 0f, DEPTH_BAND)
        val v = riding
        if (v != null) {
            v.driveTo(scene, wx)
            targetX = scene.wrapX(wx)
            arrive = then
            return
        }
        targetX = scene.wrapX(wx)
        arrive = then
    }

    fun stop() {
        targetX = null
        targetDepth = null
        arrive = null
    }

    /** Walks off toward the background and returns to the switcher wheel. */
    fun exitToWheel(scene: Scene) {
        if (leaving) return
        if (riding != null) dismount(scene)
        if (carousel != null) dismountCarousel(scene)
        releasePush()
        dropCarried(scene)
        leaving = true
        leaveT = 0f
        tappable = false
        targetDepth = 0f
        arrive = null
        targetX = scene.wrapX(x + facing * 460f)
        Sfx.play(Snd.WHOOSH)
    }

    fun faceToward(scene: Scene, wx: Float) {
        val d = scene.delta(x, wx)
        if (abs(d) > 4f) facing = if (d > 0f) 1 else -1
    }

    fun startAct(kind: Act, dur: Float) {
        act = kind
        actT = 0f
        actDur = dur
    }

    fun cheer(scene: Scene) {
        joy = 1.1f
        startAct(Act.CHEER, 0.6f)
        scene.fx.hearts(x, -235f, level, 3)
        Sfx.play(Snd.HAPPY)
    }

    // ------------------------------------------------------------- inventory

    fun hasTool(k: ItemKind): Boolean = carried?.kind == k

    fun pickUp(item: Item, scene: Scene) {
        if (item.holder != null) return
        dropCarried(scene)
        item.holder = this
        item.visible = false
        item.stored = false
        item.vx = 0f
        item.vy = 0f
        item.rot = 0f
        carried = item
        faceToward(scene, item.x)
        startAct(Act.REACH, 0.28f)
        Sfx.play(Snd.PICK)
        scene.fx.sparkles(item.x, item.y, level, 5, C.WHITE)
    }

    fun dropCarried(scene: Scene): Item? {
        val it = carried ?: return null
        carried = null
        it.holder = null
        it.visible = true
        it.baseY = depth
        it.place(scene.wrapX(x + facing * 62f), it.restY)
        Sfx.play(Snd.DROP)
        return it
    }

    fun throwCarried(scene: Scene, towardX: Float?): Item? {
        val it = carried ?: return null
        carried = null
        it.holder = null
        it.visible = true
        it.stored = false
        it.baseY = depth
        it.x = scene.wrapX(x + facing * 48f)
        it.y = depth - 140f
        it.level = level
        var dir = facing.toFloat()
        if (towardX != null) {
            val s = sgn(scene.delta(x, towardX))
            if (s != 0f) dir = s
        }
        facing = if (dir > 0f) 1 else -1
        it.launch(dir, 1f)
        startAct(Act.CHEER, 0.3f)
        Sfx.play(Snd.THROW)
        return it
    }

    /** Removes the carried item from the world entirely (eaten, cooked, stored). */
    fun consumeCarried(scene: Scene): Item? {
        val it = carried ?: return null
        carried = null
        it.holder = null
        scene.remove(it)
        return it
    }

    /** Hands the carried item to a container without destroying it. */
    fun surrenderCarried(): Item? {
        val it = carried ?: return null
        carried = null
        it.holder = null
        it.visible = false
        it.stored = true
        return it
    }

    // -------------------------------------------------------------- vehicles

    fun mount(v: Vehicle, scene: Scene) {
        if (v.rider != null && v.rider !== this) return
        // Hopping straight from one vehicle to another has to free the old one,
        // or it stays flagged as taken and nobody can ever ride it again.
        riding?.let { old ->
            if (old !== v) {
                old.rider = null
                old.stopDriving()
            }
        }
        releasePush()
        stop()
        riding = v
        v.rider = this
        v.wake()
        Sfx.play(if (v.motorised) Snd.ENGINE else Snd.POP)
        scene.fx.sparkles(x, -120f, level, 6, C.WHITE)
    }

    fun dismount(scene: Scene) {
        val v = riding ?: return
        riding = null
        v.rider = null
        v.stopDriving()
        // Drop any drive order still pending, so hopping off does not make the
        // character walk to wherever the vehicle had been heading.
        stop()
        x = scene.wrapX(v.x - v.facing * 110f)
        depth = clamp(v.y, 0f, DEPTH_BAND)
        y = depth
        level = v.level
        Sfx.play(Snd.POP)
    }

    fun mountCarousel(car: Carousel, slot: Int) {
        releasePush()
        stop()
        carousel = car
        carouselSlot = slot
        // The ride draws us, so we layer correctly among the horses and canopy.
        visible = false
    }

    fun dismountCarousel(scene: Scene) {
        val car = carousel ?: return
        car.release(this)
        carousel = null
        carouselSlot = -1
        visible = true
        depth = 170f
        y = depth
        x = scene.wrapX(car.x + 330f)
        facing = 1
        Sfx.play(Snd.POP)
    }

    /** Draws a seated character at the origin. Used by rides that own their riders. */
    fun drawMounted(c: Canvas) {
        seatedOverride = true
        draw(c)
        seatedOverride = false
    }

    fun grab(p: Pushable) {
        if (riding != null) return
        releasePush()
        pushing = p
        p.holder = this
        Sfx.play(Snd.POP)
    }

    fun releasePush() {
        pushing?.holder = null
        pushing = null
    }

    // ----------------------------------------------------------------- update

    override fun update(dt: Float, scene: Scene) {
        blinkTimer -= dt
        if (blinkTimer <= 0f) {
            blink = 0.15f
            blinkTimer = rnd(2.4f, 6.5f)
        }
        if (blink > 0f) blink -= dt
        if (joy > 0f) joy -= dt

        if (act != Act.NONE) {
            actT += dt
            if (actT >= actDur) act = Act.NONE
        }

        val td = targetDepth
        if (td != null) {
            depth = approach(depth, td, speed * 0.62f * dt)
            if (depth == td) targetDepth = null
        }

        if (leaving) {
            leaveT += dt
            val k = clamp01(leaveT / LEAVE_TIME)
            drawAlpha = 1f - k * k
            if (leaveT >= LEAVE_TIME) {
                dead = true
                scene.fx.sparkles(x, -110f, level, 8, C.WHITE)
                return
            }
        }

        if (carousel != null) {
            // Position comes from the ride; just keep the idle animation ticking.
            walkT += dt * 5.5f
            carried?.let { it.x = x; it.y = y - 128f; it.level = level }
            return
        }

        val v = riding
        if (v != null) {
            x = v.x + v.seatDX * v.facing
            y = v.y + v.seatDY
            level = v.level
            facing = v.facing
            moving = abs(v.vx) > 25f
            walkT += dt * 7f
            carried?.let { it.x = x; it.y = y - 150f; it.level = level }

            // The vehicle has finished its run, so the pending interaction can
            // fire. x stays wherever the seat is - snapping it to targetX would
            // pop the rider off the vehicle for a frame.
            if (targetX != null && v.targetX == null) {
                targetX = null
                val cb = arrive
                arrive = null
                cb?.invoke(this)
            }
            return
        }

        var moved = false
        val tx = targetX
        if (tx != null && !busy) {
            val d = scene.delta(x, tx)
            val step = speed * dt
            if (abs(d) <= max(step, 8f)) {
                x = tx
                targetX = null
                val cb = arrive
                arrive = null
                cb?.invoke(this)
            } else {
                facing = if (d > 0f) 1 else -1
                x = scene.wrapX(x + facing * step)
                moved = true
            }
        }
        moving = moved
        walkT = if (moving) walkT + dt * 9.5f else 0f

        y = depth
        val depthScale = 1f + (depth / DEPTH_BAND) * 0.16f
        drawScale =
            if (leaving) depthScale * (1f - clamp01(leaveT / LEAVE_TIME) * 0.6f) else depthScale

        pushing?.let { p ->
            p.level = level
            p.facing = facing
            p.y = y
            p.x = scene.wrapX(x + facing * p.pushOffset)
        }
        carried?.let { it.x = x; it.y = y - 128f; it.level = level }
    }

    // ------------------------------------------------------------------- art

    private val swing: Float get() = if (moving) sin(walkT) else 0f

    private val bob: Float
        get() = if (moving) abs(sin(walkT)) * 6f else sin(t * 2.1f) * 1.8f

    /** Front-hand position in local space; also drives the carried item. */
    private var handX = 30f
    private var handY = -120f
    private var handRot = 0f

    private fun computeHand() {
        val ph = if (actDur > 0f) clamp01(actT / actDur) else 0f
        val holding = carried != null
        when (act) {
            Act.WATER -> {
                handX = 60f; handY = -128f + sin(ph * TAU * 2f) * 5f; handRot = 0.85f
            }
            Act.DIG -> {
                val s = sin(ph * TAU * 3f)
                handX = 50f; handY = -90f + s * 24f; handRot = -0.45f + s * 0.75f
            }
            Act.FEED -> {
                handX = 68f + sin(ph * TAU * 2f) * 7f; handY = -108f; handRot = 0f
            }
            Act.PET -> {
                handX = 58f; handY = -118f + sin(ph * TAU * 3f) * 16f; handRot = 0f
            }
            Act.COOK -> {
                handX = 62f; handY = -114f; handRot = 0.25f
            }
            Act.BUILD -> {
                handX = 54f; handY = -114f + sin(ph * TAU * 4f) * 18f; handRot = 0f
            }
            Act.CHEER -> {
                handX = 38f; handY = -182f; handRot = -0.5f
            }
            Act.REACH -> {
                handX = 62f; handY = -96f; handRot = 0f
            }
            Act.NONE -> {
                if (holding) {
                    handX = 50f; handY = -120f; handRot = 0f
                } else {
                    handX = 34f + swing * 12f; handY = -74f + abs(swing) * 5f; handRot = 0f
                }
            }
        }
    }

    override fun draw(c: Canvas) {
        computeHand()
        // The entity transform already sits at the feet, so the contact shadow
        // is at the local origin. Riders get theirs from the vehicle or ride.
        if (riding == null && !seatedOverride) D.shadow(c, 0f, 0f, 62f, 16f, 44)
        val seated = riding != null || seatedOverride
        val b = if (seated) 0f else bob
        val y0 = -b

        drawTail(c, y0)

        // Far-side limbs, slightly darker for depth.
        val far = shade(spec.body, 0.84f)
        if (seated) drawSeatLeg(c, y0, far, -6f) else drawLeg(c, y0, -swing, far)
        drawArm(c, y0, -0.5f, far, false)

        // Body
        D.oval(c, 0f, BODY_CY + y0, BODY_RX, BODY_RY, spec.body)
        D.oval(c, 6f, BODY_CY + y0 + 10f, BODY_RX * 0.60f, BODY_RY * 0.58f, spec.belly)

        if (seated) drawSeatLeg(c, y0, spec.body, 8f) else drawLeg(c, y0, swing, spec.body)

        drawHead(c, y0)

        // Near arm reaches to the hand point, and holds whatever we picked up.
        drawArm(c, y0, 1f, spec.body, true)
        carried?.let { item ->
            c.save()
            c.translate(handX + 16f, handY + y0 - 6f)
            c.rotate(handRot * 57.29578f)
            ItemArt.draw(c, item.kind, t)
            c.restore()
        }

        if (joy > 0f) {
            val a = clamp01(joy)
            D.circle(c, -46f, -226f, 5f * a, withAlpha(C.YELLOW, 200))
            D.circle(c, 48f, -238f, 6f * a, withAlpha(C.YELLOW, 200))
        }
    }

    private fun drawLeg(c: Canvas, y0: Float, sw: Float, col: Int) {
        val kneeX = sw * 13f
        val footX = sw * 21f
        D.capsule(c, kneeX * 0.4f, HIP + y0, footX, -3f, 23f, col)
        D.oval(c, footX + 5f, -4f, 17f, 9f, shade(col, 0.86f))
    }

    private fun drawSeatLeg(c: Canvas, y0: Float, col: Int, dy: Float) {
        D.capsule(c, 4f, HIP + y0 + dy, 40f, HIP + y0 + 26f + dy, 21f, col)
        D.oval(c, 48f, HIP + y0 + 30f + dy, 15f, 9f, shade(col, 0.86f))
    }

    private fun drawArm(c: Canvas, y0: Float, side: Float, col: Int, near: Boolean) {
        val sy = SHOULDER + y0
        if (near) {
            D.capsule(c, 14f, sy, handX, handY + y0, 19f, col)
            D.circle(c, handX, handY + y0, 12f, shade(col, 0.92f))
        } else {
            val sw = -swing * 12f
            D.capsule(c, -8f, sy, -22f + sw, sy + 42f, 18f, col)
            D.circle(c, -22f + sw, sy + 42f, 11f, shade(col, 0.94f))
        }
    }

    private fun drawTail(c: Canvas, y0: Float) {
        val wag = sin(t * 3.4f + walkT) * 0.28f
        when (spec.tail) {
            Tail.PUFF -> D.circle(c, -44f, BODY_CY + y0 + 26f, 20f, spec.belly)
            Tail.SHORT -> D.oval(c, -44f, BODY_CY + y0 + 18f, 15f, 11f, shade(spec.body, 0.9f))
            Tail.LONG -> D.stroke(c, spec.body, 17f) { p ->
                p.moveTo(-36f, BODY_CY + y0 + 24f)
                p.quadTo(-84f, BODY_CY + y0 + 10f + wag * 40f, -74f, BODY_CY + y0 - 46f + wag * 30f)
            }
            Tail.BUSHY -> {
                D.stroke(c, spec.body, 30f) { p ->
                    p.moveTo(-34f, BODY_CY + y0 + 26f)
                    p.quadTo(-80f, BODY_CY + y0 + 16f, -76f, BODY_CY + y0 - 34f + wag * 24f)
                }
                D.circle(c, -76f, BODY_CY + y0 - 40f + wag * 24f, 17f, spec.belly)
            }
            Tail.NONE -> Unit
        }
    }

    private fun drawEars(c: Canvas, hy: Float) {
        val b = spec.body
        val inner = spec.inner
        when (spec.ear) {
            Ear.ROUND -> {
                D.circle(c, -32f, hy - 42f, 18f, b)
                D.circle(c, 32f, hy - 42f, 18f, b)
                D.circle(c, 33f, hy - 42f, 9f, inner)
            }
            Ear.TINY -> {
                D.circle(c, -28f, hy - 44f, 12f, b)
                D.circle(c, 28f, hy - 44f, 12f, b)
            }
            Ear.POINTY -> {
                D.tri(c, -46f, hy - 24f, -18f, hy - 40f, -34f, hy - 70f, b)
                D.tri(c, 46f, hy - 24f, 18f, hy - 40f, 34f, hy - 70f, b)
                D.tri(c, 38f, hy - 30f, 24f, hy - 38f, 33f, hy - 58f, inner)
            }
            Ear.LONG -> {
                D.oval(c, -22f, hy - 66f, 13f, 40f, b)
                D.oval(c, 24f, hy - 68f, 13f, 40f, b)
                D.oval(c, 24f, hy - 68f, 6f, 28f, inner)
            }
            Ear.FLOPPY -> {
                D.oval(c, -44f, hy - 4f, 15f, 32f, shade(b, 0.88f))
                D.oval(c, 44f, hy - 4f, 15f, 32f, shade(b, 0.88f))
            }
            Ear.NONE -> Unit
        }
    }

    private fun drawHead(c: Canvas, y0: Float) {
        val hy = HEAD_CY + y0
        drawEars(c, hy)
        D.circle(c, 0f, hy, HEAD_R, spec.body)

        val open = if (blink > 0f) 0f else 1f
        when (spec.snout) {
            Snout.CAT -> {
                D.oval(c, 16f, hy + 20f, 24f, 17f, spec.belly)
                D.tri(c, 20f, hy + 10f, 32f, hy + 10f, 26f, hy + 19f, spec.inner)
                D.line(c, 30f, hy + 16f, 52f, hy + 10f, withAlpha(C.BLACK, 120), 3f)
                D.line(c, 30f, hy + 22f, 53f, hy + 24f, withAlpha(C.BLACK, 120), 3f)
            }
            Snout.DOG -> {
                D.oval(c, 24f, hy + 22f, 27f, 20f, spec.belly)
                D.oval(c, 40f, hy + 12f, 11f, 9f, C.BLACK)
                D.line(c, 26f, hy + 30f, 26f, hy + 38f, withAlpha(C.BLACK, 150), 3f)
            }
            Snout.ROUND -> {
                D.oval(c, 14f, hy + 22f, 22f, 16f, spec.belly)
                D.oval(c, 22f, hy + 15f, 9f, 7f, C.BLACK)
            }
            Snout.BEAK -> {
                D.tri(c, 22f, hy + 2f, 62f, hy + 14f, 22f, hy + 26f, C.ORANGE)
                D.line(c, 24f, hy + 14f, 58f, hy + 14f, shade(C.ORANGE, 0.8f), 3f)
            }
        }

        D.eye(c, -14f, hy - 8f, 9f, open)
        D.eye(c, 20f, hy - 10f, 9f, open)
        D.blush(c, -30f, hy + 14f, 12f)
        D.blush(c, 38f, hy + 12f, 10f)
    }

    override fun caption(ch: Character): String = spec.name

    companion object {
        /** Head-and-shoulders badge used by the character tray. */
        fun portrait(c: Canvas, spec: CharSpec, r: Float) {
            val s = r / 52f
            c.save()
            c.scale(s, s)
            // shoulders peeking in from the bottom
            D.oval(c, 0f, 74f, 50f, 34f, spec.body)
            D.oval(c, 0f, 84f, 30f, 24f, spec.belly)
            when (spec.ear) {
                Ear.ROUND -> {
                    D.circle(c, -34f, -40f, 18f, spec.body)
                    D.circle(c, 34f, -40f, 18f, spec.body)
                }
                Ear.TINY -> {
                    D.circle(c, -30f, -44f, 12f, spec.body)
                    D.circle(c, 30f, -44f, 12f, spec.body)
                }
                Ear.POINTY -> {
                    D.tri(c, -48f, -22f, -18f, -38f, -34f, -70f, spec.body)
                    D.tri(c, 48f, -22f, 18f, -38f, 34f, -70f, spec.body)
                }
                Ear.LONG -> {
                    D.oval(c, -22f, -66f, 13f, 40f, spec.body)
                    D.oval(c, 22f, -66f, 13f, 40f, spec.body)
                    D.oval(c, 22f, -66f, 6f, 27f, spec.inner)
                }
                Ear.FLOPPY -> {
                    D.oval(c, -44f, -4f, 15f, 32f, shade(spec.body, 0.88f))
                    D.oval(c, 44f, -4f, 15f, 32f, shade(spec.body, 0.88f))
                }
                Ear.NONE -> Unit
            }
            D.circle(c, 0f, 0f, 52f, spec.body)
            when (spec.snout) {
                Snout.CAT -> {
                    D.oval(c, 0f, 22f, 24f, 16f, spec.belly)
                    D.tri(c, -6f, 12f, 6f, 12f, 0f, 21f, spec.inner)
                }
                Snout.DOG -> {
                    D.oval(c, 0f, 24f, 26f, 19f, spec.belly)
                    D.oval(c, 0f, 14f, 10f, 8f, C.BLACK)
                }
                Snout.ROUND -> {
                    D.oval(c, 0f, 22f, 21f, 15f, spec.belly)
                    D.oval(c, 0f, 15f, 8f, 6f, C.BLACK)
                }
                Snout.BEAK -> D.tri(c, -14f, 8f, 14f, 8f, 0f, 34f, C.ORANGE)
            }
            D.eye(c, -18f, -8f, 9f, 1f)
            D.eye(c, 18f, -8f, 9f, 1f)
            D.blush(c, -34f, 14f, 11f)
            D.blush(c, 34f, 14f, 11f)
            c.restore()
        }

        // Shorter legs and a wider body leave the head relatively larger, which
        // is what reads as "cute".
        const val HIP = -40f
        const val BODY_CY = -76f
        const val BODY_RX = 55f
        const val BODY_RY = 46f
        const val HEAD_CY = -148f
        const val HEAD_R = 52f
        const val SHOULDER = -96f

        /** How far toward the viewer a character may walk, in world units. */
        const val DEPTH_BAND = 320f

        const val LEAVE_TIME = 1.5f
    }
}
