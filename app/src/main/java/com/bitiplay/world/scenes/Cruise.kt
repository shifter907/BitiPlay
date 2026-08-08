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
import com.bitiplay.world.ent.Stairs
import kotlin.math.sin

/**
 * The one scene with hard ends instead of a wrap: bow at one side, stern at the
 * other, and three decks stacked vertically.
 */
class Cruise : Scene("cruise", "Cruise Ship") {

    override val width = 7200f
    override val wraps = false
    override val levelCount = 4
    override val levelNames = arrayOf("Hold", "Cabins", "Deck", "Sky")
    override val accent = C.BLUE
    override val skyTop = 0xFF49B4E6.toInt()
    override val skyBottom = 0xFFD8F0FB.toInt()

    override fun spawnLevel(index: Int): Int = 2
    override fun spawnX(index: Int): Float = 1000f + index * 165f

    override fun drawTerrain(c: Canvas, cam: Cam, level: Int) {
        val hw = cam.visW * 0.5f + 80f
        when (level) {
            3 -> drawSkyDeck(c, cam, hw)
            2 -> drawSunDeck(c, cam, hw)
            1 -> drawCabinDeck(c, cam, hw)
            else -> drawHold(c, cam, hw)
        }
    }

    /** Top of the ship: open sky, a long horizon and bare planking. */
    private fun drawSkyDeck(c: Canvas, cam: Cam, hw: Float) {
        c.save()
        c.translate(-cam.visW * 0.30f, -cam.visH * 0.52f)
        Art.sun(c, 62f, time)
        c.restore()

        Render.layer(cam, 540f, 0.25f, 30f) { px, _, i ->
            c.save()
            c.translate(px, -520f - ((i * 7) % 3) * 70f)
            Art.cloud(c, 52f + ((i * 5) % 3) * 13f, 225)
            c.restore()
        }

        // sea far below the rail
        D.rect(c, -hw, -150f, hw * 2f, 130f, C.SEA)
        D.rect(c, -hw, -150f, hw * 2f, 24f, shade(C.SEA_DEEP, 0.95f))
        for (row in 0 until 3) {
            val yy = -120f + row * 34f
            Render.tiles(cam, 320f, row * 110f + sin(time * 0.7f + row) * 44f) { dx, _, _ ->
                D.capsule(c, dx, yy, dx + 70f, yy, 6f, withAlpha(C.FOAM, 100))
            }
        }

        D.rect(c, -hw, -20f, hw * 2f, 400f, shade(C.DECK, 1.06f))
        Render.tiles(cam, 90f) { dx, _, _ ->
            D.line(c, dx, -16f, dx, 380f, shade(C.DECK, 0.92f), 4f)
        }
        D.rect(c, -hw, -20f, hw * 2f, 10f, shade(C.DECK, 1.16f))
    }

    private fun drawSunDeck(c: Canvas, cam: Cam, hw: Float) {
        c.save()
        c.translate(cam.visW * 0.30f, -cam.visH * 0.56f)
        Art.sun(c, 58f, time)
        c.restore()

        Render.layer(cam, 540f, 0.25f, 90f) { px, _, i ->
            c.save()
            c.translate(px, -470f - ((i * 5) % 3) * 60f)
            Art.cloud(c, 46f + ((i * 7) % 3) * 12f, 215)
            c.restore()
        }

        // open sea far below the rail
        D.rect(c, -hw, -206f, hw * 2f, 150f, C.SEA)
        D.rect(c, -hw, -206f, hw * 2f, 26f, shade(C.SEA_DEEP, 0.95f))
        for (row in 0 until 3) {
            val yy = -170f + row * 34f
            Render.tiles(cam, 300f, row * 100f + sin(time * 0.8f + row) * 40f) { dx, _, _ ->
                D.capsule(c, dx, yy, dx + 74f, yy, 7f, withAlpha(C.FOAM, 110))
            }
        }

        // superstructure behind
        D.rect(c, -hw, -640f, hw * 2f, 420f, C.OFF_WHITE)
        Render.tiles(cam, 225f, 40f) { dx, _, i ->
            D.circle(c, dx, -420f, 30f, C.GLASS_DARK)
            D.circleStroke(c, dx, -420f, 30f, C.WHITE, 8f)
            if (i % 2 == 0) D.rect(c, dx - 44f, -560f, 88f, 62f, C.GLASS, 8f)
        }
        D.rect(c, -hw, -228f, hw * 2f, 26f, C.BLUE_DARK)

        // deck planks
        D.rect(c, -hw, -18f, hw * 2f, 400f, C.DECK)
        Render.tiles(cam, 90f) { dx, _, _ ->
            D.line(c, dx, -14f, dx, 380f, shade(C.DECK, 0.9f), 4f)
        }
        D.rect(c, -hw, -18f, hw * 2f, 10f, shade(C.DECK, 1.1f))
    }

