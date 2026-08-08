package com.bitiplay.world.ui

import android.graphics.Canvas
import android.graphics.Paint
import com.bitiplay.world.Game
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.shade
import com.bitiplay.world.art.withAlpha
import com.bitiplay.world.art.withAlphaF
import com.bitiplay.world.core.TAU
import com.bitiplay.world.core.clamp
import com.bitiplay.world.core.clamp01
import com.bitiplay.world.core.clampInt
import com.bitiplay.world.core.lerp
import com.bitiplay.world.ent.Character
import com.bitiplay.world.ent.Roster
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class Zone { NONE, MAP, MUTE, ACTION, EXIT, WHEEL, WHEEL_PICK, DECK, CARD, SCRIM }

class HudHit(val zone: Zone, val index: Int = 0)

/** Everything drawn in screen space: the switcher wheel, buttons, world picker. */
class Hud {

    private var w = 0f
    private var h = 0f
    private var u = 10f

    // buttons
    private var btnR = 0f
    private var mapCX = 0f
    private var mapCY = 0f
    private var muteCX = 0f
    private var muteCY = 0f
    private var exitCX = 0f
    private var exitCY = 0f
    private var actCX = 0f
    private var actCY = 0f
    private var actR = 0f

    // switcher wheel
    private var wheelCX = 0f
    private var wheelCY = 0f
    private var wheelR = 0f
    private var faceR = 0f
    private var faceMagR = 0f

    /** Continuous wheel position; the entry nearest a whole number is magnified. */
    var wheelPos = 0f
        private set
    private var wheelTarget = 0f
    private var wheelDragging = false

    // decks
    private var deckX = 0f
    private var deckY0 = 0f
    private var deckStep = 0f
    private var deckR = 0f

    // picker grid
    private var cardW = 0f
    private var cardH = 0f
    private var gridX = 0f
    private var gridY = 0f
    private var gapX = 0f
    private var gapY = 0f
    private var cols = 4

    fun layout(width: Float, height: Float) {
        if (w == width && h == height) return
        w = width
        h = height
        u = h / 100f

        btnR = 6.4f * u
        exitCX = w - 9f * u
        exitCY = 9f * u
        mapCX = 9f * u
        mapCY = h - 9f * u
        muteCX = 23f * u
        muteCY = h - 9f * u

        actR = 9.6f * u
        actCX = w - 12f * u
        actCY = h - 12f * u

        // Dial scaled down 30%; faces scale with it so the spacing stays right.
        wheelCX = 1.5f * u
        wheelCY = 27f * u
        wheelR = 13.3f * u
        faceR = 3.8f * u
        faceMagR = 6.2f * u

        deckR = 5.2f * u
        deckX = w - 8.5f * u
        deckStep = 12f * u
        deckY0 = h * 0.46f

        cols = 4
        gapX = 3f * u
        gapY = 3.5f * u
        val marginX = 8f * u
        val topY = 20f * u
        cardW = (w - marginX * 2f - gapX * (cols - 1)) / cols
        cardH = min(cardW * 0.78f, (h - topY - 8f * u - gapY) * 0.5f)
        gridX = marginX
        gridY = topY
    }

    // ----------------------------------------------------------------- wheel

    fun magnified(count: Int): Int =
        if (count <= 0) -1 else clampInt(Math.round(wheelPos), 0, count - 1)

    fun rotateBy(dyPx: Float, count: Int) {
        if (count <= 1) return
        wheelDragging = true
        wheelPos = clamp(wheelPos - dyPx / (7.5f * u), 0f, (count - 1).toFloat())
        wheelTarget = wheelPos
    }

    fun snap(count: Int) {
        wheelDragging = false
        if (count <= 0) return
        wheelTarget = clamp(Math.round(wheelPos).toFloat(), 0f, (count - 1).toFloat())
    }

    /** Keeps the wheel in range as characters come and go, and eases it to rest. */
    fun clampWheel(count: Int) {
        if (count <= 0) {
            wheelPos = 0f
            wheelTarget = 0f
            return
        }
        val maxI = (count - 1).toFloat()
        if (wheelTarget > maxI) wheelTarget = maxI
        if (wheelPos > maxI) wheelPos = maxI
        if (!wheelDragging) wheelPos += (wheelTarget - wheelPos) * 0.25f
    }

