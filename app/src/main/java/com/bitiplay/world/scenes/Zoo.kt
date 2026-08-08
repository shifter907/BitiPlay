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
import com.bitiplay.world.ent.Npc
import com.bitiplay.world.ent.NpcRoster
import com.bitiplay.world.ent.Plant
import com.bitiplay.world.ent.PlantKind
import com.bitiplay.world.ent.Prop
import com.bitiplay.world.ent.PushKind
import com.bitiplay.world.ent.Pushable
import com.bitiplay.world.ent.Vehicle
import com.bitiplay.world.ent.VehicleKind
import kotlin.math.sin

class Zoo : Scene("zoo", "Zoo") {

    override val width = 21600f
    override val accent = C.LEAF_DARK
    override val skyTop = 0xFF4CBBE6.toInt()
    override val skyBottom = 0xFFE8F6D8.toInt()

    private val back = -18f

    override fun drawTerrain(c: Canvas, cam: Cam, level: Int) {
        val hw = cam.visW * 0.5f + 80f

        c.save()
        c.translate(cam.visW * 0.24f, -cam.visH * 0.58f)
        Art.sun(c, 56f, time)
        c.restore()

        Render.layer(cam, 540f, 0.25f, 140f) { px, _, i ->
            c.save()
            c.translate(px, -490f - ((i * 5) % 3) * 64f)
            Art.cloud(c, 48f + ((i * 7) % 3) * 12f, 220)
            c.restore()
        }

        // jungle canopy behind the enclosures
        Render.layer(cam, 450f, 0.5f, 70f) { px, _, i ->
            c.save()
            c.translate(px, back)
            Art.tree(c, i, time, 380f + ((i * 13) % 3) * 60f, shade(C.LEAF_DARK, 0.9f))
            c.restore()
        }
        Render.layer(cam, 225f, 0.75f, 30f) { px, _, i ->
            c.save()
            c.translate(px, back)
            Art.bushClump(c, i, 200f, 150f + ((i * 11) % 3) * 30f)
            c.restore()
        }

        // ground
        D.rect(c, -hw, back, hw * 2f, 40f, shade(C.GRASS_DARK, 0.9f))
        D.rect(c, -hw, 14f, hw * 2f, 92f, C.GRASS)
        // winding visitor path
        D.rect(c, -hw, 100f, hw * 2f, 86f, 0xFFCFC0A4.toInt())
        D.rect(c, -hw, 100f, hw * 2f, 9f, 0xFFE0D2B8.toInt())
        Render.tiles(cam, 150f, 20f) { dx, _, i ->
            if (i % 2 == 0) D.oval(c, dx, 146f, 32f, 11f, 0xFFC0B094.toInt())
        }
        D.rect(c, -hw, 184f, hw * 2f, 220f, C.GRASS)
        Render.tiles(cam, 135f, 50f) { dx, _, i ->
            if (i % 3 != 0) {
                c.save()
                c.translate(dx, 202f)
                Art.tuft(c, 24f, C.GRASS_DARK)
                c.restore()
            }
        }
        Render.tiles(cam, 675f, 260f) { dx, _, i ->
            c.save()
            c.translate(dx, 216f)
            Art.flowerPatch(c, i + 3)
            c.restore()
        }
    }

    override fun drawThumb(c: Canvas, w: Float, h: Float) {
        D.rect(c, 0f, 0f, w, h, 0xFF9FE0F2.toInt())
        D.rect(c, 0f, h * 0.6f, w, h * 0.4f, C.GRASS)
        D.rect(c, 0f, h * 0.76f, w, h * 0.14f, 0xFFCFC0A4.toInt())
        c.save()
        c.translate(w * 0.24f, h * 0.62f)
        c.scale(h / 620f, h / 620f)
        Art.tree(c, 1, 0f, 320f, C.LEAF_DARK)
        c.restore()
        // giraffe silhouette
        c.save()
        c.translate(w * 0.66f, h * 0.76f)
        val s = h / 480f
        c.scale(s, s)
        D.oval(c, 0f, -80f, 56f, 40f, 0xFFE8C067.toInt())
        D.capsule(c, 30f, -100f, 62f, -230f, 24f, 0xFFE8C067.toInt())
        D.circle(c, 66f, -246f, 24f, 0xFFE8C067.toInt())
        D.capsule(c, -30f, -50f, -34f, 0f, 14f, 0xFFE8C067.toInt())
        D.capsule(c, 26f, -50f, 30f, 0f, 14f, 0xFFE8C067.toInt())
        c.restore()
    }

