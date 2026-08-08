package com.bitiplay.world.scenes

import android.graphics.Canvas
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.shade
import com.bitiplay.world.art.withAlpha
import com.bitiplay.world.audio.Sfx
import com.bitiplay.world.audio.Snd
import com.bitiplay.world.core.Cam
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
import com.bitiplay.world.ent.Plant
import com.bitiplay.world.ent.PlantKind
import com.bitiplay.world.ent.Prop
import com.bitiplay.world.ent.PushKind
import com.bitiplay.world.ent.Pushable
import com.bitiplay.world.ent.Vehicle
import com.bitiplay.world.ent.VehicleKind
import kotlin.math.cos
import kotlin.math.sin

class Downtown : Scene("city", "Downtown") {

    override val width = 21600f
    override val accent = 0xFF6E7A8A.toInt()
    override val skyTop = 0xFF3E86C8.toInt()
    override val skyBottom = 0xFFFBD9B4.toInt()

    private val back = -20f

    private val farCols = intArrayOf(
        0xFF6B7C90.toInt(), 0xFF5D6E82.toInt(), 0xFF7A8B9E.toInt(), 0xFF66788C.toInt()
    )
    private val nearCols = intArrayOf(
        0xFF8A9AAC.toInt(), 0xFF9AA8B8.toInt(), 0xFF7E8EA0.toInt(), 0xFFA6B2C0.toInt()
    )

    override fun drawTerrain(c: Canvas, cam: Cam, level: Int) {
        val hw = cam.visW * 0.5f + 80f

        // far skyline. Tile indices go negative left of the world origin, so
        // every derived value is normalised before it becomes a size or index.
        Render.layer(cam, 300f, 0.25f) { px, _, i ->
            val hgt = 340f + ((((i * 37) % 5) + 5) % 5) * 90f
            c.save()
            c.translate(px, back)
            Art.tower(c, 250f, hgt, farCols[((i % 4) + 4) % 4], i, time)
            c.restore()
        }
        // near skyline
        Render.layer(cam, 450f, 0.5f, 120f) { px, _, i ->
            val hgt = 420f + ((((i * 53) % 4) + 4) % 4) * 80f
            c.save()
            c.translate(px, back)
            Art.tower(c, 330f, hgt, nearCols[((i % 4) + 4) % 4], i + 7, time)
            c.restore()
        }

        // street
        D.rect(c, -hw, back, hw * 2f, 26f, 0xFF3E464F.toInt())
        D.rect(c, -hw, 4f, hw * 2f, 136f, C.ROAD)
        Render.tiles(cam, 270f, 40f) { dx, _, _ ->
            D.rect(c, dx, 64f, 140f, 12f, C.ROAD_LINE, 6f)
        }
        // crosswalks
        Render.tiles(cam, 1350f, 300f) { dx, _, _ ->
            for (k in 0 until 7) D.rect(c, dx + k * 34f, 8f, 20f, 128f, withAlpha(C.WHITE, 190), 3f)
        }
        // kerb + pavement
        D.rect(c, -hw, 138f, hw * 2f, 15f, C.SIDEWALK_DARK)
        D.rect(c, -hw, 153f, hw * 2f, 250f, C.SIDEWALK)
        Render.tiles(cam, 150f) { dx, _, _ ->
            D.line(c, dx, 155f, dx, 380f, C.SIDEWALK_DARK, 3f)
        }
        D.line(c, -hw, 206f, hw, 206f, C.SIDEWALK_DARK, 3f)
        // grates and manholes
        Render.tiles(cam, 675f, 200f) { dx, _, i ->
            if (i % 2 == 0) {
                D.oval(c, dx, 182f, 32f, 13f, shade(C.SIDEWALK, 0.78f))
            } else {
                D.rect(c, dx - 30f, 172f, 60f, 24f, shade(C.SIDEWALK, 0.8f), 4f)
                for (k in 0 until 4) D.line(c, dx - 22f + k * 14f, 176f, dx - 22f + k * 14f, 192f, shade(C.SIDEWALK, 0.62f), 3f)
            }
        }
    }

