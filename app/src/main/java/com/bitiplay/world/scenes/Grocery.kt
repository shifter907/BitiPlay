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
import com.bitiplay.world.ent.Item
import com.bitiplay.world.ent.ItemKind
import com.bitiplay.world.ent.ItemArt
import com.bitiplay.world.ent.Npc
import com.bitiplay.world.ent.NpcRoster
import com.bitiplay.world.ent.Plant
import com.bitiplay.world.ent.PlantKind
import com.bitiplay.world.ent.Prop
import com.bitiplay.world.ent.PushKind
import com.bitiplay.world.ent.Pushable
import kotlin.math.sin

/** An interior scene: the sky is a ceiling, and the aisles loop forever. */
class Grocery : Scene("shop", "Grocery Store") {

    override val width = 21600f
    override val accent = C.ORANGE
    override val skyTop = 0xFFF3EFE4.toInt()
    override val skyBottom = 0xFFE2DCCC.toInt()

    private val back = -14f

    override fun drawTerrain(c: Canvas, cam: Cam, level: Int) {
        val hw = cam.visW * 0.5f + 80f

        // ceiling and lights
        D.rect(c, -hw, -900f, hw * 2f, 340f, 0xFFDCD6C6.toInt())
        Render.tiles(cam, 225f) { dx, _, _ ->
            D.rect(c, dx - 70f, -640f, 140f, 22f, withAlpha(C.YELLOW, 190), 8f)
            D.capsule(c, dx, -640f, dx, -700f, 5f, C.METAL_DARK)
        }
        D.rect(c, -hw, -566f, hw * 2f, 16f, 0xFFC8C0AE.toInt())

        // back wall
        D.rect(c, -hw, -560f, hw * 2f, 560f, 0xFFEFE9DA.toInt())
        Render.tiles(cam, 450f, 100f) { dx, _, i ->
            // wall signage
            val names = arrayOf("PRODUCE", "BAKERY", "DAIRY", "SNACKS", "DRINKS", "FROZEN")
            val cols = intArrayOf(C.GREEN, C.ORANGE, C.BLUE, C.RED, C.TEAL, C.PURPLE)
            val k = ((i % 6) + 6) % 6
            D.rect(c, dx - 130f, -520f, 260f, 62f, cols[k], 12f)
            D.label(c, names[k], dx, -478f, 30f, C.WHITE)
        }
        // tiled floor
        D.rect(c, -hw, back, hw * 2f, 30f, 0xFFCFC7B4.toInt())
        D.rect(c, -hw, 12f, hw * 2f, 420f, C.FLOOR)
        Render.tiles(cam, 150f) { dx, _, i ->
            if (i % 2 == 0) D.rect(c, dx, 16f, 150f, 412f, C.FLOOR_DARK)
            D.line(c, dx, 14f, dx, 428f, shade(C.FLOOR_DARK, 0.94f), 3f)
        }
        D.line(c, -hw, 110f, hw, 110f, shade(C.FLOOR_DARK, 0.94f), 3f)
        D.line(c, -hw, 205f, hw, 205f, shade(C.FLOOR_DARK, 0.94f), 3f)
        // shine
        Render.tiles(cam, 675f, 200f) { dx, _, _ ->
            D.oval(c, dx, 160f, 86f, 24f, withAlpha(C.WHITE, 60))
        }
    }

    override fun drawThumb(c: Canvas, w: Float, h: Float) {
        D.rect(c, 0f, 0f, w, h, 0xFFEFE9DA.toInt())
        D.rect(c, 0f, 0f, w, h * 0.16f, 0xFFDCD6C6.toInt())
        D.rect(c, w * 0.1f, h * 0.05f, w * 0.2f, h * 0.05f, withAlpha(C.YELLOW, 200), 3f)
        D.rect(c, w * 0.6f, h * 0.05f, w * 0.2f, h * 0.05f, withAlpha(C.YELLOW, 200), 3f)
        D.rect(c, 0f, h * 0.74f, w, h * 0.26f, C.FLOOR)
        for (i in 0 until 3) {
            val sx = w * (0.14f + i * 0.3f)
            D.rect(c, sx, h * 0.3f, w * 0.22f, h * 0.44f, C.METAL, 3f)
            for (k in 0 until 3) {
                D.rect(c, sx + 2f, h * (0.34f + k * 0.13f), w * 0.2f, h * 0.06f, C.ORANGE, 2f)
            }
        }
    }

