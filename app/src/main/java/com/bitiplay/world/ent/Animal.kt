package com.bitiplay.world.ent

import android.graphics.Canvas
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.shade
import com.bitiplay.world.art.withAlpha
import com.bitiplay.world.audio.Sfx
import com.bitiplay.world.audio.Snd
import com.bitiplay.world.core.TAU
import com.bitiplay.world.core.chance
import com.bitiplay.world.core.clamp
import com.bitiplay.world.core.rnd
import com.bitiplay.world.engine.Entity
import com.bitiplay.world.engine.Scene
import com.bitiplay.world.engine.Z
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class AEar { ROUND, POINTY, FLOPPY, LONG, TINY, TUFT, NONE }
enum class ASnout { MUZZLE, PIG, BEAK, ROUND, WIDE, TRUNK, LONGBEAK }
enum class ATail { SHORT, TUFT, LONG, CURLY, FAN, PUFF, NONE }
enum class AHorn { NONE, COW, GOAT, OSSICONE, MANE }
enum class APattern { NONE, SPOTS, PATCHES, STRIPES, WOOL, GIRAFFE, TUXEDO }

/**
 * One rig drives every species: an ellipse body on four (or two) legs, a neck,
 * a head, and a handful of swappable parts. Tuning numbers rather than writing
 * bespoke art keeps 21 animals consistent and cheap.
 */
enum class AnimalKind(
    val label: String,
    val body: Int,
    val belly: Int,
    val bodyRX: Float,
    val bodyRY: Float,
    val legLen: Float,
    val legThick: Float,
    val neck: Float,
    val headR: Float,
    val ear: AEar,
    val snout: ASnout,
    val tail: ATail,
    val horn: AHorn = AHorn.NONE,
    val pattern: APattern = APattern.NONE,
    val patColor: Int = 0,
    val biped: Boolean = false,
    val roam: Float = 240f,
    val speed: Float = 95f,
    val voice: Snd = Snd.POP,
    val special: Int = 0
) {
    DOG("dog", 0xFFC9975C.toInt(), 0xFFF0DFC0.toInt(), 60f, 40f, 40f, 16f, 20f, 34f,
        AEar.FLOPPY, ASnout.MUZZLE, ATail.LONG, speed = 130f, voice = Snd.WOOF),
    CAT("cat", 0xFF9A9DA6.toInt(), 0xFFF0F0F2.toInt(), 54f, 34f, 34f, 13f, 20f, 30f,
        AEar.POINTY, ASnout.MUZZLE, ATail.LONG, speed = 110f, voice = Snd.MEOW),
    COW("cow", 0xFFFAF6EE.toInt(), 0xFFF2C8CE.toInt(), 92f, 62f, 54f, 22f, 26f, 48f,
        AEar.ROUND, ASnout.WIDE, ATail.TUFT, AHorn.COW, APattern.PATCHES, 0xFF4A4038.toInt(),
        roam = 200f, speed = 60f, voice = Snd.MOO),
    PIG("pig", 0xFFF2A9B6.toInt(), 0xFFFBD3DA.toInt(), 68f, 46f, 30f, 18f, 12f, 38f,
        AEar.POINTY, ASnout.PIG, ATail.CURLY, roam = 180f, speed = 80f, voice = Snd.OINK),
    SHEEP("sheep", 0xFFF6F2EA.toInt(), 0xFFE4DDD0.toInt(), 66f, 46f, 38f, 15f, 14f, 32f,
        AEar.FLOPPY, ASnout.ROUND, ATail.PUFF, AHorn.NONE, APattern.WOOL, 0xFFE8E2D6.toInt(),
        roam = 190f, speed = 70f, voice = Snd.BAA),
    HORSE("horse", 0xFF9A6B44.toInt(), 0xFFCBA47C.toInt(), 88f, 54f, 78f, 20f, 48f, 40f,
        AEar.POINTY, ASnout.MUZZLE, ATail.LONG, AHorn.MANE, patColor = 0xFF4A3A2C.toInt(),
        roam = 300f, speed = 150f, voice = Snd.NEIGH),
    GOAT("goat", 0xFFE8E1D2.toInt(), 0xFFF6F2E8.toInt(), 58f, 38f, 44f, 14f, 20f, 30f,
        AEar.FLOPPY, ASnout.MUZZLE, ATail.SHORT, AHorn.GOAT, roam = 200f, speed = 105f,
        voice = Snd.BAA),
    BUNNY("bunny", 0xFFEFE7DC.toInt(), 0xFFFFFFFF.toInt(), 44f, 32f, 22f, 12f, 12f, 26f,
        AEar.LONG, ASnout.ROUND, ATail.PUFF, roam = 190f, speed = 120f, voice = Snd.SQUEAK),
    CHICKEN("chicken", 0xFFFFFFFF.toInt(), 0xFFF2E4C8.toInt(), 34f, 38f, 26f, 8f, 10f, 22f,
        AEar.NONE, ASnout.BEAK, ATail.FAN, biped = true, roam = 150f, speed = 95f,
        voice = Snd.CLUCK),
    DUCK("duck", 0xFFF6F0E2.toInt(), 0xFFFFD24A.toInt(), 38f, 32f, 20f, 8f, 14f, 22f,
        AEar.NONE, ASnout.BEAK, ATail.SHORT, biped = true, roam = 150f, speed = 85f,
        voice = Snd.QUACK),
    GOOSE("goose", 0xFFFFFFFF.toInt(), 0xFFE8E2D0.toInt(), 42f, 34f, 26f, 9f, 36f, 22f,
        AEar.NONE, ASnout.BEAK, ATail.SHORT, biped = true, roam = 160f, speed = 90f,
        voice = Snd.HONK),
    ELEPHANT("elephant", 0xFF9BA6B2.toInt(), 0xFFBFC8D2.toInt(), 108f, 76f, 62f, 32f, 16f, 56f,
        AEar.ROUND, ASnout.TRUNK, ATail.TUFT, roam = 220f, speed = 55f, voice = Snd.TRUMPET),
    GIRAFFE("giraffe", 0xFFE8C067.toInt(), 0xFFF3DFA8.toInt(), 76f, 54f, 96f, 20f, 178f, 32f,
        AEar.TINY, ASnout.MUZZLE, ATail.TUFT, AHorn.OSSICONE, APattern.GIRAFFE, 0xFFB07C33.toInt(),
        roam = 220f, speed = 70f, voice = Snd.HONK),
    MONKEY("monkey", 0xFF9C6B49.toInt(), 0xFFDDB68C.toInt(), 44f, 36f, 30f, 12f, 14f, 30f,
        AEar.ROUND, ASnout.ROUND, ATail.LONG, roam = 200f, speed = 130f, voice = Snd.MONKEY),
    PENGUIN("penguin", 0xFF3A4450.toInt(), 0xFFFFFFFF.toInt(), 38f, 46f, 16f, 9f, 8f, 28f,
        AEar.NONE, ASnout.BEAK, ATail.SHORT, pattern = APattern.TUXEDO, patColor = 0xFFFFFFFF.toInt(),
        biped = true, roam = 170f, speed = 75f, voice = Snd.HONK),
    LION("lion", 0xFFE0A04E.toInt(), 0xFFF3D9A8.toInt(), 76f, 50f, 44f, 20f, 20f, 36f,
        AEar.ROUND, ASnout.MUZZLE, ATail.TUFT, AHorn.MANE, patColor = 0xFFB5702C.toInt(),
        roam = 220f, speed = 110f, voice = Snd.ROAR),
    ZEBRA("zebra", 0xFFF6F4EE.toInt(), 0xFFFFFFFF.toInt(), 80f, 50f, 70f, 18f, 42f, 36f,
        AEar.POINTY, ASnout.MUZZLE, ATail.TUFT, AHorn.MANE, APattern.STRIPES, 0xFF3A3A3A.toInt(),
        roam = 260f, speed = 140f, voice = Snd.NEIGH),
    PARROT("parrot", 0xFFE0483F.toInt(), 0xFF4EA0E0.toInt(), 28f, 34f, 18f, 7f, 10f, 20f,
        AEar.NONE, ASnout.LONGBEAK, ATail.FAN, biped = true, roam = 60f, speed = 60f,
        voice = Snd.PARROT),
    SEAGULL("seagull", 0xFFFFFFFF.toInt(), 0xFFEDEDED.toInt(), 30f, 26f, 20f, 7f, 12f, 18f,
        AEar.NONE, ASnout.BEAK, ATail.SHORT, biped = true, roam = 200f, speed = 110f,
        voice = Snd.SEAGULL),
    PIGEON("pigeon", 0xFF8C93A0.toInt(), 0xFFB9BFC9.toInt(), 26f, 24f, 16f, 6f, 12f, 16f,
        AEar.NONE, ASnout.BEAK, ATail.FAN, biped = true, roam = 170f, speed = 100f,
        voice = Snd.PIGEON),
    CRAB("crab", 0xFFE8573F.toInt(), 0xFFF08A72.toInt(), 44f, 26f, 16f, 7f, 0f, 0f,
        AEar.NONE, ASnout.ROUND, ATail.NONE, roam = 220f, speed = 90f, voice = Snd.CLICK,
        special = 1),
    TURTLE("turtle", 0xFF6DA85C.toInt(), 0xFFC7A96B.toInt(), 56f, 32f, 16f, 12f, 18f, 24f,
        AEar.NONE, ASnout.ROUND, ATail.SHORT, roam = 120f, speed = 34f, voice = Snd.SQUEAK,
        special = 2)
}

