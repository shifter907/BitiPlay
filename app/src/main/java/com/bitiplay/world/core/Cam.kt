package com.bitiplay.world.core

import kotlin.math.max

/**
 * Maps world units to screen pixels.
 *
 * World space: `y = 0` is the ground line, negative `y` is up. Horizontal space
 * loops for wrapping scenes, so screen positions are always computed from a
 * shortest-path delta rather than an absolute difference.
 *
 * Vertical "levels" (the cruise ship's decks) are authored in identical local
 * coordinates and separated at render time by [levelHeight]; [levelF] is
 * fractional so deck changes animate as a slide.
 */
class Cam {

    /** World x at the centre of the screen. */
    var x = 0f

    /** World y at the centre of the screen. */
    var y = -315f

    /** Current (possibly fractional) deck index. */
    var levelF = 0f

    var screenW = 1f
        private set
    var screenH = 1f
        private set

    /** Pixels per world unit. */
    var scale = 1f
        private set

    /** Visible height in world units. */
    var visH = DESIGN_H
        private set

    /** Visible width in world units. */
    var visW = DESIGN_W_MAX
        private set

    /** Vertical spacing between decks, in world units. */
    var levelHeight = DESIGN_H
        private set

    /** The camera y that puts the ground line near the bottom of the screen. */
    var restY = -315f
        private set

    fun resize(w: Float, h: Float) {
        screenW = max(1f, w)
        screenH = max(1f, h)
        // Fit the design height, but never reveal more than DESIGN_W_MAX of world
        // width on very wide devices.
        scale = max(screenH / DESIGN_H, screenW / DESIGN_W_MAX)
        visH = screenH / scale
        visW = screenW / scale
        levelHeight = visH
        restY = -GROUND_FRACTION * visH
    }

    fun screenXForDelta(dx: Float): Float = screenW * 0.5f + dx * scale

    fun screenY(worldY: Float, level: Int): Float =
        screenH * 0.5f + (worldY - y + (levelF - level) * levelHeight) * scale

    fun worldXAt(sx: Float): Float = x + (sx - screenW * 0.5f) / scale

    fun worldYAt(sy: Float): Float = y + (sy - screenH * 0.5f) / scale

    /** True when content on [level] can possibly be on screen. */
    fun levelVisible(level: Int): Boolean = kotlin.math.abs(levelF - level) < 1.35f

    companion object {
        // Zoomed out ~30% from the original 900/1900 so more of each world fits
        // on screen at once.
        const val DESIGN_H = 1290f
        const val DESIGN_W_MAX = 2720f

        /** Ground line sits this fraction of the visible height below centre. */
        const val GROUND_FRACTION = 0.25f
    }
}
