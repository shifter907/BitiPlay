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
import com.bitiplay.world.core.chance
import com.bitiplay.world.core.sgn
import com.bitiplay.world.engine.Entity
import com.bitiplay.world.engine.Scene
import com.bitiplay.world.engine.Z
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class VehicleKind(
    val label: String,
    val topSpeed: Float,
    val motorised: Boolean,
    val seatDX: Float,
    val seatDY: Float,
    val wheelR: Float,
    val bodyLen: Float
) {
    // seatDY is where the rider's FEET go, so it sits well below the visible
    // seat - the body reaches up from there.
    BIKE("bike", 340f, false, -4f, -64f, 36f, 170f),
    SCOOTER("scooter", 300f, false, -10f, -44f, 26f, 150f),
    TRIKE("trike", 250f, false, -4f, -38f, 30f, 150f),
    CAR("car", 430f, true, -12f, -58f, 40f, 300f),
    TAXI("taxi", 430f, true, -12f, -58f, 40f, 300f),
    JEEP("jeep", 400f, true, -12f, -40f, 46f, 300f),
    GOKART("go-kart", 470f, true, 4f, -40f, 32f, 220f),
    TRACTOR("tractor", 250f, true, -18f, -100f, 62f, 260f),
    BOAT("boat", 320f, true, 0f, -64f, 0f, 320f),
    TRAIN("train", 270f, true, -6f, -62f, 30f, 320f)
}

/** A rideable. Tap to hop on, tap the ground to drive, tap it again to hop off. */
class Vehicle(val kind: VehicleKind, startX: Float, val paint: Int = defaultPaint(kind)) : Entity() {

    var rider: Character? = null
    var vx = 0f
    var targetX: Float? = null

    private var wheelRot = 0f
    private var puffTimer = 0f
    private var bounce = 0f

    val motorised: Boolean get() = kind.motorised
    val seatDX: Float get() = kind.seatDX
    val seatDY: Float get() = kind.seatDY

    init {
        x = startX
        z = Z.MAIN
        tappable = true
        hitW = kind.bodyLen + 60f
        hitH = 200f
        hitCY = -100f
        useRange = 190f
        cullPad = 420f
    }

    override fun approachOffset(): Float = kind.bodyLen * 0.5f + 40f

    fun wake() {
        bounce = 1f
    }

    private var hornCooldown = 0f

    fun driveTo(scene: Scene, wx: Float) {
        // A toot when a road car pulls away from a standstill.
        if (hornCooldown <= 0f && abs(vx) < 25f &&
            (kind == VehicleKind.CAR || kind == VehicleKind.TAXI || kind == VehicleKind.JEEP)
        ) {
            hornCooldown = 3.5f
            Sfx.play(Snd.HORN)
        }
        targetX = scene.wrapX(wx)
    }

    fun stopDriving() {
        targetX = null
    }

    override fun update(dt: Float, scene: Scene) {
        if (bounce > 0f) bounce -= dt * 3f
        if (hornCooldown > 0f) hornCooldown -= dt

        val tx = targetX
        if (tx != null && rider != null) {
            val d = scene.delta(x, tx)
            if (abs(d) < 34f) {
                targetX = null
            } else {
                val dir = sgn(d)
                facing = if (dir > 0f) 1 else -1
                vx = approach(vx, dir * kind.topSpeed, 900f * dt)
            }
        } else {
            vx = approach(vx, 0f, 1100f * dt)
        }

        if (vx != 0f) {
            x = scene.wrapX(x + vx * dt)
            if (kind.wheelR > 0f) wheelRot += vx * dt / kind.wheelR
        }

        if (motorised && abs(vx) > 60f) {
            puffTimer -= dt
            if (puffTimer <= 0f) {
                puffTimer = 0.16f
                scene.fx.smoke(x - facing * kind.bodyLen * 0.5f, -70f, level, 1)
            }
        }
        if (kind == VehicleKind.BOAT && abs(vx) > 40f && chance(dt * 9f)) {
            scene.fx.splash(x - facing * 150f, -20f, level, 2, C.FOAM)
        }
    }