class Animal(val kind: AnimalKind, startX: Float) : Entity() {

    private val homeX = startX

    /** How far it wanders from home. Penned animals get a small value. */
    var roam = kind.roam

    private var mode = 0        // 0 idle, 1 walk, 2 eat, 3 happy
    private var modeT = rnd(0.6f, 2.4f)
    private var dir = if (chance(0.5f)) 1f else -1f
    private var walkT = rnd(0f, 6f)
    private var eatBob = 0f
    private var hop = 0f
    private val seed = (startX.toInt() * 31) and 0xFFFF

    init {
        x = startX
        z = Z.MAIN
        tappable = true
        val h = kind.legLen + kind.bodyRY * 2f + kind.neck + kind.headR * 2f
        hitW = kind.bodyRX * 2.4f + 40f
        hitH = h + 40f
        hitCY = -h * 0.5f
        useRange = kind.bodyRX + 130f
        cullPad = kind.bodyRX * 2f + 260f
    }

    override fun approachOffset(): Float = kind.bodyRX + 70f

    fun pen(halfWidth: Float): Animal {
        roam = halfWidth
        return this
    }

    override fun update(dt: Float, scene: Scene) {
        modeT -= dt
        if (hop > 0f) hop -= dt

        when (mode) {
            2, 3 -> if (modeT <= 0f) {
                mode = 0
                modeT = rnd(1f, 2.6f)
            }
            else -> {
                if (modeT <= 0f) {
                    mode = if (chance(0.55f)) 1 else 0
                    modeT = if (mode == 1) rnd(0.9f, 2.6f) else rnd(1.2f, 3.4f)
                    dir = if (chance(0.5f)) 1f else -1f
                }
                if (mode == 1) {
                    val off = scene.delta(homeX, x)
                    if (off > roam) dir = -1f
                    if (off < -roam) dir = 1f
                    facing = if (dir > 0f) 1 else -1
                    x = scene.wrapX(x + dir * kind.speed * dt)
                    walkT += dt * (kind.speed / 18f)
                }
            }
        }
        if (mode == 2) eatBob = 1f else eatBob = clamp(eatBob - dt * 2.5f, 0f, 1f)
    }

    override fun caption(ch: Character): String =
        if (ch.carried?.kind?.food == true) "feed the ${kind.label}" else "pet the ${kind.label}"

    override fun onUse(ch: Character, scene: Scene): Boolean {
        ch.faceToward(scene, x)
        val held = ch.carried
        if (held != null && held.kind.food) {
            ch.consumeCarried(scene)
            ch.startAct(Act.FEED, 0.7f)
            mode = 2
            modeT = 1.5f
            scene.fx.crumbs(x + facing * kind.bodyRX * 0.7f, -kind.legLen - 10f, level, C.CREAM, 8)
            scene.fx.hearts(x, -(kind.legLen + kind.bodyRY * 2f + kind.neck + kind.headR), level, 4)
            Sfx.play(kind.voice)
            return true
        }
        ch.startAct(Act.PET, 0.7f)
        mode = 3
        modeT = 1.0f
        hop = 0.5f
        scene.fx.hearts(x, -(kind.legLen + kind.bodyRY * 2f + kind.neck + kind.headR), level, 3)
        Sfx.play(kind.voice)
        return true
    }

    // ------------------------------------------------------------------- art

    private val bodyCY: Float get() = -(kind.legLen + kind.bodyRY)
    private val headCX: Float get() = kind.bodyRX * 0.78f
    private val headCY: Float
        get() = bodyCY - kind.bodyRY * 0.45f - kind.neck - kind.headR * 0.45f