    override fun drawThumb(c: Canvas, w: Float, h: Float) {
        D.rect(c, 0f, 0f, w, h, 0xFF6FA8DA.toInt())
        for (i in 0 until 5) {
            val bh = h * (0.36f + (i % 3) * 0.14f)
            D.rect(c, i * w / 5f + 3f, h * 0.72f - bh, w / 5f - 6f, bh, nearCols[i % 4], 3f)
            for (k in 0 until 4) {
                D.rect(c, i * w / 5f + 9f, h * 0.72f - bh + 8f + k * 12f, w / 5f - 18f, 7f,
                    withAlpha(C.YELLOW, 200), 2f)
            }
        }
        D.rect(c, 0f, h * 0.72f, w, h * 0.28f, C.ROAD)
        D.rect(c, 0f, h * 0.9f, w, h * 0.1f, C.SIDEWALK)
        D.rect(c, w * 0.36f, h * 0.74f, w * 0.26f, h * 0.12f, C.YELLOW, 4f)
    }

    override fun spawnX(index: Int): Float = 900f + index * 165f

    override fun build() {
        // shopfronts along the pavement
        val shopNames = arrayOf("BAKERY", "TOYS", "BOOKS", "PIZZA", "MUSIC", "FLOWERS")
        val shopCols = intArrayOf(C.RED, C.TEAL, C.PURPLE, C.ORANGE, C.BLUE, C.PINK)
        for (i in 0 until 6) {
            add(Prop(360f + i * 900f, back, Z.BACK, 1f, i) { cc, p ->
                val col = shopCols[p.seed % 6]
                D.rect(cc, -220f, -420f, 440f, 420f, 0xFFB9A896.toInt(), 8f)
                D.rect(cc, -200f, -300f, 400f, 300f, C.GLASS, 8f)
                D.rect(cc, -200f, -300f, 400f, 300f, withAlpha(C.WHITE, 40), 8f)
                D.line(cc, -60f, -300f, -60f, 0f, withAlpha(C.WHITE, 120), 6f)
                D.rect(cc, 96f, -240f, 110f, 240f, shade(col, 0.85f), 6f)
                D.circle(cc, 118f, -120f, 8f, C.YELLOW)
                Art.awning(cc, 460f, -352f, col, C.CREAM)
                Art.signBoard(cc, shopNames[p.seed % 6], 300f, 74f, col)
                upperWindows(cc)
            }.sized(470f, 500f, -250f))
        }

        // traffic lights
        for (i in 0 until 4) {
            add(Prop(700f + i * 1350f, back, Z.BACK, 1f, i) { cc, p ->
                D.capsule(cc, 0f, 0f, 0f, -420f, 13f, 0xFF3E464F.toInt())
                D.rect(cc, -34f, -560f, 68f, 150f, 0xFF2E363E.toInt(), 12f)
                val phase = ((p.t * 0.45f).toInt()) % 3
                D.circle(cc, 0f, -524f, 19f, if (phase == 0) C.RED else shade(C.RED, 0.35f))
                D.circle(cc, 0f, -484f, 19f, if (phase == 1) C.YELLOW else shade(C.YELLOW, 0.35f))
                D.circle(cc, 0f, -444f, 19f, if (phase == 2) C.GREEN else shade(C.GREEN, 0.35f))
            }.sized(110f, 600f, -300f))
        }

        // subway entrance
        add(Prop(2600f, back, Z.BACK, 1f, 0) { cc, _ ->
            D.rect(cc, -170f, -60f, 340f, 60f, shade(C.SIDEWALK, 0.7f), 8f)
            D.shape(cc, 0xFF33404E.toInt()) { p ->
                p.moveTo(-140f, -60f); p.lineTo(140f, -60f)
                p.lineTo(120f, -300f); p.lineTo(-120f, -300f)
            }
            D.rect(cc, -150f, -318f, 300f, 40f, C.GREEN_DARK, 10f)
            D.label(cc, "SUBWAY", 0f, -288f, 34f, C.WHITE)
            D.capsule(cc, -150f, -60f, -150f, -330f, 10f, C.METAL_DARK)
            D.capsule(cc, 150f, -60f, 150f, -330f, 10f, C.METAL_DARK)
        }.sized(360f, 360f, -180f).interactive("peek down the subway") { ch, sc, p ->
            ch.startAct(Act.REACH, 0.4f)
            sc.fx.smoke(p.x, -80f, p.level, 5)
            Sfx.play(Snd.WHOOSH)
            true
        })

        // hot dog cart
        add(Dispenser(1750f, ItemKind.SAUSAGE_RAW, "a sausage for the grill", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 100f, 16f)
            D.circle(cc, -70f, -30f, 30f, C.BLACK)
            D.circle(cc, -70f, -30f, 14f, C.METAL)
            D.circle(cc, 70f, -30f, 30f, C.BLACK)
            D.circle(cc, 70f, -30f, 14f, C.METAL)
            D.rect(cc, -104f, -150f, 208f, 110f, C.RED, 10f)
            D.rect(cc, -104f, -166f, 208f, 22f, C.METAL, 8f)
            D.capsule(cc, 0f, -166f, 0f, -260f, 8f, C.METAL_DARK)
            for (i in 0 until 6) {
                D.arcFill(cc, 0f, -260f, 130f, 60f, 180f + i * 30f, 30f, if (i % 2 == 0) C.YELLOW else C.RED)
            }
            D.label(cc, "HOT DOGS", 0f, -86f, 24f, C.WHITE)
        }.sized(260f, 300f, -150f))

