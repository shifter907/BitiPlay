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
import com.bitiplay.world.ent.DigSpot
import com.bitiplay.world.ent.Dispenser
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

class Farm : Scene("farm", "Farm") {

    override val width = 21600f
    override val accent = C.GRASS_DARK
    override val skyTop = 0xFF5CC3EA.toInt()
    override val skyBottom = 0xFFE4F4D8.toInt()

    private val back = -16f

    override fun drawTerrain(c: Canvas, cam: Cam, level: Int) {
        val hw = cam.visW * 0.5f + 80f

        c.save()
        c.translate(-cam.visW * 0.34f, -cam.visH * 0.58f)
        Art.sun(c, 58f, time)
        c.restore()

        Render.layer(cam, 450f, 0.25f, 200f) { px, _, i ->
            c.save()
            c.translate(px, -480f - ((i * 3) % 3) * 70f)
            Art.cloud(c, 54f + ((i * 7) % 3) * 12f, 235)
            c.restore()
        }

        // rolling fields behind
        Render.layer(cam, 900f, 0.25f) { px, _, i ->
            c.save()
            c.translate(px, back)
            Art.hill(c, 1400f, 210f + ((i * 5) % 3) * 60f, shade(C.GRASS_DARK, 0.82f))
            c.restore()
        }
        Render.layer(cam, 675f, 0.5f, 120f) { px, _, i ->
            c.save()
            c.translate(px, back)
            Art.hill(c, 900f, 130f + ((i * 7) % 3) * 40f, shade(C.GRASS_DARK, 0.96f))
            c.restore()
        }

        // ploughed field strip behind the walk line
        D.rect(c, -hw, back, hw * 2f, 26f, C.GRASS_DARK)

        // ground
        D.rect(c, -hw, 8f, hw * 2f, 100f, C.GRASS)
        D.rect(c, -hw, 8f, hw * 2f, 9f, C.GRASS_LIGHT)
        // dirt track
        D.rect(c, -hw, 104f, hw * 2f, 66f, C.DIRT)
        D.rect(c, -hw, 104f, hw * 2f, 8f, shade(C.DIRT, 1.15f))
        Render.tiles(cam, 150f, 20f) { dx, _, i ->
            if (i % 2 == 0) D.oval(c, dx, 138f, 30f, 9f, shade(C.DIRT, 0.86f))
        }
        D.rect(c, -hw, 168f, hw * 2f, 230f, C.GRASS)
        Render.tiles(cam, 135f, 60f) { dx, _, i ->
            if (i % 3 != 1) {
                c.save()
                c.translate(dx, 188f)
                Art.tuft(c, 24f, C.GRASS_DARK)
                c.restore()
            }
        }
        // furrows in the foreground
        Render.tiles(cam, 225f, 40f) { dx, _, _ ->
            D.capsule(c, dx, 212f, dx + 150f, 212f, 10f, shade(C.GRASS, 0.9f))
        }
    }

    override fun drawThumb(c: Canvas, w: Float, h: Float) {
        D.rect(c, 0f, 0f, w, h, 0xFFA8DDF0.toInt())
        D.rect(c, 0f, h * 0.58f, w, h * 0.42f, C.GRASS)
        D.rect(c, 0f, h * 0.74f, w, h * 0.12f, C.DIRT)
        c.save()
        c.translate(w * 0.36f, h * 0.62f)
        c.scale(h / 700f, h / 700f)
        barn(c, 0f)
        c.restore()
        c.save()
        c.translate(w * 0.76f, h * 0.9f)
        c.scale(h / 520f, h / 520f)
        D.oval(c, 0f, -46f, 44f, 30f, C.WHITE)
        D.circle(c, 34f, -74f, 24f, C.WHITE)
        D.oval(c, -18f, -52f, 16f, 14f, 0xFF4A4038.toInt())
        c.restore()
    }