    override fun draw(c: Canvas) {
        when (kind.special) {
            1 -> {
                drawCrab(c)
                return
            }
            2 -> {
                drawTurtle(c)
                return
            }
        }
        val hopY = if (hop > 0f) -abs(sin(hop * TAU)) * 26f else 0f
        c.save()
        c.translate(0f, hopY)
        // Species whose silhouette the shared rig cannot produce get their own.
        when (kind) {
            AnimalKind.HORSE, AnimalKind.ZEBRA -> drawEquine(c)
            AnimalKind.GIRAFFE -> drawGiraffe(c)
            AnimalKind.ELEPHANT -> drawElephant(c)
            AnimalKind.LION -> drawLion(c)
            AnimalKind.MONKEY -> drawMonkey(c)
            else -> if (kind.biped) drawBiped(c) else drawQuadruped(c)
        }
        c.restore()
    }

    // ------------------------------------------------------- bespoke species

    private val trim: Int
        get() = if (kind.patColor != 0) kind.patColor else shade(kind.body, 0.66f)

    /** Jointed leg with a knee and a hoof, for horses, zebras and giraffes. */
    private fun hoofLeg(c: Canvas, cx: Float, top: Float, sw: Float, col: Int, thick: Float) {
        val kneeY = top + (0f - top) * 0.45f
        val kx = cx + sw * 7f
        val fx = cx + sw * 18f
        D.capsule(c, cx, top, kx, kneeY, thick, col)
        D.capsule(c, kx, kneeY, fx, -6f, thick * 0.66f, col)
        D.rectC(c, fx, -6f, thick * 0.95f, 12f, shade(col, 0.62f), 4f)
    }

    private fun drawEquine(c: Canvas) {
        val bx = kind.bodyRX
        val by = kind.bodyRY
        val col = kind.body
        val far = shade(col, 0.84f)
        val bcy = -(kind.legLen + by) + breathe
        val sw = swing
        D.shadow(c, 0f, 0f, bx * 1.15f, bx * 0.28f)

        hoofLeg(c, -bx * 0.5f, bcy + by * 0.4f, -sw, far, kind.legThick)
        hoofLeg(c, bx * 0.6f, bcy + by * 0.4f, sw, far, kind.legThick)

        // tail hanging from a high dock
        val wag = sin(t * 2.2f + seed) * 0.2f
        D.stroke(c, trim, by * 0.4f) { p ->
            p.moveTo(-bx * 0.88f, bcy - by * 0.45f)
            p.quadTo(-bx * 1.3f, bcy + by * 0.3f + wag * 50f, -bx * 1.05f, bcy + by * 1.6f)
        }

        // deep barrel, higher at the withers than the croup
        D.shape(c, col) { p ->
            p.moveTo(-bx * 0.92f, bcy - by * 0.5f)
            p.quadTo(0f, bcy - by * 1.02f, bx * 0.88f, bcy - by * 0.78f)
            p.quadTo(bx * 1.12f, bcy + by * 0.3f, bx * 0.7f, bcy + by * 0.92f)
            p.quadTo(0f, bcy + by * 1.14f, -bx * 0.86f, bcy + by * 0.78f)
        }
        D.oval(c, bx * 0.1f, bcy + by * 0.5f, bx * 0.55f, by * 0.4f, kind.belly)
        drawPattern(c, bcy)

        hoofLeg(c, -bx * 0.36f, bcy + by * 0.4f, sw, col, kind.legThick)
        hoofLeg(c, bx * 0.76f, bcy + by * 0.4f, -sw, col, kind.legThick)

        // neck wedge up to the poll
        val r = kind.headR
        val nx = bx * 0.7f
        val ny = bcy - by * 0.62f
        val hx = nx + kind.neck * 0.5f
        val hy = ny - kind.neck * 0.95f - r * 0.2f + breathe + eatBob * (kind.neck + kind.legLen) * 0.55f
        D.shape(c, col) { p ->
            p.moveTo(nx - bx * 0.26f, ny + by * 0.42f)
            p.lineTo(hx - r * 0.62f, hy + r * 0.4f)
            p.lineTo(hx + r * 0.34f, hy + r * 0.52f)
            p.lineTo(nx + bx * 0.32f, ny + by * 0.6f)
        }
        D.stroke(c, trim, r * 0.38f) { p ->
            p.moveTo(nx - bx * 0.14f, ny + by * 0.12f)
            p.quadTo((nx + hx) * 0.5f - 10f, (ny + hy) * 0.5f - 14f, hx - r * 0.42f, hy - r * 0.45f)
        }

        c.save()
        c.translate(hx, hy)
        c.rotate(20f)
        D.tri(c, -r * 0.52f, -r * 0.3f, -r * 0.16f, -r * 0.36f, -r * 0.4f, -r * 1.0f, shade(col, 0.9f))
        D.tri(c, -r * 0.26f, -r * 0.34f, r * 0.06f, -r * 0.36f, -r * 0.12f, -r * 1.02f, col)
        D.oval(c, 0f, 0f, r * 1.12f, r * 0.6f, col)
        D.oval(c, r * 0.92f, r * 0.1f, r * 0.4f, r * 0.38f, shade(col, 1.1f))
        D.circle(c, r * 1.1f, r * 0.08f, r * 0.11f, C.BLACK)
        D.circle(c, -r * 0.2f, -r * 0.26f, r * 0.15f, C.BLACK)
        c.restore()
    }