    override fun spawnX(index: Int): Float = 700f + index * 165f

    /** Shelving unit with produce boxes drawn from a list of item kinds. */
    private fun shelf(cc: Canvas, kinds: Array<ItemKind>, t: Float, tint: Int) {
        D.shadow(cc, 0f, 0f, 150f, 22f)
        D.rect(cc, -150f, -360f, 300f, 360f, C.METAL, 8f)
        D.rect(cc, -150f, -360f, 300f, 26f, tint, 8f)
        for (row in 0 until 3) {
            val sy = -80f - row * 96f
            D.rect(cc, -146f, sy, 292f, 16f, shade(C.METAL, 0.82f), 5f)
            for (k in 0 until 3) {
                val kind = kinds[(row * 3 + k) % kinds.size]
                cc.save()
                cc.translate(-96f + k * 96f, sy - 34f)
                cc.scale(0.78f, 0.78f)
                ItemArt.draw(cc, kind, t)
                cc.restore()
            }
        }
        D.rect(cc, -150f, -16f, 300f, 16f, shade(C.METAL, 0.7f), 5f)
    }

    override fun build() {
        // ---- aisles --------------------------------------------------------
        val produce = arrayOf(ItemKind.APPLE, ItemKind.CARROT, ItemKind.TOMATO, ItemKind.LETTUCE,
            ItemKind.BANANA, ItemKind.PUMPKIN)
        val bakery = arrayOf(ItemKind.BREAD, ItemKind.DONUT, ItemKind.CUPCAKE, ItemKind.PRETZEL)
        val dairy = arrayOf(ItemKind.MILK, ItemKind.EGG, ItemKind.JUICE)
        val snacks = arrayOf(ItemKind.POPCORN, ItemKind.PIZZA, ItemKind.MARSHMALLOW, ItemKind.DONUT)

        class Aisle(
            val x: Float,
            val give: ItemKind,
            val label: String,
            val kinds: Array<ItemKind>,
            val tint: Int
        )

        val aisles = listOf(
            Aisle(700f, ItemKind.APPLE, "take an apple", produce, C.GREEN),
            Aisle(1150f, ItemKind.CARROT, "take a carrot", produce, C.GREEN),
            Aisle(1600f, ItemKind.BREAD, "take some bread", bakery, C.ORANGE),
            Aisle(2050f, ItemKind.DONUT, "take a donut", bakery, C.ORANGE),
            Aisle(2500f, ItemKind.MILK, "take some milk", dairy, C.BLUE),
            Aisle(2950f, ItemKind.EGG, "take some eggs", dairy, C.BLUE),
            Aisle(3400f, ItemKind.POPCORN, "take popcorn", snacks, C.RED),
            Aisle(3850f, ItemKind.PIZZA, "take a pizza", snacks, C.RED),
            Aisle(4300f, ItemKind.JUICE, "take a juice", dairy, C.TEAL),
            Aisle(4750f, ItemKind.WATERMELON, "take a watermelon", produce, C.GREEN)
        )
        for (a in aisles) {
            add(Dispenser(a.x, a.give, a.label, Z.MAIN) { cc, d ->
                shelf(cc, a.kinds, d.t, a.tint)
            }.sized(320f, 400f, -200f))
        }

        // ---- produce island ------------------------------------------------
        add(Dispenser(5150f, ItemKind.TOMATO, "fresh tomatoes", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 130f, 20f)
            D.shape(cc, C.WOOD) { p ->
                p.moveTo(-140f, -160f); p.lineTo(140f, -160f)
                p.lineTo(110f, 0f); p.lineTo(-110f, 0f)
            }
            D.oval(cc, 0f, -160f, 140f, 34f, C.WOOD_LIGHT)
            for (i in 0 until 9) {
                val ax = -96f + (i * 27) % 192
                val ay = -172f - (i % 3) * 16f
                D.circle(cc, ax, ay, 19f, if (i % 3 == 0) C.RED else shade(C.RED, 1.08f))
                D.star(cc, ax, ay - 14f, 8f, 3f, 5, C.LEAF)
            }
        }.sized(320f, 260f, -130f))