    private fun barn(c: Canvas, t: Float) {
        D.rect(c, -230f, -320f, 460f, 320f, C.RED, 8f)
        D.shape(c, C.RED_DARK) { p ->
            p.moveTo(-256f, -320f); p.lineTo(-150f, -430f)
            p.lineTo(150f, -430f); p.lineTo(256f, -320f)
        }
        D.rect(c, -262f, -330f, 524f, 18f, shade(C.RED_DARK, 0.85f), 6f)
        // big doors
        D.rect(c, -110f, -230f, 220f, 230f, C.OFF_WHITE, 6f)
        D.rect(c, -110f, -230f, 220f, 230f, shade(C.CREAM, 0.96f), 6f)
        D.line(c, 0f, -230f, 0f, 0f, C.RED_DARK, 8f)
        D.line(c, -104f, -226f, -6f, -6f, C.RED_DARK, 8f)
        D.line(c, -6f, -226f, -104f, -6f, C.RED_DARK, 8f)
        D.line(c, 104f, -226f, 6f, -6f, C.RED_DARK, 8f)
        D.line(c, 6f, -226f, 104f, -6f, C.RED_DARK, 8f)
        // hayloft
        D.rect(c, -46f, -400f, 92f, 74f, C.OFF_WHITE, 6f)
        D.rect(c, -38f, -392f, 76f, 58f, 0xFF3A2C22.toInt(), 4f)
        // weather vane
        D.capsule(c, 0f, -430f, 0f, -486f, 6f, C.METAL_DARK)
        D.tri(c, -30f, -474f, 26f, -486f, 26f, -462f, C.METAL_DARK)
    }

    override fun spawnX(index: Int): Float = 1050f + index * 165f

    override fun build() {
        // barn + silo
        add(Prop(700f, back, Z.BACK, 1f, 0) { cc, p -> barn(cc, p.t) }.sized(560f, 500f, -250f))
        add(Prop(1180f, back, Z.FAR, 0.75f, 0) { cc, _ ->
            D.rect(cc, -80f, -470f, 160f, 470f, C.METAL, 12f)
            for (i in 0 until 7) D.line(cc, -78f, -60f - i * 62f, 78f, -60f - i * 62f, C.METAL_DARK, 5f)
            D.arcFill(cc, 0f, -470f, 86f, 84f, 180f, 180f, C.METAL_DARK)
            D.circle(cc, 0f, -474f, 14f, C.YELLOW_DARK)
        }.sized(200f, 560f, -280f))

        // windmill with turning blades
        add(Prop(4900f, back, Z.FAR, 0.75f, 0) { cc, p ->
            D.shape(cc, C.WOOD) { pp ->
                pp.moveTo(-70f, 0f); pp.lineTo(-36f, -420f)
                pp.lineTo(36f, -420f); pp.lineTo(70f, 0f)
            }
            D.rect(cc, -46f, -440f, 92f, 34f, C.WOOD_DARK, 8f)
            sails(cc, p.t)
        }.sized(320f, 520f, -260f))

        // fences
        for (i in 0 until 6) {
            add(Prop(300f + i * 900f, back, Z.BACK, 1f, i) { cc, _ ->
                Art.fence(cc, 320f, 130f, C.WOOD_LIGHT)
            }.sized(320f, 140f, -70f))
        }

        add(Prop(2100f, 196f, Z.MAIN, 1f, 0) { cc, _ ->
            // water trough
            D.shadow(cc, 0f, 0f, 90f, 15f)
            D.rect(cc, -90f, -70f, 180f, 70f, C.WOOD, 8f)
            D.rect(cc, -78f, -60f, 156f, 22f, C.SEA_LIGHT, 5f)
        }.sized(200f, 100f, -50f))

        // crops
        add(Plant(PlantKind.CORN, 2380f, 2))
        add(Plant(PlantKind.CORN, 2560f, 1))
        add(Plant(PlantKind.CORN, 2740f, 3))
        add(Plant(PlantKind.TOMATO, 2960f, 2))
        add(Plant(PlantKind.LETTUCE, 3120f, 3))
        add(Plant(PlantKind.PUMPKIN_VINE, 3300f, 2))
        add(Plant(PlantKind.CARROTS, 3480f, 1))
        add(Plant(PlantKind.SUNFLOWER, 3660f, 3))

        add(DigSpot(3900f, ItemKind.SEED_BAG))
        add(DigSpot(4040f))

        add(Item(ItemKind.WATERING_CAN, 2260f))
        add(Item(ItemKind.SHOVEL, 3820f))
        add(Item(ItemKind.HAY, 1560f))
        add(Item(ItemKind.HAY, 1660f))
        add(Item(ItemKind.CARROT, 4460f))
        add(Item(ItemKind.APPLE, 5180f))

        add(Pushable(PushKind.WHEELBARROW, 2000f))
        add(Pushable(PushKind.ZOO_CART, 4200f, C.WOOD))

        // well
        add(Prop(4560f, 20f, Z.MAIN, 1f, 0) { cc, p ->
            D.shadow(cc, 0f, 0f, 92f, 16f)
            D.rect(cc, -80f, -110f, 160f, 110f, C.METAL_DARK, 10f)
            D.oval(cc, 0f, -110f, 80f, 22f, 0xFF2E3A44.toInt())
            D.capsule(cc, -66f, -110f, -66f, -250f, 12f, C.WOOD)
            D.capsule(cc, 66f, -110f, 66f, -250f, 12f, C.WOOD)
            D.shape(cc, C.RED) { pp ->
                pp.moveTo(-108f, -250f); pp.lineTo(0f, -320f); pp.lineTo(108f, -250f)
            }
            val swingY = -150f + sin(p.t * 1.4f) * 8f
            D.capsule(cc, 0f, -244f, 0f, swingY, 4f, C.WOOD_DARK)
            D.rect(cc, -22f, swingY, 44f, 34f, C.WOOD, 5f)
        }.sized(240f, 340f, -170f).interactive("the old well") { ch, sc, p ->
            ch.startAct(Act.REACH, 0.4f)
            sc.fx.bubbles(p.x, -120f, p.level, 5)
            Sfx.play(Snd.SPLASH)
            true
        })

        // chicken coop hands out eggs
        add(Dispenser(1900f, ItemKind.EGG, "collect an egg", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 100f, 16f)
            D.rect(cc, -90f, -130f, 180f, 130f, C.WOOD_LIGHT, 8f)
            D.shape(cc, C.RED_DARK) { p ->
                p.moveTo(-108f, -130f); p.lineTo(0f, -210f); p.lineTo(108f, -130f)
            }
            D.arcFill(cc, 0f, 0f, 40f, 62f, 180f, 180f, 0xFF3A2C22.toInt())
            D.capsule(cc, -20f, 0f, 60f, -60f, 10f, C.WOOD)
        }.sized(230f, 240f, -120f))