    private fun drawGiraffe(c: Canvas) {
        val bx = kind.bodyRX
        val by = kind.bodyRY
        val col = kind.body
        val far = shade(col, 0.85f)
        val bcy = -(kind.legLen + by) + breathe
        val sw = swing
        D.shadow(c, 0f, 0f, bx * 1.05f, bx * 0.26f)

        hoofLeg(c, -bx * 0.46f, bcy + by * 0.4f, -sw, far, kind.legThick)
        hoofLeg(c, bx * 0.56f, bcy + by * 0.4f, sw, far, kind.legThick)
        D.stroke(c, trim, by * 0.22f) { p ->
            p.moveTo(-bx * 0.88f, bcy - by * 0.3f)
            p.quadTo(-bx * 1.16f, bcy + by * 0.6f, -bx * 1.0f, bcy + by * 1.5f)
        }
        D.circle(c, -bx * 1.0f, bcy + by * 1.6f, by * 0.24f, trim)

        // back slopes down from the shoulder
        D.shape(c, col) { p ->
            p.moveTo(-bx * 0.9f, bcy - by * 0.1f)
            p.quadTo(-bx * 0.2f, bcy - by * 0.78f, bx * 0.9f, bcy - by * 0.98f)
            p.quadTo(bx * 1.1f, bcy + by * 0.3f, bx * 0.66f, bcy + by * 0.9f)
            p.quadTo(0f, bcy + by * 1.06f, -bx * 0.84f, bcy + by * 0.62f)
        }
        drawPattern(c, bcy)

        hoofLeg(c, -bx * 0.32f, bcy + by * 0.4f, sw, col, kind.legThick)
        hoofLeg(c, bx * 0.72f, bcy + by * 0.4f, -sw, col, kind.legThick)

        // the long neck, tapering
        val r = kind.headR
        val nx = bx * 0.66f
        val ny = bcy - by * 0.8f
        val dip = eatBob * (kind.neck * 0.75f)
        val hx = nx + kind.neck * 0.3f
        val hy = ny - kind.neck + dip
        D.shape(c, col) { p ->
            p.moveTo(nx - bx * 0.3f, ny + by * 0.5f)
            p.lineTo(hx - r * 0.5f, hy + r * 0.3f)
            p.lineTo(hx + r * 0.34f, hy + r * 0.4f)
            p.lineTo(nx + bx * 0.34f, ny + by * 0.7f)
        }
        // neck spots follow the taper
        for (k in 0 until 7) {
            val u = (k + 0.5f) / 7f
            val sxp = nx + (hx - nx) * u
            val syp = ny + (hy - ny) * u
            D.ngon(c, sxp - r * 0.06f, syp, r * 0.24f * (1f - u * 0.35f), 6, kind.patColor, u * 2f)
        }
        D.stroke(c, trim, r * 0.26f) { p ->
            p.moveTo(nx - bx * 0.16f, ny + by * 0.2f)
            p.quadTo((nx + hx) * 0.5f - 12f, (ny + hy) * 0.5f, hx - r * 0.44f, hy - r * 0.2f)
        }

        c.save()
        c.translate(hx, hy)
        c.rotate(14f)
        // ossicones
        D.capsule(c, -r * 0.24f, -r * 0.35f, -r * 0.32f, -r * 1.15f, r * 0.16f, col)
        D.circle(c, -r * 0.32f, -r * 1.2f, r * 0.2f, trim)
        D.capsule(c, r * 0.1f, -r * 0.4f, r * 0.04f, -r * 1.2f, r * 0.16f, col)
        D.circle(c, r * 0.04f, -r * 1.25f, r * 0.2f, trim)
        D.oval(c, 0f, 0f, r * 1.0f, r * 0.58f, col)
        D.tri(c, -r * 0.5f, -r * 0.1f, -r * 0.2f, -r * 0.15f, -r * 1.0f, -r * 0.5f, shade(col, 0.9f))
        D.oval(c, r * 0.86f, r * 0.14f, r * 0.36f, r * 0.34f, shade(col, 1.12f))
        D.circle(c, r * 1.0f, r * 0.12f, r * 0.1f, C.BLACK)
        D.circle(c, -r * 0.06f, -r * 0.24f, r * 0.16f, C.BLACK)
        c.restore()
    }

    private fun drawElephant(c: Canvas) {
        val bx = kind.bodyRX
        val by = kind.bodyRY
        val col = kind.body
        val far = shade(col, 0.85f)
        val bcy = -(kind.legLen + by) + breathe
        val sw = swing
        D.shadow(c, 0f, 0f, bx * 1.1f, bx * 0.28f)

        // pillar legs
        for (p in 0 until 2) {
            val cc = if (p == 0) far else col
            val o = if (p == 0) 0f else 26f
            val s = if (p == 0) -sw else sw
            D.capsule(c, -bx * 0.5f + o, bcy + by * 0.5f, -bx * 0.5f + o + s * 8f, -10f, kind.legThick, cc)
            D.capsule(c, bx * 0.55f + o, bcy + by * 0.5f, bx * 0.55f + o - s * 8f, -10f, kind.legThick, cc)
            D.oval(c, -bx * 0.5f + o + s * 8f, -8f, kind.legThick * 0.62f, 10f, shade(cc, 0.72f))
            D.oval(c, bx * 0.55f + o - s * 8f, -8f, kind.legThick * 0.62f, 10f, shade(cc, 0.72f))
        }

        D.stroke(c, col, by * 0.14f) { p ->
            p.moveTo(-bx * 0.92f, bcy - by * 0.2f)
            p.quadTo(-bx * 1.12f, bcy + by * 0.5f, -bx * 1.02f, bcy + by * 1.1f)
        }

        // huge rounded body
        D.oval(c, 0f, bcy, bx, by, col)
        D.oval(c, bx * 0.05f, bcy + by * 0.45f, bx * 0.66f, by * 0.44f, kind.belly)

        val r = kind.headR
        val hx = bx * 0.78f
        val hy = bcy - by * 0.5f + breathe
        // ear behind the head
        D.oval(c, hx - r * 0.5f, hy + r * 0.15f, r * 0.92f, r * 1.05f, shade(col, 0.8f))
        D.circle(c, hx, hy, r, col)
        D.oval(c, hx - r * 0.15f, hy + r * 0.1f, r * 0.85f, r * 0.98f, shade(col, 1.06f))
        // trunk
        val curl = sin(t * 1.4f + seed) * 0.16f + eatBob * 0.5f
        D.stroke(c, col, r * 0.42f) { p ->
            p.moveTo(hx + r * 0.55f, hy + r * 0.25f)
            p.quadTo(hx + r * 1.5f, hy + r * 1.1f, hx + r * (1.15f + curl), hy + r * 2.15f)
        }
        D.circle(c, hx + r * (1.15f + curl), hy + r * 2.25f, r * 0.19f, shade(col, 0.86f))
        // tusks
        D.stroke(c, C.OFF_WHITE, r * 0.13f) { p ->
            p.moveTo(hx + r * 0.42f, hy + r * 0.55f)
            p.quadTo(hx + r * 0.95f, hy + r * 1.0f, hx + r * 1.05f, hy + r * 0.6f)
        }
        D.circle(c, hx + r * 0.34f, hy - r * 0.18f, r * 0.15f, C.BLACK)
        D.circle(c, hx - r * 0.28f, hy - r * 0.2f, r * 0.13f, C.BLACK)
    }