        add(Grill(1980f))

        // newsstand gives a pretzel
        add(Dispenser(3400f, ItemKind.PRETZEL, "a warm pretzel", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 84f, 14f)
            D.rect(cc, -90f, -180f, 180f, 180f, 0xFF3E86C8.toInt(), 8f)
            D.rect(cc, -74f, -160f, 148f, 70f, C.CREAM, 5f)
            for (i in 0 until 3) D.rect(cc, -66f + i * 48f, -152f, 40f, 54f, C.OFF_WHITE, 3f)
            Art.awning(cc, 210f, -228f, C.BLUE_DARK, C.CREAM)
        }.sized(230f, 280f, -140f))

        // bus stop
        add(Prop(4200f, back, Z.BACK, 1f, 0) { cc, _ ->
            D.rect(cc, -180f, -320f, 360f, 26f, C.TEAL, 8f)
            D.capsule(cc, -170f, 0f, -170f, -310f, 11f, C.METAL_DARK)
            D.capsule(cc, 170f, 0f, 170f, -310f, 11f, C.METAL_DARK)
            D.rect(cc, -170f, -290f, 340f, 200f, withAlpha(C.GLASS, 130), 6f)
            D.rect(cc, -150f, -100f, 300f, 22f, C.WOOD, 6f)
            D.capsule(cc, -120f, -78f, -120f, 0f, 9f, C.METAL_DARK)
            D.capsule(cc, 120f, -78f, 120f, 0f, 9f, C.METAL_DARK)
        }.sized(380f, 360f, -180f))

        // fire hydrant that can spray
        add(Prop(3100f, 200f, Z.MAIN, 1f, 0) { cc, p ->
            D.shadow(cc, 0f, 0f, 40f, 9f)
            D.rect(cc, -26f, -80f, 52f, 80f, C.RED, 10f)
            D.arcFill(cc, 0f, -80f, 26f, 22f, 180f, 180f, shade(C.RED, 1.15f))
            D.rect(cc, -40f, -56f, 80f, 16f, C.RED_DARK, 6f)
            D.circle(cc, 0f, -104f, 10f, C.RED_DARK)
            if (p.state == 1) {
                D.oval(cc, 60f, -60f + sin(p.t * 9f) * 4f, 40f, 12f, withAlpha(C.SEA_LIGHT, 170))
            }
        }.sized(120f, 140f, -70f).interactive("the hydrant") { ch, sc, p ->
            p.state = 1 - p.state
            ch.startAct(Act.PET, 0.6f)
            if (p.state == 1) {
                sc.fx.splash(p.x + 70f, -60f, p.level, 14, C.SEA_LIGHT)
                Sfx.play(Snd.SPLASH)
            }
            true
        })

        add(Vehicle(VehicleKind.TAXI, 1200f))
        add(Vehicle(VehicleKind.TAXI, 4600f))
        add(Vehicle(VehicleKind.CAR, 2900f, C.RED))
        add(Vehicle(VehicleKind.SCOOTER, 3700f, C.TEAL))
        add(Vehicle(VehicleKind.BIKE, 5100f, C.ORANGE))

        add(Pushable(PushKind.SHOPPING_CART, 4900f))

        add(Item(ItemKind.PIZZA, 3260f))
        add(Item(ItemKind.DONUT, 800f))
        add(Item(ItemKind.BALL, 4400f))
        add(Item(ItemKind.KEY, 2400f))
        add(Item(ItemKind.FLOWER, 5250f))

        add(Plant(PlantKind.SAPLING, 1550f, 3))
        add(Plant(PlantKind.SAPLING, 3900f, 2))
        add(Plant(PlantKind.FLOWERS, 4750f, 3))

        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(2), 1520f))
        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(4), 3050f))
        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(0), 4550f))

        add(Animal(AnimalKind.PIGEON, 1000f))
        add(Animal(AnimalKind.PIGEON, 1090f))
        add(Animal(AnimalKind.PIGEON, 3550f))
        add(Animal(AnimalKind.DOG, 2200f))
        add(Animal(AnimalKind.CAT, 4980f))

        for (i in 0 until 5) {
            add(Prop(1000f + i * 1080f, 200f, Z.MAIN, 1f, i) { cc, _ -> Art.bench(cc, 150f) }
                .sized(170f, 140f, -70f))
        }
        for (i in 0 until 6) {
            add(Prop(520f + i * 900f, back, Z.BACK, 1f, i) { cc, _ -> Art.lamppost(cc, 330f) }
                .sized(90f, 360f, -180f))
        }

        buildMore()
    }

    /** Three more city blocks. */
    private fun buildMore() {
        val shopNames = arrayOf("BAKERY", "TOYS", "BOOKS", "PIZZA", "MUSIC", "FLOWERS")
        val shopCols = intArrayOf(C.RED, C.TEAL, C.PURPLE, C.ORANGE, C.BLUE, C.PINK)

        for (b in 1 until 4) {
            val ox = b * 5400f

            for (i in 0 until 6) {
                add(Prop(ox + 360f + i * 900f, back, Z.BACK, 1f, i + b * 2) { cc, p ->
                    val col = shopCols[p.seed % 6]
                    D.rect(cc, -220f, -420f, 440f, 420f, 0xFFB9A896.toInt(), 8f)
                    D.rect(cc, -200f, -300f, 400f, 300f, C.GLASS, 8f)
                    D.rect(cc, 96f, -240f, 110f, 240f, shade(col, 0.85f), 6f)
                    D.circle(cc, 118f, -120f, 8f, C.YELLOW)
                    Art.awning(cc, 460f, -352f, col, C.CREAM)
                    Art.signBoard(cc, shopNames[p.seed % 6], 300f, 74f, col)
                    upperWindows(cc)
                }.sized(470f, 500f, -250f))
                add(Prop(ox + 520f + i * 900f, back, Z.BACK, 1f, i) { cc, _ -> Art.lamppost(cc, 330f) }
                    .sized(90f, 360f, -180f))
            }
            for (i in 0 until 4) {
                add(Prop(ox + 700f + i * 1350f, back, Z.BACK, 1f, i) { cc, p ->
                    D.capsule(cc, 0f, 0f, 0f, -420f, 13f, 0xFF3E464F.toInt())
                    D.rect(cc, -34f, -560f, 68f, 150f, 0xFF2E363E.toInt(), 12f)
                    val phase = ((p.t * 0.45f).toInt()) % 3
                    D.circle(cc, 0f, -524f, 19f, if (phase == 0) C.RED else shade(C.RED, 0.35f))
                    D.circle(cc, 0f, -484f, 19f, if (phase == 1) C.YELLOW else shade(C.YELLOW, 0.35f))
                    D.circle(cc, 0f, -444f, 19f, if (phase == 2) C.GREEN else shade(C.GREEN, 0.35f))
                }.sized(110f, 600f, -300f))
                add(Prop(ox + 1000f + i * 1350f, 200f, Z.MAIN, 1f, i) { cc, _ -> Art.bench(cc, 150f) }
                    .sized(170f, 140f, -70f))
            }

            add(Vehicle(if (b % 2 == 0) VehicleKind.TAXI else VehicleKind.CAR,
                ox + 1200f, intArrayOf(C.RED, C.GREEN, C.PURPLE, C.TEAL)[b]))
            add(Vehicle(VehicleKind.TAXI, ox + 3800f))
            add(Vehicle(if (b == 2) VehicleKind.SCOOTER else VehicleKind.BIKE,
                ox + 4700f, C.ORANGE))

            add(Plant(PlantKind.SAPLING, ox + 1550f, 3))
            add(Plant(PlantKind.SAPLING, ox + 3900f, 2))
            add(Item(ItemKind.PIZZA, ox + 2600f))
            add(Item(ItemKind.DONUT, ox + 4300f))
            add(Animal(AnimalKind.PIGEON, ox + 900f))
            add(Animal(AnimalKind.PIGEON, ox + 980f))
            add(Animal(AnimalKind.DOG, ox + 3300f))
            add(Npc(NpcRoster.spec(b), ox + 1700f))
            add(Npc(NpcRoster.spec(b + 2), ox + 3400f))

            when (b) {
                1 -> {
                    // plaza fountain
                    add(Prop(ox + 2700f, 60f, Z.MAIN, 1f, 0) { cc, p ->
                        D.shadow(cc, 0f, 0f, 200f, 28f)
                        D.oval(cc, 0f, 10f, 210f, 54f, C.SIDEWALK_DARK)
                        D.oval(cc, 0f, 0f, 190f, 46f, C.SEA_LIGHT)
                        D.ovalStroke(cc, 0f, 0f, 190f, 46f, C.SIDEWALK, 12f)
                        D.capsule(cc, 0f, -6f, 0f, -140f, 22f, C.SIDEWALK)
                        D.oval(cc, 0f, -150f, 84f, 22f, C.SIDEWALK)
                        for (k in 0 until 6) {
                            val a = k * 1.047f
                            val sp = 60f + sin(p.t * 3f + k) * 14f
                            D.capsule(cc, 0f, -160f, cos(a) * sp, -100f + sin(a) * 18f, 7f,
                                withAlpha(C.FOAM, 190))
                        }
                        D.circle(cc, 0f, -168f, 16f, C.SEA_LIGHT)
                    }.sized(440f, 260f, -130f).interactive("splash in the fountain") { ch, sc, p ->
                        ch.startAct(Act.PET, 0.6f)
                        sc.fx.splash(p.x, -40f, p.level, 14, C.SEA_LIGHT)
                        Sfx.play(Snd.SPLASH)
                        true
                    })
                }
                2 -> {
                    add(Dispenser(ox + 2600f, ItemKind.SAUSAGE_RAW, "a sausage for the grill", Z.MAIN) { cc, _ ->
                        D.shadow(cc, 0f, 0f, 100f, 16f)
                        D.circle(cc, -70f, -30f, 30f, C.BLACK)
                        D.circle(cc, 70f, -30f, 30f, C.BLACK)
                        D.rect(cc, -104f, -150f, 208f, 110f, C.RED, 10f)
                        D.rect(cc, -104f, -166f, 208f, 22f, C.METAL, 8f)
                        D.capsule(cc, 0f, -166f, 0f, -260f, 8f, C.METAL_DARK)
                        for (i in 0 until 6) {
                            D.arcFill(cc, 0f, -260f, 130f, 60f, 180f + i * 30f, 30f,
                                if (i % 2 == 0) C.YELLOW else C.RED)
                        }
                    }.sized(260f, 300f, -150f))
                    add(Grill(ox + 2850f))
                    // construction hoarding
                    add(Prop(ox + 4200f, back, Z.BACK, 1f, 0) { cc, _ ->
                        D.rect(cc, -240f, -260f, 480f, 260f, C.YELLOW_DARK, 8f)
                        for (k in 0 until 8) {
                            D.shape(cc, C.BLACK) { p ->
                                val bx = -240f + k * 60f
                                p.moveTo(bx, -260f); p.lineTo(bx + 30f, -260f)
                                p.lineTo(bx, -200f); p.lineTo(bx - 30f, -200f)
                            }
                        }
                        D.capsule(cc, -60f, -260f, 60f, -520f, 18f, C.ORANGE)
                        D.capsule(cc, 60f, -520f, 220f, -520f, 14f, C.ORANGE)
                        D.capsule(cc, 180f, -520f, 180f, -400f, 5f, C.METAL_DARK)
                        D.rect(cc, 152f, -400f, 56f, 40f, C.METAL, 6f)
                    }.sized(520f, 560f, -280f))
                }
                else -> {
                    add(Prop(ox + 2600f, back, Z.BACK, 1f, 0) { cc, _ ->
                        D.rect(cc, -170f, -60f, 340f, 60f, shade(C.SIDEWALK, 0.7f), 8f)
                        D.shape(cc, 0xFF33404E.toInt()) { p ->
                            p.moveTo(-140f, -60f); p.lineTo(140f, -60f)
                            p.lineTo(120f, -300f); p.lineTo(-120f, -300f)
                        }
                        D.rect(cc, -150f, -318f, 300f, 40f, C.GREEN_DARK, 10f)
                        D.label(cc, "SUBWAY", 0f, -288f, 34f, C.WHITE)
                    }.sized(360f, 360f, -180f).interactive("peek down the subway") { ch, sc, p ->
                        ch.startAct(Act.REACH, 0.4f)
                        sc.fx.smoke(p.x, -80f, p.level, 5)
                        Sfx.play(Snd.WHOOSH)
                        true
                    })
                    add(Pushable(PushKind.SHOPPING_CART, ox + 4000f))
                    add(Item(ItemKind.FLOWER, ox + 3100f))
                }
            }
        }
    }

    /** A few lit windows above the shopfronts. */
    private fun upperWindows(cc: Canvas) {
        for (k in 0 until 3) {
            D.rect(cc, -150f + k * 110f, -400f, 74f, 56f, withAlpha(C.YELLOW, 200), 5f)
        }
    }
}
