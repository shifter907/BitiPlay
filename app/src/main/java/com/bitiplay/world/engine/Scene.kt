package com.bitiplay.world.engine

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Shader
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.core.Cam
import com.bitiplay.world.core.clamp
import com.bitiplay.world.core.wrapDelta
import com.bitiplay.world.core.wrapPos
import com.bitiplay.world.ent.Character
import com.bitiplay.world.fx.Particles

/**
 * One environment.
 *
 * Scenes are horizontal strips that loop: the right edge is the left edge, so
 * you can keep scrolling forever in either direction. [wraps] is false for the
 * cruise ship, which has bow and stern instead.
 *
 * Widths are deliberately multiples of 600 so repeating ground detail lines up
 * across the wrap seam (see [Render.tiles]).
 */
abstract class Scene(val id: String, val title: String) {

    open val width: Float = 5400f
    open val wraps: Boolean = true

    /** Number of stacked decks. 1 for everything except the cruise ship. */
    open val levelCount: Int = 1

    /** Names shown on the deck switcher. */
    open val levelNames: Array<String> = arrayOf("")

    open val skyTop: Int = C.SKY_TOP
    open val skyBottom: Int = C.SKY_LOW

    val entities = ArrayList<Entity>()
    val characters = ArrayList<Character>()
    val fx = Particles()

    var time = 0f
    var built = false

    /** Set once the first playable character has been placed in this scene. */
    var seeded = false

    private val toAdd = ArrayList<Entity>()
    private class Timer(var left: Float, val fn: () -> Unit)

    private val timers = ArrayList<Timer>()
    private var skyShader: LinearGradient? = null
    private var skyShaderH = -1f
    private var skyShaderTop = 0
    private var skyShaderBottom = 0

    // ------------------------------------------------------------- lifecycle

    fun <T : Entity> add(e: T): T {
        toAdd.add(e)
        return e
    }

    fun remove(e: Entity) {
        e.dead = true
    }

    /** Populate the scene. Called once, lazily. */
    protected abstract fun build()

    /** Where each character stands when the scene is first entered. */
    open fun spawnX(index: Int): Float = wrapX(width * 0.5f + (index - 2) * 190f)

    /** Which deck characters start on. */
    open fun spawnLevel(index: Int): Int = 0

    fun ensureBuilt() {
        if (built) return
        built = true
        build()
        flush()
    }

    fun flush() {
        if (toAdd.isEmpty()) return
        for (e in toAdd) {
            entities.add(e)
            e.onAdded(this)
            // Only playable characters go in the roster; NPCs are just scenery
            // that happens to walk around.
            if (e is Character && e.playable) characters.add(e)
        }
        toAdd.clear()
    }

    /** Runs [fn] after [delay] seconds - used to land an effect on its animation beat. */
    fun after(delay: Float, fn: () -> Unit) {
        timers.add(Timer(delay, fn))
    }

    fun update(dt: Float) {
        time += dt
        if (timers.isNotEmpty()) {
            var i = 0
            while (i < timers.size) {
                val tm = timers[i]
                tm.left -= dt
                if (tm.left <= 0f) {
                    timers.removeAt(i)
                    tm.fn()
                } else {
                    i++
                }
            }
        }
        for (i in entities.indices) {
            val e = entities[i]
            e.t += dt
            e.update(dt, this)
        }
        fx.update(dt)
        var w = 0
        for (i in entities.indices) {
            val e = entities[i]
            if (!e.dead) {
                entities[w] = e
                w++
            } else if (e is Character) {
                characters.remove(e)
            }
        }
        while (entities.size > w) entities.removeAt(entities.size - 1)
        flush()
    }

    // ------------------------------------------------------- world geometry

    /** Shortest signed distance from [fromX] to [toX], honouring the wrap seam. */
    fun delta(fromX: Float, toX: Float): Float =
        if (wraps) wrapDelta(toX - fromX, width) else toX - fromX

    fun wrapX(v: Float): Float = if (wraps) wrapPos(v, width) else clamp(v, 40f, width - 40f)

    /** Ground line for a deck. Always 0 in local space; decks are offset by the camera. */
    open fun groundY(level: Int): Float = 0f

    // ------------------------------------------------------------- rendering

    open fun drawSky(c: Canvas, cam: Cam) {
        val top = skyTop
        val bottom = skyBottom
        if (skyShader == null || skyShaderH != cam.screenH ||
            skyShaderTop != top || skyShaderBottom != bottom
        ) {
            skyShaderH = cam.screenH
            skyShaderTop = top
            skyShaderBottom = bottom
            skyShader = LinearGradient(
                0f, 0f, 0f, cam.screenH, top, bottom, Shader.TileMode.CLAMP
            )
        }
        D.rectShader(c, 0f, 0f, cam.screenW, cam.screenH, skyShader!!)
    }

    /**
     * Draws ground and any repeating backdrop for one deck.
     * Canvas is already in camera-relative world space: x is the offset from the
     * camera centre in world units, y is world y with 0 at the ground line.
     */
    abstract fun drawTerrain(c: Canvas, cam: Cam, level: Int)

    /**
     * Drawn after the entities, in the same camera-relative world space as
     * [drawTerrain]. Use for things that must sit in front of the characters -
     * deck railings, foreground grass, shop counters.
     */
    open fun drawFore(c: Canvas, cam: Cam, level: Int) {}

    /** Drawn on top of everything in the scene, still in screen space. */
    open fun drawOverlay(c: Canvas, cam: Cam) {}

    /**
     * Drive any positional looping audio for this scene, e.g. fairground music
     * that swells as you approach the ride. Called once per frame while this
     * scene is current; loops not refreshed here fade out automatically.
     */
    open fun ambient(cam: Cam) {}

    /** Colour used for this scene's card in the world picker. */
    open val accent: Int = C.GREEN

    /** Miniature drawn on the world-picker card, filling a [w] x [h] box at the origin. */
    open fun drawThumb(c: Canvas, w: Float, h: Float) {
        D.rect(c, 0f, 0f, w, h, skyTop)
        D.rect(c, 0f, h * 0.68f, w, h * 0.32f, accent)
    }
}