        // ---- freezer -------------------------------------------------------
        add(Prop(4550f, back, Z.BACK, 1f, 0) { cc, p ->
            D.rect(cc, -170f, -400f, 340f, 400f, 0xFFB7D8E8.toInt(), 10f)
            D.rect(cc, -150f, -370f, 300f, 330f, withAlpha(C.GLASS, 210), 8f)
            D.line(cc, 0f, -370f, 0f, -40f, C.METAL, 8f)
            for (row in 0 until 3) {
                D.rect(cc, -142f, -320f + row * 100f, 284f, 12f, C.METAL, 4f)
            }
            val frost = 120 + (sin(p.t) * 30f).toInt()
            D.rect(cc, -150f, -370f, 300f, 330f, withAlpha(C.WHITE, frost), 8f)
            D.label(cc, "FROZEN", 0f, -420f, 34f, C.BLUE_DARK)
        }.sized(380f, 460f, -230f))

        // ---- checkout ------------------------------------------------------
        add(Prop(200f, 40f, Z.MAIN, 1f, 0) { cc, p ->
            D.shadow(cc, 0f, 0f, 170f, 24f)
            D.rect(cc, -190f, -120f, 380f, 120f, C.CREAM, 10f)
            D.rect(cc, -190f, -140f, 380f, 24f, shade(C.CREAM, 0.82f), 8f)
            // conveyor
            D.rect(cc, -170f, -152f, 250f, 18f, 0xFF44484E.toInt(), 6f)
            val off = (p.t * 40f) % 40f
            for (k in 0 until 7) {
                D.line(cc, -166f + k * 40f + off, -150f, -166f + k * 40f + off, -136f, 0xFF5C666F.toInt(), 4f)
            }
            // register
            D.rect(cc, 96f, -240f, 96f, 100f, C.METAL, 8f)
            D.rect(cc, 106f, -228f, 76f, 42f, C.GLASS, 5f)
            for (k in 0 until 6) {
                D.rect(cc, 108f + (k % 3) * 26f, -176f + (k / 3) * 20f, 20f, 14f, C.OFF_WHITE, 3f)
            }
            D.rect(cc, -60f, -186f, 60f, 46f, C.RED, 6f)
        }.sized(420f, 280f, -140f).interactive("beep it through the till") { ch, sc, p ->
            val held = ch.carried
            ch.startAct(Act.REACH, 0.45f)
            if (held != null) {
                sc.fx.sparkles(p.x, -170f, p.level, 8, C.YELLOW)
                Sfx.play(Snd.CHIME)
            } else {
                Sfx.play(Snd.UI)
            }
            true
        })

        // ---- carts ---------------------------------------------------------
        add(Pushable(PushKind.SHOPPING_CART, 520f))
        add(Pushable(PushKind.SHOPPING_CART, 2300f))
        add(Pushable(PushKind.SHOPPING_CART, 4050f))

        // ---- loose bits ----------------------------------------------------
        add(Item(ItemKind.BALL, 1380f))
        add(Item(ItemKind.BREAD, 620f))
        add(Item(ItemKind.APPLE, 3200f))
        add(Item(ItemKind.BANANA, 2760f))
        add(Item(ItemKind.CUPCAKE, 4480f))
        add(Item(ItemKind.FLOWER, 5300f))

