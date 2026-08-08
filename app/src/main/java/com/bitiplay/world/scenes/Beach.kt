package com.bitiplay.world.scenes

import android.graphics.Canvas
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.shade
import com.bitiplay.world.art.withAlpha
import com.bitiplay.world.audio.Sfx
import com.bitiplay.world.audio.Snd
import com.bitiplay.world.core.Cam
import com.bitiplay.world.core.rnd
import com.bitiplay.world.engine.Render
import com.bitiplay.world.engine.Scene
import com.bitiplay.world.engine.Z
import com.bitiplay.world.ent.Act
import com.bitiplay.world.ent.Animal
import com.bitiplay.world.ent.AnimalKind
import com.bitiplay.world.ent.DigSpot
import com.bitiplay.world.ent.Dispenser
import com.bitiplay.world.ent.Grill
import com.bitiplay.world.ent.Item
import com.bitiplay.world.ent.ItemKind
import com.bitiplay.world.ent.Npc
import com.bitiplay.world.ent.NpcRoster
import com.bitiplay.world.ent.Prop
import com.bitiplay.world.ent.PushKind
import com.bitiplay.world.ent.Pushable
import com.bitiplay.world.ent.Tent
import com.bitiplay.world.ent.Vehicle
import com.bitiplay.world.ent.VehicleKind
import kotlin.math.sin

class Beach : Scene("beach", "Beach") {

    override val width = 21600f
    override val accent = C.SAND
    override val skyTop = 0xFF3FB6E8.toInt()
    override val skyBottom = 0xFFDDF3FB.toInt()

    private val horizon = -262f
    private val shore = -34f

    override fun drawTerrain(c: Canvas, cam: Cam, level: Int) {
        val hw = cam.visW * 0.5f + 80f

        c.save()
        c.translate(cam.visW * 0.28f, -cam.visH * 0.58f)
        Art.sun(c, 70f, time)
        c.restore()

        Render.layer(cam, 540f, 0.25f, 60f) { px, _, i ->
            c.save()
            c.translate(px, -500f - ((i * 5) % 3) * 62f)
            Art.cloud(c, 46f + ((i * 11) % 3) * 13f, 225)
            c.restore()
        }

        // sea
        D.rect(c, -hw, horizon, hw * 2f, 40f, shade(C.SEA_DEEP, 0.92f))
        D.rect(c, -hw, horizon + 34f, hw * 2f, -horizon + shore - 34f, C.SEA)
        D.rect(c, -hw, shore - 74f, hw * 2f, 44f, C.SEA_LIGHT)
        for (row in 0 until 5) {
            val yy = horizon + 52f + row * 44f
            Render.tiles(cam, 270f, row * 90f + sin(time * 0.9f + row) * 34f) { dx, _, _ ->
                D.capsule(c, dx, yy, dx + 88f, yy, 8f, withAlpha(C.FOAM, 120))
            }
        }
        // surf line
        Render.tiles(cam, 225f, sin(time * 1.2f) * 26f) { dx, _, _ ->
            D.arcFill(c, dx, shore - 4f, 116f, 26f, 180f, 180f, withAlpha(C.FOAM, 215))
        }
        D.rect(c, -hw, shore - 6f, hw * 2f, 14f, withAlpha(C.FOAM, 200))

        // sand
        D.rect(c, -hw, shore + 6f, hw * 2f, 60f, C.SAND_DARK)
        D.rect(c, -hw, shore + 52f, hw * 2f, 420f, C.SAND)
        Render.tiles(cam, 150f, 30f) { dx, _, i ->
            if (i % 2 == 0) D.oval(c, dx, 120f + (i % 3) * 40f, 26f, 8f, shade(C.SAND, 0.94f))
        }
        Render.tiles(cam, 450f, 120f) { dx, _, i ->
            when (i % 3) {
                0 -> D.circle(c, dx, 190f, 9f, C.PINK)
                1 -> D.star(c, dx, 214f, 13f, 6f, 5, shade(C.ORANGE, 1.1f))
                else -> D.oval(c, dx, 176f, 13f, 8f, C.CREAM)
            }
        }
    }

    override fun drawThumb(c: Canvas, w: Float, h: Float) {
        D.rect(c, 0f, 0f, w, h, 0xFF8FD9F3.toInt())
        D.circle(c, w * 0.2f, h * 0.2f, h * 0.12f, C.SUN)
        D.rect(c, 0f, h * 0.42f, w, h * 0.24f, C.SEA)
        D.rect(c, 0f, h * 0.62f, w, h * 0.38f, C.SAND)
        c.save()
        c.translate(w * 0.74f, h * 0.66f)
        c.scale(h / 620f, h / 620f)
        Art.palm(c, 2, 0f, 340f)
        c.restore()
        D.arcFill(c, w * 0.3f, h * 0.78f, w * 0.13f, h * 0.16f, 180f, 180f, C.RED)
    }