    override fun spawnX(index: Int): Float = 800f + index * 165f

    /** Enclosure fence drawn behind an animal, plus a name board. */
    private fun enclosure(cc: Canvas, w: Float, label: String, col: Int) {
        D.rect(cc, -w * 0.5f, -230f, w, 230f, withAlpha(shade(col, 1.3f), 70), 12f)
        D.capsule(cc, -w * 0.5f, 0f, -w * 0.5f, -250f, 12f, C.WOOD_DARK)
        D.capsule(cc, w * 0.5f, 0f, w * 0.5f, -250f, 12f, C.WOOD_DARK)
        D.capsule(cc, -w * 0.5f - 8f, -250f, w * 0.5f + 8f, -250f, 12f, C.WOOD)
        D.capsule(cc, -w * 0.5f - 8f, -150f, w * 0.5f + 8f, -150f, 8f, C.WOOD)
        D.capsule(cc, -w * 0.5f - 8f, -60f, w * 0.5f + 8f, -60f, 8f, C.WOOD)
        signPost(cc, -w * 0.5f + 90f, label, col)
    }

    private fun signPost(cc: Canvas, sx: Float, label: String, col: Int) {
        D.capsule(cc, sx, 30f, sx, -70f, 10f, C.WOOD_DARK)
        D.rect(cc, sx - 90f, -150f, 180f, 80f, shade(col, 0.7f), 10f)
        D.rect(cc, sx - 82f, -142f, 164f, 64f, col, 8f)
        D.label(cc, label, sx, -100f, 28f, C.WHITE)
    }

