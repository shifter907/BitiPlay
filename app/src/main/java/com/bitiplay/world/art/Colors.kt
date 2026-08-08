package com.bitiplay.world.art

import com.bitiplay.world.core.clamp
import com.bitiplay.world.core.clamp01

/** Flat, saturated storybook palette shared by every environment. */
object C {

    // ---- sky & water ----
    const val SKY_TOP = 0xFF4FBFEA.toInt()
    const val SKY_MID = 0xFF8FD9F3.toInt()
    const val SKY_LOW = 0xFFCFEFFA.toInt()
    const val CLOUD = 0xFFFFFFFF.toInt()
    const val CLOUD_SHADE = 0xFFDCEEF7.toInt()
    const val SUN = 0xFFFFD34E.toInt()
    const val SEA = 0xFF2E9BD1.toInt()
    const val SEA_DEEP = 0xFF1C6FA8.toInt()
    const val SEA_LIGHT = 0xFF63C3E8.toInt()
    const val FOAM = 0xFFE8F8FF.toInt()
    const val POOL = 0xFF54C8E8.toInt()

    // ---- terrain ----
    const val GRASS = 0xFF69C267.toInt()
    const val GRASS_DARK = 0xFF4CA24E.toInt()
    const val GRASS_LIGHT = 0xFF8BD684.toInt()
    const val SAND = 0xFFF2DCA2.toInt()
    const val SAND_DARK = 0xFFDCC286.toInt()
    const val DIRT = 0xFFA9764B.toInt()
    const val DIRT_DARK = 0xFF7F5735.toInt()
    const val ROAD = 0xFF585C63.toInt()
    const val ROAD_DARK = 0xFF44484E.toInt()
    const val ROAD_LINE = 0xFFF6E27A.toInt()
    const val SIDEWALK = 0xFFD8D5CB.toInt()
    const val SIDEWALK_DARK = 0xFFBFBCB2.toInt()
    const val FLOOR = 0xFFEDE9DF.toInt()
    const val FLOOR_DARK = 0xFFD6D1C5.toInt()
    const val DECK = 0xFFD8A96A.toInt()
    const val DECK_DARK = 0xFFB98B52.toInt()

    // ---- materials ----
    const val WOOD = 0xFFB57C4B.toInt()
    const val WOOD_DARK = 0xFF8C5D34.toInt()
    const val WOOD_LIGHT = 0xFFD79E68.toInt()
    const val METAL = 0xFFB9C2CC.toInt()
    const val METAL_DARK = 0xFF8B95A1.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val OFF_WHITE = 0xFFF6F2E8.toInt()
    const val BLACK = 0xFF2E2A2A.toInt()
    const val GLASS = 0xFF9FD8EE.toInt()
    const val GLASS_DARK = 0xFF6FB6D6.toInt()
    const val SHADOW = 0x33000000

    // ---- brights ----
    const val RED = 0xFFF4564C.toInt()
    const val RED_DARK = 0xFFCE3A32.toInt()
    const val ORANGE = 0xFFFF9E42.toInt()
    const val ORANGE_DARK = 0xFFDE7C24.toInt()
    const val YELLOW = 0xFFFFD24A.toInt()
    const val YELLOW_DARK = 0xFFE0B22C.toInt()
    const val GREEN = 0xFF6BC46A.toInt()
    const val GREEN_DARK = 0xFF479B48.toInt()
    const val TEAL = 0xFF3FC1B0.toInt()
    const val BLUE = 0xFF4E8FE0.toInt()
    const val BLUE_DARK = 0xFF2F68B4.toInt()
    const val PURPLE = 0xFF9A73D8.toInt()
    const val PURPLE_DARK = 0xFF7551B0.toInt()
    const val PINK = 0xFFF58BB4.toInt()
    const val PINK_DARK = 0xFFD46691.toInt()
    const val BROWN = 0xFF8E6142.toInt()
    const val CREAM = 0xFFFCE9C4.toInt()

    // ---- foliage ----
    const val LEAF = 0xFF57B85C.toInt()
    const val LEAF_DARK = 0xFF3E9145.toInt()
    const val LEAF_LIGHT = 0xFF7FD177.toInt()
    const val TRUNK = 0xFF8C6039.toInt()

    // ---- ui ----
    const val UI_PANEL = 0xF2FFFFFF.toInt()
    const val UI_PANEL_DIM = 0xCC1B2430.toInt()
    const val UI_ACCENT = 0xFFFF9E42.toInt()
    const val UI_TEXT = 0xFF33404E.toInt()
    const val UI_TEXT_LIGHT = 0xFFFFFFFF.toInt()
    const val UI_SCRIM = 0xA6101820.toInt()
}

/** Multiplies RGB by [f] (values above 1 lighten toward white). */
fun shade(color: Int, f: Float): Int {
    val a = (color ushr 24) and 0xFF
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val nr: Int
    val ng: Int
    val nb: Int
    if (f <= 1f) {
        nr = (r * f).toInt()
        ng = (g * f).toInt()
        nb = (b * f).toInt()
    } else {
        val t = clamp01(f - 1f)
        nr = (r + (255 - r) * t).toInt()
        ng = (g + (255 - g) * t).toInt()
        nb = (b + (255 - b) * t).toInt()
    }
    return (a shl 24) or
        (clamp(nr.toFloat(), 0f, 255f).toInt() shl 16) or
        (clamp(ng.toFloat(), 0f, 255f).toInt() shl 8) or
        clamp(nb.toFloat(), 0f, 255f).toInt()
}

fun withAlpha(color: Int, alpha: Int): Int =
    (clamp(alpha.toFloat(), 0f, 255f).toInt() shl 24) or (color and 0x00FFFFFF)

fun withAlphaF(color: Int, alpha: Float): Int = withAlpha(color, (clamp01(alpha) * 255f).toInt())

fun mix(a: Int, b: Int, t: Float): Int {
    val u = clamp01(t)
    val aa = ((a ushr 24) and 0xFF) + (((b ushr 24) and 0xFF) - ((a ushr 24) and 0xFF)) * u
    val rr = ((a shr 16) and 0xFF) + (((b shr 16) and 0xFF) - ((a shr 16) and 0xFF)) * u
    val gg = ((a shr 8) and 0xFF) + (((b shr 8) and 0xFF) - ((a shr 8) and 0xFF)) * u
    val bb = (a and 0xFF) + ((b and 0xFF) - (a and 0xFF)) * u
    return (aa.toInt() shl 24) or (rr.toInt() shl 16) or (gg.toInt() shl 8) or bb.toInt()
}
