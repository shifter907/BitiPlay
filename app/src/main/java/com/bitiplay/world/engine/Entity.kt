package com.bitiplay.world.engine

import android.graphics.Canvas
import com.bitiplay.world.ent.Character

/** Draw layers, back to front. */
object Z {
    const val SKY = 0
    const val FAR = 10
    const val MID = 20
    const val DECAL = 30
    const val BACK = 40
    const val MAIN = 50
    const val FRONT = 60
    const val NEAR = 70
    const val OVER = 80
}

/**
 * Everything that lives in a scene. Local draw space has the entity's anchor at
 * the origin, +x forward and -y up, so [draw] implementations never care about
 * the camera, the wrap seam, or which deck they are on.
 */
abstract class Entity {

    /** Anchor position: world x, and y where 0 is the ground line. */
    var x = 0f
    var y = 0f

    /** Deck index for multi-level scenes. */
    var level = 0

    var z = Z.MAIN

    /** 1 = moves with the world, 0 = pinned to the camera (infinitely distant). */
    var parallax = 1f

    /** -1 mirrors the local draw space. */
    var facing = 1

    /** Seconds since spawn. */
    var t = 0f

    var dead = false

    /** Hidden entities still update but are neither drawn nor tappable. */
    var visible = true

    /** Extra draw scale on top of the camera scale. Used for depth and pop-ins. */
    var drawScale = 1f

    /** 0..1. Anything below 1 costs an offscreen layer, so keep it transient. */
    var drawAlpha = 1f

    /** Scratch: signed distance from the camera, filled in each frame by [Render]. */
    @JvmField
    var screenDx = 0f

    /** Tap target box, relative to the anchor. */
    var hitW = 130f
    var hitH = 160f
    var hitCY = -80f

    /** Whether a tap can select this entity at all. */
    var tappable = false

    /** Extra margin used when culling off-screen entities. */
    var cullPad = 320f

    /** How close a character must be to use it. */
    var useRange = 165f

    /**
     * Added to the tap-distance score, in world units. Positive values make an
     * entity lose ties, so a ball lying at a character's feet wins over the
     * character standing on it.
     */
    var pickBias = 0f

    /** Horizontal offset from [x] where a character should stand to use it. */
    open fun approachOffset(): Float = 0f

    /** Depth a character should walk to in order to reach this. */
    open fun standDepth(): Float = y

    open fun onAdded(scene: Scene) {}

    open fun update(dt: Float, scene: Scene) {}

    abstract fun draw(c: Canvas)

    /** Fired when the active character reaches this entity. Returns true if handled. */
    open fun onUse(ch: Character, scene: Scene): Boolean = false

    /**
     * Fired the instant the player taps, before any walking happens.
     * Return true to consume the tap entirely (no walk).
     */
    open fun onTapDirect(ch: Character, scene: Scene): Boolean = false

    /** Short caption shown in the HUD when this is the pending target. */
    open fun caption(ch: Character): String? = null

    fun hitTestLocal(lx: Float, ly: Float): Boolean {
        val hw = hitW * 0.5f
        val top = hitCY - hitH * 0.5f
        val bot = hitCY + hitH * 0.5f
        return lx >= -hw && lx <= hw && ly >= top && ly <= bot
    }
}