    override fun caption(ch: Character): String =
        if (rider === ch) "hop off" else if (rider != null) "taken" else "ride the ${kind.label}"

    override fun onUse(ch: Character, scene: Scene): Boolean {
        if (rider === ch) {
            ch.dismount(scene)
            return true
        }
        if (rider != null) return false
        ch.mount(this, scene)
        return true
    }

    // ------------------------------------------------------------------- art

    override fun draw(c: Canvas) {
        val hop = if (bounce > 0f) sin(bounce * TAU * 0.5f) * 8f else 0f
        val rattle = if (abs(vx) > 30f) sin(t * 34f) * 1.6f else 0f
        c.save()
        c.translate(0f, -hop + rattle)
        when (kind) {
            VehicleKind.BIKE -> drawBike(c)
            VehicleKind.SCOOTER -> drawScooter(c)
            VehicleKind.TRIKE -> drawTrike(c)
            VehicleKind.CAR, VehicleKind.TAXI -> drawCar(c, kind == VehicleKind.TAXI)
            VehicleKind.JEEP -> drawJeep(c)
            VehicleKind.GOKART -> drawKart(c)
            VehicleKind.TRACTOR -> drawTractor(c)
            VehicleKind.BOAT -> drawBoat(c)
            VehicleKind.TRAIN -> drawTrain(c)
        }
        c.restore()
    }

    private fun wheel(c: Canvas, cx: Float, cy: Float, r: Float, rim: Int = C.BLACK, hub: Int = C.METAL) {
        D.circle(c, cx, cy, r, rim)
        D.circle(c, cx, cy, r * 0.52f, hub)
        for (i in 0 until 4) {
            val a = wheelRot + i * TAU / 4f
            D.line(c, cx, cy, cx + cos(a) * r * 0.46f, cy + sin(a) * r * 0.46f, shade(hub, 0.75f), 4f)
        }
        D.circle(c, cx, cy, r * 0.16f, shade(hub, 0.7f))
    }

    private fun drawBike(c: Canvas) {
        val r = kind.wheelR
        D.shadow(c, 0f, 0f, 90f, 16f)
        wheel(c, -62f, -r, r)
        wheel(c, 62f, -r, r)
        D.stroke(c, paint, 10f) { p ->
            p.moveTo(-62f, -r); p.lineTo(-6f, -r)
            p.lineTo(-24f, -96f); p.lineTo(-62f, -r)
            p.moveTo(-6f, -r); p.lineTo(30f, -100f)
            p.lineTo(62f, -r); p.moveTo(-24f, -96f); p.lineTo(30f, -100f)
        }
        D.capsule(c, 30f, -100f, 44f, -140f, 8f, shade(paint, 0.8f))
        D.capsule(c, 34f, -142f, 62f, -146f, 9f, C.BLACK)
        D.rectC(c, -26f, -104f, 40f, 14f, C.BLACK, 7f)
        D.circle(c, -6f, -r, 13f, C.METAL_DARK)
        D.line(c, -6f, -r, -6f + cos(wheelRot * 1.4f) * 20f, -r + sin(wheelRot * 1.4f) * 20f, C.METAL, 6f)
    }

    private fun drawScooter(c: Canvas) {
        val r = kind.wheelR
        D.shadow(c, 0f, 0f, 74f, 13f)
        wheel(c, -52f, -r, r, C.BLACK, C.METAL)
        wheel(c, 52f, -r, r, C.BLACK, C.METAL)
        D.rectC(c, 0f, -34f, 130f, 16f, paint, 8f)
        D.capsule(c, 46f, -40f, 56f, -150f, 11f, shade(paint, 0.85f))
        D.capsule(c, 34f, -152f, 74f, -152f, 10f, C.BLACK)
    }