    private fun drawLion(c: Canvas) {
        val bx = kind.bodyRX
        val by = kind.bodyRY
        val col = kind.body
        val far = shade(col, 0.85f)
        val bcy = -(kind.legLen + by) + breathe
        val sw = swing
        D.shadow(c, 0f, 0f, bx * 1.15f, bx * 0.3f)

        leg(c, -bx * 0.55f, -sw, far)
        leg(c, bx * 0.6f, sw, far)

        // tufted tail
        val wag = sin(t * 2.6f + seed) * 0.3f
        D.stroke(c, col, kind.legThick * 0.42f) { p ->
            p.moveTo(-bx * 0.88f, bcy - by * 0.1f)
            p.quadTo(-bx * 1.35f, bcy - by * 0.5f + wag * 40f, -bx * 1.1f, bcy - by * 1.0f + wag * 30f)
        }
        D.circle(c, -bx * 1.1f, bcy - by * 1.1f + wag * 30f, kind.legThick * 0.5f, trim)

        D.shape(c, col) { p ->
            p.moveTo(-bx * 0.92f, bcy - by * 0.35f)
            p.quadTo(0f, bcy - by * 0.95f, bx * 0.85f, bcy - by * 0.6f)
            p.quadTo(bx * 1.08f, bcy + by * 0.4f, bx * 0.7f, bcy + by * 0.95f)
            p.quadTo(0f, bcy + by * 1.12f, -bx * 0.88f, bcy + by * 0.8f)
        }
        D.oval(c, bx * 0.08f, bcy + by * 0.5f, bx * 0.58f, by * 0.42f, kind.belly)

        leg(c, -bx * 0.4f, sw, col)
        leg(c, bx * 0.74f, -sw, col)

        val r = kind.headR
        val hx = bx * 0.86f
        val hy = bcy - by * 0.75f + breathe
        // the mane is the whole silhouette
        for (k in 0 until 11) {
            val a = k * TAU / 11f
            D.circle(c, hx + cos(a) * r * 0.92f, hy + sin(a) * r * 0.92f, r * 0.5f, trim)
        }
        D.circle(c, hx, hy, r * 1.05f, shade(trim, 1.12f))
        D.circle(c, hx, hy, r * 0.78f, col)
        D.oval(c, hx + r * 0.34f, hy + r * 0.36f, r * 0.5f, r * 0.36f, kind.belly)
        D.tri(c, hx + r * 0.16f, hy + r * 0.2f, hx + r * 0.52f, hy + r * 0.2f, hx + r * 0.34f, hy + r * 0.4f, C.BLACK)
        D.circle(c, hx + r * 0.34f, hy - r * 0.22f, r * 0.13f, C.BLACK)
        D.circle(c, hx - r * 0.22f, hy - r * 0.24f, r * 0.12f, C.BLACK)
        D.line(c, hx + r * 0.5f, hy + r * 0.3f, hx + r * 1.1f, hy + r * 0.15f, withAlpha(C.BLACK, 110), 3f)
        D.line(c, hx + r * 0.5f, hy + r * 0.44f, hx + r * 1.12f, hy + r * 0.5f, withAlpha(C.BLACK, 110), 3f)
    }

    private fun drawMonkey(c: Canvas) {
        val bx = kind.bodyRX
        val by = kind.bodyRY
        val col = kind.body
        val far = shade(col, 0.84f)
        val bcy = -(kind.legLen + by) + breathe
        val sw = swing
        val r = kind.headR
        D.shadow(c, 0f, 0f, bx * 1.15f, bx * 0.32f)

        // curled prehensile tail
        val curl = sin(t * 1.8f + seed) * 0.2f
        D.stroke(c, col, kind.legThick * 0.5f) { p ->
            p.moveTo(-bx * 0.7f, bcy + by * 0.2f)
            p.quadTo(-bx * 1.7f, bcy + by * 0.5f, -bx * 1.55f, bcy - by * 0.9f + curl * 30f)
            p.quadTo(-bx * 1.4f, bcy - by * 1.7f, -bx * 0.85f, bcy - by * 1.35f + curl * 20f)
        }

        // far limbs
        D.capsule(c, -bx * 0.3f, bcy + by * 0.3f, -bx * 0.55f + sw * 10f, -6f, kind.legThick, far)
        D.capsule(c, bx * 0.3f, bcy - by * 0.4f, bx * 0.8f - sw * 14f, bcy + by * 0.9f, kind.legThick * 0.85f, far)

        // hunched body
        D.oval(c, 0f, bcy, bx, by, col)
        D.oval(c, bx * 0.06f, bcy + by * 0.34f, bx * 0.62f, by * 0.56f, kind.belly)

        // near limbs: long arms reaching the ground
        D.capsule(c, -bx * 0.15f, bcy + by * 0.35f, -bx * 0.35f - sw * 10f, -6f, kind.legThick, col)
        D.oval(c, -bx * 0.35f - sw * 10f, -6f, kind.legThick * 0.6f, 8f, shade(col, 0.76f))
        D.capsule(c, bx * 0.42f, bcy - by * 0.35f, bx * 0.92f + sw * 14f, bcy + by * 1.0f, kind.legThick * 0.9f, col)
        D.circle(c, bx * 0.95f + sw * 14f, bcy + by * 1.05f, kind.legThick * 0.55f, shade(col, 0.8f))

        // head: round, ears out to the sides, flat pale face
        val hx = bx * 0.62f
        val hy = bcy - by * 0.95f + breathe + eatBob * by * 0.7f
        D.circle(c, hx - r * 0.95f, hy + r * 0.1f, r * 0.42f, col)
        D.circle(c, hx - r * 0.95f, hy + r * 0.1f, r * 0.24f, kind.belly)
        D.circle(c, hx + r * 0.95f, hy + r * 0.1f, r * 0.42f, col)
        D.circle(c, hx + r * 0.95f, hy + r * 0.1f, r * 0.24f, kind.belly)
        D.circle(c, hx, hy, r, col)
        D.oval(c, hx + r * 0.1f, hy + r * 0.18f, r * 0.76f, r * 0.72f, kind.belly)
        D.oval(c, hx + r * 0.14f, hy + r * 0.46f, r * 0.4f, r * 0.24f, shade(kind.belly, 0.9f))
        D.circle(c, hx + r * 0.02f, hy + r * 0.34f, r * 0.07f, C.BLACK)
        D.circle(c, hx + r * 0.3f, hy + r * 0.34f, r * 0.07f, C.BLACK)
        D.circle(c, hx - r * 0.2f, hy - r * 0.16f, r * 0.15f, C.BLACK)
        D.circle(c, hx + r * 0.42f, hy - r * 0.16f, r * 0.15f, C.BLACK)
        D.arcLine(c, hx + r * 0.16f, hy + r * 0.52f, r * 0.28f, r * 0.16f, 20f, 140f, C.BLACK, 3f)
    }