        add(Plant(PlantKind.FLOWERS, 900f, 3))
        add(Plant(PlantKind.BUSH, 3620f, 3))

        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(1), 1950f))
        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(4), 3720f))

        add(Animal(AnimalKind.CAT, 1800f))
        add(Animal(AnimalKind.DOG, 4200f))

        // stacked crates in the foreground
        for (i in 0 until 4) {
            add(Prop(1000f + i * 1350f, 198f, Z.MAIN, 1f, i) { cc, p ->
                D.shadow(cc, 0f, 0f, 80f, 14f)
                val n = 1 + p.seed % 2
                for (k in 0 until n) {
                    D.rect(cc, -66f, -80f - k * 84f, 132f, 80f, if (k % 2 == 0) C.WOOD else C.WOOD_LIGHT, 8f)
                    D.line(cc, -56f, -70f - k * 84f, 56f, -70f - k * 84f, C.WOOD_DARK, 5f)
                    D.line(cc, -56f, -20f - k * 84f, 56f, -20f - k * 84f, C.WOOD_DARK, 5f)
                }
            }.sized(160f, 200f, -100f))
        }

        buildMore()
    }

    /** Three more departments further down the store. */
    private fun buildMore() {
        val produce = arrayOf(ItemKind.APPLE, ItemKind.CARROT, ItemKind.TOMATO, ItemKind.LETTUCE,
            ItemKind.BANANA, ItemKind.PUMPKIN)
        val bakery = arrayOf(ItemKind.BREAD, ItemKind.DONUT, ItemKind.CUPCAKE, ItemKind.PRETZEL)
        val dairy = arrayOf(ItemKind.MILK, ItemKind.EGG, ItemKind.JUICE)
        val snacks = arrayOf(ItemKind.POPCORN, ItemKind.PIZZA, ItemKind.MARSHMALLOW, ItemKind.DONUT)
        val gives = arrayOf(
            ItemKind.WATERMELON, ItemKind.CUPCAKE, ItemKind.JUICE, ItemKind.PRETZEL,
            ItemKind.LETTUCE, ItemKind.MILK, ItemKind.PIZZA, ItemKind.BANANA,
            ItemKind.PUMPKIN, ItemKind.BREAD
        )
        val kindSets = arrayOf(produce, bakery, dairy, snacks)
        val tints = intArrayOf(C.GREEN, C.ORANGE, C.BLUE, C.RED)

        for (b in 1 until 4) {
            val ox = b * 5400f
            for (i in 0 until 10) {
                val kinds = kindSets[(i + b) % kindSets.size]
                val tint = tints[(i + b) % tints.size]
                val give = gives[(i * 3 + b) % gives.size]
                add(Dispenser(ox + 500f + i * 450f, give, "take a ${give.label}", Z.MAIN) { cc, d ->
                    shelf(cc, kinds, d.t, tint)
                }.sized(320f, 400f, -200f))
            }

            add(Pushable(PushKind.SHOPPING_CART, ox + 900f))
            add(Pushable(PushKind.SHOPPING_CART, ox + 3600f))
            add(Item(ItemKind.BALL, ox + 2200f))
            add(Npc(NpcRoster.spec(b), ox + 1600f))
            add(Npc(NpcRoster.spec(b + 3), ox + 4100f))
            add(Animal(if (b % 2 == 0) AnimalKind.CAT else AnimalKind.DOG, ox + 2900f))

            for (i in 0 until 3) {
                add(Prop(ox + 1200f + i * 1500f, 198f, Z.MAIN, 1f, i + b) { cc, p ->
                    D.shadow(cc, 0f, 0f, 80f, 14f)
                    val n = 1 + p.seed % 2
                    for (k in 0 until n) {
                        D.rect(cc, -66f, -80f - k * 84f, 132f, 80f,
                            if (k % 2 == 0) C.WOOD else C.WOOD_LIGHT, 8f)
                        D.line(cc, -56f, -70f - k * 84f, 56f, -70f - k * 84f, C.WOOD_DARK, 5f)
                    }
                }.sized(160f, 200f, -100f))
            }

            when (b) {
                1 -> {
                    // deli counter
                    add(Dispenser(ox + 4800f, ItemKind.MEAT, "at the deli counter", Z.MAIN) { cc, _ ->
                        D.shadow(cc, 0f, 0f, 160f, 22f)
                        D.rect(cc, -190f, -150f, 380f, 150f, C.CREAM, 10f)
                        D.rect(cc, -190f, -230f, 380f, 84f, withAlpha(C.GLASS, 200), 8f)
                        D.rect(cc, -200f, -252f, 400f, 26f, C.RED_DARK, 8f)
                        D.label(cc, "DELI", 0f, -60f, 34f, C.RED_DARK)
                        for (k in 0 until 4) {
                            D.oval(cc, -130f + k * 86f, -170f, 32f, 14f, 0xFFD4756B.toInt())
                        }
                    }.sized(420f, 300f, -150f))
                }
                2 -> {
                    // bakery ovens
                    add(Dispenser(ox + 4800f, ItemKind.BREAD, "fresh from the oven", Z.MAIN) { cc, p ->
                        D.shadow(cc, 0f, 0f, 150f, 22f)
                        D.rect(cc, -170f, -260f, 340f, 260f, 0xFF9A6B49.toInt(), 12f)
                        for (k in 0 until 2) {
                            val oy = -70f - k * 110f
                            D.arcFill(cc, -60f + k * 120f, oy, 66f, 60f, 180f, 180f, 0xFF2E2018.toInt())
                            val glow = 0.5f + 0.5f * sin(p.t * 2f + k)
                            D.arcFill(cc, -60f + k * 120f, oy, 46f, 40f, 180f, 180f,
                                withAlpha(C.ORANGE, (110 + glow * 120).toInt()))
                        }
                        D.rect(cc, -180f, -292f, 360f, 30f, C.ORANGE_DARK, 8f)
                        D.label(cc, "BAKERY", 0f, -270f, 30f, C.WHITE)
                    }.sized(380f, 340f, -170f))
                }
                else -> {
                    // second checkout bank
                    for (k in 0 until 2) {
                        add(Prop(ox + 4400f + k * 520f, 40f, Z.MAIN, 1f, k) { cc, p ->
                            D.shadow(cc, 0f, 0f, 170f, 24f)
                            D.rect(cc, -190f, -120f, 380f, 120f, C.CREAM, 10f)
                            D.rect(cc, -190f, -140f, 380f, 24f, shade(C.CREAM, 0.82f), 8f)
                            D.rect(cc, -170f, -152f, 250f, 18f, 0xFF44484E.toInt(), 6f)
                            val off = (p.t * 40f) % 40f
                            for (q in 0 until 7) {
                                D.line(cc, -166f + q * 40f + off, -150f,
                                    -166f + q * 40f + off, -136f, 0xFF5C666F.toInt(), 4f)
                            }
                            D.rect(cc, 96f, -240f, 96f, 100f, C.METAL, 8f)
                            D.rect(cc, 106f, -228f, 76f, 42f, C.GLASS, 5f)
                        }.sized(420f, 280f, -140f)
                            .interactive("beep it through the till") { ch, sc, p ->
                                ch.startAct(Act.REACH, 0.45f)
                                if (ch.carried != null) {
                                    sc.fx.sparkles(p.x, -170f, p.level, 8, C.YELLOW)
                                    Sfx.play(Snd.CASH)
                                } else {
                                    Sfx.play(Snd.UI)
                                }
                                true
                            })
                    }
                }
            }
        }
    }
}
