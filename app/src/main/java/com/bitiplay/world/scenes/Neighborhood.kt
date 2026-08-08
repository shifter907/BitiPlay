package com.bitiplay.world.scenes

import android.graphics.Canvas
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.shade
import com.bitiplay.world.core.Cam
import com.bitiplay.world.engine.Render
import com.bitiplay.world.engine.Scene
import com.bitiplay.world.engine.Z
import com.bitiplay.world.ent.Animal
import com.bitiplay.world.ent.AnimalKind
import com.bitiplay.world.ent.DigSpot
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
import com.bitiplay.world.ent.Tent
import com.bitiplay.world.ent.Vehicle
import com.bitiplay.world.ent.VehicleKind
import kotlin.math.sin

class Neighborhood : Scene("hood", "Neighborhood") {

    override val width = 21600f
    override val accent = C.GRASS
    override val skyTop = C.SKY_TOP
    override val skyBottom = C.SKY_LOW

    /** Everything on the far side of the street sits slightly above the road. */
    private val back = -18f

    override fun drawTerrain(c: Canvas, cam: Cam, level: Int) {
        val hw = cam.visW * 0.5f + 80f

        c.save()
        c.translate(-cam.visW * 0.30f, -cam.visH * 0.60f)
        Art.sun(c, 62f, time)
        c.restore()

        Render.layer(cam, 450f, 0.25f, 110f) { px, _, i ->
            c.save()
            c.translate(px, -520f - ((i * 7) % 3) * 76f)
            Art.cloud(c, 50f + ((i * 13) % 4) * 11f, 240)
            c.restore()
        }

        Render.layer(cam, 900f, 0.25f) { px, _, i ->
            c.save()
            c.translate(px, back)
            Art.hill(c, 1250f, 250f + ((i * 11) % 3) * 66f, shade(C.GRASS_DARK, 0.86f))
            c.restore()
        }

        Render.layer(cam, 450f, 0.5f, 90f) { px, _, i ->
            c.save()
            c.translate(px, back)
            Art.pine(c, i, time, 210f + ((i * 17) % 4) * 44f)
            c.restore()
        }

        // street
        D.rect(c, -hw, back, hw * 2f, 136f, C.ROAD)
        D.rect(c, -hw, back, hw * 2f, 9f, shade(C.ROAD, 1.22f))
        Render.tiles(cam, 270f, 60f) { dx, _, _ ->
            D.rect(c, dx, 46f, 132f, 12f, C.ROAD_LINE, 6f)
        }
        // kerb, pavement, verge
        D.rect(c, -hw, 118f, hw * 2f, 13f, C.SIDEWALK_DARK)
        D.rect(c, -hw, 131f, hw * 2f, 48f, C.SIDEWALK)
        Render.tiles(cam, 150f) { dx, _, _ ->
            D.line(c, dx, 133f, dx, 177f, C.SIDEWALK_DARK, 3f)
        }
        D.rect(c, -hw, 179f, hw * 2f, 220f, C.GRASS)
        D.rect(c, -hw, 179f, hw * 2f, 8f, C.GRASS_LIGHT)
        Render.tiles(cam, 135f, 40f) { dx, _, i ->
            if (i % 3 != 0) {
                c.save()
                c.translate(dx, 199f)
                Art.tuft(c, 22f, C.GRASS_DARK)
                c.restore()
            }
        }
        Render.tiles(cam, 675f, 200f) { dx, _, i ->
            c.save()
            c.translate(dx, 212f)
            Art.flowerPatch(c, i)
            c.restore()
        }
    }