    private val swing: Float get() = if (mode == 1) sin(walkT) else 0f

    private val breathe: Float get() = sin(t * 1.9f + seed) * 1.6f

    private fun leg(c: Canvas, cx: Float, sw: Float, col: Int) {
        val fx = cx + sw * 12f
        D.capsule(c, cx, bodyCY + kind.bodyRY * 0.4f, fx, -kind.legThick * 0.35f, kind.legThick, col)
        D.oval(c, fx, -kind.legThick * 0.3f, kind.legThick * 0.72f, kind.legThick * 0.42f, shade(col, 0.8f))
    }

    private fun drawQuadruped(c: Canvas) {
        D.shadow(c, 0f, 0f, kind.bodyRX * 1.15f, kind.bodyRX * 0.3f)
        val far = shade(kind.body, 0.85f)
        val bcy = bodyCY + breathe

        leg(c, -kind.bodyRX * 0.55f, -swing, far)
        leg(c, kind.bodyRX * 0.6f, swing, far)

        drawTail(c, bcy)

        D.oval(c, 0f, bcy, kind.bodyRX, kind.bodyRY, kind.body)
        D.oval(c, kind.bodyRX * 0.08f, bcy + kind.bodyRY * 0.42f,
            kind.bodyRX * 0.66f, kind.bodyRY * 0.48f, kind.belly)
        drawPattern(c, bcy)

        leg(c, -kind.bodyRX * 0.42f, swing, kind.body)
        leg(c, kind.bodyRX * 0.72f, -swing, kind.body)

        val dip = eatBob * (kind.neck * 0.4f + kind.legLen * 0.55f)
        val hx = headCX
        val hy = headCY + breathe + dip

        if (kind.neck > 4f) {
            D.capsule(
                c, kind.bodyRX * 0.42f, bcy - kind.bodyRY * 0.35f,
                hx - kind.headR * 0.15f, hy + kind.headR * 0.55f,
                kind.headR * 1.05f, kind.body
            )
        }
        if (kind.horn == AHorn.MANE) {
            D.circle(c, hx - kind.headR * 0.25f, hy, kind.headR * 1.42f, kind.patColor)
        }
        drawHead(c, hx, hy)
    }

    private fun drawBiped(c: Canvas) {
        D.shadow(c, 0f, 0f, kind.bodyRX * 1.2f, kind.bodyRX * 0.34f)
        val bcy = -(kind.legLen + kind.bodyRY) + breathe
        // legs
        val sw = swing
        D.capsule(c, -8f + sw * 8f, bcy + kind.bodyRY * 0.5f, -10f + sw * 14f, -3f, kind.legThick, C.ORANGE)
        D.capsule(c, 8f - sw * 8f, bcy + kind.bodyRY * 0.5f, 10f - sw * 14f, -3f, kind.legThick, C.ORANGE_DARK)
        D.oval(c, -10f + sw * 14f + 4f, -3f, 12f, 5f, C.ORANGE_DARK)
        D.oval(c, 10f - sw * 14f + 4f, -3f, 12f, 5f, C.ORANGE_DARK)

        drawTail(c, bcy)
        D.oval(c, 0f, bcy, kind.bodyRX, kind.bodyRY, kind.body)
        if (kind.pattern == APattern.TUXEDO) {
            D.oval(c, kind.bodyRX * 0.12f, bcy + kind.bodyRY * 0.12f,
                kind.bodyRX * 0.68f, kind.bodyRY * 0.78f, kind.patColor)
        } else {
            D.oval(c, kind.bodyRX * 0.1f, bcy + kind.bodyRY * 0.34f,
                kind.bodyRX * 0.6f, kind.bodyRY * 0.5f, kind.belly)
        }
        // wing
        D.oval(c, kind.bodyRX * 0.18f, bcy, kind.bodyRX * 0.5f, kind.bodyRY * 0.6f, shade(kind.body, 0.88f))

        val dip = eatBob * kind.bodyRY * 0.8f
        val hy = bcy - kind.bodyRY - kind.neck - kind.headR * 0.4f + dip
        if (kind.neck > 4f) {
            D.capsule(c, 0f, bcy - kind.bodyRY * 0.4f, kind.headR * 0.3f, hy + kind.headR * 0.5f,
                kind.headR * 0.9f, kind.body)
        }
        drawHead(c, kind.headR * 0.32f, hy)
        if (kind == AnimalKind.CHICKEN) {
            D.circle(c, kind.headR * 0.1f, hy - kind.headR * 0.95f, 7f, C.RED)
            D.circle(c, kind.headR * 0.5f, hy - kind.headR * 1.05f, 6f, C.RED)
        }
    }

    private fun drawHead(c: Canvas, hx: Float, hy: Float) {
        drawEars(c, hx, hy)
        D.circle(c, hx, hy, kind.headR, kind.body)
        if (kind.horn != AHorn.NONE && kind.horn != AHorn.MANE) drawHorns(c, hx, hy)

        val r = kind.headR
        when (kind.snout) {
            ASnout.MUZZLE -> {
                D.oval(c, hx + r * 0.55f, hy + r * 0.35f, r * 0.55f, r * 0.4f, kind.belly)
                D.oval(c, hx + r * 0.9f, hy + r * 0.18f, r * 0.2f, r * 0.16f, C.BLACK)
            }
            ASnout.WIDE -> {
                D.oval(c, hx + r * 0.6f, hy + r * 0.4f, r * 0.7f, r * 0.5f, kind.belly)
                D.circle(c, hx + r * 0.8f, hy + r * 0.3f, r * 0.11f, shade(kind.belly, 0.6f))
                D.circle(c, hx + r * 0.4f, hy + r * 0.42f, r * 0.11f, shade(kind.belly, 0.6f))
            }
            ASnout.PIG -> {
                D.oval(c, hx + r * 0.75f, hy + r * 0.3f, r * 0.36f, r * 0.3f, shade(kind.belly, 0.94f))
                D.circle(c, hx + r * 0.66f, hy + r * 0.3f, r * 0.08f, C.BLACK)
                D.circle(c, hx + r * 0.88f, hy + r * 0.3f, r * 0.08f, C.BLACK)
            }
            ASnout.ROUND -> D.oval(c, hx + r * 0.7f, hy + r * 0.25f, r * 0.18f, r * 0.15f, C.BLACK)
            ASnout.BEAK -> D.tri(
                c, hx + r * 0.5f, hy - r * 0.15f,
                hx + r * 1.6f, hy + r * 0.1f, hx + r * 0.5f, hy + r * 0.4f, C.ORANGE
            )
            ASnout.LONGBEAK -> {
                D.tri(c, hx + r * 0.3f, hy - r * 0.4f, hx + r * 1.5f, hy + r * 0.2f,
                    hx + r * 0.35f, hy + r * 0.5f, C.YELLOW_DARK)
                D.arcLine(c, hx + r * 0.6f, hy + r * 0.15f, r * 0.6f, r * 0.5f, -60f, 110f, C.BLACK, 4f)
            }
            ASnout.TRUNK -> {
                D.stroke(c, kind.body, r * 0.38f) { p ->
                    p.moveTo(hx + r * 0.5f, hy + r * 0.3f)
                    p.quadTo(hx + r * 1.5f, hy + r * 1.2f, hx + r * 1.1f, hy + r * 2.2f)
                }
                D.circle(c, hx + r * 1.08f, hy + r * 2.3f, r * 0.2f, shade(kind.body, 0.85f))
                D.capsule(c, hx + r * 0.55f, hy + r * 0.85f, hx + r * 0.6f, hy + r * 1.3f, r * 0.14f, C.OFF_WHITE)
            }
        }

        val open = 1f
        D.eye(c, hx + r * 0.42f, hy - r * 0.22f, r * 0.19f, open)
        if (!kind.biped) D.eye(c, hx - r * 0.3f, hy - r * 0.2f, r * 0.17f, open)
    }