    private fun facePos(k: Int, out: FloatArray) {
        val a = (k - wheelPos) * STEP
        out[0] = wheelCX + cos(a) * wheelR
        out[1] = wheelCY + sin(a) * wheelR
        out[2] = a
    }

    private val scratch = FloatArray(3)

    // --------------------------------------------------------------- hit test

    fun hitTest(game: Game, x: Float, y: Float): HudHit {
        if (game.pickerOpen) {
            val n = game.scenes.size
            for (i in 0 until n) {
                val col = i % cols
                val row = i / cols
                val cx = gridX + col * (cardW + gapX)
                val cy = gridY + row * (cardH + gapY)
                if (x >= cx && x <= cx + cardW && y >= cy && y <= cy + cardH) {
                    return HudHit(Zone.CARD, i)
                }
            }
            return HudHit(Zone.SCRIM)
        }

        if (game.wheelCount > 0) {
            val mi = magnified(game.wheelCount)
            if (mi >= 0) {
                facePos(mi, scratch)
                if (dist2(x, y, scratch[0], scratch[1]) < sq(faceMagR * 1.2f)) {
                    return HudHit(Zone.WHEEL_PICK, mi)
                }
            }
            // Anywhere else on the dial ring rotates it.
            val d2 = dist2(x, y, wheelCX, wheelCY)
            if (d2 > sq(wheelR - faceR * 1.5f) && d2 < sq(wheelR + faceR * 1.6f)) {
                return HudHit(Zone.WHEEL)
            }
        }

        if (game.active() != null && dist2(x, y, exitCX, exitCY) < sq(btnR * 1.25f)) {
            return HudHit(Zone.EXIT)
        }
        if (dist2(x, y, mapCX, mapCY) < sq(btnR * 1.25f)) return HudHit(Zone.MAP)
        if (dist2(x, y, muteCX, muteCY) < sq(btnR * 1.25f)) return HudHit(Zone.MUTE)
        if (dist2(x, y, actCX, actCY) < sq(actR * 1.15f)) return HudHit(Zone.ACTION)

        val levels = game.scene.levelCount
        if (levels > 1) {
            for (i in 0 until levels) {
                val cy = deckY0 - i * deckStep
                if (dist2(x, y, deckX, cy) < sq(deckR * 1.25f)) return HudHit(Zone.DECK, i)
            }
        }
        return HudHit(Zone.NONE)
    }

    /** True while a point is still over the dial, i.e. not yet dropped in the world. */
    fun overWheelPlate(x: Float, y: Float): Boolean =
        dist2(x, y, wheelCX, wheelCY) < sq(wheelR + faceMagR * 1.15f)

    private fun sq(v: Float) = v * v
    private fun dist2(x: Float, y: Float, cx: Float, cy: Float): Float {
        val dx = x - cx
        val dy = y - cy
        return dx * dx + dy * dy
    }

    // ------------------------------------------------------------------ draw

    fun draw(c: Canvas, game: Game) {
        drawWheel(c, game)
        drawTitleChip(c, game)

        if (game.active() != null) {
            drawRoundButton(c, exitCX, exitCY, btnR, C.UI_PANEL)
            drawExitGlyph(c, exitCX, exitCY, btnR * 0.66f)
        }

        drawRoundButton(c, mapCX, mapCY, btnR, C.UI_PANEL)
        drawMapGlyph(c, mapCX, mapCY, btnR * 0.62f)

        drawRoundButton(c, muteCX, muteCY, btnR, C.UI_PANEL)
        drawSpeakerGlyph(c, muteCX, muteCY, btnR * 0.58f, game.soundOn)

        drawActionButton(c, game)

        if (game.scene.levelCount > 1) drawDecks(c, game)
        if (game.toastT > 0f) drawToast(c, game)
        if (game.dragSpec >= 0) drawDragGhost(c, game)
        if (game.pickerOpen) drawPicker(c, game)
    }

    private fun drawRoundButton(c: Canvas, cx: Float, cy: Float, r: Float, col: Int) {
        D.circle(c, cx, cy + r * 0.09f, r, withAlpha(C.BLACK, 40))
        D.circle(c, cx, cy, r, col)
    }