    override fun build() {
        // entrance arch
        add(Prop(80f, back, Z.BACK, 1f, 0) { cc, _ ->
            D.capsule(cc, -200f, 0f, -200f, -400f, 26f, C.WOOD_DARK)
            D.capsule(cc, 200f, 0f, 200f, -400f, 26f, C.WOOD_DARK)
            D.rect(cc, -230f, -470f, 460f, 80f, C.LEAF_DARK, 16f)
            D.label(cc, "ZOO", 0f, -410f, 60f, C.YELLOW)
            D.circle(cc, -160f, -430f, 22f, C.ORANGE)
            D.circle(cc, 160f, -430f, 22f, C.ORANGE)
        }.sized(500f, 520f, -260f))

        // ---- enclosures ----------------------------------------------------
        add(Prop(900f, back, Z.BACK, 1f, 0) { cc, _ -> enclosure(cc, 520f, "ELEPHANT", C.BLUE) }
            .sized(560f, 300f, -150f))
        add(Animal(AnimalKind.ELEPHANT, 900f).pen(170f))

        add(Prop(1750f, back, Z.BACK, 1f, 0) { cc, _ -> enclosure(cc, 520f, "GIRAFFE", C.ORANGE) }
            .sized(560f, 300f, -150f))
        add(Animal(AnimalKind.GIRAFFE, 1740f).pen(170f))

        add(Prop(2560f, back, Z.BACK, 1f, 0) { cc, _ -> enclosure(cc, 500f, "MONKEYS", C.GREEN) }
            .sized(540f, 300f, -150f))
        add(Prop(2560f, back, Z.MID, 1f, 0) { cc, p ->
            // climbing frame
            D.capsule(cc, -120f, 0f, -120f, -300f, 16f, C.WOOD)
            D.capsule(cc, 120f, 0f, 120f, -240f, 16f, C.WOOD)
            D.capsule(cc, -130f, -300f, 130f, -240f, 14f, C.WOOD_DARK)
            D.stroke(cc, C.CREAM, 6f) { path ->
                path.moveTo(-90f, -288f); path.quadTo(0f, -190f + sin(p.t) * 8f, 90f, -252f)
            }
        }.sized(300f, 320f, -160f))
        add(Animal(AnimalKind.MONKEY, 2500f).pen(150f))
        add(Animal(AnimalKind.MONKEY, 2640f).pen(150f))

        add(Prop(3400f, back, Z.BACK, 1f, 0) { cc, _ -> enclosure(cc, 520f, "PENGUINS", C.TEAL) }
            .sized(560f, 300f, -150f))
        add(Prop(3400f, 40f, Z.DECAL, 1f, 0) { cc, p ->
            D.oval(cc, 0f, 20f, 230f, 66f, C.OFF_WHITE)
            D.oval(cc, 0f, 20f, 200f, 52f, C.POOL)
            for (k in 0 until 3) {
                val yy = 6f + k * 18f + sin(p.t * 1.5f + k) * 4f
                D.capsule(cc, -120f + k * 40f, yy, -20f + k * 40f, yy, 6f, withAlpha(C.WHITE, 120))
            }
        }.sized(480f, 140f, -20f))
        add(Animal(AnimalKind.PENGUIN, 3300f).pen(150f))
        add(Animal(AnimalKind.PENGUIN, 3450f).pen(150f))
        add(Animal(AnimalKind.PENGUIN, 3560f).pen(150f))

        add(Prop(4250f, back, Z.BACK, 1f, 0) { cc, _ -> enclosure(cc, 520f, "LIONS", C.YELLOW_DARK) }
            .sized(560f, 300f, -150f))
        add(Prop(4130f, 30f, Z.MID, 1f, 0) { cc, p -> Art.rockCluster(cc, p.seed, 78f) }
            .sized(200f, 180f, -90f))
        add(Animal(AnimalKind.LION, 4280f).pen(160f))

        add(Prop(5050f, back, Z.BACK, 1f, 0) { cc, _ -> enclosure(cc, 500f, "ZEBRAS", C.PURPLE) }
            .sized(540f, 300f, -150f))
        add(Animal(AnimalKind.ZEBRA, 5040f).pen(160f))

        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(3), 1620f))
        add(com.bitiplay.world.ent.Npc(com.bitiplay.world.ent.NpcRoster.spec(5), 3820f))

        add(Animal(AnimalKind.TURTLE, 3900f).pen(120f))
        add(Animal(AnimalKind.PARROT, 2260f).pen(50f))
        add(Animal(AnimalKind.PIGEON, 1400f))

        // parrot perch
        add(Prop(2260f, 0f, Z.MID, 1f, 0) { cc, _ ->
            D.capsule(cc, 0f, 0f, 0f, -220f, 12f, C.WOOD_DARK)
            D.capsule(cc, -70f, -220f, 70f, -220f, 12f, C.WOOD)
            D.circle(cc, 0f, -238f, 12f, C.YELLOW_DARK)
        }.sized(180f, 250f, -125f))

        // ---- feeding ---------------------------------------------------
        add(Pushable(PushKind.ZOO_CART, 1300f))

        add(Dispenser(1180f, ItemKind.HAY, "hay for the big ones", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 80f, 14f)
            D.rect(cc, -80f, -120f, 160f, 120f, C.WOOD, 8f)
            D.rect(cc, -88f, -142f, 176f, 26f, C.WOOD_DARK, 8f)
            D.label(cc, "HAY", 0f, -60f, 30f, C.CREAM)
            D.rect(cc, -50f, -176f, 100f, 40f, C.YELLOW_DARK, 10f)
        }.sized(220f, 220f, -110f))

        add(Dispenser(2900f, ItemKind.BAMBOO, "bamboo shoots", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 74f, 13f)
            D.rect(cc, -72f, -120f, 144f, 120f, C.LEAF_DARK, 8f)
            for (i in 0 until 4) D.capsule(cc, -46f + i * 30f, -120f, -46f + i * 30f, -200f, 12f, C.LEAF)
            D.label(cc, "FEED", 0f, -56f, 26f, C.CREAM)
        }.sized(200f, 230f, -115f))

        add(Dispenser(3760f, ItemKind.FISH, "fish for the penguins", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 74f, 13f)
            D.rect(cc, -72f, -110f, 144f, 110f, C.TEAL, 8f)
            D.rect(cc, -56f, -160f, 112f, 56f, C.METAL, 6f)
            D.oval(cc, -20f, -178f, 26f, 12f, C.BLUE)
            D.oval(cc, 24f, -180f, 26f, 12f, C.BLUE_DARK)
        }.sized(200f, 220f, -110f))

        add(Dispenser(4620f, ItemKind.MEAT, "meat for the lions", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 74f, 13f)
            D.rect(cc, -72f, -120f, 144f, 120f, C.RED_DARK, 8f)
            D.rect(cc, -56f, -104f, 112f, 54f, C.GLASS, 5f)
            D.label(cc, "MEAT", 0f, -30f, 26f, C.CREAM)
        }.sized(200f, 220f, -110f))

        add(Dispenser(5250f, ItemKind.PEANUT, "peanuts", Z.MAIN) { cc, _ ->
            D.shadow(cc, 0f, 0f, 62f, 12f)
            D.capsule(cc, 0f, 0f, 0f, -110f, 14f, C.METAL_DARK)
            D.circle(cc, 0f, -160f, 56f, C.RED)
            D.circle(cc, 0f, -160f, 42f, C.GLASS)
            for (i in 0 until 7) D.circle(cc, -26f + (i * 11) % 52, -178f + (i * 13) % 34, 9f, C.CREAM)
            D.rect(cc, -20f, -110f, 40f, 16f, C.METAL, 4f)
        }.sized(150f, 240f, -120f))

        add(Item(ItemKind.WATERING_CAN, 2100f))
        add(Item(ItemKind.BANANA, 2760f))
        add(Item(ItemKind.CARROT, 4900f))
        add(Item(ItemKind.NET, 3660f))

        add(Plant(PlantKind.BUSH, 660f, 3))
        add(Plant(PlantKind.FLOWERS, 3080f, 2))
        add(Plant(PlantKind.SAPLING, 4780f, 2))

        add(Vehicle(VehicleKind.TRAIN, 640f, C.GREEN_DARK))
        add(Vehicle(VehicleKind.JEEP, 4980f))
        add(Vehicle(VehicleKind.BIKE, 2360f, C.YELLOW))

        for (i in 0 until 5) {
            add(Prop(1000f + i * 1080f, 200f, Z.MAIN, 1f, i) { cc, _ -> Art.bench(cc, 160f) }
                .sized(180f, 140f, -70f))
        }
        add(Prop(2000f, 200f, Z.MAIN, 1f, 0) { cc, _ ->
            // rubbish bin
            D.shadow(cc, 0f, 0f, 40f, 9f)
            D.shape(cc, C.GREEN_DARK) { p ->
                p.moveTo(-38f, -110f); p.lineTo(38f, -110f)
                p.lineTo(30f, 0f); p.lineTo(-30f, 0f)
            }
            D.rect(cc, -44f, -126f, 88f, 20f, shade(C.GREEN_DARK, 0.8f), 6f)
        }.sized(110f, 150f, -70f).interactive("tidy up") { ch, sc, p ->
            val held = ch.carried
            if (held != null) {
                ch.consumeCarried(sc)
                ch.startAct(Act.REACH, 0.4f)
                sc.fx.sparkles(p.x, -130f, p.level, 6, C.GREEN)
                Sfx.play(Snd.DROP)
            } else {
                Sfx.play(Snd.POP)
            }
            true
        })

        buildMore()
    }

    /** Three more wings of the zoo. */
    private fun buildMore() {
        for (b in 1 until 4) {
            val ox = b * 5400f

            for (i in 0 until 4) {
                add(Prop(ox + 300f + i * 1300f, back, Z.BACK, 1f, i + b) { cc, p ->
                    Art.tree(cc, p.seed, p.t, 380f + (p.seed % 3) * 50f, C.LEAF_DARK)
                }.sized(300f, 420f, -210f))
                add(Prop(ox + 800f + i * 1300f, 200f, Z.MAIN, 1f, i) { cc, _ -> Art.bench(cc, 160f) }
                    .sized(180f, 140f, -70f))
            }

            // three enclosures per wing, different residents each time
            val sets = arrayOf(
                arrayOf(
                    Triple(AnimalKind.HORSE, "PONIES", C.ORANGE),
                    Triple(AnimalKind.GOAT, "GOATS", C.GREEN),
                    Triple(AnimalKind.SHEEP, "SHEEP", C.TEAL)
                ),
                arrayOf(
                    Triple(AnimalKind.ELEPHANT, "ELEPHANT", C.BLUE),
                    Triple(AnimalKind.MONKEY, "APES", C.GREEN_DARK),
                    Triple(AnimalKind.PARROT, "AVIARY", C.PURPLE)
                ),
                arrayOf(
                    Triple(AnimalKind.TURTLE, "REPTILES", C.YELLOW_DARK),
                    Triple(AnimalKind.PENGUIN, "PENGUINS", C.TEAL),
                    Triple(AnimalKind.LION, "BIG CATS", C.RED_DARK)
                )
            )
            val wing = sets[(b - 1) % sets.size]
            for (i in wing.indices) {
                val ex = ox + 900f + i * 1500f
                val (kind, label, col) = wing[i]
                add(Prop(ex, back, Z.BACK, 1f, i) { cc, _ -> enclosure(cc, 520f, label, col) }
                    .sized(560f, 300f, -150f))
                add(Animal(kind, ex - 60f).pen(160f))
                add(Animal(kind, ex + 90f).pen(160f))
            }

            add(Dispenser(ox + 2200f, ItemKind.HAY, "food for the animals", Z.MAIN) { cc, _ ->
                D.shadow(cc, 0f, 0f, 80f, 14f)
                D.rect(cc, -80f, -120f, 160f, 120f, C.WOOD, 8f)
                D.rect(cc, -88f, -142f, 176f, 26f, C.WOOD_DARK, 8f)
                D.rect(cc, -50f, -176f, 100f, 40f, C.YELLOW_DARK, 10f)
            }.sized(220f, 220f, -110f))

            add(Dispenser(ox + 4200f, if (b == 3) ItemKind.MEAT else ItemKind.PEANUT,
                "a treat to share", Z.MAIN) { cc, _ ->
                D.shadow(cc, 0f, 0f, 62f, 12f)
                D.capsule(cc, 0f, 0f, 0f, -110f, 14f, C.METAL_DARK)
                D.circle(cc, 0f, -160f, 56f, C.RED)
                D.circle(cc, 0f, -160f, 42f, C.GLASS)
                for (i in 0 until 7) D.circle(cc, -26f + (i * 11) % 52, -178f + (i * 13) % 34, 9f, C.CREAM)
            }.sized(150f, 240f, -120f))

            add(Pushable(PushKind.ZOO_CART, ox + 3000f))
            add(Item(ItemKind.WATERING_CAN, ox + 1500f))
            add(Item(ItemKind.BANANA, ox + 3600f))
            add(Plant(PlantKind.BUSH, ox + 600f, 3))
            add(Plant(PlantKind.FLOWERS, ox + 4800f, 2))
            add(Npc(NpcRoster.spec(b + 2), ox + 1900f))
            add(Npc(NpcRoster.spec(b + 4), ox + 3800f))
            add(Vehicle(if (b == 2) VehicleKind.JEEP else VehicleKind.TRAIN,
                ox + 5000f, C.GREEN_DARK))
        }
    }
}