    private fun drawCabinDeck(c: Canvas, cam: Cam, hw: Float) {
        D.rect(c, -hw, -860f, hw * 2f, 1200f, 0xFF2E4358.toInt())
        // wall
        D.rect(c, -hw, -640f, hw * 2f, 640f, 0xFFE8DFCE.toInt())
        D.rect(c, -hw, -640f, hw * 2f, 40f, 0xFFC9BCA4.toInt())
        Render.tiles(cam, 150f) { dx, _, i ->
            D.rect(c, dx, -600f, 8f, 600f, 0xFFD8CDB6.toInt())
            if (i % 3 == 0) {
                D.circle(c, dx + 75f, -420f, 34f, C.GLASS_DARK)
                D.circleStroke(c, dx + 75f, -420f, 34f, C.METAL, 9f)
                D.arcFill(c, dx + 75f, -430f, 26f, 18f, 180f, 180f, withAlpha(C.WHITE, 90))
            }
        }
        // ceiling lights
        Render.tiles(cam, 300f, 60f) { dx, _, _ ->
            D.rect(c, dx - 34f, -654f, 68f, 18f, withAlpha(C.YELLOW, 210), 8f)
        }
        // skirting and carpet
        D.rect(c, -hw, -34f, hw * 2f, 36f, 0xFF9C6B49.toInt())
        D.rect(c, -hw, 2f, hw * 2f, 420f, 0xFF9C3E44.toInt())
        Render.tiles(cam, 135f, 30f) { dx, _, i ->
            if (i % 2 == 0) D.rect(c, dx, 40f, 66f, 300f, 0xFF8A353B.toInt(), 8f)
            D.star(c, dx + 33f, 150f, 16f, 7f, 5, withAlpha(C.YELLOW, 60))
        }
    }

    private fun drawHold(c: Canvas, cam: Cam, hw: Float) {
        D.rect(c, -hw, -860f, hw * 2f, 1200f, 0xFF3A4652.toInt())
        D.rect(c, -hw, -620f, hw * 2f, 620f, 0xFF57646F.toInt())
        // ribs
        Render.tiles(cam, 225f) { dx, _, _ ->
            D.rect(c, dx, -620f, 22f, 620f, 0xFF48545E.toInt())
            for (k in 0 until 6) D.circle(c, dx + 11f, -580f + k * 100f, 6f, 0xFF6E7C88.toInt())
        }
        // pipes
        D.capsule(c, -hw, -560f, hw, -560f, 20f, 0xFF7A8792.toInt())
        D.capsule(c, -hw, -510f, hw, -510f, 13f, 0xFF6A7783.toInt())
        Render.tiles(cam, 300f, 80f) { dx, _, _ ->
            D.rect(c, dx - 16f, -576f, 32f, 32f, 0xFF8B98A4.toInt(), 5f)
        }
        // caged lamps
        Render.tiles(cam, 450f, 120f) { dx, _, _ ->
            D.capsule(c, dx, -620f, dx, -586f, 5f, C.METAL_DARK)
            D.circle(c, dx, -578f, 17f, withAlpha(C.YELLOW, 220))
            D.circleStroke(c, dx, -578f, 19f, C.METAL_DARK, 4f)
        }
        // floor
        D.rect(c, -hw, -18f, hw * 2f, 400f, 0xFF6B7681.toInt())
        Render.tiles(cam, 180f) { dx, _, _ ->
            D.line(c, dx, -14f, dx, 380f, 0xFF5C666F.toInt(), 5f)
        }
        D.rect(c, -hw, -18f, hw * 2f, 9f, 0xFF7E8A95.toInt())
    }

