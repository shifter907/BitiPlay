package com.bitiplay.world.ent

import android.graphics.Canvas
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.shade
import com.bitiplay.world.audio.Sfx
import com.bitiplay.world.audio.Snd
import com.bitiplay.world.core.TAU
import com.bitiplay.world.core.rnd
import com.bitiplay.world.core.wobble
import com.bitiplay.world.engine.Entity
import com.bitiplay.world.engine.Scene
import com.bitiplay.world.engine.Z
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Everything a character can pick up.
 *
 * [food] marks something an animal will accept, [tool] marks something whose
 * presence in the hands unlocks an action (watering, digging, scooping).
 */
enum class ItemKind(
    val label: String,
    val size: Float = 62f,
    val food: Boolean = false,
    val tool: Boolean = false,
    val rolls: Boolean = false
) {
    BALL("ball", 58f, rolls = true),
    BEACHBALL("beach ball", 70f, rolls = true),
    FRISBEE("frisbee", 62f),
    TEDDY("teddy", 62f),
    BALLOON("balloon", 66f),
    WATERING_CAN("watering can", 68f, tool = true),
    SHOVEL("shovel", 76f, tool = true),
    BUCKET("bucket", 60f, tool = true),
    NET("net", 78f, tool = true),
    APPLE("apple", 52f, food = true, rolls = true),
    CARROT("carrot", 58f, food = true),
    BANANA("banana", 58f, food = true),
    WATERMELON("watermelon", 64f, food = true),
    CORN("corn", 60f, food = true),
    TOMATO("tomato", 50f, food = true, rolls = true),
    LETTUCE("lettuce", 58f, food = true),
    PUMPKIN("pumpkin", 64f, food = true, rolls = true),
    HAY("hay", 66f, food = true),
    BONE("bone", 58f, food = true),
    FISH("fish", 62f, food = true),
    PEANUT("peanut", 48f, food = true),
    BAMBOO("bamboo", 72f, food = true),
    MEAT("meat", 60f, food = true),
    SEED_BAG("seeds", 60f, food = true),
    PATTY_RAW("raw patty", 54f, food = true),
    BURGER("burger", 60f, food = true),
    SAUSAGE_RAW("raw sausage", 56f, food = true),
    HOTDOG("hot dog", 62f, food = true),
    CORN_GRILLED("grilled corn", 60f, food = true),
    FISH_COOKED("cooked fish", 62f, food = true),
    MARSHMALLOW("marshmallow", 60f, food = true),
    MARSHMALLOW_TOASTED("toasted marshmallow", 60f, food = true),
    PIZZA("pizza", 62f, food = true),
    DONUT("donut", 56f, food = true),
    ICE_CREAM("ice cream", 66f, food = true),
    CUPCAKE("cupcake", 56f, food = true),
    PRETZEL("pretzel", 58f, food = true),
    POPCORN("popcorn", 60f, food = true),
    EGG("egg", 46f, food = true),
    BREAD("bread", 62f, food = true),
    MILK("milk", 58f, food = true),
    JUICE("juice", 56f, food = true),
    SHELL("shell", 50f),
    STARFISH("starfish", 56f),
    COCONUT("coconut", 54f, food = true, rolls = true),
    FLOWER("flower", 58f),
    LOG("log", 64f),
    ROCK("rock", 52f, rolls = true),
    TICKET("ticket", 52f),
    GIFT("gift", 58f),
    TREASURE("treasure", 62f),
    LANTERN("lantern", 60f),
    KEY("key", 50f);

    /** What this becomes on a grill, or null if it is not cookable. */
    val cooked: ItemKind?
        get() = when (this) {
            PATTY_RAW -> BURGER
            SAUSAGE_RAW -> HOTDOG
            CORN -> CORN_GRILLED
            FISH -> FISH_COOKED
            MARSHMALLOW -> MARSHMALLOW_TOASTED
            else -> null
        }
}

/** A loose object in the world. */
class Item(var kind: ItemKind, startX: Float, startY: Float = -kind.size * 0.42f) : Entity() {

    var vx = 0f
    var vy = 0f
    var rot = 0f
    var spin = 0f

    /** Null when the item is lying in the world. */
    var holder: Entity? = null