    override fun spawnX(index: Int): Float = 1200f + index * 165f

    override fun build() {
        val palmXs = floatArrayOf(320f, 1480f, 2760f, 3980f, 4880f)
        for (i in palmXs.indices) {
            add(Prop(palmXs[i], 120f, Z.BACK, 1f, i) { cc, p ->
                Art.palm(cc, p.seed, p.t, 400f + (p.seed % 3) * 40f)
            }.sized(320f, 440f, -230f))
        }

        // beach umbrella you can open and close
        add(Prop(900f, 140f, Z.MAIN, 1f, 0) { cc, p ->
            D.shadow(cc, 0f, 0f, 60f, 14f)
            D.capsule(cc, 0f, 0f, 0f, -230f, 9f, C.WOOD)
            if (p.state == 0) {
                for (i in 0 until 6) {
                    val a0 = 180f + i * 30f
                    D.arcFill(cc, 0f, -230f, 180f, 96f, a0, 30f,
                        if (i % 2 == 0) C.RED else C.OFF_WHITE)
                }
                D.circle(cc, 0f, -236f, 12f, C.RED_DARK)
            } else {
                D.capsule(cc, 0f, -60f, 0f, -256f, 26f, C.RED)
            }
        }.sized(340f, 280f, -150f).interactive("the beach umbrella") { ch, sc, p ->
            p.state = 1 - p.state
            ch.startAct(Act.REACH, 0.35f)
            sc.fx.sparkles(p.x, -230f, p.level, 5, C.WHITE)
            Sfx.play(Snd.WHOOSH)
            true
        })

        add(Prop(1080f, 190f, Z.MAIN, 1f, 0) { cc, _ ->
            D.shadow(cc, 0f, 0f, 90f, 16f)
            D.capsule(cc, -70f, -10f, -34f, -84f, 11f, C.WOOD)
            D.capsule(cc, 62f, -10f, 40f, -60f, 11f, C.WOOD)
            D.shape(cc, C.TEAL) { p ->
                p.moveTo(-84f, -74f); p.lineTo(-24f, -96f)
                p.lineTo(78f, -52f); p.lineTo(70f, -34f); p.lineTo(-80f, -56f)
            }
        }.sized(200f, 130f, -65f))

        // sandcastle that you build up
        add(Prop(2260f, 170f, Z.MAIN, 1f, 0) { cc, p ->
            D.shadow(cc, 0f, 0f, 90f, 16f)
            val s = p.state
            if (s == 0) {
                D.arcFill(cc, 0f, 4f, 74f, 34f, 180f, 180f, C.SAND_DARK)
            } else {
                D.rect(cc, -80f, -70f, 160f, 74f, C.SAND_DARK, 6f)
                for (i in 0 until 5) D.rect(cc, -80f + i * 34f, -86f, 22f, 20f, C.SAND_DARK, 4f)
                if (s >= 2) {
                    D.rect(cc, -58f, -150f, 44f, 84f, shade(C.SAND_DARK, 1.06f), 5f)
                    D.rect(cc, 16f, -150f, 44f, 84f, shade(C.SAND_DARK, 1.06f), 5f)
                    D.tri(cc, -62f, -150f, -10f, -150f, -36f, -190f, C.RED)
                    D.tri(cc, 12f, -150f, 64f, -150f, 38f, -190f, C.RED)
                }
                if (s >= 3) {
                    D.rect(cc, -26f, -196f, 52f, 128f, C.SAND_DARK, 5f)
                    D.tri(cc, -32f, -196f, 32f, -196f, 0f, -246f, C.BLUE)
                    D.capsule(cc, 0f, -246f, 0f, -276f, 5f, C.WOOD)
                    D.tri(cc, 2f, -276f, 2f, -252f, 44f, -264f, C.YELLOW)
                }
            }
        }.sized(240f, 300f, -150f).interactive("build the sandcastle") { ch, sc, p ->
            if (p.state >= 3) {
                sc.fx.sparkles(p.x, -260f, p.level, 8, C.YELLOW)
                Sfx.play(Snd.HAPPY)
                return@interactive true
            }
            ch.startAct(Act.BUILD, 0.8f)
            sc.after(0.5f) {
                p.state++
                sc.fx.dust(p.x, 0f, p.level, 6)
                Sfx.play(Snd.BUILD)
                if (p.state == 3) sc.fx.confetti(p.x, -250f, p.level, 14)
            }
            true
        })

        add(DigSpot(2560f, ItemKind.SHELL))
        add(DigSpot(2700f, ItemKind.TREASURE))
        add(DigSpot(3500f))
        add(DigSpot(4380f, ItemKind.STARFISH))

        add(Item(ItemKind.BUCKET, 2400f))
        add(Item(ItemKind.SHOVEL, 2140f))
        add(Item(ItemKind.BEACHBALL, 1720f))
        add(Item(ItemKind.SHELL, 3140f))
        add(Item(ItemKind.STARFISH, 3980f))
        add(Item(ItemKind.COCONUT, 4900f))
        add(Item(ItemKind.NET, 3660f))

        add(Pushable(PushKind.WAGON, 1900f, C.TEAL))

        add(Tent(4620f, C.BLUE))
        add(Grill(4180f))
        add(Dispenser(3820f, ItemKind.FISH, "fresh fish", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 90f, 16f)
            D.rect(cc, -84f, -120f, 168f, 120f, C.WOOD, 8f)
            D.rect(cc, -92f, -136f, 184f, 22f, C.BLUE_DARK, 8f)
            D.label(cc, "FISH", 0f, -76f, 30f, C.OFF_WHITE)
            D.oval(cc, -34f, -150f, 30f, 14f, C.BLUE)
            D.oval(cc, 30f, -150f, 30f, 14f, C.TEAL)
        }.sized(210f, 190f, -95f))