    override fun drawFore(c: Canvas, cam: Cam, level: Int) {
        if (level < 2) return
        // deck railing in front of everyone
        val hw = cam.visW * 0.5f + 80f
        D.capsule(c, -hw, 118f, hw, 118f, 12f, C.OFF_WHITE)
        D.capsule(c, -hw, 168f, hw, 168f, 8f, C.OFF_WHITE)
        Render.tiles(cam, 150f) { dx, _, _ ->
            D.capsule(c, dx, 214f, dx, 112f, 9f, C.OFF_WHITE)
        }
    }

    override fun drawThumb(c: Canvas, w: Float, h: Float) {
        D.rect(c, 0f, 0f, w, h, 0xFF8FD9F3.toInt())
        D.rect(c, 0f, h * 0.6f, w, h * 0.4f, C.SEA)
        D.shape(c, C.WHITE) { p ->
            p.moveTo(w * 0.1f, h * 0.52f); p.lineTo(w * 0.9f, h * 0.52f)
            p.lineTo(w * 0.8f, h * 0.76f); p.lineTo(w * 0.2f, h * 0.76f)
        }
        D.rect(c, w * 0.24f, h * 0.3f, w * 0.5f, h * 0.22f, C.OFF_WHITE)
        D.rect(c, w * 0.56f, h * 0.16f, w * 0.12f, h * 0.16f, C.RED)
        for (i in 0 until 4) D.circle(c, w * (0.3f + i * 0.11f), h * 0.42f, h * 0.03f, C.GLASS_DARK)
    }

    override fun build() {
        // ---------------------------------------------------- deck (level 2)
        add(Prop(300f, -18f, Z.BACK, 1f, 0) { cc, p ->
            // funnel
            D.rect(cc, -70f, -520f, 140f, 300f, C.RED, 16f)
            D.rect(cc, -70f, -520f, 140f, 40f, 0xFF2E3A44.toInt(), 12f)
            D.oval(cc, 0f, -520f, 70f, 18f, 0xFF22303A.toInt())
            if ((p.t * 0.7f).toInt() % 2 == 0) {
                D.circle(cc, 10f, -570f, 26f, withAlpha(C.WHITE, 120))
                D.circle(cc, 40f, -620f, 34f, withAlpha(C.WHITE, 80))
            }
        }.sized(200f, 560f, -320f).also { it.level = 2 })

        add(Prop(3320f, -18f, Z.BACK, 1f, 0) { cc, _ ->
            // wheelhouse
            D.rect(cc, -150f, -360f, 300f, 360f, C.OFF_WHITE, 14f)
            D.rect(cc, -128f, -330f, 256f, 110f, C.GLASS, 8f)
            D.rect(cc, -150f, -376f, 300f, 22f, C.BLUE_DARK, 8f)
            D.capsule(cc, 0f, -376f, 0f, -470f, 8f, C.METAL_DARK)
            D.tri(cc, 4f, -470f, 4f, -430f, 70f, -450f, C.RED)
        }.sized(330f, 500f, -250f).also { it.level = 2 })

        // pool
        add(Prop(1750f, 30f, Z.DECAL, 1f, 0) { cc, p ->
            D.rect(cc, -260f, -70f, 520f, 190f, C.OFF_WHITE, 22f)
            D.rect(cc, -232f, -50f, 464f, 150f, C.POOL, 18f)
            for (k in 0 until 4) {
                val yy = -30f + k * 36f + sin(p.t * 1.6f + k) * 5f
                D.capsule(cc, -190f + k * 28f, yy, -60f + k * 28f, yy, 7f, withAlpha(C.WHITE, 120))
            }
        }.sized(540f, 200f, -20f).interactive("splash in the pool") { ch, sc, p ->
            ch.startAct(Act.PET, 0.7f)
            sc.fx.splash(ch.x, -40f, ch.level, 14, C.SEA_LIGHT)
            sc.fx.bubbles(p.x, -40f, p.level, 5)
            Sfx.play(Snd.SPLASH)
            true
        }.also { it.level = 2 })

        for (i in 0 until 3) {
            add(Prop(2350f + i * 210f, 150f, Z.MAIN, 1f, i) { cc, _ ->
                D.shadow(cc, 0f, 0f, 90f, 14f)
                D.shape(cc, C.TEAL) { p ->
                    p.moveTo(-86f, -30f); p.lineTo(40f, -30f)
                    p.lineTo(40f, -14f); p.lineTo(-86f, -14f)
                }
                D.shape(cc, shade(C.TEAL, 1.1f)) { p ->
                    p.moveTo(-86f, -30f); p.lineTo(-40f, -110f)
                    p.lineTo(-22f, -102f); p.lineTo(-56f, -28f)
                }
                D.capsule(cc, -70f, -12f, -70f, 0f, 8f, C.METAL)
                D.capsule(cc, 28f, -12f, 28f, 0f, 8f, C.METAL)
            }.sized(200f, 140f, -60f).also { it.level = 2 })
        }

        add(Grill(2900f).also { it.level = 2 })
        add(Dispenser(3060f, ItemKind.PATTY_RAW, "burger patty", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 76f, 13f)
            D.rect(cc, -76f, -120f, 152f, 120f, C.METAL, 8f)
            D.rect(cc, -60f, -104f, 120f, 54f, C.GLASS, 6f)
            D.label(cc, "GALLEY", 0f, -26f, 24f, C.BLUE_DARK)
        }.sized(200f, 200f, -100f).also { it.level = 2 })