    /** World y of the ground plane this item rests on - its depth in the scene. */
    var baseY = 0f

    /** True while a container (grill, cart, crate) owns the item. */
    var stored = false

    private var resting = true
    private var bounces = 0

    init {
        x = startX
        y = startY
        z = Z.MAIN
        tappable = true
        hitW = kind.size + 34f
        hitH = kind.size + 34f
        hitCY = 0f
        useRange = 130f
        cullPad = 160f
    }

    val restY: Float get() = baseY - kind.size * 0.42f

    fun launch(dirX: Float, power: Float = 1f) {
        holder = null
        stored = false
        visible = true
        resting = false
        bounces = 0
        vx = dirX * 520f * power
        vy = -600f * power
        spin = if (kind.rolls) dirX * 9f else rnd(-7f, 7f)
    }

    fun place(px: Float, py: Float) {
        holder = null
        stored = false
        visible = true
        x = px
        y = py
        vx = 0f
        vy = 0f
        resting = false
    }

    override fun update(dt: Float, scene: Scene) {
        if (holder != null || stored) return

        if (!resting) {
            vy += 2100f * dt
            x += vx * dt
            y += vy * dt
            rot += spin * dt

            val floor = restY
            if (y >= floor) {
                y = floor
                if (vy > 260f && bounces < 3) {
                    vy = -vy * 0.36f
                    vx *= 0.62f
                    bounces++
                    if (bounces == 1) Sfx.play(Snd.DROP)
                    scene.fx.dust(x, 0f, level, 3)
                } else {
                    vy = 0f
                    // Round things keep rolling for a moment.
                    vx *= if (kind.rolls) 0.985f else 0.72f
                    if (abs(vx) < 12f) {
                        vx = 0f
                        resting = true
                        if (!kind.rolls) rot = 0f
                    }
                }
            }
            if (kind.rolls && y >= floor - 1f) {
                spin = vx / (kind.size * 0.42f)
                rot += spin * dt
            }
            x = scene.wrapX(x)
        }
    }

    override fun draw(c: Canvas) {
        val groundOffset = baseY - y
        if (resting && abs(rot) < 0.001f) {
            D.shadow(c, 0f, groundOffset, kind.size * 0.42f, kind.size * 0.14f, 40)
        } else if (holder == null) {
            D.shadow(c, 0f, groundOffset, kind.size * 0.34f, kind.size * 0.1f, 26)
        }
        c.save()
        if (rot != 0f) c.rotate(rot * 57.29578f)
        ItemArt.draw(c, kind, t)
        c.restore()
    }

    override fun standDepth(): Float = baseY

    override fun caption(ch: Character): String = kind.label

    override fun onUse(ch: Character, scene: Scene): Boolean {
        if (holder != null || stored) return false
        ch.pickUp(this, scene)
        return true
    }
}

/** All item artwork. Each shape is drawn centred on the origin. */
object ItemArt {