        // seed stall
        add(Dispenser(3980f, ItemKind.SEED_BAG, "take some seeds", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 86f, 15f)
            D.rect(cc, -76f, -110f, 152f, 110f, C.WOOD, 6f)
            Art.awning(cc, 190f, -160f, C.GREEN, C.CREAM)
            D.circle(cc, -30f, -132f, 16f, C.LEAF_DARK)
            D.circle(cc, 20f, -136f, 16f, C.YELLOW_DARK)
        }.sized(210f, 200f, -100f))

        // hay bale stack in the foreground
        add(Prop(1380f, 202f, Z.MAIN, 1f, 0) { cc, _ ->
            D.shadow(cc, 0f, 0f, 100f, 16f)
            for (i in 0 until 2) {
                for (k in 0 until (2 - i)) {
                    val bx = -60f + k * 120f + i * 60f
                    val by = -50f - i * 92f
                    D.rect(cc, bx - 62f, by - 46f, 124f, 92f, C.YELLOW_DARK, 14f)
                    D.rect(cc, bx - 62f, by - 46f, 124f, 74f, shade(C.YELLOW, 0.98f), 12f)
                    D.line(cc, bx - 26f, by - 40f, bx - 26f, by + 34f, C.WOOD_DARK, 5f)
                    D.line(cc, bx + 26f, by - 40f, bx + 26f, by + 34f, C.WOOD_DARK, 5f)
                }
            }
        }.sized(280f, 220f, -110f))

        add(Vehicle(VehicleKind.TRACTOR, 1700f))
        add(Vehicle(VehicleKind.BIKE, 4700f, C.GREEN))

        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(1), 2550f))
        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(4), 4450f))

        add(Animal(AnimalKind.COW, 2160f).pen(220f))
        add(Animal(AnimalKind.COW, 2460f).pen(200f))
        add(Animal(AnimalKind.PIG, 3060f).pen(180f))
        add(Animal(AnimalKind.PIG, 3200f).pen(170f))
        add(Animal(AnimalKind.SHEEP, 4340f).pen(200f))
        add(Animal(AnimalKind.SHEEP, 4520f).pen(190f))
        add(Animal(AnimalKind.HORSE, 5060f).pen(260f))
        add(Animal(AnimalKind.GOAT, 900f).pen(200f))
        add(Animal(AnimalKind.CHICKEN, 1780f).pen(150f))
        add(Animal(AnimalKind.CHICKEN, 1980f).pen(150f))
        add(Animal(AnimalKind.CHICKEN, 2080f).pen(140f))
        add(Animal(AnimalKind.DUCK, 4620f).pen(170f))
        add(Animal(AnimalKind.GOOSE, 620f).pen(170f))
        add(Animal(AnimalKind.DOG, 1240f))

        buildMore()
    }

    /** Three more farmsteads down the valley. */
    private fun buildMore() {
        for (b in 1 until 4) {
            val ox = b * 5400f

            for (i in 0 until 6) {
                add(Prop(ox + 200f + i * 900f, back, Z.BACK, 1f, i + b) { cc, _ ->
                    Art.fence(cc, 320f, 130f, C.WOOD_LIGHT)
                }.sized(320f, 140f, -70f))
            }
            for (i in 0 until 3) {
                add(Prop(ox + 700f + i * 1700f, back, Z.BACK, 1f, i + b * 2) { cc, p ->
                    Art.tree(cc, p.seed, p.t, 340f + (p.seed % 3) * 40f, C.LEAF_DARK)
                }.sized(280f, 380f, -190f))
            }

            // crop rows
            val crops = arrayOf(
                PlantKind.CORN, PlantKind.TOMATO, PlantKind.LETTUCE,
                PlantKind.PUMPKIN_VINE, PlantKind.CARROTS, PlantKind.SUNFLOWER
            )
            for (i in 0 until 6) {
                add(Plant(crops[(i + b) % crops.size], ox + 1800f + i * 200f, (i + b) % 4))
            }
            add(Item(ItemKind.WATERING_CAN, ox + 1650f))
            add(Item(ItemKind.SHOVEL, ox + 3200f))
            add(DigSpot(ox + 3400f, if (b == 2) ItemKind.TREASURE else ItemKind.SEED_BAG))
            add(Pushable(PushKind.WHEELBARROW, ox + 3000f))

            add(Npc(NpcRoster.spec(b + 1), ox + 2400f))
            add(Npc(NpcRoster.spec(b + 4), ox + 4300f))

            when (b) {
                1 -> {
                    // duck pond
                    add(Prop(ox + 4200f, 40f, Z.DECAL, 1f, 0) { cc, p ->
                        D.oval(cc, 0f, 30f, 300f, 76f, shade(C.GRASS_DARK, 0.9f))
                        D.oval(cc, 0f, 26f, 280f, 66f, C.SEA_LIGHT)
                        for (k in 0 until 4) {
                            val yy = 4f + k * 18f + sin(p.t * 1.4f + k) * 5f
                            D.capsule(cc, -160f + k * 50f, yy, -60f + k * 50f, yy, 7f,
                                withAlpha(C.FOAM, 130))
                        }
                    }.sized(600f, 160f, -20f))
                    add(Animal(AnimalKind.DUCK, ox + 4100f).pen(180f))
                    add(Animal(AnimalKind.DUCK, ox + 4300f).pen(180f))
                    add(Animal(AnimalKind.GOOSE, ox + 4450f).pen(180f))
                    add(Animal(AnimalKind.SHEEP, ox + 900f).pen(220f))
                    add(Animal(AnimalKind.SHEEP, ox + 1100f).pen(220f))
                    add(Vehicle(VehicleKind.TRACTOR, ox + 2900f))
                }
                2 -> {
                    // scarecrow watching the fields
                    add(Prop(ox + 2700f, 30f, Z.MAIN, 1f, 0) { cc, p ->
                        D.shadow(cc, 0f, 0f, 60f, 12f)
                        D.capsule(cc, 0f, 0f, 0f, -300f, 13f, C.WOOD)
                        D.capsule(cc, -140f, -230f, 140f, -222f, 11f, C.WOOD)
                        D.rect(cc, -70f, -270f, 140f, 130f, C.RED_DARK, 10f)
                        for (k in 0 until 4) {
                            val a = -0.6f + k * 0.4f
                            D.capsule(cc, -132f, -228f, -132f - cos(a) * 30f, -228f + sin(a) * 30f,
                                7f, C.YELLOW_DARK)
                        }
                        D.circle(cc, 0f, -318f, 46f, 0xFFE0C083.toInt())
                        D.arcFill(cc, 0f, -350f, 78f, 36f, 180f, 180f, C.YELLOW_DARK)
                        D.rect(cc, -78f, -354f, 156f, 12f, C.YELLOW_DARK, 5f)
                        D.circle(cc, -16f, -324f, 6f, C.BLACK)
                        D.circle(cc, 16f, -324f, 6f, C.BLACK)
                        D.arcLine(cc, 0f, -306f, 20f, 12f, 20f, 140f, C.BLACK, 4f)
                        if (sin(p.t) > 0.9f) D.circle(cc, 60f, -330f, 5f, C.WHITE)
                    }.sized(300f, 420f, -210f).interactive("say hi to the scarecrow") { ch, sc, p ->
                        ch.cheer(sc)
                        sc.fx.leaves(p.x, -260f, p.level, 5)
                        Sfx.play(Snd.CREAK)
                        true
                    })
                    add(Animal(AnimalKind.PIG, ox + 900f).pen(190f))
                    add(Animal(AnimalKind.PIG, ox + 1080f).pen(190f))
                    add(Animal(AnimalKind.GOAT, ox + 4400f).pen(200f))
                    add(Animal(AnimalKind.CHICKEN, ox + 4600f).pen(150f))
                }
                else -> {
                    add(Prop(ox + 1000f, back, Z.FAR, 0.75f, 0) { cc, _ ->
                        D.rect(cc, -80f, -470f, 160f, 470f, C.METAL, 12f)
                        for (i in 0 until 7) {
                            D.line(cc, -78f, -60f - i * 62f, 78f, -60f - i * 62f, C.METAL_DARK, 5f)
                        }
                        D.arcFill(cc, 0f, -470f, 86f, 84f, 180f, 180f, C.METAL_DARK)
                    }.sized(200f, 560f, -280f))
                    add(Dispenser(ox + 3800f, ItemKind.EGG, "collect an egg", Z.MAIN) { cc, _ ->
                        D.shadow(cc, 0f, 0f, 100f, 16f)
                        D.rect(cc, -90f, -130f, 180f, 130f, C.WOOD_LIGHT, 8f)
                        D.shape(cc, C.RED_DARK) { p ->
                            p.moveTo(-108f, -130f); p.lineTo(0f, -210f); p.lineTo(108f, -130f)
                        }
                        D.arcFill(cc, 0f, 0f, 40f, 62f, 180f, 180f, 0xFF3A2C22.toInt())
                    }.sized(230f, 240f, -120f))
                    add(Animal(AnimalKind.CHICKEN, ox + 3700f).pen(160f))
                    add(Animal(AnimalKind.CHICKEN, ox + 3900f).pen(160f))
                    add(Animal(AnimalKind.HORSE, ox + 900f).pen(260f))
                    add(Animal(AnimalKind.COW, ox + 4700f).pen(210f))
                    add(Vehicle(VehicleKind.BIKE, ox + 2200f, C.GREEN))
                }
            }
        }
    }

    /** Turning windmill sails. */
    private fun sails(cc: Canvas, t: Float) {
        val a = t * 0.55f
        for (i in 0 until 4) {
            val ang = a + i * 1.5707964f
            val ex = kotlin.math.cos(ang) * 180f
            val ey = -440f + kotlin.math.sin(ang) * 180f
            D.capsule(cc, 0f, -440f, ex, ey, 20f, C.OFF_WHITE)
            D.capsule(cc, 0f, -440f, ex * 0.9f, -440f + (ey + 440f) * 0.9f, 8f, withAlpha(C.WOOD_DARK, 200))
        }
        D.circle(cc, 0f, -440f, 16f, C.WOOD_DARK)
    }
}
