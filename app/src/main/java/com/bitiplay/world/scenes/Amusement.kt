package com.bitiplay.world.scenes

import android.graphics.Canvas
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.shade
import com.bitiplay.world.art.withAlpha
import com.bitiplay.world.audio.Sfx
import com.bitiplay.world.audio.Snd
import com.bitiplay.world.core.Cam
import com.bitiplay.world.core.TAU
import com.bitiplay.world.core.clamp01
import com.bitiplay.world.core.rnd
import com.bitiplay.world.engine.Render
import com.bitiplay.world.engine.Scene
import com.bitiplay.world.engine.Z
import com.bitiplay.world.ent.Act
import com.bitiplay.world.ent.Animal
import com.bitiplay.world.ent.AnimalKind
import com.bitiplay.world.ent.Dispenser
import com.bitiplay.world.ent.Grill
import com.bitiplay.world.ent.Item
import com.bitiplay.world.ent.ItemKind
import com.bitiplay.world.ent.Npc
import com.bitiplay.world.ent.NpcRoster
import com.bitiplay.world.ent.Prop
import com.bitiplay.world.ent.PushKind
import com.bitiplay.world.ent.Pushable
import com.bitiplay.world.ent.Vehicle
import com.bitiplay.world.ent.VehicleKind
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class Amusement : Scene("park", "Amusement Park") {

    override val width = 21600f
    override val accent = C.PURPLE
    override val skyTop = 0xFF57A8E8.toInt()
    override val skyBottom = 0xFFFFE0C8.toInt()

    private val back = -18f

    /** Where the fairground organ plays from. */
    private val carouselXs = ArrayList<Float>()

    override fun ambient(cam: Cam) {
        var best = 0f
        for (i in carouselXs.indices) {
            val d = abs(delta(cam.x, carouselXs[i]))
            val v = clamp01(1f - (d - 420f) / 1500f)
            if (v > best) best = v
        }
        if (best > 0f) com.bitiplay.world.audio.Loops.set("carousel", "loop_carousel", best * 0.72f)
    }

    override fun drawTerrain(c: Canvas, cam: Cam, level: Int) {
        val hw = cam.visW * 0.5f + 80f

        Render.layer(cam, 540f, 0.25f, 40f) { px, _, i ->
            c.save()
            c.translate(px, -500f - ((i * 5) % 3) * 66f)
            Art.cloud(c, 48f + ((i * 11) % 3) * 12f, 210)
            c.restore()
        }

        // bunting strung across the sky
        Render.layer(cam, 270f, 0.75f, 0f) { px, _, i ->
            val sag = 40f
            D.stroke(c, withAlpha(C.BLACK, 90), 4f) { p ->
                p.moveTo(px, -560f)
                p.quadTo(px + 135f, -560f + sag, px + 270f, -560f)
            }
            val cols = intArrayOf(C.RED, C.YELLOW, C.GREEN, C.BLUE, C.PINK)
            for (k in 0 until 4) {
                val u = (k + 1) / 5f
                val fx = px + 270f * u
                val fy = -560f + sag * (4f * u * (1f - u))
                val ci = (((i * 3 + k) % cols.size) + cols.size) % cols.size
                D.tri(c, fx - 15f, fy, fx + 15f, fy, fx, fy + 40f, cols[ci])
            }
        }

        Render.layer(cam, 450f, 0.5f, 200f) { px, _, i ->
            c.save()
            c.translate(px, back)
            Art.pine(c, i, time, 170f + ((i * 13) % 3) * 30f)
            c.restore()
        }

        // plaza
        D.rect(c, -hw, back, hw * 2f, 40f, shade(C.SIDEWALK, 0.9f))
        D.rect(c, -hw, 16f, hw * 2f, 380f, C.SIDEWALK)
        Render.tiles(cam, 150f) { dx, _, i ->
            if (i % 2 == 0) D.rect(c, dx, 22f, 150f, 372f, shade(C.SIDEWALK, 0.96f))
            D.line(c, dx, 20f, dx, 394f, C.SIDEWALK_DARK, 3f)
        }
        D.line(c, -hw, 110f, hw, 110f, C.SIDEWALK_DARK, 3f)
        D.line(c, -hw, 205f, hw, 205f, C.SIDEWALK_DARK, 3f)
        Render.tiles(cam, 450f, 120f) { dx, _, i ->
            D.star(c, dx, 158f, 32f, 14f, 5, withAlpha(if (i % 2 == 0) C.YELLOW else C.PINK, 90))
        }
    }

    override fun drawThumb(c: Canvas, w: Float, h: Float) {
        D.rect(c, 0f, 0f, w, h, 0xFF7FC5EE.toInt())
        D.rect(c, 0f, h * 0.66f, w, h * 0.34f, C.SIDEWALK)
        c.save()
        c.translate(w * 0.34f, h * 0.68f)
        c.scale(h / 820f, h / 820f)
        ferris(c, 0.6f)
        c.restore()
        c.save()
        c.translate(w * 0.78f, h * 0.7f)
        c.scale(h / 800f, h / 800f)
        carousel(c, 0.4f)
        c.restore()
    }

    // ------------------------------------------------------------------ rides

    private fun ferris(c: Canvas, spin: Float) {
        val r = 235f
        val cy = -360f
        D.capsule(c, -150f, 0f, 0f, cy, 22f, C.METAL_DARK)
        D.capsule(c, 150f, 0f, 0f, cy, 22f, C.METAL_DARK)
        D.circleStroke(c, 0f, cy, r, C.METAL, 14f)
        D.circleStroke(c, 0f, cy, r * 0.66f, C.METAL, 8f)
        val cols = intArrayOf(C.RED, C.YELLOW, C.GREEN, C.BLUE, C.PINK, C.PURPLE, C.TEAL, C.ORANGE)
        for (i in 0 until 8) {
            val a = spin + i * TAU / 8f
            val gx = cos(a) * r
            val gy = cy + sin(a) * r
            D.capsule(c, 0f, cy, gx, gy, 6f, C.METAL)
            D.capsule(c, gx, gy, gx, gy + 26f, 5f, C.METAL_DARK)
            D.rect(c, gx - 30f, gy + 24f, 60f, 48f, cols[i], 12f)
            D.rect(c, gx - 24f, gy + 30f, 48f, 20f, withAlpha(C.WHITE, 160), 8f)
        }
        D.circle(c, 0f, cy, 26f, C.YELLOW)
        D.circle(c, 0f, cy, 13f, C.YELLOW_DARK)
    }

    /**
     * Carousel. Horses ride an ellipse so the far side reads as further away:
     * they are drawn back half first, then the centre column, then the near
     * half, and each scales and bobs with its position.
     */
    private fun carousel(c: Canvas, spin: Float) {
        val rx = 168f
        val ry = 40f
        val platY = -30f
        val domeY = -330f

        D.shadow(c, 0f, 0f, 220f, 34f)

        // stepped base
        D.oval(c, 0f, 10f, rx + 34f, ry + 16f, shade(C.SIDEWALK, 0.78f))
        D.oval(c, 0f, -4f, rx + 22f, ry + 10f, shade(C.CREAM, 0.86f))
        D.oval(c, 0f, platY, rx + 12f, ry + 4f, C.CREAM)

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
        D.ovalStroke(c, 0f, platY, rx, ry, C.YELLOW_DARK, 5f)

        val n = 6
        // back half, then the mast, then the near half
        for (pass in 0 until 2) {
            for (i in 0 until n) {
                val a = spin + i * TAU / n
                val depth = sin(a)
                val isBack = depth < 0f
                if ((pass == 0) != isBack) continue
                val hx = cos(a) * rx * 0.84f
                val hy = platY + depth * ry * 0.84f
                val s = 0.74f + depth * 0.12f
                val bob = sin(spin * 2.4f + i * 1.9f) * 18f
                D.capsule(c, hx, hy + 10f, hx, domeY + 30f, 7f * s, C.YELLOW_DARK)
                D.circle(c, hx, domeY + 34f, 8f * s, C.YELLOW)
                horse(c, hx, hy + bob - 10f, s, spin * 3f + i)
            }
            if (pass == 0) {
                D.capsule(c, 0f, platY, 0f, domeY, 22f, C.YELLOW_DARK)
                D.capsule(c, -6f, platY, -6f, domeY, 9f, C.YELLOW)
                for (k in 0 until 5) {
                    D.oval(c, 0f, platY - 44f - k * 58f, 26f, 7f, shade(C.RED, 1.1f))
                }
            }
        }

        // scalloped canopy
        for (i in 0 until 14) {
            val a0 = 180f + i * (180f / 14f)
            D.arcFill(c, 0f, domeY, rx + 34f, 104f, a0, 180f / 14f,
                if (i % 2 == 0) C.RED else C.CREAM)
        }
        for (i in 0 until 14) {
            val t = (i + 0.5f) / 14f
            val sx = -(rx + 34f) + t * 2f * (rx + 34f)
            D.circle(c, sx, domeY, 15f, if (i % 2 == 0) C.RED else C.CREAM)
        }
        D.ovalStroke(c, 0f, domeY, rx + 34f, 18f, C.YELLOW_DARK, 6f)

        // rim bulbs
        for (i in 0 until 15) {
            val t = i / 14f
            val lx = -(rx + 30f) + t * 2f * (rx + 30f)
            val on = ((i + (spin * 2.4f).toInt()) % 3) != 0
            D.circle(c, lx, domeY - 16f, 6.5f, if (on) C.YELLOW else shade(C.YELLOW, 0.55f))
        }

        // finial and flag
        D.capsule(c, 0f, domeY - 100f, 0f, domeY - 150f, 9f, C.YELLOW_DARK)
        D.circle(c, 0f, domeY - 158f, 16f, C.YELLOW)
        D.stroke(c, C.RED, 4f) { p ->
            p.moveTo(2f, domeY - 172f); p.lineTo(2f, domeY - 210f)
        }
        D.shape(c, C.RED) { p ->
            val fy = domeY - 208f
            p.moveTo(4f, fy)
            p.quadTo(34f, fy + 6f, 60f, fy + 2f)
            p.lineTo(60f, fy + 26f)
            p.quadTo(34f, fy + 30f, 4f, fy + 24f)
        }
    }

    private fun horse(c: Canvas, x: Float, y: Float, s: Float, gait: Float) {
        c.save()
        c.translate(x, y)
        c.scale(s, s)
        val kick = sin(gait) * 7f
        // tail
        D.stroke(c, C.CREAM, 11f) { p ->
            p.moveTo(-44f, -52f); p.quadTo(-72f, -40f, -66f, -6f)
        }
        // far legs
        D.capsule(c, -24f, -34f, -32f + kick, 2f, 11f, shade(C.OFF_WHITE, 0.86f))
        D.capsule(c, 22f, -34f, 30f - kick, 2f, 11f, shade(C.OFF_WHITE, 0.86f))
        // body
        D.oval(c, 0f, -48f, 48f, 29f, C.OFF_WHITE)
        // near legs
        D.capsule(c, -18f, -34f, -26f - kick, 4f, 12f, C.OFF_WHITE)
        D.capsule(c, 26f, -34f, 34f + kick, 4f, 12f, C.OFF_WHITE)
        // saddle
        D.shape(c, C.RED) { p ->
            p.moveTo(-20f, -66f); p.lineTo(24f, -66f)
            p.lineTo(20f, -46f); p.lineTo(-16f, -46f)
        }
        D.capsule(c, -18f, -56f, 22f, -56f, 5f, C.YELLOW)
        // neck and head
        D.capsule(c, 28f, -58f, 48f, -96f, 19f, C.OFF_WHITE)
        D.circle(c, 54f, -102f, 20f, C.OFF_WHITE)
        D.oval(c, 68f, -96f, 12f, 9f, shade(C.OFF_WHITE, 0.9f))
        D.tri(c, 44f, -116f, 56f, -116f, 50f, -134f, C.OFF_WHITE)
        D.circle(c, 62f, -108f, 4f, C.BLACK)
        D.circle(c, 74f, -94f, 2.5f, shade(C.OFF_WHITE, 0.6f))
        // mane and bridle
        D.stroke(c, C.PINK, 9f) { p ->
            p.moveTo(38f, -114f); p.quadTo(22f, -84f, 14f, -58f)
        }
        D.capsule(c, 48f, -96f, 66f, -92f, 4f, C.YELLOW_DARK)
        c.restore()
    }

    // ---------------------------------------------------------- carnival games

    /** High striker: swing the mallet, send the puck up, ring the bell. */
    private fun highStriker(px: Float): Prop {
        val towerH = 520f
        return Prop(px, back, Z.MAIN, 1f, 0) { cc, p ->
            D.shadow(cc, 0f, 0f, 90f, 16f)
            // base and mallet
            D.rect(cc, -84f, -70f, 168f, 70f, C.WOOD, 8f)
            D.rect(cc, -92f, -84f, 184f, 18f, C.WOOD_DARK, 6f)
            val swing = if (p.state == 1 && p.f0 < 0.12f) -0.9f else 0.25f
            cc.save()
            cc.translate(72f, -84f)
            cc.rotate(swing * 57.29578f)
            D.capsule(cc, 0f, 0f, 0f, -96f, 9f, C.WOOD)
            D.rectC(cc, 0f, -104f, 54f, 30f, C.METAL_DARK, 8f)
            cc.restore()
            // tower
            D.rect(cc, -30f, -towerH - 70f, 60f, towerH, shade(C.RED, 0.9f), 8f)
            D.rect(cc, -18f, -towerH - 70f, 12f, towerH, shade(C.RED, 1.2f))
            for (k in 0 until 8) {
                val ty = -96f - k * (towerH - 40f) / 8f
                D.line(cc, -30f, ty, 30f, ty, withAlpha(C.CREAM, 150), 3f)
            }
            // puck
            val py = -96f - p.f0 * (towerH - 70f)
            D.rectC(cc, 0f, py, 76f, 26f, C.YELLOW, 8f)
            D.circle(cc, 0f, py, 8f, C.YELLOW_DARK)
            // bell
            val ring = if (p.state == 3) sin(p.t * 40f) * 6f else 0f
            D.arcFill(cc, ring, -towerH - 76f, 34f, 40f, 180f, 180f, C.YELLOW_DARK)
            D.circle(cc, ring, -towerH - 74f, 8f, shade(C.YELLOW_DARK, 0.7f))
            D.capsule(cc, -46f, -towerH - 78f, 46f, -towerH - 78f, 7f, C.METAL_DARK)
            Art.signBoard(cc, "RING IT!", 190f, 56f, C.PURPLE)
        }.sized(240f, 640f, -320f).onUpdate { dt, _, p ->
            when (p.state) {
                1 -> {
                    p.f0 += dt * 2.1f
                    if (p.f0 >= p.f1) {
                        p.f0 = p.f1
                        p.state = if (p.f1 > 0.965f) 3 else 2
                    }
                }
                2 -> {
                    p.f0 -= dt * 1.15f
                    if (p.f0 <= 0f) {
                        p.f0 = 0f
                        p.state = 0
                    }
                }
                3 -> {
                    p.f0 -= dt * 0.8f
                    if (p.f0 <= 0f) {
                        p.f0 = 0f
                        p.state = 0
                    }
                }
            }
        }.interactive("swing the mallet") { ch, sc, p ->
            if (p.state != 0) return@interactive false
            ch.faceToward(sc, p.x)
            ch.startAct(Act.BUILD, 0.9f)
            sc.after(0.42f) {
                p.f1 = rnd(0.55f, 1.02f).coerceAtMost(1f)
                p.state = 1
                Sfx.play(Snd.THUD)
                sc.fx.dust(p.x + 70f, -80f, p.level, 6)
            }
            sc.after(0.42f + 0.48f) {
                if (p.state == 3) {
                    Sfx.play(Snd.BELL)
                    sc.fx.confetti(p.x, -600f, p.level, 24)
                    sc.fx.sparkles(p.x, -600f, p.level, 12, C.YELLOW)
                    val prize = sc.add(Item(ItemKind.TICKET, sc.wrapX(p.x + 130f), -140f))
                    prize.launch(0.4f, 0.7f)
                } else {
                    Sfx.play(Snd.CLICK)
                }
            }
            true
        }
    }

    /** Ring toss: pitch a hoop at the bottles. */
    private fun ringToss(px: Float): Prop {
        return Prop(px, back, Z.MAIN, 1f, 0) { cc, p ->
            D.shadow(cc, 0f, 0f, 120f, 18f)
            D.rect(cc, -150f, -150f, 300f, 150f, C.WOOD, 8f)
            D.rect(cc, -160f, -168f, 320f, 24f, C.WOOD_DARK, 8f)
            Art.awning(cc, 340f, -300f, C.TEAL, C.CREAM)
            // bottles on the shelf
            for (k in 0 until 5) {
                val bx = -110f + k * 55f
                val hit = p.state == 3 && k == 2
                D.rect(cc, bx - 16f, -216f, 32f, 52f, if (hit) C.GREEN else C.GREEN_DARK, 6f)
                D.rect(cc, bx - 7f, -244f, 14f, 32f, if (hit) C.GREEN else C.GREEN_DARK, 4f)
                D.circle(cc, bx, -248f, 9f, C.YELLOW_DARK)
                if (hit) D.circleStroke(cc, bx, -216f, 26f, C.PURPLE, 8f)
            }
            // flying ring
            if (p.state == 1 || p.state == 2) {
                val u = clamp01(p.f0)
                val rxp = -190f + u * 190f
                val ryp = -120f - sin(u * 3.14159f) * 190f
                D.ovalStroke(cc, rxp, ryp, 30f, 12f, C.PURPLE, 9f)
            }
            Art.signBoard(cc, "RING TOSS", 260f, 58f, C.RED)
            // prize shelf
            D.circle(cc, -108f, -290f, 22f, C.PINK)
            D.circle(cc, 0f, -292f, 22f, C.YELLOW)
            D.circle(cc, 108f, -290f, 22f, C.TEAL)
        }.sized(360f, 400f, -200f).onUpdate { dt, _, p ->
            if (p.state == 1 || p.state == 2) {
                p.f0 += dt * 1.25f
                if (p.f0 >= 1f) {
                    p.f0 = 0f
                    p.state = if (p.state == 2) 3 else 0
                }
            } else if (p.state == 3) {
                p.f1 += dt
                if (p.f1 > 2.2f) {
                    p.f1 = 0f
                    p.state = 0
                }
            }
        }.interactive("toss a ring") { ch, sc, p ->
            if (p.state != 0) return@interactive false
            ch.faceToward(sc, p.x)
            ch.startAct(Act.CHEER, 0.5f)
            val win = rnd(0f, 1f) < 0.45f
            p.f0 = 0f
            p.state = if (win) 2 else 1
            Sfx.play(Snd.WHOOSH)
            sc.after(0.8f) {
                if (p.state == 3 || win) {
                    Sfx.play(Snd.RING)
                    Sfx.play(Snd.WIN)
                    sc.fx.confetti(p.x, -300f, p.level, 18)
                    val prize = sc.add(Item(ItemKind.TEDDY, sc.wrapX(p.x - 150f), -140f))
                    prize.launch(-0.35f, 0.7f)
                } else {
                    Sfx.play(Snd.CLICK)
                    sc.fx.dust(p.x - 40f, -20f, p.level, 4)
                }
            }
            true
        }
    }

    /** Teacups: three cups spinning on a turntable. */
    private fun teacups(px: Float): Prop =
        Prop(px, 40f, Z.MAIN, 1f, 0) { cc, p ->
            D.shadow(cc, 0f, 0f, 200f, 30f)
            D.oval(cc, 0f, 0f, 210f, 46f, shade(C.SIDEWALK, 0.8f))
            D.oval(cc, 0f, -16f, 190f, 40f, C.TEAL)
            D.ovalStroke(cc, 0f, -16f, 190f, 40f, shade(C.TEAL, 0.7f), 6f)
            val cols = intArrayOf(C.PINK, C.YELLOW, C.PURPLE)
            for (pass in 0 until 2) {
                for (i in 0 until 3) {
                    val a = p.f0 + i * TAU / 3f
                    val d = sin(a)
                    if ((pass == 0) != (d < 0f)) continue
                    val cx = cos(a) * 120f
                    val cy = -16f + d * 26f
                    val s = 0.82f + d * 0.14f
                    cc.save()
                    cc.translate(cx, cy)
                    cc.scale(s, s)
                    cc.rotate(sin(p.f0 * 3.1f + i * 2f) * 9f)
                    D.shape(cc, cols[i]) { path ->
                        path.moveTo(-52f, -76f); path.lineTo(52f, -76f)
                        path.lineTo(38f, -6f); path.lineTo(-38f, -6f)
                    }
                    D.oval(cc, 0f, -76f, 52f, 14f, shade(cols[i], 1.2f))
                    D.arcLine(cc, 60f, -46f, 22f, 26f, -80f, 160f, cols[i], 10f)
                    D.oval(cc, 0f, -2f, 40f, 10f, shade(cols[i], 0.75f))
                    cc.restore()
                }
            }
        }.sized(440f, 220f, -110f)
            .onUpdate { dt, _, p -> p.f0 += dt * (0.6f + p.f1); p.f1 = maxOf(0f, p.f1 - dt * 0.35f) }
            .interactive("spin the teacups") { ch, sc, p ->
                p.f1 = 2.0f
                ch.cheer(sc)
                sc.fx.notes(p.x, -180f, p.level, 3)
                Sfx.play(Snd.SPRING)
                true
            }

    /** Drop tower: the car climbs, hangs, then falls. */
    private fun dropTower(px: Float): Prop =
        Prop(px, back, Z.BACK, 1f, 0) { cc, p ->
            val h = 560f
            D.shadow(cc, 0f, 0f, 110f, 18f)
            D.capsule(cc, -50f, 0f, -50f, -h, 16f, C.METAL_DARK)
            D.capsule(cc, 50f, 0f, 50f, -h, 16f, C.METAL_DARK)
            for (k in 0 until 9) {
                val ty = -30f - k * (h - 40f) / 9f
                D.line(cc, -50f, ty, 50f, ty - 26f, C.METAL, 5f)
            }
            D.rect(cc, -70f, -h - 40f, 140f, 40f, C.PURPLE, 10f)
            D.tri(cc, -34f, -h - 40f, 34f, -h - 40f, 0f, -h - 92f, C.YELLOW)
            val cy = -60f - p.f0 * (h - 130f)
            D.rectC(cc, 0f, cy, 150f, 44f, C.RED, 12f)
            for (k in -1..1) {
                D.rectC(cc, k * 46f, cy + 6f, 30f, 26f, withAlpha(C.CREAM, 210), 6f)
            }
            D.capsule(cc, -60f, cy - 22f, 60f, cy - 22f, 7f, C.METAL)
        }.sized(220f, 700f, -350f).onUpdate { dt, _, p ->
            when (p.state) {
                0 -> {
                    p.f0 += dt * 0.28f
                    if (p.f0 >= 1f) {
                        p.f0 = 1f; p.state = 1; p.f1 = 0f
                    }
                }
                1 -> {
                    p.f1 += dt
                    if (p.f1 > 1.4f) p.state = 2
                }
                else -> {
                    p.f0 -= dt * 2.6f
                    if (p.f0 <= 0f) {
                        p.f0 = 0f; p.state = 0
                    }
                }
            }
        }.interactive("watch the drop tower") { ch, sc, p ->
            ch.cheer(sc)
            if (p.state == 1) {
                p.state = 2
                Sfx.play(Snd.WHOOSH)
                sc.fx.confetti(p.x, -300f, p.level, 12)
            } else {
                Sfx.play(Snd.UI)
            }
            true
        }

    override fun spawnX(index: Int): Float = 1450f + index * 165f

    // ------------------------------------------------------------------ build

    override fun build() {
        // Four fairground zones, each with its own headline attraction.
        buildEntrance(120f)
        buildFerrisZone(1000f)
        buildCoasterZone(6000f)
        buildGamesZone(11600f)
        buildFarZone(16800f)
        buildFiller()
    }

    private fun buildEntrance(x: Float) {
        add(Prop(x, back, Z.BACK, 1f, 0) { cc, p ->
            D.capsule(cc, -190f, 0f, -190f, -380f, 24f, C.PURPLE_DARK)
            D.capsule(cc, 190f, 0f, 190f, -380f, 24f, C.PURPLE_DARK)
            D.arcFill(cc, 0f, -380f, 214f, 120f, 180f, 180f, C.PURPLE)
            D.label(cc, "FUN PARK", 0f, -420f, 54f, C.YELLOW)
            for (i in 0 until 9) {
                val on = ((i + (p.t * 3f).toInt()) % 3) != 0
                D.circle(cc, -180f + i * 45f, -474f, 9f, if (on) C.YELLOW else C.YELLOW_DARK)
            }
        }.sized(460f, 520f, -260f))
    }

    private fun buildFerrisZone(x: Float) {
        add(Prop(x, back, Z.FAR, 1f, 0) { cc, p -> ferris(cc, p.f0) }
            .onUpdate { dt, _, p -> p.f0 += dt * (0.25f + p.f1); p.f1 = maxOf(0f, p.f1 - dt * 0.16f) }
            .sized(540f, 620f, -320f)
            .interactive("give the wheel a spin") { ch, sc, p ->
                p.f1 = 1.1f
                ch.cheer(sc)
                sc.fx.confetti(p.x, -360f, p.level, 20)
                Sfx.play(Snd.CHIME)
                true
            })

        // Set well back from the walking line so it reads as further away.
        carouselXs.add(x + 1150f)
        add(com.bitiplay.world.ent.Carousel(x + 1150f, -150f))

        add(Dispenser(x + 500f, ItemKind.BALLOON, "a balloon", Z.MAIN) { cc, p ->
            D.shadow(cc, 0f, 0f, 70f, 13f)
            D.rect(cc, -66f, -110f, 132f, 110f, C.WOOD, 6f)
            Art.awning(cc, 170f, -158f, C.RED, C.WHITE)
            val cols = intArrayOf(C.RED, C.YELLOW, C.BLUE, C.GREEN, C.PINK)
            for (i in 0 until 5) {
                val bx = -60f + i * 30f
                val by = -230f - (i % 2) * 26f + sin(p.t * 1.2f + i) * 6f
                D.stroke(cc, C.WHITE, 3f) { path -> path.moveTo(bx, by + 30f); path.lineTo(-10f, -160f) }
                D.oval(cc, bx, by, 22f, 27f, cols[i])
            }
        }.sized(220f, 320f, -160f))

        add(Vehicle(VehicleKind.TRAIN, x - 300f, C.PURPLE))
        add(Item(ItemKind.ICE_CREAM, x + 700f))
        add(Npc(NpcRoster.spec(0), x + 850f))
    }

    private fun buildCoasterZone(x: Float) {
        add(Prop(x, back, Z.FAR, 0.5f, 0) { cc, p ->
            val w = 900f
            D.stroke(cc, C.METAL_DARK, 9f) { path ->
                path.moveTo(-w * 0.5f, 0f); path.lineTo(-w * 0.5f, -140f)
                path.moveTo(-w * 0.2f, 0f); path.lineTo(-w * 0.2f, -400f)
                path.moveTo(w * 0.12f, 0f); path.lineTo(w * 0.12f, -210f)
                path.moveTo(w * 0.42f, 0f); path.lineTo(w * 0.42f, -300f)
            }
            D.stroke(cc, C.RED, 12f) { path ->
                path.moveTo(-w * 0.5f, -140f)
                path.cubicTo(-w * 0.3f, -470f, -w * 0.1f, -470f, 0f, -230f)
                path.cubicTo(w * 0.1f, -40f, w * 0.3f, -420f, w * 0.5f, -300f)
            }
            val u = (p.f0 % 1f)
            val tx = -w * 0.5f + w * u
            val ty = -150f - sin(u * TAU * 1.6f) * 170f
            for (k in 0 until 3) {
                D.rect(cc, tx - 34f + k * 34f, ty - 34f, 30f, 30f, if (k == 0) C.YELLOW else C.BLUE, 7f)
            }
        }.onUpdate { dt, _, p -> p.f0 += dt * 0.13f }.sized(940f, 520f, -260f))

        add(Prop(x + 1100f, back, Z.BACK, 1f, 0) { cc, p ->
            D.shadow(cc, 0f, 0f, 130f, 20f)
            D.capsule(cc, 0f, 0f, 0f, -440f, 22f, C.METAL_DARK)
            D.oval(cc, 0f, -440f, 150f, 30f, C.RED)
            for (i in 0 until 5) {
                val a = p.f0 + i * TAU / 5f
                val px2 = cos(a) * 150f
                val fly = 90f + sin(p.f0 * 0.7f) * 30f
                D.capsule(cc, px2 * 0.6f, -436f, px2, -436f + fly, 4f, C.METAL)
                D.rect(cc, px2 - 20f, -436f + fly, 40f, 26f, if (i % 2 == 0) C.YELLOW else C.TEAL, 8f)
            }
            D.oval(cc, 0f, -470f, 90f, 24f, C.YELLOW_DARK)
        }.onUpdate { dt, _, p -> p.f0 += dt * 0.9f }.sized(360f, 520f, -260f))

        add(Dispenser(x + 500f, ItemKind.POPCORN, "popcorn!", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 76f, 14f)
            D.rect(cc, -76f, -140f, 152f, 140f, C.RED, 8f)
            D.rect(cc, -60f, -124f, 120f, 74f, C.GLASS, 6f)
            for (i in 0 until 7) {
                D.circle(cc, -46f + (i * 17) % 96, -70f - (i * 13) % 40, 11f, C.CREAM)
            }
            Art.awning(cc, 190f, -190f, C.RED, C.CREAM)
        }.sized(220f, 260f, -130f))

        add(Dispenser(x + 1600f, ItemKind.TICKET, "grab a ticket", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 70f, 13f)
            D.rect(cc, -70f, -160f, 140f, 160f, C.PURPLE, 8f)
            D.rect(cc, -54f, -140f, 108f, 60f, C.GLASS, 6f)
            D.arcFill(cc, 0f, -160f, 84f, 44f, 180f, 180f, C.YELLOW)
            D.label(cc, "TICKETS", 0f, -46f, 22f, C.WHITE)
        }.sized(200f, 240f, -120f))

        add(Vehicle(VehicleKind.GOKART, x + 1900f, C.RED))
        add(Vehicle(VehicleKind.GOKART, x + 2100f, C.BLUE))
        add(Vehicle(VehicleKind.GOKART, x + 2300f, C.GREEN))
        add(Item(ItemKind.BALL, x + 800f))
        add(Npc(NpcRoster.spec(2), x + 1300f))
    }

    private fun buildGamesZone(x: Float) {
        add(highStriker(x))
        add(ringToss(x + 900f))

        // prize booth
        add(Dispenser(x + 1700f, ItemKind.TEDDY, "pick a prize", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 110f, 18f)
            D.rect(cc, -140f, -160f, 280f, 160f, C.PINK_DARK, 10f)
            Art.awning(cc, 320f, -220f, C.PINK, C.CREAM)
            for (k in 0 until 4) {
                D.circle(cc, -100f + k * 66f, -196f, 24f,
                    intArrayOf(C.YELLOW, C.TEAL, C.PURPLE, C.ORANGE)[k])
            }
            D.label(cc, "PRIZES", 0f, -70f, 30f, C.WHITE)
        }.sized(340f, 280f, -140f))

        add(Grill(x + 2200f))
        add(Dispenser(x + 2400f, ItemKind.SAUSAGE_RAW, "a corn dog to grill", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 74f, 13f)
            D.rect(cc, -74f, -130f, 148f, 130f, C.CREAM, 8f)
            Art.awning(cc, 186f, -178f, C.YELLOW, C.RED)
            D.label(cc, "SNACKS", 0f, -60f, 26f, C.RED_DARK)
        }.sized(210f, 240f, -120f))

        add(Pushable(PushKind.SHOPPING_CART, x + 1300f))
        add(Item(ItemKind.PRETZEL, x + 600f))
        add(Npc(NpcRoster.spec(5), x + 1100f))
        add(Npc(NpcRoster.spec(3), x + 2000f))
    }

    private fun buildFarZone(x: Float) {
        add(teacups(x))
        add(dropTower(x + 1100f))

        add(Dispenser(x + 600f, ItemKind.DONUT, "a fresh donut", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 80f, 14f)
            D.rect(cc, -80f, -130f, 160f, 130f, C.CREAM, 8f)
            Art.awning(cc, 200f, -180f, C.PURPLE, C.WHITE)
            D.circle(cc, -40f, -160f, 22f, C.PINK)
            D.circle(cc, -40f, -160f, 8f, C.CREAM)
            D.circle(cc, 34f, -162f, 22f, 0xFFD79B54.toInt())
            D.circle(cc, 34f, -162f, 8f, C.CREAM)
        }.sized(220f, 250f, -125f))

        // picnic area
        for (k in 0 until 3) {
            add(Prop(x + 1700f + k * 260f, 190f, Z.MAIN, 1f, k) { cc, _ ->
                D.shadow(cc, 0f, 0f, 110f, 16f)
                D.rect(cc, -110f, -96f, 220f, 20f, C.WOOD, 6f)
                D.capsule(cc, -70f, -76f, -70f, 0f, 12f, C.WOOD_DARK)
                D.capsule(cc, 70f, -76f, 70f, 0f, 12f, C.WOOD_DARK)
                D.rect(cc, -130f, -50f, 60f, 14f, C.WOOD, 5f)
                D.rect(cc, 70f, -50f, 60f, 14f, C.WOOD, 5f)
            }.sized(280f, 130f, -65f))
        }

        add(Vehicle(VehicleKind.SCOOTER, x + 2500f, C.TEAL))
        add(Item(ItemKind.TEDDY, x + 2700f))
        add(Item(ItemKind.CUPCAKE, x + 900f))
        add(Npc(NpcRoster.spec(1), x + 2200f))
        add(Animal(AnimalKind.DOG, x + 2400f))
    }

    /** Lamps, benches, bins and birds spread the length of the park. */
    private fun buildFiller() {
        var i = 0
        var x = 400f
        while (x < width) {
            add(Prop(x, 196f, Z.MAIN, 1f, i) { cc, _ -> Art.bench(cc, 160f) }
                .sized(180f, 140f, -70f))
            add(Prop(x + 340f, back, Z.BACK, 1f, i) { cc, _ -> Art.lamppost(cc, 280f) }
                .sized(90f, 320f, -160f))
            if (i % 3 == 0) {
                add(Prop(x + 700f, 180f, Z.MAIN, 1f, i) { cc, _ ->
                    D.shadow(cc, 0f, 0f, 40f, 9f)
                    D.shape(cc, C.PURPLE_DARK) { p ->
                        p.moveTo(-38f, -104f); p.lineTo(38f, -104f)
                        p.lineTo(30f, 0f); p.lineTo(-30f, 0f)
                    }
                    D.rect(cc, -44f, -120f, 88f, 20f, shade(C.PURPLE_DARK, 0.8f), 6f)
                }.sized(110f, 150f, -70f))
            }
            if (i % 2 == 0) add(Animal(AnimalKind.PIGEON, x + 520f))
            i++
            x += 900f
        }
    }
}