        add(Dispenser(1200f, ItemKind.JUICE, "a cold drink", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 90f, 15f)
            D.rect(cc, -110f, -130f, 220f, 130f, C.WOOD, 10f)
            D.rect(cc, -120f, -150f, 240f, 26f, C.WOOD_DARK, 8f)
            Art.awning(cc, 250f, -206f, C.TEAL, C.WHITE)
            D.circle(cc, -50f, -170f, 16f, C.ORANGE)
            D.circle(cc, 0f, -172f, 16f, C.PINK)
            D.circle(cc, 50f, -170f, 16f, C.YELLOW)
        }.sized(280f, 260f, -130f).also { it.level = 2 })

        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(1), 1620f, 520f)
            .also { it.level = 2 })
        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(3), 2450f, 520f)
            .also { it.level = 1 })
        add(Item(ItemKind.BEACHBALL, 2150f).also { it.level = 2 })
        add(Item(ItemKind.ICE_CREAM, 1450f).also { it.level = 2 })
        add(Animal(AnimalKind.SEAGULL, 2600f).also { it.level = 2 })
        add(Animal(AnimalKind.SEAGULL, 900f).also { it.level = 2 })

        add(Prop(650f, 120f, Z.MAIN, 1f, 0) { cc, _ ->
            D.circle(cc, 0f, -60f, 52f, C.RED)
            D.circle(cc, 0f, -60f, 30f, C.OFF_WHITE)
            for (i in 0 until 4) {
                val a = i * 1.5707964f + 0.78f
                D.capsule(
                    cc, kotlin.math.cos(a) * 32f, -60f + kotlin.math.sin(a) * 32f,
                    kotlin.math.cos(a) * 52f, -60f + kotlin.math.sin(a) * 52f, 14f, C.OFF_WHITE
                )
            }
        }.sized(130f, 130f, -60f).also { it.level = 2 })

        add(Stairs(3480f, 1, "down to the cabins").also { it.level = 2 })
        add(Stairs(420f, 1, "down to the cabins").also { it.level = 2 })

        // -------------------------------------------------- cabins (level 1)
        add(Stairs(3480f, 2, "up to the deck").also { it.level = 1 })
        add(Stairs(420f, 2, "up to the deck").also { it.level = 1 })
        add(Stairs(1900f, 0, "down to the hold").also { it.level = 1 })

        val doorCols = intArrayOf(C.RED_DARK, C.BLUE_DARK, C.GREEN_DARK, C.PURPLE_DARK)
        for (i in 0 until 7) {
            add(Prop(700f + i * 340f, -34f, Z.BACK, 1f, i) { cc, p ->
                val open = p.state == 1
                D.rect(cc, -75f, -300f, 150f, 300f, shade(doorCols[p.seed % 4], 0.8f), 8f)
                if (open) {
                    D.rect(cc, -66f, -290f, 132f, 290f, 0xFF3A2C22.toInt(), 6f)
                    D.rect(cc, -20f, -290f, 86f, 290f, doorCols[p.seed % 4], 6f)
                } else {
                    D.rect(cc, -66f, -290f, 132f, 290f, doorCols[p.seed % 4], 6f)
                    D.circle(cc, 44f, -150f, 9f, C.YELLOW)
                }
                D.rect(cc, -34f, -330f, 68f, 24f, C.METAL, 5f)
                D.label(cc, "${101 + p.seed}", 0f, -312f, 20f, C.UI_TEXT)
            }.sized(180f, 340f, -170f).interactive("open the cabin") { ch, sc, p ->
                p.state = 1 - p.state
                ch.startAct(Act.REACH, 0.35f)
                Sfx.play(if (p.state == 1) Snd.POP else Snd.DROP)
                sc.fx.sparkles(p.x, -160f, p.level, 4, C.WHITE)
                true
            }.also { it.level = 1 })
        }

        add(Dispenser(2600f, ItemKind.KEY, "a cabin key", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 120f, 18f)
            D.rect(cc, -150f, -150f, 300f, 150f, C.WOOD, 10f)
            D.rect(cc, -160f, -172f, 320f, 26f, C.WOOD_LIGHT, 8f)
            D.label(cc, "RECEPTION", 0f, -76f, 30f, C.CREAM)
            D.circle(cc, 96f, -200f, 20f, C.YELLOW)
        }.sized(340f, 240f, -120f).also { it.level = 1 })

        add(Pushable(PushKind.LUGGAGE_CART, 1400f).also { it.level = 1 })
        add(Item(ItemKind.GIFT, 3100f).also { it.level = 1 })
        add(Item(ItemKind.LANTERN, 2300f).also { it.level = 1 })
        add(Item(ItemKind.WATERING_CAN, 1150f).also { it.level = 1 })
        add(Plant(PlantKind.BUSH, 1700f, 3).also { it.level = 1 })
        add(Plant(PlantKind.FLOWERS, 3260f, 2).also { it.level = 1 })
        add(Animal(AnimalKind.CAT, 2050f).also { it.level = 1 })

        // ---------------------------------------------------- hold (level 0)
        add(Stairs(1900f, 1, "up to the cabins").also { it.level = 0 })

        for (i in 0 until 9) {
            add(Prop(500f + i * 330f, 0f, Z.BACK, 1f, i) { cc, p ->
                val n = 1 + p.seed % 3
                for (k in 0 until n) {
                    val by = -k * 106f
                    D.rect(cc, -56f, by - 100f, 112f, 100f, if (k % 2 == 0) C.WOOD else C.WOOD_LIGHT, 7f)
                    D.line(cc, -50f, by - 92f, 50f, by - 8f, C.WOOD_DARK, 6f)
                    D.line(cc, 50f, by - 92f, -50f, by - 8f, C.WOOD_DARK, 6f)
                }
            }.sized(140f, 340f, -170f).also { it.level = 0 })
        }

        // engine with moving pistons
        add(Prop(2900f, -18f, Z.BACK, 1f, 0) { cc, p ->
            D.rect(cc, -230f, -380f, 460f, 380f, 0xFF4E5A66.toInt(), 12f)
            D.rect(cc, -230f, -400f, 460f, 30f, 0xFF3E4A54.toInt(), 8f)
            for (k in 0 until 3) {
                val px = -140f + k * 140f
                val ph = sin(p.t * 3.4f + k * 2f) * 26f
                D.rect(cc, px - 30f, -330f, 60f, 130f, 0xFF6C7A86.toInt(), 8f)
                D.capsule(cc, px, -196f + ph, px, -120f + ph, 18f, C.METAL)
                D.rect(cc, px - 44f, -110f + ph, 88f, 34f, C.ORANGE_DARK, 7f)
            }
            D.circle(cc, 160f, -300f, 40f, C.RED)
            D.circleStroke(cc, 160f, -300f, 40f, C.METAL_DARK, 8f)
            D.label(cc, "ENGINE", 0f, -40f, 34f, C.YELLOW)
        }.sized(500f, 440f, -220f).interactive("the engine room") { ch, sc, p ->
            ch.startAct(Act.BUILD, 0.7f)
            sc.fx.smoke(p.x, -400f, p.level, 4)
            Sfx.play(Snd.ENGINE)
            true
        }.also { it.level = 0 })

        add(Pushable(PushKind.WAGON, 1250f, C.METAL).also { it.level = 0 })
        add(Item(ItemKind.TREASURE, 3300f).also { it.level = 0 })
        add(Item(ItemKind.LOG, 800f).also { it.level = 0 })
        add(Item(ItemKind.ROCK, 2400f).also { it.level = 0 })
        add(Item(ItemKind.LANTERN, 1600f).also { it.level = 0 })
        add(Animal(AnimalKind.CAT, 2650f).also { it.level = 0 })

        buildAft()
        buildSkyDeck()
    }

    /** The ship is twice as long now, so the far half gets its own facilities. */
    private fun buildAft() {
        // deck (level 2)
        add(Stairs(6900f, 1, "down to the cabins").also { it.level = 2 })
        add(Stairs(6900f, 2, "up to the deck").also { it.level = 1 })
        add(Stairs(5200f, 0, "down to the hold").also { it.level = 1 })
        add(Stairs(5200f, 1, "up to the cabins").also { it.level = 0 })

        add(Prop(4600f, 30f, Z.DECAL, 1f, 0) { cc, p ->
            // shuffleboard court
            D.rect(cc, -300f, -40f, 600f, 150f, shade(C.DECK, 0.9f), 10f)
            D.rect(cc, -280f, -26f, 560f, 122f, shade(C.DECK, 1.12f), 8f)
            for (k in 0 until 4) {
                D.line(cc, -220f + k * 140f, -20f, -260f + k * 140f, 90f, C.BLUE_DARK, 5f)
            }
            D.circle(cc, -120f + sin(p.t) * 6f, 44f, 20f, C.RED)
            D.circle(cc, 90f, 60f, 20f, C.YELLOW)
        }.sized(620f, 180f, -20f).interactive("play shuffleboard") { ch, sc, p ->
            ch.startAct(Act.BUILD, 0.6f)
            sc.fx.sparkles(p.x, -40f, p.level, 6, C.WHITE)
            Sfx.play(Snd.CLICK)
            true
        }.also { it.level = 2 })

        add(Grill(5600f).also { it.level = 2 })
        add(Item(ItemKind.FRISBEE, 4200f).also { it.level = 2 })
        add(Item(ItemKind.BEACHBALL, 6200f).also { it.level = 2 })
        add(Npc(NpcRoster.spec(2), 5900f).also { it.level = 2 })
        add(Animal(AnimalKind.SEAGULL, 6500f).also { it.level = 2 })

        // cabins (level 1)
        val doorCols = intArrayOf(C.RED_DARK, C.BLUE_DARK, C.GREEN_DARK, C.PURPLE_DARK)
        for (i in 0 until 7) {
            add(Prop(4100f + i * 340f, -34f, Z.BACK, 1f, i + 7) { cc, p ->
                val open = p.state == 1
                D.rect(cc, -75f, -300f, 150f, 300f, shade(doorCols[p.seed % 4], 0.8f), 8f)
                D.rect(cc, -66f, -290f, 132f, 290f,
                    if (open) 0xFF3A2C22.toInt() else doorCols[p.seed % 4], 6f)
                if (!open) D.circle(cc, 44f, -150f, 9f, C.YELLOW)
                D.rect(cc, -34f, -330f, 68f, 24f, C.METAL, 5f)
                D.label(cc, "${201 + p.seed}", 0f, -312f, 20f, C.UI_TEXT)
            }.sized(180f, 340f, -170f).interactive("open the cabin") { ch, sc, p ->
                p.state = 1 - p.state
                ch.startAct(Act.REACH, 0.35f)
                Sfx.play(if (p.state == 1) Snd.CREAK else Snd.DROP)
                true
            }.also { it.level = 1 })
        }
        add(Npc(NpcRoster.spec(5), 4600f).also { it.level = 1 })
        add(Item(ItemKind.GIFT, 6400f).also { it.level = 1 })

        // hold (level 0)
        for (i in 0 until 8) {
            add(Prop(4000f + i * 380f, 0f, Z.BACK, 1f, i + 3) { cc, p ->
                val n = 1 + p.seed % 3
                for (k in 0 until n) {
                    val by = -k * 106f
                    D.rect(cc, -56f, by - 100f, 112f, 100f,
                        if (k % 2 == 0) C.WOOD else C.WOOD_LIGHT, 7f)
                    D.line(cc, -50f, by - 92f, 50f, by - 8f, C.WOOD_DARK, 6f)
                    D.line(cc, 50f, by - 92f, -50f, by - 8f, C.WOOD_DARK, 6f)
                }
            }.sized(140f, 340f, -170f).also { it.level = 0 })
        }
        add(Item(ItemKind.TREASURE, 6600f).also { it.level = 0 })
        add(Pushable(PushKind.WAGON, 4400f, C.METAL).also { it.level = 0 })
    }

    /** The new top deck: slide, putting green and the bridge. */
    private fun buildSkyDeck() {
        add(Stairs(3480f, 2, "down to the deck").also { it.level = 3 })
        add(Stairs(420f, 2, "down to the deck").also { it.level = 3 })
        add(Stairs(6900f, 2, "down to the deck").also { it.level = 3 })
        add(Stairs(3480f, 3, "up to the sky deck").also { it.level = 2 })
        add(Stairs(420f, 3, "up to the sky deck").also { it.level = 2 })

        // water slide
        add(Prop(1400f, 20f, Z.MAIN, 1f, 0) { cc, p ->
            D.shadow(cc, 0f, 0f, 190f, 26f)
            D.capsule(cc, -180f, 0f, -180f, -420f, 18f, C.METAL_DARK)
            D.capsule(cc, -120f, 0f, -120f, -420f, 18f, C.METAL_DARK)
            D.stroke(cc, C.TEAL, 34f) { path ->
                path.moveTo(-150f, -420f)
                path.cubicTo(60f, -400f, 40f, -160f, 230f, -110f)
            }
            D.stroke(cc, shade(C.TEAL, 1.3f), 14f) { path ->
                path.moveTo(-150f, -410f)
                path.cubicTo(60f, -390f, 40f, -150f, 230f, -100f)
            }
            D.oval(cc, 300f, -60f, 130f, 42f, C.POOL)
            D.ovalStroke(cc, 300f, -60f, 130f, 42f, C.OFF_WHITE, 12f)
            val u = (p.f0 % 1f)
            if (u < 0.75f) {
                val sx = -150f + u * 500f
                val sy = -420f + u * u * 430f
                D.circle(cc, sx, sy, 20f, C.YELLOW)
                D.circle(cc, sx - 26f, sy - 8f, 9f, withAlpha(C.FOAM, 170))
            }
        }.sized(560f, 500f, -250f).onUpdate { dt, _, p -> p.f0 += dt * 0.42f }
            .interactive("take the slide") { ch, sc, p ->
                ch.cheer(sc)
                sc.fx.splash(p.x + 300f, -60f, p.level, 14, C.SEA_LIGHT)
                Sfx.play(Snd.SPLASH)
                true
            }.also { it.level = 3 })

        // putting green
        add(Prop(3000f, 40f, Z.DECAL, 1f, 0) { cc, p ->
            D.oval(cc, 0f, 20f, 320f, 80f, C.GRASS_DARK)
            D.oval(cc, 0f, 14f, 300f, 70f, C.GRASS)
            D.circle(cc, 150f, 20f, 22f, 0xFF2E2018.toInt())
            D.capsule(cc, 150f, 10f, 150f, -140f, 5f, C.OFF_WHITE)
            D.tri(cc, 152f, -140f, 152f, -104f, 216f, -122f, C.RED)
            D.circle(cc, -120f + sin(p.t * 0.8f) * 40f, 30f, 12f, C.WHITE)
        }.sized(660f, 200f, -40f).interactive("have a putt") { ch, sc, p ->
            ch.startAct(Act.BUILD, 0.6f)
            sc.fx.sparkles(p.x + 150f, -30f, p.level, 6, C.YELLOW)
            Sfx.play(Snd.CLICK)
            true
        }.also { it.level = 3 })

        // funnel top and bridge
        add(Prop(300f, -20f, Z.BACK, 1f, 0) { cc, p ->
            D.rect(cc, -70f, -420f, 140f, 260f, C.RED, 16f)
            D.rect(cc, -70f, -420f, 140f, 40f, 0xFF2E3A44.toInt(), 12f)
            D.oval(cc, 0f, -420f, 70f, 18f, 0xFF22303A.toInt())
            if ((p.t * 0.7f).toInt() % 2 == 0) {
                D.circle(cc, 12f, -470f, 28f, withAlpha(C.WHITE, 120))
                D.circle(cc, 44f, -524f, 36f, withAlpha(C.WHITE, 80))
            }
        }.sized(200f, 460f, -260f).interactive("sound the horn") { ch, sc, p ->
            ch.cheer(sc)
            sc.fx.smoke(p.x, -440f, p.level, 6)
            Sfx.play(Snd.BOAT_HORN)
            true
        }.also { it.level = 3 })

        add(Prop(6600f, -20f, Z.BACK, 1f, 0) { cc, _ ->
            D.rect(cc, -170f, -300f, 340f, 300f, C.OFF_WHITE, 14f)
            D.rect(cc, -146f, -272f, 292f, 110f, C.GLASS, 8f)
            D.rect(cc, -170f, -318f, 340f, 24f, C.BLUE_DARK, 8f)
            D.capsule(cc, 0f, -318f, 0f, -420f, 8f, C.METAL_DARK)
            D.capsule(cc, -60f, -420f, 60f, -420f, 6f, C.METAL_DARK)
            D.circle(cc, 0f, -420f, 14f, C.METAL)
        }.sized(370f, 440f, -220f).also { it.level = 3 })

        // loungers and refreshments
        for (i in 0 until 4) {
            add(Prop(3800f + i * 240f, 150f, Z.MAIN, 1f, i) { cc, _ ->
                D.shadow(cc, 0f, 0f, 90f, 14f)
                D.shape(cc, C.PINK) { p ->
                    p.moveTo(-86f, -30f); p.lineTo(40f, -30f)
                    p.lineTo(40f, -14f); p.lineTo(-86f, -14f)
                }
                D.shape(cc, shade(C.PINK, 1.1f)) { p ->
                    p.moveTo(-86f, -30f); p.lineTo(-40f, -110f)
                    p.lineTo(-22f, -102f); p.lineTo(-56f, -28f)
                }
                D.capsule(cc, -70f, -12f, -70f, 0f, 8f, C.METAL)
                D.capsule(cc, 28f, -12f, 28f, 0f, 8f, C.METAL)
            }.sized(200f, 140f, -60f).also { it.level = 3 })
        }

        add(Dispenser(2300f, ItemKind.JUICE, "a cold drink", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 90f, 15f)
            D.rect(cc, -110f, -130f, 220f, 130f, C.WOOD, 10f)
            D.rect(cc, -120f, -150f, 240f, 26f, C.WOOD_DARK, 8f)
            Art.awning(cc, 250f, -206f, C.TEAL, C.WHITE)
            D.circle(cc, -50f, -170f, 16f, C.ORANGE)
            D.circle(cc, 0f, -172f, 16f, C.PINK)
            D.circle(cc, 50f, -170f, 16f, C.YELLOW)
        }.sized(280f, 260f, -130f).also { it.level = 3 })

        add(Item(ItemKind.BEACHBALL, 2700f).also { it.level = 3 })
        add(Item(ItemKind.ICE_CREAM, 5200f).also { it.level = 3 })
        add(Npc(NpcRoster.spec(0), 4600f).also { it.level = 3 })
        add(Npc(NpcRoster.spec(4), 2000f).also { it.level = 3 })
        add(Animal(AnimalKind.SEAGULL, 5600f).also { it.level = 3 })
        add(Animal(AnimalKind.SEAGULL, 1200f).also { it.level = 3 })
    }
}
