package com.bitiplay.world.engine

import android.graphics.Canvas
import com.bitiplay.world.core.Cam
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

object Render {

    private val drawList = ArrayList<Entity>(200)

    private val order = Comparator<Entity> { a, b ->
        if (a.z != b.z) {
            a.z - b.z
        } else {
            // Lower on screen means nearer the viewer, so it draws last.
            val d = a.y - b.y
            if (d < 0f) -1 else if (d > 0f) 1 else 0
        }
    }

    fun draw(c: Canvas, scene: Scene, cam: Cam) {
        scene.drawSky(c, cam)

        val lo = maxOf(0, floor(cam.levelF - 1.05f).toInt())
        val hi = minOf(scene.levelCount - 1, ceil(cam.levelF + 1.05f).toInt())

        for (lv in lo..hi) {
            c.save()
            applyLevel(c, cam, lv)
            scene.drawTerrain(c, cam, lv)
            c.restore()
        }

        val halfW = cam.visW * 0.5f
        drawList.clear()
        for (i in scene.entities.indices) {
            val e = scene.entities[i]
            if (e.dead || !e.visible) continue
            if (e.level < lo || e.level > hi) continue
            val dx = scene.delta(cam.x * e.parallax, e.x)
            if (abs(dx) > halfW + e.cullPad) continue
            e.screenDx = dx
            drawList.add(e)
        }
        drawList.sortWith(order)

        for (i in drawList.indices) {
            val e = drawList[i]
            c.save()
            c.translate(cam.screenW * 0.5f + e.screenDx * cam.scale, cam.screenY(e.y, e.level))
            val s = cam.scale * e.drawScale
            c.scale(if (e.facing < 0) -s else s, s)
            if (e.drawAlpha < 0.99f) {
                val a = (e.drawAlpha * 255f).toInt().coerceIn(0, 255)
                c.saveLayerAlpha(-360f, -520f, 360f, 160f, a)
                e.draw(c)
                c.restore()
            } else {
                e.draw(c)
            }
            c.restore()
        }

        scene.fx.forEachActive { p ->
            if (p.level in lo..hi) {
                val dx = scene.delta(cam.x, p.x)
                if (abs(dx) <= halfW + 140f) {
                    c.save()
                    c.translate(cam.screenW * 0.5f + dx * cam.scale, cam.screenY(p.y, p.level))
                    c.scale(cam.scale, cam.scale)
                    p.draw(c)
                    c.restore()
                }
            }
        }

        for (lv in lo..hi) {
            c.save()
            applyLevel(c, cam, lv)
            scene.drawFore(c, cam, lv)
            c.restore()
        }

        scene.drawOverlay(c, cam)
    }

    /** Puts the canvas into camera-relative world space for one deck. */
    fun applyLevel(c: Canvas, cam: Cam, level: Int) {
        c.translate(cam.screenW * 0.5f, cam.screenY(0f, level))
        c.scale(cam.scale, cam.scale)
    }

    /**
     * Topmost tappable entity under a screen point, or null.
     * Boxes are grown by [pad] world units so small props stay easy to hit.
     */
    fun pick(scene: Scene, cam: Cam, sx: Float, sy: Float, pad: Float = 26f): Entity? {
        var best: Entity? = null
        var bestScore = Float.MAX_VALUE
        val level = Math.round(cam.levelF)
        for (i in scene.entities.indices) {
            val e = scene.entities[i]
            if (e.dead || !e.visible || !e.tappable) continue
            if (e.level != level) continue
            val dx = scene.delta(cam.x * e.parallax, e.x)
            val ex = cam.screenW * 0.5f + dx * cam.scale
            val ey = cam.screenY(e.y, e.level)
            val lx = (sx - ex) / cam.scale
            val ly = (sy - ey) / cam.scale
            val hw = e.hitW * 0.5f + pad
            val top = e.hitCY - e.hitH * 0.5f - pad
            val bot = e.hitCY + e.hitH * 0.5f + pad
            if (lx < -hw || lx > hw || ly < top || ly > bot) continue
            // Score by distance to the hit box centre so small, specific targets
            // beat the large scenery (and the character) they sit in front of.
            val cy = ly - e.hitCY
            val score = kotlin.math.sqrt(lx * lx + cy * cy) + e.pickBias
            if (score < bestScore) {
                bestScore = score
                best = e
            }
        }
        return best
    }

    /**
     * Walks the repeating world positions currently on screen for a parallax
     * layer. [factor] 1 is the play plane, smaller values sit further away.
     *
     * The camera x is never wrapped (the game keeps it modulo four scene
     * widths), so distant layers never jump when you cross the seam. Keep
     * [spacing] a divisor of `width / 4` and [factor] in {0.25, 0.5, 0.75, 1}.
     */
    inline fun layer(
        cam: Cam,
        spacing: Float,
        factor: Float,
        phase: Float = 0f,
        action: (px: Float, worldX: Float, index: Int) -> Unit
    ) {
        val camL = cam.x * factor
        val half = cam.visW * 0.5f + spacing
        val startIdx = floor((camL - half - phase) / spacing).toInt()
        val endIdx = ceil((camL + half - phase) / spacing).toInt()
        var i = startIdx
        while (i <= endIdx) {
            val wx = i * spacing + phase
            action(wx - camL, wx, i)
            i++
        }
    }

    /** Repeating detail on the play plane. */
    inline fun tiles(
        cam: Cam,
        spacing: Float,
        phase: Float = 0f,
        action: (dx: Float, worldX: Float, index: Int) -> Unit
    ) = layer(cam, spacing, 1f, phase, action)
}