    override fun drawThumb(c: Canvas, w: Float, h: Float) {
        D.rect(c, 0f, 0f, w, h, C.SKY_MID)
        D.circle(c, w * 0.82f, h * 0.2f, h * 0.13f, C.SUN)
        D.rect(c, 0f, h * 0.52f, w, h * 0.48f, C.GRASS)
        D.rect(c, 0f, h * 0.72f, w, h * 0.18f, C.ROAD)
        c.save()
        c.translate(w * 0.3f, h * 0.56f)
        c.scale(h / 620f, h / 620f)
        Art.house(c, 300f, 220f, C.CREAM, C.RED, 0)
        c.restore()
        c.save()
        c.translate(w * 0.72f, h * 0.56f)
        c.scale(h / 700f, h / 700f)
        Art.tree(c, 1, 0f, 330f)
        c.restore()
    }

    override fun spawnX(index: Int): Float = 1500f + index * 165f

    /** Four streets' worth of neighbourhood, each block with its own character. */
    override fun build() {
        for (b in 0 until 4) {
            buildBlock(b * 5400f, b)
        }
        buildOldBlock()
    }

    private fun buildBlock(ox: Float, b: Int) {
        val walls = intArrayOf(C.CREAM, 0xFFCFE3F2.toInt(), 0xFFF6DDD0.toInt(), 0xFFDCEFD6.toInt())
        val roofs = intArrayOf(C.RED, C.BLUE_DARK, C.ORANGE_DARK, C.PURPLE_DARK)

        // houses and greenery along the whole block
        for (i in 0 until 5) {
            val hx = ox + 300f + i * 1020f
            add(Prop(hx, back, Z.FAR, 0.75f, i + b) { cc, p ->
                Art.house(cc, 420f, 300f, walls[p.seed % walls.size], roofs[p.seed % roofs.size], p.seed)
            }.sized(520f, 440f, -220f))
        }
        for (i in 0 until 5) {
            add(Prop(ox + 760f + i * 1020f, back, Z.BACK, 1f, i + b * 3) { cc, p ->
                Art.tree(cc, p.seed, p.t, 330f + (p.seed % 3) * 34f)
            }.sized(280f, 380f, -190f))
        }
        for (i in 0 until 5) {
            add(Prop(ox + 480f + i * 1020f, back, Z.BACK, 1f, i) { cc, _ ->
                Art.fence(cc, 300f, 120f)
            }.sized(300f, 130f, -65f))
        }
        add(Prop(ox + 1900f, back, Z.BACK, 1f, 0) { cc, _ -> Art.lamppost(cc, 300f) }
            .sized(90f, 330f, -165f))
        add(Prop(ox + 4100f, back, Z.BACK, 1f, 1) { cc, _ -> Art.lamppost(cc, 300f) }
            .sized(90f, 330f, -165f))
        add(Prop(ox + 2440f, 140f, Z.MAIN, 1f, 0) { cc, _ -> Art.bench(cc, 170f) }
            .sized(190f, 140f, -70f))

        // one themed corner per block
        when (b) {
            0 -> {
                add(Plant(PlantKind.FLOWERS, ox + 2620f, 2))
                add(Plant(PlantKind.TOMATO, ox + 2790f, 1))
                add(Plant(PlantKind.CARROTS, ox + 2960f, 0))
                add(Plant(PlantKind.SUNFLOWER, ox + 3130f, 2))
                add(Item(ItemKind.WATERING_CAN, ox + 2480f))
                add(Pushable(PushKind.WHEELBARROW, ox + 3620f))
                add(DigSpot(ox + 3320f, ItemKind.BONE))
                add(DigSpot(ox + 3470f))
            }
            1 -> {
                add(Tent(ox + 2400f, C.GREEN_DARK))
                add(Grill(ox + 2800f))
                add(Prop(ox + 2620f, 0f, Z.MAIN, 1f, 0) { cc, p ->
                    D.capsule(cc, -50f, 0f, 0f, -70f, 10f, C.WOOD_DARK)
                    D.capsule(cc, 50f, 0f, 0f, -70f, 10f, C.WOOD_DARK)
                    D.capsule(cc, -34f, -12f, 34f, -12f, 12f, C.WOOD)
                    val f = sin(p.t * 8f) * 4f
                    D.oval(cc, 0f, -24f, 34f + f, 14f, C.ORANGE)
                    D.oval(cc, 0f, -30f, 18f, 9f, C.YELLOW)
                }.sized(140f, 110f, -55f))
                add(Item(ItemKind.MARSHMALLOW, ox + 2500f))
                add(Item(ItemKind.PATTY_RAW, ox + 3000f))
                add(DigSpot(ox + 4200f, ItemKind.TREASURE))
            }
            2 -> {
                // little playground
                add(Prop(ox + 2500f, 40f, Z.MAIN, 1f, 0) { cc, p ->
                    D.shadow(cc, 0f, 0f, 150f, 20f)
                    D.capsule(cc, -130f, 0f, -130f, -260f, 14f, C.METAL_DARK)
                    D.capsule(cc, 130f, 0f, 130f, -260f, 14f, C.METAL_DARK)
                    D.capsule(cc, -140f, -260f, 140f, -260f, 14f, C.METAL_DARK)
                    val sw = sin(p.t * 1.6f) * 0.28f
                    for (k in -1..1 step 2) {
                        val sx = k * 62f
                        val ex = sx + sin(p.t * 1.6f + k) * 40f
                        D.capsule(cc, sx, -256f, ex, -90f, 5f, C.METAL)
                        D.capsule(cc, sx + 26f, -256f, ex + 26f, -90f, 5f, C.METAL)
                        D.rect(cc, ex - 6f, -92f, 38f, 12f, if (k < 0) C.RED else C.BLUE, 5f)
                    }
                    if (sw > 9f) D.circle(cc, 0f, 0f, 1f, C.WHITE)
                }.sized(320f, 300f, -150f).interactive("push the swings") { ch, sc, p ->
                    ch.startAct(com.bitiplay.world.ent.Act.PET, 0.6f)
                    sc.fx.sparkles(p.x, -200f, p.level, 6, C.WHITE)
                    com.bitiplay.world.audio.Sfx.play(com.bitiplay.world.audio.Snd.CREAK)
                    true
                })
                add(Prop(ox + 3100f, 60f, Z.MAIN, 1f, 0) { cc, _ ->
                    D.shadow(cc, 0f, 0f, 130f, 18f)
                    D.capsule(cc, -90f, 0f, -90f, -220f, 12f, C.METAL_DARK)
                    D.shape(cc, C.YELLOW) { p ->
                        p.moveTo(-104f, -220f); p.lineTo(-60f, -220f)
                        p.lineTo(120f, -10f); p.lineTo(60f, -10f)
                    }
                    D.capsule(cc, -60f, -226f, -60f, -10f, 8f, C.METAL)
                }.sized(280f, 260f, -130f))
                add(Item(ItemKind.BALL, ox + 2900f))
                add(Item(ItemKind.FRISBEE, ox + 3400f))
            }
            else -> {
                add(Plant(PlantKind.BUSH, ox + 2400f, 3))
                add(Plant(PlantKind.SAPLING, ox + 2700f, 2))
                add(Plant(PlantKind.FLOWERS, ox + 3000f, 3))
                add(Dispenser(ox + 3400f, ItemKind.APPLE, "pick an apple", Z.MAIN) { cc, _ ->
                    Art.tree(cc, 5, 0f, 400f, C.LEAF_DARK)
                    D.circle(cc, -60f, -250f, 15f, C.RED)
                    D.circle(cc, 46f, -286f, 15f, C.RED)
                    D.circle(cc, 8f, -214f, 15f, C.RED)
                }.sized(300f, 430f, -215f))
                add(Pushable(PushKind.WAGON, ox + 3900f))
                add(DigSpot(ox + 4400f, ItemKind.KEY))
            }
        }

        // vehicles, residents and pets
        add(Vehicle(
            when (b) {
                0 -> VehicleKind.BIKE
                1 -> VehicleKind.CAR
                2 -> VehicleKind.TRIKE
                else -> VehicleKind.SCOOTER
            },
            ox + 1500f, intArrayOf(C.TEAL, C.BLUE, C.PINK, C.PURPLE)[b]
        ))
        add(Vehicle(if (b % 2 == 0) VehicleKind.CAR else VehicleKind.BIKE,
            ox + 4600f, intArrayOf(C.RED, C.GREEN, C.YELLOW, C.ORANGE)[b]))

        add(Npc(NpcRoster.spec(b), ox + 1200f))
        add(Npc(NpcRoster.spec(b + 3), ox + 3800f))
        add(Animal(
            when (b) {
                0 -> AnimalKind.DOG
                1 -> AnimalKind.CAT
                2 -> AnimalKind.BUNNY
                else -> AnimalKind.DOG
            },
            ox + 900f
        ))
        add(Animal(AnimalKind.PIGEON, ox + 2020f))
        add(Animal(AnimalKind.PIGEON, ox + 2120f))
    }