    private fun drawWheel(c: Canvas, game: Game) {
        val n = game.wheelCount
        // dial plate, anchored off the left edge
        D.circle(c, wheelCX, wheelCY, wheelR + faceR * 1.5f, withAlpha(C.BLACK, 34))
        D.circle(c, wheelCX, wheelCY, wheelR + faceR * 1.36f, withAlphaF(C.UI_PANEL_DIM, 0.72f))
        D.circleStroke(c, wheelCX, wheelCY, wheelR, withAlpha(C.WHITE, 46), 1.6f * u)

        if (n == 0) {
            D.label(
                c, "everyone's out!", wheelCX + wheelR * 0.55f, wheelCY + 1.2f * u,
                3.4f * u, withAlpha(C.WHITE, 190)
            )
            return
        }

        val mi = magnified(n)
        for (k in 0 until n) {
            facePos(k, scratch)
            val a = scratch[2]
            if (abs(a) > 1.85f) continue
            val closeness = 1f - clamp01(abs(a) / 1.85f)
            val r = lerp(faceR * 0.7f, faceMagR, closeness * closeness)
            val isMag = k == mi
            // The magnified face is the one being dragged, so hide it while it travels.
            if (isMag && game.dragSpec >= 0) continue
            drawFace(c, scratch[0], scratch[1], r, Roster.all[game.wheelSpec(k)], isMag)
        }

        if (mi >= 0 && game.dragSpec < 0) {
            facePos(mi, scratch)
            val pull = sin(game.time * 2.2f) * 0.6f * u
            D.label(
                c, "drag me in", scratch[0] + faceMagR * 0.2f,
                scratch[1] + faceMagR + 4.2f * u + pull, 3.2f * u, withAlpha(C.WHITE, 210)
            )
        }
    }

    private fun drawFace(c: Canvas, cx: Float, cy: Float, r: Float, spec: com.bitiplay.world.ent.CharSpec, big: Boolean) {
        D.circle(c, cx, cy + r * 0.1f, r, withAlpha(C.BLACK, 50))
        D.circle(c, cx, cy, r, if (big) C.UI_ACCENT else C.UI_PANEL)
        D.circle(c, cx, cy, r * 0.86f, withAlpha(C.WHITE, 240))
        c.save()
        c.clipRect(cx - r * 0.86f, cy - r * 0.86f, cx + r * 0.86f, cy + r * 0.86f)
        c.translate(cx, cy + r * 0.14f)
        Character.portrait(c, spec, r * 0.62f)
        c.restore()
        if (big) D.circleStroke(c, cx, cy, r, C.UI_ACCENT, r * 0.13f)
    }

    private fun drawDragGhost(c: Canvas, game: Game) {
        val spec = Roster.all[clampInt(game.dragSpec, 0, Roster.all.size - 1)]
        val r = faceMagR * 1.06f
        D.circle(c, game.dragX, game.dragY + r * 0.12f, r, withAlpha(C.BLACK, 60))
        D.circle(c, game.dragX, game.dragY, r, C.UI_ACCENT)
        D.circle(c, game.dragX, game.dragY, r * 0.87f, withAlpha(C.WHITE, 245))
        c.save()
        c.clipRect(game.dragX - r * 0.87f, game.dragY - r * 0.87f, game.dragX + r * 0.87f, game.dragY + r * 0.87f)
        c.translate(game.dragX, game.dragY + r * 0.14f)
        Character.portrait(c, spec, r * 0.62f)
        c.restore()
        // drop hint
        if (game.dragY > h * 0.26f) {
            D.circleStroke(c, game.dragX, game.dragY, r * 1.25f, withAlpha(C.WHITE, 150), 0.7f * u)
        }
    }

    private fun drawTitleChip(c: Canvas, game: Game) {
        val title = game.scene.title
        val ts = 4.6f * u
        val tw = D.measure(title, ts)
        val padX = 3.4f * u
        val hh = btnR * 1.62f
        val x0 = w * 0.5f - (tw + padX * 2f) * 0.5f
        val y0 = 3.2f * u
        D.rect(c, x0, y0 + hh * 0.06f, tw + padX * 2f, hh, withAlpha(C.BLACK, 36), hh * 0.5f)
        D.rect(c, x0, y0, tw + padX * 2f, hh, C.UI_PANEL, hh * 0.5f)
        D.label(c, title, w * 0.5f, y0 + hh * 0.5f + ts * 0.36f, ts, C.UI_TEXT)
    }