    private fun drawEars(c: Canvas, hx: Float, hy: Float) {
        val r = kind.headR
        when (kind.ear) {
            AEar.ROUND -> {
                D.circle(c, hx - r * 0.55f, hy - r * 0.75f, r * 0.42f, shade(kind.body, 0.9f))
                D.circle(c, hx + r * 0.5f, hy - r * 0.8f, r * 0.42f, kind.body)
            }
            AEar.TINY -> {
                D.circle(c, hx - r * 0.4f, hy - r * 0.85f, r * 0.22f, kind.body)
                D.circle(c, hx + r * 0.42f, hy - r * 0.9f, r * 0.22f, kind.body)
            }
            AEar.POINTY -> {
                D.tri(c, hx - r * 0.85f, hy - r * 0.4f, hx - r * 0.15f, hy - r * 0.7f,
                    hx - r * 0.6f, hy - r * 1.3f, shade(kind.body, 0.9f))
                D.tri(c, hx + r * 0.8f, hy - r * 0.45f, hx + r * 0.1f, hy - r * 0.72f,
                    hx + r * 0.55f, hy - r * 1.32f, kind.body)
            }
            AEar.LONG -> {
                D.oval(c, hx - r * 0.35f, hy - r * 1.5f, r * 0.25f, r * 0.95f, shade(kind.body, 0.92f))
                D.oval(c, hx + r * 0.42f, hy - r * 1.55f, r * 0.25f, r * 0.95f, kind.body)
                D.oval(c, hx + r * 0.42f, hy - r * 1.55f, r * 0.11f, r * 0.62f, C.PINK)
            }
            AEar.FLOPPY -> {
                D.oval(c, hx - r * 0.9f, hy + r * 0.05f, r * 0.3f, r * 0.72f, shade(kind.body, 0.86f))
                D.oval(c, hx + r * 0.88f, hy + r * 0.02f, r * 0.3f, r * 0.72f, shade(kind.body, 0.94f))
            }
            AEar.TUFT -> {
                D.tri(c, hx - r * 0.5f, hy - r * 0.8f, hx - r * 0.1f, hy - r * 0.8f,
                    hx - r * 0.3f, hy - r * 1.4f, shade(kind.body, 0.8f))
            }
            AEar.NONE -> Unit
        }
    }

    private fun drawHorns(c: Canvas, hx: Float, hy: Float) {
        val r = kind.headR
        when (kind.horn) {
            AHorn.COW -> {
                D.stroke(c, C.CREAM, r * 0.2f) { p ->
                    p.moveTo(hx - r * 0.55f, hy - r * 0.7f)
                    p.quadTo(hx - r * 1.3f, hy - r * 1.1f, hx - r * 1.15f, hy - r * 0.45f)
                    p.moveTo(hx + r * 0.5f, hy - r * 0.75f)
                    p.quadTo(hx + r * 1.25f, hy - r * 1.15f, hx + r * 1.1f, hy - r * 0.5f)
                }
            }
            AHorn.GOAT -> {
                D.stroke(c, C.CREAM, r * 0.17f) { p ->
                    p.moveTo(hx - r * 0.3f, hy - r * 0.85f)
                    p.quadTo(hx - r * 0.7f, hy - r * 1.7f, hx - r * 0.05f, hy - r * 1.8f)
                    p.moveTo(hx + r * 0.35f, hy - r * 0.85f)
                    p.quadTo(hx + r * 0.75f, hy - r * 1.7f, hx + r * 0.1f, hy - r * 1.85f)
                }
            }
            AHorn.OSSICONE -> {
                D.capsule(c, hx - r * 0.3f, hy - r * 0.8f, hx - r * 0.38f, hy - r * 1.5f, r * 0.16f, kind.patColor)
                D.circle(c, hx - r * 0.38f, hy - r * 1.55f, r * 0.2f, C.BROWN)
                D.capsule(c, hx + r * 0.32f, hy - r * 0.82f, hx + r * 0.4f, hy - r * 1.5f, r * 0.16f, kind.patColor)
                D.circle(c, hx + r * 0.4f, hy - r * 1.55f, r * 0.2f, C.BROWN)
            }
            else -> Unit
        }
    }