    /** Mailbox and a couple of odds and ends kept from the original street. */
    private fun buildOldBlock() {
        // mailbox that pops open
        add(Prop(1260f, back, Z.MAIN, 1f, 0) { cc, p ->
            D.capsule(cc, 0f, 0f, 0f, -120f, 14f, C.WOOD)
            val open = p.state == 1
            D.rect(cc, -46f, -186f, 92f, 66f, C.BLUE, 12f)
            D.arcFill(cc, 0f, -186f, 46f, 34f, 180f, 180f, shade(C.BLUE, 1.15f))
            if (open) {
                D.rect(cc, 40f, -172f, 44f, 12f, shade(C.BLUE, 0.8f), 5f)
                D.rect(cc, 12f, -206f, 60f, 40f, C.OFF_WHITE, 4f)
            }
            D.capsule(cc, -52f, -196f, -52f, -160f, 7f, C.RED)
            D.rect(cc, -66f, -204f, 30f, 20f, C.RED, 4f)
        }.sized(140f, 220f, -120f).interactive("open the mailbox") { ch, sc, p ->
            p.state = if (p.state == 1) 0 else 1
            ch.startAct(com.bitiplay.world.ent.Act.REACH, 0.35f)
            sc.fx.sparkles(p.x, -200f, p.level, 5, C.WHITE)
            com.bitiplay.world.audio.Sfx.play(com.bitiplay.world.audio.Snd.POP)
            true
        })

        // tools and toys scattered the length of the neighbourhood
        add(Item(ItemKind.SHOVEL, 3230f))
        add(Item(ItemKind.TEDDY, 2180f))
        add(Item(ItemKind.BALL, 6400f))
        add(Item(ItemKind.WATERING_CAN, 8600f))
        add(Item(ItemKind.SHOVEL, 12400f))
        add(Item(ItemKind.WATERING_CAN, 16900f))
        add(Item(ItemKind.CARROT, 19400f))
        add(Item(ItemKind.FRISBEE, 14800f))

        add(Plant(PlantKind.BUSH, 640f, 3))
        add(Plant(PlantKind.BUSH, 10800f, 3))
        add(Plant(PlantKind.BUSH, 20900f, 3))

        // cool box next to the camp block's grill
        add(Dispenser(8500f, ItemKind.PATTY_RAW, "grab a patty", Z.MAIN) { cc, _ ->
            D.rect(cc, -70f, -120f, 140f, 120f, C.OFF_WHITE, 10f)
            D.rect(cc, -70f, -136f, 140f, 20f, C.RED, 8f)
            D.rect(cc, -46f, -104f, 92f, 60f, C.GLASS, 6f)
            D.label(cc, "COOL", 0f, -70f, 26f, C.BLUE_DARK)
        }.sized(180f, 180f, -90f))
    }
}