    private fun drawExitGlyph(c: Canvas, cx: Float, cy: Float, r: Float) {
        // a door with an arrow heading out of it
        D.rect(c, cx - r, cy - r, r * 1.15f, r * 2f, C.TEAL, r * 0.18f)
        D.rect(c, cx - r * 0.9f, cy - r * 0.86f, r * 0.95f, r * 1.72f, shade(C.TEAL, 1.2f), r * 0.14f)
        D.circle(c, cx - r * 0.1f, cy + r * 0.1f, r * 0.13f, C.WHITE)
        D.capsule(c, cx + r * 0.25f, cy, cx + r * 0.95f, cy, r * 0.22f, C.UI_TEXT)
        D.tri(
            c, cx + r * 1.15f, cy, cx + r * 0.6f, cy - r * 0.42f,
            cx + r * 0.6f, cy + r * 0.42f, C.UI_TEXT
        )
    }

    private fun drawMapGlyph(c: Canvas, cx: Float, cy: Float, r: Float) {
        D.circle(c, cx, cy, r, C.TEAL)
        D.shape(c, C.GREEN_DARK) { p ->
            p.moveTo(cx - r, cy - r * 0.2f)
            p.quadTo(cx - r * 0.3f, cy - r * 0.85f, cx + r * 0.25f, cy - r * 0.2f)
            p.quadTo(cx + r * 0.7f, cy + r * 0.3f, cx + r, cy + r * 0.1f)
            p.lineTo(cx + r, cy + r); p.lineTo(cx - r, cy + r)
        }
        D.circleStroke(c, cx, cy, r, C.WHITE, r * 0.16f)
    }

    private fun drawSpeakerGlyph(c: Canvas, cx: Float, cy: Float, r: Float, on: Boolean) {
        D.shape(c, C.UI_TEXT) { p ->
            p.moveTo(cx - r * 0.9f, cy - r * 0.35f)
            p.lineTo(cx - r * 0.35f, cy - r * 0.35f)
            p.lineTo(cx + r * 0.15f, cy - r * 0.95f)
            p.lineTo(cx + r * 0.15f, cy + r * 0.95f)
            p.lineTo(cx - r * 0.35f, cy + r * 0.35f)
            p.lineTo(cx - r * 0.9f, cy + r * 0.35f)
        }
        if (on) {
            D.arcLine(c, cx + r * 0.1f, cy, r * 0.7f, r * 0.7f, -55f, 110f, C.UI_TEXT, r * 0.2f)
            D.arcLine(c, cx + r * 0.1f, cy, r * 1.1f, r * 1.1f, -50f, 100f, C.UI_TEXT, r * 0.18f)
        } else {
            D.line(c, cx + r * 0.4f, cy - r * 0.5f, cx + r * 1.2f, cy + r * 0.5f, C.RED, r * 0.24f)
            D.line(c, cx + r * 1.2f, cy - r * 0.5f, cx + r * 0.4f, cy + r * 0.5f, C.RED, r * 0.24f)
        }
    }

    private fun drawActionButton(c: Canvas, game: Game) {
        if (game.active() == null) return
        val kind = game.actionKind()
        val col = when (kind) {
            Game.ACT_THROW -> C.ORANGE
            Game.ACT_OFF -> C.RED
            Game.ACT_RELEASE -> C.PURPLE
            else -> C.TEAL
        }
        val pulse = 1f + sin(game.time * 2.4f) * 0.02f
        val r = actR * pulse
        D.circle(c, actCX, actCY + r * 0.1f, r, withAlpha(C.BLACK, 55))
        D.circle(c, actCX, actCY, r, col)
        D.circle(c, actCX, actCY - r * 0.14f, r * 0.86f, shade(col, 1.16f))

        val g = r * 0.5f
        when (kind) {
            Game.ACT_THROW -> {
                D.arcLine(c, actCX - g * 0.2f, actCY + g * 0.2f, g * 1.1f, g * 0.9f, 200f, 120f, C.WHITE, g * 0.22f)
                D.circle(c, actCX + g * 0.75f, actCY - g * 0.55f, g * 0.42f, C.WHITE)
            }
            Game.ACT_OFF -> {
                D.capsule(c, actCX, actCY - g * 0.9f, actCX, actCY + g * 0.5f, g * 0.3f, C.WHITE)
                D.tri(c, actCX - g * 0.7f, actCY + g * 0.35f, actCX + g * 0.7f, actCY + g * 0.35f,
                    actCX, actCY + g * 1.1f, C.WHITE)
            }
            Game.ACT_RELEASE -> {
                D.circle(c, actCX, actCY + g * 0.25f, g * 0.6f, C.WHITE)
                for (i in 0 until 4) {
                    val a = -TAU * 0.42f + i * 0.30f
                    D.capsule(c, actCX + cos(a) * g * 0.3f, actCY + sin(a) * g * 0.3f,
                        actCX + cos(a) * g * 1.05f, actCY + sin(a) * g * 1.05f, g * 0.22f, C.WHITE)
                }
            }
            else -> D.star(c, actCX, actCY, g * 1.1f, g * 0.48f, 5, C.WHITE)
        }
    }