    private fun drawTail(c: Canvas, bcy: Float) {
        val bx = -kind.bodyRX * 0.9f
        val wag = sin(t * 4f + seed) * 0.3f
        when (kind.tail) {
            ATail.SHORT -> D.oval(c, bx - 6f, bcy, kind.bodyRX * 0.2f, kind.bodyRY * 0.22f, shade(kind.body, 0.9f))
            ATail.PUFF -> D.circle(c, bx - 8f, bcy + kind.bodyRY * 0.2f, kind.bodyRY * 0.36f, kind.belly)
            ATail.CURLY -> D.stroke(c, shade(kind.body, 0.9f), 8f) { p ->
                p.moveTo(bx, bcy - 6f)
                p.quadTo(bx - 34f, bcy - 30f, bx - 12f, bcy - 34f)
                p.quadTo(bx + 4f, bcy - 36f, bx - 6f, bcy - 16f)
            }
            ATail.LONG -> D.stroke(c, shade(kind.body, 0.92f), kind.legThick * 0.62f) { p ->
                p.moveTo(bx, bcy)
                p.quadTo(bx - kind.bodyRX * 0.8f, bcy + wag * 40f,
                    bx - kind.bodyRX * 0.7f, bcy - kind.bodyRY * 1.2f + wag * 26f)
            }
            ATail.TUFT -> {
                D.capsule(c, bx, bcy - kind.bodyRY * 0.3f, bx - 18f, bcy + kind.bodyRY * 0.7f + wag * 12f,
                    kind.legThick * 0.35f, shade(kind.body, 0.9f))
                D.circle(c, bx - 20f, bcy + kind.bodyRY * 0.8f + wag * 12f, kind.legThick * 0.44f,
                    if (kind.patColor != 0) kind.patColor else shade(kind.body, 0.7f))
            }
            ATail.FAN -> {
                for (i in 0 until 4) {
                    val a = -0.35f - i * 0.28f
                    D.capsule(c, bx, bcy, bx - cos(a) * 46f, bcy + sin(a) * 46f, 12f, shade(kind.body, 0.88f + i * 0.04f))
                }
            }
            ATail.NONE -> Unit
        }
    }

    private fun drawPattern(c: Canvas, bcy: Float) {
        when (kind.pattern) {
            APattern.PATCHES -> {
                D.oval(c, -kind.bodyRX * 0.42f, bcy - kind.bodyRY * 0.28f,
                    kind.bodyRX * 0.34f, kind.bodyRY * 0.36f, kind.patColor)
                D.oval(c, kind.bodyRX * 0.36f, bcy + kind.bodyRY * 0.2f,
                    kind.bodyRX * 0.26f, kind.bodyRY * 0.3f, kind.patColor)
            }
            APattern.SPOTS -> {
                for (i in 0 until 4) {
                    val a = i * 1.7f + seed
                    D.circle(c, cos(a) * kind.bodyRX * 0.5f, bcy + sin(a) * kind.bodyRY * 0.5f,
                        kind.bodyRY * 0.16f, kind.patColor)
                }
            }
            APattern.STRIPES -> {
                for (i in -2..2) {
                    val sx = i * kind.bodyRX * 0.34f
                    val h = kind.bodyRY * (1f - abs(i) * 0.16f)
                    D.rectC(c, sx, bcy, kind.bodyRX * 0.12f, h * 1.75f, kind.patColor, 6f)
                }
            }
            APattern.GIRAFFE -> {
                for (i in 0 until 5) {
                    val a = i * 1.31f + seed
                    D.ngon(c, cos(a) * kind.bodyRX * 0.55f, bcy + sin(a) * kind.bodyRY * 0.55f,
                        kind.bodyRY * 0.22f, 6, kind.patColor, a)
                }
            }
            APattern.WOOL -> {
                for (i in 0 until 9) {
                    val a = i * TAU / 9f
                    D.circle(c, cos(a) * kind.bodyRX * 0.86f, bcy + sin(a) * kind.bodyRY * 0.86f,
                        kind.bodyRY * 0.35f, kind.patColor)
                }
                D.oval(c, 0f, bcy, kind.bodyRX * 0.8f, kind.bodyRY * 0.8f, kind.body)
            }
            APattern.TUXEDO, APattern.NONE -> Unit
        }
    }

    private fun drawCrab(c: Canvas) {
        val bob = sin(t * 3f + seed) * 3f
        D.shadow(c, 0f, 0f, 52f, 14f)
        val cy = -30f + bob
        for (i in 0 until 3) {
            val a = 0.5f + i * 0.42f
            D.capsule(c, -20f, cy, -20f - cos(a) * 40f, -4f, 6f, shade(kind.body, 0.85f))
            D.capsule(c, 20f, cy, 20f + cos(a) * 40f, -4f, 6f, shade(kind.body, 0.85f))
        }
        val claw = sin(t * 4f + seed) * 0.3f
        D.capsule(c, -34f, cy - 4f, -60f, cy - 22f, 9f, kind.body)
        D.oval(c, -66f, cy - 28f + claw * 6f, 16f, 12f, kind.belly)
        D.capsule(c, 34f, cy - 4f, 60f, cy - 22f, 9f, kind.body)
        D.oval(c, 66f, cy - 28f - claw * 6f, 16f, 12f, kind.belly)
        D.oval(c, 0f, cy, kind.bodyRX, kind.bodyRY, kind.body)
        D.arcLine(c, 0f, cy + 4f, 24f, 16f, 200f, 140f, shade(kind.body, 0.8f), 4f)
        D.capsule(c, -12f, cy - 20f, -13f, cy - 36f, 4f, kind.body)
        D.capsule(c, 12f, cy - 20f, 13f, cy - 36f, 4f, kind.body)
        D.circle(c, -13f, cy - 40f, 8f, C.WHITE)
        D.circle(c, 13f, cy - 40f, 8f, C.WHITE)
        D.circle(c, -12f, cy - 40f, 4f, C.BLACK)
        D.circle(c, 14f, cy - 40f, 4f, C.BLACK)
    }

    private fun drawTurtle(c: Canvas) {
        val bob = sin(t * 1.4f + seed) * 2f
        D.shadow(c, 0f, 0f, 62f, 16f)
        val cy = -32f + bob
        D.capsule(c, -34f, cy + 10f, -50f, -6f, 14f, kind.body)
        D.capsule(c, 30f, cy + 10f, 46f, -6f, 14f, kind.body)
        D.capsule(c, 40f, cy - 4f, 74f, cy - 16f, 20f, kind.body)
        D.circle(c, 78f, cy - 20f, 22f, kind.body)
        D.circle(c, 88f, cy - 26f, 4f, C.BLACK)
        D.oval(c, 90f, cy - 14f, 6f, 4f, shade(kind.body, 0.7f))
        D.arcFill(c, 0f, cy + 16f, kind.bodyRX, kind.bodyRY * 1.5f, 180f, 180f, kind.belly)
        D.arcLine(c, 0f, cy + 16f, kind.bodyRX * 0.6f, kind.bodyRY * 0.9f, 180f, 180f,
            shade(kind.belly, 0.78f), 5f)
        D.line(c, -28f, cy + 2f, -28f, cy + 16f, shade(kind.belly, 0.78f), 5f)
        D.line(c, 0f, cy - 12f, 0f, cy + 16f, shade(kind.belly, 0.78f), 5f)
        D.line(c, 28f, cy + 2f, 28f, cy + 16f, shade(kind.belly, 0.78f), 5f)
        D.capsule(c, -50f, cy + 12f, -66f, cy + 6f, 10f, kind.body)
    }
}
