package com.bitiplay.world.ent

import android.graphics.Canvas
import com.bitiplay.world.engine.Entity
import com.bitiplay.world.engine.Scene
import com.bitiplay.world.engine.Z

/**
 * Scenery with bespoke artwork supplied by the scene that builds it.
 *
 * Props carry a little generic state ([state], [f0], [f1]) so a scene can give
 * one a behaviour - a door that opens, a ride that spins, a slide you can go
 * down - without needing a dedicated class for every one.
 */
class Prop(
    px: Float,
    py: Float = 0f,
    layer: Int = Z.MID,
    par: Float = 1f,
    val seed: Int = 0,
    val drawer: (Canvas, Prop) -> Unit
) : Entity() {

    var state = 0
    var f0 = 0f
    var f1 = 0f

    var captionText: String? = null
    var onUseFn: ((Character, Scene, Prop) -> Boolean)? = null
    var updateFn: ((Float, Scene, Prop) -> Unit)? = null

    init {
        x = px
        y = py
        z = layer
        parallax = par
        tappable = false
        hitW = 200f
        hitH = 240f
        hitCY = -120f
        cullPad = 500f
    }

    /** Makes this prop tappable with the given behaviour. */
    fun interactive(caption: String?, fn: (Character, Scene, Prop) -> Boolean): Prop {
        captionText = caption
        onUseFn = fn
        tappable = true
        return this
    }

    fun sized(w: Float, h: Float, cy: Float = -h * 0.5f): Prop {
        hitW = w
        hitH = h
        hitCY = cy
        cullPad = w * 0.75f + 260f
        return this
    }

    fun onUpdate(fn: (Float, Scene, Prop) -> Unit): Prop {
        updateFn = fn
        return this
    }

    override fun update(dt: Float, scene: Scene) {
        updateFn?.invoke(dt, scene, this)
    }

    override fun draw(c: Canvas) = drawer(c, this)

    override fun caption(ch: Character): String? = captionText

    override fun onUse(ch: Character, scene: Scene): Boolean =
        onUseFn?.invoke(ch, scene, this) ?: false
}