    private fun drawDecks(c: Canvas, game: Game) {
        val n = game.scene.levelCount
        val names = game.scene.levelNames
        val cur = game.active()?.level ?: 0
        D.rect(
            c, deckX - deckR * 1.5f, deckY0 - (n - 1) * deckStep - deckR * 1.6f,
            deckR * 3f, (n - 1) * deckStep + deckR * 3.2f, withAlpha(C.BLACK, 45), deckR * 1.5f
        )
        for (i in 0 until n) {
            val cy = deckY0 - i * deckStep
            val on = i == cur
            D.circle(c, deckX, cy, deckR, if (on) C.UI_ACCENT else C.UI_PANEL)
            val nm = names.getOrElse(i) { "${i + 1}" }
            D.label(c, nm, deckX, cy + deckR * 0.34f, deckR * 0.9f, if (on) C.WHITE else C.UI_TEXT)
        }
    }

    private fun drawToast(c: Canvas, game: Game) {
        val a = clamp01(game.toastT / 0.4f)
        val ts = 4.4f * u
        val tw = D.measure(game.toastText, ts)
        val padX = 3.2f * u
        val hh = 8.2f * u
        val x0 = w * 0.5f - (tw + padX * 2f) * 0.5f
        val y0 = h - 12f * u
        D.rect(c, x0, y0, tw + padX * 2f, hh, withAlphaF(C.UI_PANEL_DIM, a * 0.86f), hh * 0.5f)
        D.label(c, game.toastText, w * 0.5f, y0 + hh * 0.5f + ts * 0.36f, ts, withAlphaF(C.WHITE, a))
    }

    // ---------------------------------------------------------------- picker

    private fun drawPicker(c: Canvas, game: Game) {
        val a = clamp01(game.pickerT / 0.22f)
        D.rect(c, 0f, 0f, w, h, withAlphaF(C.UI_SCRIM, a))
        D.label(c, "Where shall we go?", w * 0.5f, gridY - 5.5f * u, 6.2f * u, withAlphaF(C.WHITE, a))

        for (i in game.scenes.indices) {
            val col = i % cols
            val row = i / cols
            val cx = gridX + col * (cardW + gapX)
            val cy = gridY + row * (cardH + gapY)
            val sc = game.scenes[i]
            val here = i == game.sceneIndex
            val r = 2.6f * u

            D.rect(c, cx, cy + 0.8f * u, cardW, cardH, withAlphaF(C.BLACK, a * 0.3f), r)
            D.rect(c, cx, cy, cardW, cardH, withAlphaF(C.WHITE, a), r)

            val inset = 0.9f * u
            val thumbH = cardH - 7.4f * u
            c.save()
            c.clipRect(cx + inset, cy + inset, cx + cardW - inset, cy + inset + thumbH)
            c.translate(cx + inset, cy + inset)
            sc.drawThumb(c, cardW - inset * 2f, thumbH)
            c.restore()

            D.label(
                c, sc.title, cx + cardW * 0.5f, cy + cardH - 2.3f * u,
                4.2f * u, withAlphaF(C.UI_TEXT, a)
            )
            if (here) D.rectStroke(c, cx, cy, cardW, cardH, withAlphaF(C.UI_ACCENT, a), 0.8f * u, r)
        }

        D.label(
            c, "tap anywhere else to close", w * 0.5f, h - 3.4f * u,
            3.6f * u, withAlphaF(C.WHITE, a * 0.8f), Paint.Align.CENTER
        )
    }

    companion object {
        /** Angular spacing between wheel entries, in radians. */
        const val STEP = 0.62f
    }
}