    fun draw(c: Canvas, k: ItemKind, t: Float) {
        when (k) {
            ItemKind.BALL -> {
                D.circle(c, 0f, 0f, 29f, C.RED)
                D.arcLine(c, 0f, 0f, 29f, 29f, 200f, 140f, C.WHITE, 8f)
                D.circle(c, -10f, -10f, 7f, com.bitiplay.world.art.withAlpha(C.WHITE, 150))
            }
            ItemKind.BEACHBALL -> {
                D.circle(c, 0f, 0f, 35f, C.WHITE)
                D.arcFill(c, 0f, 0f, 35f, 35f, -90f, 60f, C.RED)
                D.arcFill(c, 0f, 0f, 35f, 35f, 30f, 60f, C.YELLOW)
                D.arcFill(c, 0f, 0f, 35f, 35f, 150f, 60f, C.BLUE)
                D.circle(c, 0f, 0f, 7f, C.WHITE)
            }
            ItemKind.FRISBEE -> {
                D.oval(c, 0f, 0f, 31f, 12f, C.PURPLE)
                D.oval(c, 0f, -3f, 22f, 7f, shade(C.PURPLE, 1.25f))
            }
            ItemKind.TEDDY -> {
                D.circle(c, -13f, -18f, 8f, C.BROWN)
                D.circle(c, 13f, -18f, 8f, C.BROWN)
                D.circle(c, 0f, -12f, 17f, shade(C.BROWN, 1.12f))
                D.oval(c, 0f, 14f, 16f, 18f, C.BROWN)
                D.oval(c, 0f, -6f, 8f, 6f, C.CREAM)
                D.circle(c, -6f, -14f, 3f, C.BLACK)
                D.circle(c, 6f, -14f, 3f, C.BLACK)
                D.circle(c, 0f, -6f, 3f, C.BLACK)
            }
            ItemKind.BALLOON -> {
                D.stroke(c, C.WHITE, 3f) { p ->
                    p.moveTo(0f, 8f); p.quadTo(8f, 22f, 0f, 33f)
                }
                D.oval(c, 0f, -8f, 22f, 26f, C.RED)
                D.tri(c, -5f, 16f, 5f, 16f, 0f, 24f, shade(C.RED, 0.85f))
                D.oval(c, -8f, -16f, 6f, 9f, com.bitiplay.world.art.withAlpha(C.WHITE, 140))
            }
            ItemKind.WATERING_CAN -> {
                D.rectC(c, -2f, 6f, 40f, 32f, C.GREEN, 8f)
                D.quad(c, 16f, -2f, 34f, -18f, 40f, -12f, 20f, 6f, C.GREEN)
                D.circle(c, 37f, -15f, 7f, shade(C.GREEN, 1.2f))
                D.arcLine(c, -18f, -2f, 14f, 16f, 100f, 160f, shade(C.GREEN, 0.8f), 7f)
                D.rectC(c, -2f, -12f, 26f, 8f, shade(C.GREEN, 0.82f), 4f)
            }
            ItemKind.SHOVEL -> {
                D.capsule(c, -6f, -34f, 4f, 12f, 9f, C.WOOD)
                D.rectC(c, -8f, -34f, 20f, 8f, C.WOOD_DARK, 4f)
                D.shape(c, C.METAL) { p ->
                    p.moveTo(-12f, 10f); p.lineTo(18f, 10f)
                    p.lineTo(14f, 34f); p.lineTo(-8f, 34f)
                }
            }
            ItemKind.BUCKET -> {
                D.shape(c, C.BLUE) { p ->
                    p.moveTo(-24f, -14f); p.lineTo(24f, -14f)
                    p.lineTo(17f, 24f); p.lineTo(-17f, 24f)
                }
                D.oval(c, 0f, -14f, 24f, 7f, shade(C.BLUE, 1.25f))
                D.arcLine(c, 0f, -16f, 22f, 22f, 195f, 150f, C.METAL_DARK, 5f)
            }
            ItemKind.NET -> {
                D.capsule(c, -4f, 36f, 2f, -6f, 8f, C.WOOD)
                D.circleStroke(c, 2f, -20f, 20f, C.METAL, 6f)
                D.stroke(c, com.bitiplay.world.art.withAlpha(C.WHITE, 190), 3f) { p ->
                    p.moveTo(-16f, -26f); p.lineTo(20f, -14f)
                    p.moveTo(-16f, -14f); p.lineTo(20f, -26f)
                }
            }
            ItemKind.APPLE -> {
                D.circle(c, -7f, 3f, 20f, C.RED)
                D.circle(c, 7f, 3f, 20f, C.RED)
                D.rectC(c, 0f, -19f, 5f, 14f, C.WOOD_DARK, 2f)
                D.oval(c, 12f, -22f, 11f, 6f, C.LEAF)
                D.oval(c, -8f, -4f, 5f, 8f, com.bitiplay.world.art.withAlpha(C.WHITE, 110))
            }
            ItemKind.CARROT -> {
                D.shape(c, C.ORANGE) { p ->
                    p.moveTo(-13f, -12f); p.lineTo(13f, -12f); p.lineTo(2f, 32f)
                }
                D.stroke(c, shade(C.ORANGE, 0.82f), 3f) { p ->
                    p.moveTo(-8f, 0f); p.lineTo(-2f, 2f)
                    p.moveTo(2f, 10f); p.lineTo(8f, 8f)
                }
                D.oval(c, -9f, -22f, 6f, 12f, C.LEAF)
                D.oval(c, 0f, -26f, 6f, 14f, C.LEAF_LIGHT)
                D.oval(c, 9f, -22f, 6f, 12f, C.LEAF)
            }
            ItemKind.BANANA -> {
                D.stroke(c, C.YELLOW, 17f) { p ->
                    p.moveTo(-22f, -14f); p.quadTo(2f, 22f, 24f, -8f)
                }
                D.stroke(c, shade(C.YELLOW, 0.85f), 4f) { p ->
                    p.moveTo(-16f, -8f); p.quadTo(2f, 16f, 19f, -6f)
                }
                D.circle(c, -22f, -15f, 5f, C.WOOD_DARK)
            }
            ItemKind.WATERMELON -> {
                D.arcFill(c, 0f, 12f, 32f, 32f, 180f, 180f, C.GREEN_DARK)
                D.arcFill(c, 0f, 12f, 27f, 27f, 180f, 180f, C.RED)
                D.circle(c, -12f, 0f, 3f, C.BLACK)
                D.circle(c, 2f, -4f, 3f, C.BLACK)
                D.circle(c, 14f, 1f, 3f, C.BLACK)
            }
            ItemKind.CORN, ItemKind.CORN_GRILLED -> {
                val grilled = k == ItemKind.CORN_GRILLED
                D.oval(c, 0f, 0f, 15f, 30f, if (grilled) shade(C.YELLOW, 0.9f) else C.YELLOW)
                for (i in 0..3) {
                    D.line(c, -11f, -18f + i * 12f, 11f, -18f + i * 12f, shade(C.YELLOW, 0.8f), 3f)
                }
                if (grilled) {
                    D.line(c, -13f, -8f, 13f, -12f, shade(C.BROWN, 0.7f), 4f)
                    D.line(c, -13f, 10f, 13f, 6f, shade(C.BROWN, 0.7f), 4f)
                }
                D.oval(c, -16f, 10f, 8f, 20f, C.LEAF)
                D.oval(c, 16f, 10f, 8f, 20f, C.LEAF_DARK)
            }
            ItemKind.TOMATO -> {
                D.circle(c, 0f, 3f, 22f, C.RED)
                D.circle(c, -7f, -5f, 6f, com.bitiplay.world.art.withAlpha(C.WHITE, 100))
                D.star(c, 0f, -16f, 12f, 4f, 5, C.LEAF)
                D.rectC(c, 0f, -24f, 5f, 10f, C.LEAF_DARK, 2f)
            }
            ItemKind.LETTUCE -> {
                D.circle(c, 0f, 4f, 25f, C.LEAF_DARK)
                D.circle(c, -11f, -2f, 15f, C.LEAF)
                D.circle(c, 11f, -2f, 15f, C.LEAF)
                D.circle(c, 0f, -10f, 14f, C.LEAF_LIGHT)
            }
            ItemKind.PUMPKIN -> {
                D.oval(c, 0f, 5f, 30f, 25f, C.ORANGE)
                D.oval(c, -14f, 5f, 10f, 24f, shade(C.ORANGE, 0.88f))
                D.oval(c, 14f, 5f, 10f, 24f, shade(C.ORANGE, 0.88f))
                D.rectC(c, 0f, -22f, 8f, 14f, C.LEAF_DARK, 3f)
            }
            ItemKind.HAY -> {
                D.rectC(c, 0f, 0f, 58f, 40f, C.YELLOW_DARK, 10f)
                D.rectC(c, 0f, 0f, 58f, 30f, shade(C.YELLOW, 0.95f), 8f)
                D.line(c, -16f, -18f, -16f, 18f, C.WOOD_DARK, 5f)
                D.line(c, 16f, -18f, 16f, 18f, C.WOOD_DARK, 5f)
                for (i in -2..2) D.line(c, i * 9f, -13f, i * 9f + 4f, 13f, shade(C.YELLOW, 0.82f), 2f)
            }
            ItemKind.BONE -> {
                D.rectC(c, 0f, 0f, 40f, 13f, C.OFF_WHITE, 6f)
                D.circle(c, -21f, -7f, 9f, C.OFF_WHITE)
                D.circle(c, -21f, 7f, 9f, C.OFF_WHITE)
                D.circle(c, 21f, -7f, 9f, C.OFF_WHITE)
                D.circle(c, 21f, 7f, 9f, C.OFF_WHITE)
            }
            ItemKind.FISH, ItemKind.FISH_COOKED -> {
                val col = if (k == ItemKind.FISH_COOKED) shade(C.ORANGE, 0.9f) else C.BLUE
                D.oval(c, 2f, 0f, 26f, 16f, col)
                D.tri(c, -20f, 0f, -34f, -13f, -34f, 13f, shade(col, 0.85f))
                D.tri(c, 4f, -14f, -6f, -24f, 14f, -22f, shade(col, 0.85f))
                D.circle(c, 16f, -4f, 4f, C.WHITE)
                D.circle(c, 17f, -4f, 2f, C.BLACK)
                if (k == ItemKind.FISH_COOKED) {
                    D.line(c, -6f, -8f, -2f, 8f, shade(C.BROWN, 0.6f), 3f)
                    D.line(c, 8f, -8f, 12f, 8f, shade(C.BROWN, 0.6f), 3f)
                }
            }
            ItemKind.PEANUT -> {
                D.circle(c, -9f, -6f, 13f, C.CREAM)
                D.circle(c, 8f, 6f, 14f, C.CREAM)
                D.oval(c, 0f, 0f, 12f, 14f, C.CREAM)
                D.circle(c, -12f, -9f, 2.5f, shade(C.CREAM, 0.78f))
                D.circle(c, 10f, 8f, 2.5f, shade(C.CREAM, 0.78f))
            }
            ItemKind.BAMBOO -> {
                D.rectC(c, 0f, 0f, 13f, 66f, C.LEAF_DARK, 5f)
                D.line(c, -7f, -18f, 7f, -18f, shade(C.LEAF_DARK, 0.75f), 4f)
                D.line(c, -7f, 8f, 7f, 8f, shade(C.LEAF_DARK, 0.75f), 4f)
                D.oval(c, 16f, -22f, 16f, 6f, C.LEAF)
                D.oval(c, -16f, 2f, 16f, 6f, C.LEAF)
            }
            ItemKind.MEAT -> {
                D.oval(c, 3f, 0f, 24f, 18f, 0xFFD4756B.toInt())
                D.oval(c, 3f, 0f, 15f, 11f, 0xFFB9564F.toInt())
                D.rectC(c, -22f, 0f, 18f, 9f, C.OFF_WHITE, 4f)
                D.circle(c, -30f, -5f, 6f, C.OFF_WHITE)
                D.circle(c, -30f, 5f, 6f, C.OFF_WHITE)
            }
            ItemKind.SEED_BAG -> {
                D.shape(c, 0xFFCBA76A.toInt()) { p ->
                    p.moveTo(-20f, -12f); p.lineTo(20f, -12f)
                    p.lineTo(24f, 28f); p.lineTo(-24f, 28f)
                }
                D.rectC(c, 0f, -14f, 34f, 10f, shade(0xFFCBA76A.toInt(), 0.82f), 4f)
                D.circle(c, -7f, 8f, 5f, C.LEAF_DARK)
                D.circle(c, 6f, 14f, 5f, C.LEAF_DARK)
                D.circle(c, 8f, 1f, 5f, C.LEAF_DARK)
            }
            ItemKind.PATTY_RAW -> {
                D.oval(c, 0f, 0f, 27f, 13f, 0xFFC96F72.toInt())
                D.oval(c, 0f, -3f, 24f, 10f, 0xFFDD8487.toInt())
            }
            ItemKind.BURGER -> {
                D.arcFill(c, 0f, 4f, 30f, 24f, 180f, 180f, 0xFFE0A85E.toInt())
                D.oval(c, 0f, 4f, 30f, 8f, C.LEAF)
                D.oval(c, 0f, 12f, 29f, 9f, 0xFF8A5A44.toInt())
                D.rectC(c, 0f, 22f, 54f, 12f, 0xFFD9A05B.toInt(), 6f)
                D.circle(c, -10f, -8f, 2.5f, C.CREAM)
                D.circle(c, 6f, -12f, 2.5f, C.CREAM)
                D.circle(c, 15f, -4f, 2.5f, C.CREAM)
            }
            ItemKind.SAUSAGE_RAW -> {
                D.stroke(c, 0xFFD98B87.toInt(), 20f) { p ->
                    p.moveTo(-22f, 6f); p.quadTo(0f, -12f, 22f, 6f)
                }
            }
            ItemKind.HOTDOG -> {
                D.stroke(c, 0xFFE3AE6A.toInt(), 26f) { p ->
                    p.moveTo(-26f, 8f); p.lineTo(26f, 8f)
                }
                D.stroke(c, 0xFFC4674F.toInt(), 17f) { p ->
                    p.moveTo(-24f, 2f); p.quadTo(0f, -10f, 24f, 2f)
                }
                D.stroke(c, C.YELLOW, 5f) { p ->
                    p.moveTo(-18f, 0f); p.quadTo(-8f, -10f, 0f, -2f)
                    p.quadTo(8f, 6f, 18f, -4f)
                }
            }
            ItemKind.MARSHMALLOW, ItemKind.MARSHMALLOW_TOASTED -> {
                val col = if (k == ItemKind.MARSHMALLOW) C.WHITE else 0xFFC98A4B.toInt()
                D.capsule(c, -2f, 34f, -2f, 6f, 5f, C.WOOD)
                D.rectC(c, 0f, -8f, 30f, 34f, col, 12f)
                D.oval(c, 0f, -24f, 15f, 6f, shade(col, 1.08f))
            }
            ItemKind.PIZZA -> {
                D.tri(c, -26f, 22f, 26f, 22f, 0f, -26f, 0xFFE8B562.toInt())
                D.tri(c, -21f, 17f, 21f, 17f, 0f, -18f, C.YELLOW)
                D.rectC(c, 0f, 22f, 52f, 12f, 0xFFD79B54.toInt(), 6f)
                D.circle(c, -8f, 8f, 5f, C.RED)
                D.circle(c, 9f, 6f, 5f, C.RED)
                D.circle(c, 0f, -6f, 5f, C.RED)
            }
            ItemKind.DONUT -> {
                D.circle(c, 0f, 0f, 27f, 0xFFD79B54.toInt())
                D.circle(c, 0f, -3f, 26f, C.PINK)
                D.circle(c, 0f, 0f, 9f, 0xFFD79B54.toInt())
                D.line(c, -14f, -10f, -8f, -14f, C.YELLOW, 4f)
                D.line(c, 10f, -8f, 15f, -13f, C.TEAL, 4f)
                D.line(c, 2f, 14f, 8f, 12f, C.WHITE, 4f)
                D.line(c, -12f, 10f, -7f, 14f, C.PURPLE, 4f)
            }
            ItemKind.ICE_CREAM -> {
                D.tri(c, -14f, 2f, 14f, 2f, 0f, 34f, 0xFFD9A05B.toInt())
                D.line(c, -8f, 10f, 4f, 4f, shade(0xFFD9A05B.toInt(), 0.8f), 2.5f)
                D.circle(c, 0f, -4f, 16f, C.PINK)
                D.circle(c, -6f, -18f, 13f, C.CREAM)
                D.circle(c, 7f, -20f, 11f, 0xFF9AD8B0.toInt())
                D.circle(c, 2f, -32f, 5f, C.RED)
            }
            ItemKind.CUPCAKE -> {
                D.shape(c, C.PINK_DARK) { p ->
                    p.moveTo(-20f, 2f); p.lineTo(20f, 2f)
                    p.lineTo(15f, 28f); p.lineTo(-15f, 28f)
                }
                for (i in -2..2) D.line(c, i * 7.5f, 4f, i * 7.5f, 26f, shade(C.PINK_DARK, 0.85f), 2.5f)
                D.circle(c, -9f, -4f, 12f, C.CREAM)
                D.circle(c, 9f, -4f, 12f, C.CREAM)
                D.circle(c, 0f, -16f, 12f, C.WHITE)
                D.circle(c, 0f, -28f, 6f, C.RED)
            }
            ItemKind.PRETZEL -> {
                val col = 0xFFB5793F.toInt()
                D.circleStroke(c, -11f, -2f, 13f, col, 9f)
                D.circleStroke(c, 11f, -2f, 13f, col, 9f)
                D.stroke(c, col, 9f) { p ->
                    p.moveTo(-16f, 8f); p.quadTo(0f, 28f, 16f, 8f)
                }
            }
            ItemKind.POPCORN -> {
                D.shape(c, C.RED) { p ->
                    p.moveTo(-20f, -6f); p.lineTo(20f, -6f)
                    p.lineTo(16f, 28f); p.lineTo(-16f, 28f)
                }
                for (i in -2..2) D.line(c, i * 8f, -4f, i * 8f, 26f, C.WHITE, 4f)
                D.circle(c, -12f, -10f, 9f, C.CREAM)
                D.circle(c, 0f, -16f, 10f, C.OFF_WHITE)
                D.circle(c, 12f, -9f, 9f, C.CREAM)
            }
            ItemKind.EGG -> {
                D.oval(c, 0f, 0f, 17f, 22f, C.OFF_WHITE)
                D.oval(c, -5f, -6f, 5f, 7f, C.WHITE)
            }
            ItemKind.BREAD -> {
                D.arcFill(c, 0f, 12f, 30f, 26f, 180f, 180f, 0xFFCE9553.toInt())
                D.rectC(c, 0f, 16f, 60f, 16f, 0xFFE0AC6C.toInt(), 6f)
                D.line(c, -14f, -6f, -8f, -14f, shade(0xFFCE9553.toInt(), 0.82f), 4f)
                D.line(c, 2f, -8f, 8f, -16f, shade(0xFFCE9553.toInt(), 0.82f), 4f)
            }
            ItemKind.MILK -> {
                D.rectC(c, 0f, 6f, 34f, 40f, C.WHITE, 4f)
                D.tri(c, -17f, -14f, 17f, -14f, 0f, -32f, C.OFF_WHITE)
                D.rectC(c, 0f, 4f, 24f, 14f, C.BLUE, 3f)
                D.circle(c, 0f, -28f, 5f, C.BLUE_DARK)
            }
            ItemKind.JUICE -> {
                D.shape(c, 0xFFFFB347.toInt()) { p ->
                    p.moveTo(-16f, -14f); p.lineTo(16f, -14f)
                    p.lineTo(12f, 26f); p.lineTo(-12f, 26f)
                }
                D.oval(c, 0f, -14f, 16f, 5f, shade(0xFFFFB347.toInt(), 1.2f))
                D.capsule(c, 6f, -16f, 14f, -34f, 6f, C.RED)
            }
            ItemKind.SHELL -> {
                D.arcFill(c, 0f, 16f, 26f, 30f, 180f, 180f, C.PINK)
                D.stroke(c, shade(C.PINK, 0.82f), 3f) { p ->
                    p.moveTo(0f, 16f); p.lineTo(-18f, -2f)
                    p.moveTo(0f, 16f); p.lineTo(0f, -14f)
                    p.moveTo(0f, 16f); p.lineTo(18f, -2f)
                }
            }
            ItemKind.STARFISH -> {
                D.star(c, 0f, 0f, 28f, 12f, 5, C.ORANGE)
                D.circle(c, 0f, 0f, 4f, shade(C.ORANGE, 0.8f))
                D.circle(c, -8f, -6f, 3f, shade(C.ORANGE, 0.8f))
                D.circle(c, 8f, -6f, 3f, shade(C.ORANGE, 0.8f))
            }
            ItemKind.COCONUT -> {
                D.circle(c, 0f, 0f, 25f, C.WOOD_DARK)
                D.circle(c, -8f, -8f, 4f, shade(C.WOOD_DARK, 0.7f))
                D.circle(c, 4f, -10f, 4f, shade(C.WOOD_DARK, 0.7f))
                D.circle(c, -2f, -1f, 4f, shade(C.WOOD_DARK, 0.7f))
            }
            ItemKind.FLOWER -> {
                D.capsule(c, 0f, 30f, 0f, 2f, 5f, C.LEAF_DARK)
                D.oval(c, 12f, 16f, 11f, 5f, C.LEAF)
                for (i in 0 until 5) {
                    val a = i * TAU / 5f - TAU * 0.25f
                    D.circle(c, cos(a) * 15f, sin(a) * 15f - 6f, 11f, C.PINK)
                }
                D.circle(c, 0f, -6f, 9f, C.YELLOW)
            }
            ItemKind.LOG -> {
                D.rectC(c, 0f, 0f, 62f, 30f, C.WOOD, 6f)
                D.oval(c, -31f, 0f, 8f, 15f, C.WOOD_LIGHT)
                D.oval(c, -31f, 0f, 4f, 8f, shade(C.WOOD_LIGHT, 0.82f))
                D.line(c, -14f, -8f, 20f, -8f, C.WOOD_DARK, 3f)
                D.line(c, -10f, 8f, 24f, 8f, C.WOOD_DARK, 3f)
            }
            ItemKind.ROCK -> {
                D.shape(c, C.METAL_DARK) { p ->
                    p.moveTo(-25f, 16f); p.lineTo(-15f, -14f)
                    p.lineTo(8f, -19f); p.lineTo(25f, 2f); p.lineTo(18f, 16f)
                }
                D.shape(c, shade(C.METAL_DARK, 1.2f)) { p ->
                    p.moveTo(-15f, -14f); p.lineTo(8f, -19f); p.lineTo(2f, -2f)
                }
            }
            ItemKind.TICKET -> {
                D.rectC(c, 0f, 0f, 54f, 32f, C.YELLOW, 5f)
                D.circle(c, -27f, 0f, 6f, com.bitiplay.world.art.withAlpha(C.BLACK, 0))
                D.circle(c, -27f, 0f, 6f, C.SKY_LOW)
                D.circle(c, 27f, 0f, 6f, C.SKY_LOW)
                D.line(c, -12f, -9f, -12f, 9f, C.YELLOW_DARK, 3f)
                D.star(c, 8f, 0f, 9f, 4f, 5, C.YELLOW_DARK)
            }
            ItemKind.GIFT -> {
                D.rectC(c, 0f, 6f, 48f, 40f, C.RED, 5f)
                D.rectC(c, 0f, 6f, 12f, 40f, C.YELLOW)
                D.rectC(c, 0f, -6f, 48f, 10f, C.YELLOW, 3f)
                D.circle(c, -8f, -18f, 9f, C.YELLOW)
                D.circle(c, 8f, -18f, 9f, C.YELLOW)
            }
            ItemKind.TREASURE -> {
                D.rectC(c, 0f, 12f, 56f, 28f, C.WOOD, 4f)
                D.arcFill(c, 0f, -2f, 28f, 20f, 180f, 180f, C.WOOD_DARK)
                D.rectC(c, 0f, -2f, 56f, 8f, C.YELLOW_DARK, 2f)
                D.rectC(c, 0f, 8f, 12f, 14f, C.YELLOW, 2f)
                D.circle(c, 0f, 10f, 3f, C.WOOD_DARK)
            }
            ItemKind.LANTERN -> {
                D.arcLine(c, 0f, -18f, 10f, 10f, 180f, 180f, C.METAL_DARK, 4f)
                D.rectC(c, 0f, -14f, 26f, 8f, C.METAL_DARK, 3f)
                D.shape(c, com.bitiplay.world.art.withAlpha(C.YELLOW, 220)) { p ->
                    p.moveTo(-14f, -10f); p.lineTo(14f, -10f)
                    p.lineTo(11f, 22f); p.lineTo(-11f, 22f)
                }
                D.circle(c, 0f, 6f, 7f, C.WHITE)
                D.rectC(c, 0f, 24f, 30f, 8f, C.METAL_DARK, 3f)
            }
            ItemKind.KEY -> {
                D.circleStroke(c, -14f, -2f, 12f, C.YELLOW_DARK, 7f)
                D.rectC(c, 10f, -2f, 34f, 7f, C.YELLOW_DARK, 3f)
                D.rectC(c, 20f, 5f, 7f, 12f, C.YELLOW_DARK, 2f)
                D.rectC(c, 8f, 5f, 7f, 10f, C.YELLOW_DARK, 2f)
            }
        }
    }

    /** Small icon version used by HUD chips and shelf displays. */
    fun icon(c: Canvas, k: ItemKind, size: Float) {
        c.save()
        val s = size / 62f
        c.scale(s, s)
        draw(c, k, 0f)
        c.restore()
    }
}