    private fun drawTrike(c: Canvas) {
        val r = kind.wheelR
        D.shadow(c, 0f, 0f, 80f, 14f)
        wheel(c, -54f, -22f, 22f, C.BLACK, C.RED)
        wheel(c, -20f, -22f, 22f, C.BLACK, C.RED)
        wheel(c, 56f, -r, r, C.BLACK, C.RED)
        D.rectC(c, 0f, -56f, 120f, 16f, paint, 8f)
        D.rectC(c, -36f, -78f, 52f, 26f, shade(paint, 0.85f), 10f)
        D.capsule(c, 44f, -62f, 54f, -122f, 10f, C.METAL)
        D.capsule(c, 34f, -124f, 72f, -124f, 9f, C.BLACK)
    }

    private fun drawCar(c: Canvas, taxi: Boolean) {
        val col = if (taxi) C.YELLOW else paint
        val r = kind.wheelR
        D.shadow(c, 0f, 0f, 150f, 22f)
        wheel(c, -88f, -r, r)
        wheel(c, 88f, -r, r)
        D.rectC(c, 0f, -84f, 300f, 78f, col, 26f)
        // Open top, so the rider actually reads as sitting in it.
        D.shape(c, C.GLASS) { p ->
            p.moveTo(34f, -118f); p.quadTo(74f, -168f, 104f, -166f)
            p.lineTo(108f, -118f)
        }
        D.stroke(c, shade(col, 0.82f), 9f) { p ->
            p.moveTo(32f, -118f); p.quadTo(74f, -172f, 106f, -168f)
        }
        D.capsule(c, -112f, -120f, 30f, -120f, 10f, shade(col, 0.86f))
        D.circle(c, 138f, -92f, 15f, C.CREAM)
        D.circle(c, -140f, -92f, 12f, C.RED)
        D.rectC(c, 0f, -48f, 300f, 12f, shade(col, 0.78f), 6f)
        if (taxi) {
            // Checker block on the rear deck - text would mirror when driving left.
            D.rectC(c, -92f, -136f, 62f, 22f, C.BLACK, 6f)
            for (i in 0 until 4) {
                D.rect(c, -121f + i * 15f, -145f + (i % 2) * 9f, 15f, 9f, C.YELLOW)
            }
        }
    }

    private fun drawJeep(c: Canvas) {
        val r = kind.wheelR
        D.shadow(c, 0f, 0f, 155f, 22f)
        wheel(c, -92f, -r, r, C.BLACK, C.OFF_WHITE)
        wheel(c, 92f, -r, r, C.BLACK, C.OFF_WHITE)
        D.rectC(c, 0f, -100f, 300f, 84f, paint, 14f)
        D.rectC(c, -20f, -150f, 190f, 22f, shade(paint, 0.85f), 8f)
        D.capsule(c, -110f, -152f, -110f, -196f, 9f, C.METAL_DARK)
        D.capsule(c, 96f, -152f, 96f, -196f, 9f, C.METAL_DARK)
        D.rectC(c, -8f, -200f, 230f, 16f, C.LEAF_DARK, 8f)
        for (i in 0 until 5) D.rectC(c, -60f + i * 30f, -196f, 6f, 14f, shade(C.LEAF_DARK, 0.8f))
        D.circle(c, 140f, -108f, 14f, C.CREAM)
        D.rectC(c, 0f, -62f, 300f, 12f, shade(paint, 0.75f), 6f)
    }

    private fun drawKart(c: Canvas) {
        val r = kind.wheelR
        D.shadow(c, 0f, 0f, 116f, 18f)
        wheel(c, -76f, -r, r, C.BLACK, C.YELLOW)
        wheel(c, 76f, -r, r * 1.1f, C.BLACK, C.YELLOW)
        D.shape(c, paint) { p ->
            p.moveTo(-104f, -44f); p.lineTo(104f, -44f)
            p.lineTo(96f, -78f); p.lineTo(6f, -78f)
            p.lineTo(-16f, -104f); p.lineTo(-92f, -104f); p.lineTo(-104f, -70f)
        }
        D.rectC(c, -50f, -116f, 66f, 30f, shade(paint, 0.82f), 10f)
        D.capsule(c, 30f, -84f, 46f, -122f, 9f, C.METAL_DARK)
        D.circle(c, 48f, -126f, 17f, C.BLACK)
        D.circle(c, 48f, -126f, 8f, C.METAL)
        D.rectC(c, -104f, -60f, 18f, 44f, C.RED, 6f)
    }