        // ice cream stand
        add(Dispenser(1360f, ItemKind.ICE_CREAM, "an ice cream", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 110f, 18f)
            D.rect(cc, -100f, -150f, 200f, 150f, C.OFF_WHITE, 10f)
            Art.awning(cc, 220f, -196f, C.PINK, C.WHITE)
            D.rect(cc, -70f, -120f, 140f, 56f, C.GLASS, 6f)
            D.circle(cc, 0f, -250f, 34f, C.CREAM)
            D.circle(cc, -18f, -272f, 26f, C.PINK)
            D.tri(cc, -30f, -236f, 30f, -236f, 0f, -186f, 0xFFD9A05B.toInt())
        }.sized(250f, 300f, -150f))

        add(Vehicle(VehicleKind.BOAT, 3300f).also { it.y = -96f })
        add(Vehicle(VehicleKind.BIKE, 4740f, C.YELLOW))

        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(2), 2050f))
        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(5), 4250f))

        add(Animal(AnimalKind.CRAB, 2900f))
        add(Animal(AnimalKind.CRAB, 4600f))
        add(Animal(AnimalKind.SEAGULL, 1600f))
        add(Animal(AnimalKind.SEAGULL, 3400f))
        add(Animal(AnimalKind.SEAGULL, 5100f))
        add(Animal(AnimalKind.TURTLE, 620f))

        // rock pools in the foreground
        for (i in 0 until 4) {
            add(Prop(700f + i * 1350f, 196f, Z.MAIN, 1f, i) { cc, p ->
                Art.rockCluster(cc, p.seed, 46f)
            }.sized(140f, 120f, -60f))
        }

        buildMore()
    }

    /** Three more stretches of coast beyond the first bay. */
    private fun buildMore() {
        for (b in 1 until 4) {
            val ox = b * 5400f

            for (i in 0 until 4) {
                add(Prop(ox + 320f + i * 1300f, 120f, Z.BACK, 1f, i + b) { cc, p ->
                    Art.palm(cc, p.seed, p.t, 400f + (p.seed % 3) * 40f)
                }.sized(320f, 440f, -230f))
            }
            for (i in 0 until 4) {
                add(Prop(ox + 900f + i * 1300f, 196f, Z.MAIN, 1f, i + b) { cc, p ->
                    Art.rockCluster(cc, p.seed, 46f)
                }.sized(140f, 120f, -60f))
            }

            add(DigSpot(ox + 1500f, if (b == 2) ItemKind.TREASURE else ItemKind.SHELL))
            add(DigSpot(ox + 1660f))
            add(DigSpot(ox + 3900f, ItemKind.STARFISH))
            add(Item(ItemKind.BUCKET, ox + 1300f))
            add(Item(ItemKind.SHOVEL, ox + 1400f))
            add(Item(ItemKind.BEACHBALL, ox + 2600f))
            add(Item(ItemKind.SHELL, ox + 4400f))
            add(Item(ItemKind.COCONUT, ox + 4900f))

            add(Animal(AnimalKind.CRAB, ox + 2200f))
            add(Animal(AnimalKind.SEAGULL, ox + 800f))
            add(Animal(AnimalKind.SEAGULL, ox + 3400f))
            add(Npc(NpcRoster.spec(b), ox + 1800f))
            add(Npc(NpcRoster.spec(b + 2), ox + 4200f))

            when (b) {
                1 -> {
                    // a jetty running out into the water
                    add(Prop(ox + 2900f, -60f, Z.BACK, 1f, 0) { cc, _ ->
                        for (k in 0 until 7) {
                            D.capsule(cc, -260f + k * 90f, 60f, -260f + k * 90f, -40f, 12f, C.WOOD_DARK)
                        }
                        D.rect(cc, -300f, -56f, 620f, 22f, C.WOOD, 6f)
                        D.rect(cc, -300f, -78f, 620f, 10f, C.WOOD_LIGHT, 4f)
                    }.sized(640f, 160f, -50f))
                    add(Vehicle(VehicleKind.BOAT, ox + 3300f).also { it.y = -96f })
                    add(Tent(ox + 4600f, C.TEAL))
                }
                2 -> {
                    add(Grill(ox + 2000f))
                    add(Dispenser(ox + 2200f, ItemKind.FISH, "fresh fish", Z.MAIN) { cc, _ ->
                        D.shadow(cc, 0f, 0f, 90f, 16f)
                        D.rect(cc, -84f, -120f, 168f, 120f, C.WOOD, 8f)
                        D.rect(cc, -92f, -136f, 184f, 22f, C.BLUE_DARK, 8f)
                        D.label(cc, "FISH", 0f, -76f, 30f, C.OFF_WHITE)
                    }.sized(210f, 190f, -95f))
                    // volleyball net
                    add(Prop(ox + 3600f, 150f, Z.MAIN, 1f, 0) { cc, _ ->
                        D.capsule(cc, -180f, 0f, -180f, -240f, 12f, C.WOOD)
                        D.capsule(cc, 180f, 0f, 180f, -240f, 12f, C.WOOD)
                        D.stroke(cc, withAlpha(C.WHITE, 200), 4f) { p ->
                            for (k in 0 until 8) {
                                val gx = -180f + k * 51f
                                p.moveTo(gx, -230f); p.lineTo(gx, -120f)
                            }
                            for (k in 0 until 4) {
                                val gy = -230f + k * 37f
                                p.moveTo(-180f, gy); p.lineTo(180f, gy)
                            }
                        }
                    }.sized(400f, 270f, -135f))
                    add(Item(ItemKind.BEACHBALL, ox + 3450f))
                }
                else -> {
                    // lighthouse on the point
                    add(Prop(ox + 2400f, 60f, Z.BACK, 1f, 0) { cc, p ->
                        D.shadow(cc, 0f, 0f, 130f, 20f)
                        D.shape(cc, C.OFF_WHITE) { path ->
                            path.moveTo(-96f, 0f); path.lineTo(-56f, -460f)
                            path.lineTo(56f, -460f); path.lineTo(96f, 0f)
                        }
                        for (k in 0 until 3) {
                            val yy = -70f - k * 140f
                            D.shape(cc, C.RED) { path ->
                                val w0 = 92f - k * 12f
                                val w1 = 84f - k * 12f
                                path.moveTo(-w0, yy); path.lineTo(w0, yy)
                                path.lineTo(w1, yy - 60f); path.lineTo(-w1, yy - 60f)
                            }
                        }
                        D.rect(cc, -64f, -530f, 128f, 74f, C.METAL_DARK, 8f)
                        val glow = 0.5f + 0.5f * sin(p.t * 2.2f)
                        D.circle(cc, 0f, -494f, 26f, withAlpha(C.YELLOW, (120 + glow * 135).toInt()))
                        D.arcFill(cc, 0f, -534f, 74f, 34f, 180f, 180f, C.RED_DARK)
                    }.sized(230f, 580f, -290f))
                    add(Dispenser(ox + 3200f, ItemKind.ICE_CREAM, "an ice cream", Z.MAIN) { cc, _ ->
                        D.shadow(cc, 0f, 0f, 110f, 18f)
                        D.rect(cc, -100f, -150f, 200f, 150f, C.OFF_WHITE, 10f)
                        Art.awning(cc, 220f, -196f, C.PINK, C.WHITE)
                        D.rect(cc, -70f, -120f, 140f, 56f, C.GLASS, 6f)
                        D.circle(cc, 0f, -250f, 34f, C.CREAM)
                        D.circle(cc, -18f, -272f, 26f, C.PINK)
                        D.tri(cc, -30f, -236f, 30f, -236f, 0f, -186f, 0xFFD9A05B.toInt())
                    }.sized(250f, 300f, -150f))
                    add(Animal(AnimalKind.TURTLE, ox + 4700f))
                }
            }
        }
    }
}