    private fun drawTractor(c: Canvas) {
        val r = kind.wheelR
        D.shadow(c, 0f, 0f, 150f, 22f)
        wheel(c, -86f, -r, r, C.BLACK, C.YELLOW)
        wheel(c, 86f, -34f, 34f, C.BLACK, C.YELLOW)
        D.rectC(c, 20f, -100f, 170f, 74f, paint, 12f)
        D.rectC(c, -60f, -140f, 110f, 100f, shade(paint, 0.9f), 14f)
        D.rectC(c, 96f, -168f, 26f, 70f, C.METAL_DARK, 8f)
        D.rectC(c, -66f, -196f, 20f, 60f, C.METAL_DARK, 6f)
        D.circle(c, -66f, -200f, 14f, C.METAL)
        D.capsule(c, 26f, -152f, 40f, -190f, 9f, C.METAL_DARK)
        D.circle(c, 42f, -194f, 16f, C.BLACK)
        D.rectC(c, 110f, -110f, 30f, 20f, C.CREAM, 5f)
    }

    private fun drawBoat(c: Canvas) {
        D.shape(c, paint) { p ->
            p.moveTo(-160f, -60f); p.lineTo(160f, -60f)
            p.quadTo(140f, 16f, 60f, 20f); p.lineTo(-90f, 20f)
            p.quadTo(-152f, 14f, -160f, -60f)
        }
        D.rectC(c, 0f, -64f, 320f, 16f, shade(paint, 0.82f), 8f)
        D.rectC(c, -30f, -108f, 130f, 76f, C.OFF_WHITE, 10f)
        D.rectC(c, -30f, -120f, 150f, 14f, shade(paint, 0.85f), 6f)
        D.rectC(c, 10f, -104f, 40f, 34f, C.GLASS, 5f)
        D.rectC(c, -60f, -104f, 40f, 34f, C.GLASS, 5f)
        D.capsule(c, 106f, -120f, 106f, -196f, 8f, C.METAL_DARK)
        D.tri(c, 110f, -192f, 110f, -146f, 168f, -170f, C.RED)
        D.circle(c, -122f, -34f, 20f, C.RED)
        D.circle(c, -122f, -34f, 11f, C.OFF_WHITE)
    }

    private fun drawTrain(c: Canvas) {
        val r = kind.wheelR
        D.shadow(c, 0f, 0f, 165f, 22f)
        wheel(c, -100f, -r, r, C.BLACK, C.RED)
        wheel(c, -30f, -r, r, C.BLACK, C.RED)
        wheel(c, 90f, -r * 1.2f, r * 1.2f, C.BLACK, C.RED)
        D.rectC(c, 0f, -84f, 320f, 60f, paint, 10f)
        D.rectC(c, 78f, -136f, 150f, 62f, shade(paint, 1.1f), 12f)
        D.circle(c, 148f, -140f, 18f, C.METAL_DARK)
        D.rectC(c, -66f, -150f, 116f, 90f, C.OFF_WHITE, 10f)
        D.rectC(c, -66f, -160f, 136f, 14f, shade(paint, 0.85f), 6f)
        D.rectC(c, -90f, -136f, 34f, 34f, C.GLASS, 5f)
        D.rectC(c, -40f, -136f, 34f, 34f, C.GLASS, 5f)
        D.circle(c, 152f, -96f, 14f, C.YELLOW)
    }

    companion object {
        fun defaultPaint(kind: VehicleKind): Int = when (kind) {
            VehicleKind.BIKE -> C.TEAL
            VehicleKind.SCOOTER -> C.PURPLE
            VehicleKind.TRIKE -> C.RED
            VehicleKind.CAR -> C.BLUE
            VehicleKind.TAXI -> C.YELLOW
            VehicleKind.JEEP -> 0xFF7FA05A.toInt()
            VehicleKind.GOKART -> C.RED
            VehicleKind.TRACTOR -> C.GREEN_DARK
            VehicleKind.BOAT -> C.WHITE
            VehicleKind.TRAIN -> C.RED
        }
    }
}
