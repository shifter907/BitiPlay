package com.bitiplay.world

import android.content.Context
import android.graphics.Canvas
import com.bitiplay.world.art.C
import com.bitiplay.world.art.D
import com.bitiplay.world.art.withAlphaF
import com.bitiplay.world.audio.Loops
import com.bitiplay.world.audio.Sfx
import com.bitiplay.world.audio.Snd
import com.bitiplay.world.core.Cam
import com.bitiplay.world.core.clamp
import com.bitiplay.world.core.clampInt
import com.bitiplay.world.core.damp
import com.bitiplay.world.core.rndInt
import com.bitiplay.world.core.wrapPos
import com.bitiplay.world.engine.Render
import com.bitiplay.world.engine.Scene
import com.bitiplay.world.ent.Character
import com.bitiplay.world.ent.Roster
import com.bitiplay.world.scenes.Amusement
import com.bitiplay.world.scenes.Beach
import com.bitiplay.world.scenes.Cruise
import com.bitiplay.world.scenes.Downtown
import com.bitiplay.world.scenes.Farm
import com.bitiplay.world.scenes.Grocery
import com.bitiplay.world.scenes.Neighborhood
import com.bitiplay.world.scenes.Zoo
import com.bitiplay.world.ui.Hud
import com.bitiplay.world.ui.HudHit
import com.bitiplay.world.ui.Zone
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/** Owns the world, the camera, input routing and the HUD. */
class Game(private val ctx: Context) {

    val cam = Cam()
    val hud = Hud()

    val scenes: List<Scene> = listOf(
        Neighborhood(), Beach(), Farm(), Amusement(),
        Cruise(), Downtown(), Zoo(), Grocery(),
    )

    var sceneIndex = 0
        private set

    val scene: Scene get() = scenes[sceneIndex]

    /** Index into [Roster.all] of the character currently under control. */
    var activeSpec = 0
        private set

    var time = 0f
        private set

    var soundOn = true
        private set

    var pickerOpen = false
        private set
    var pickerT = 0f
        private set

    var toastText = ""
        private set
    var toastT = 0f
        private set

    // ---- switcher wheel ----------------------------------------------------

    /** Spec indices not currently standing in the scene, in roster order. */
    private val wheelBuf = IntArray(Roster.all.size)

    var wheelCount = 0
        private set

    fun wheelSpec(k: Int): Int = wheelBuf[clampInt(k, 0, wheelBuf.size - 1)]

    /** Spec being dragged out of the wheel, or -1. */
    var dragSpec = -1
        private set
    var dragX = 0f
        private set
    var dragY = 0f
        private set
    private var dragMoved = false

    private var camFollow = true
    private var flingVX = 0f
    private var fade = 0f
    private var fadeDir = 0
    private var pendingScene = -1
    private var downHit: HudHit? = null
    private var started = false
    private var lastMoveY = 0f

    fun active(): Character? {
        val list = scene.characters
        for (i in list.indices) {
            val ch = list[i]
            if (ch.slot == activeSpec && !ch.leaving) return ch
        }
        return null
    }

    // ------------------------------------------------------------- lifecycle

    fun start() {
        if (started) return
        started = true
        loadState()
        Sfx.enabled = soundOn
        com.bitiplay.world.audio.Music.enabled = soundOn
        prepare(scene)
        val a = active()
        if (a != null) {
            cam.x = a.x
            cam.levelF = a.level.toFloat()
        }
        refreshWheel()
        toast(scene.title)
    }

    fun resize(width: Float, height: Float) {
        cam.resize(width, height)
        cam.y = cam.restY
        hud.layout(width, height)
        start()
        normalizeCam()
    }

    /**
     * Each scene starts with exactly one randomly chosen friend already there;
     * the rest wait in the switcher wheel until dragged in.
     */
    private fun prepare(s: Scene) {
        s.ensureBuilt()
        if (!s.seeded) {
            s.seeded = true
            val i = rndInt(0, Roster.all.size)
            val ch = Character(Roster.all[i], i)
            ch.x = s.wrapX(s.spawnX(i))
            ch.level = s.spawnLevel(i)
            ch.depth = 110f
            ch.y = ch.depth
            s.add(ch)
            s.flush()
            activeSpec = i
        } else if (active() == null) {
            val next = firstPresent(s)
            if (next != null) activeSpec = next.slot
        }
    }

    private fun firstPresent(s: Scene): Character? {
        for (i in s.characters.indices) {
            val ch = s.characters[i]
            if (!ch.leaving) return ch
        }
        return null
    }

    private fun refreshWheel() {
        var n = 0
        for (i in Roster.all.indices) {
            // Someone still walking out counts as present, so their face only
            // reappears on the wheel once they have actually gone.
            var present = false
            val list = scene.characters
            for (k in list.indices) {
                if (list[k].slot == i) {
                    present = true
                    break
                }
            }
            if (!present) {
                wheelBuf[n] = i
                n++
            }
        }
        wheelCount = n
    }

    private fun spawnCharacter(specIndex: Int, wx: Float, depth: Float) {
        val ch = Character(Roster.all[specIndex], specIndex)
        ch.x = scene.wrapX(wx)
        ch.depth = clamp(depth, 0f, Character.DEPTH_BAND)
        ch.y = ch.depth
        ch.level = clampInt(Math.round(cam.levelF), 0, scene.levelCount - 1)
        scene.add(ch)
        scene.flush()
        activeSpec = specIndex
        camFollow = true
        refreshWheel()
        scene.fx.sparkles(ch.x, -110f, ch.level, 12, C.WHITE)
        Sfx.play(Snd.CHIME)
        toast(ch.spec.name)
    }

    // ------------------------------------------------------------------ save

    private fun loadState() {
        try {
            val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            sceneIndex = clampInt(p.getInt("scene", 0), 0, scenes.size - 1)
            soundOn = p.getBoolean("sound", true)
        } catch (_: Throwable) {
        }
    }

    fun saveState() {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("scene", sceneIndex)
                .putBoolean("sound", soundOn)
                .apply()
        } catch (_: Throwable) {
        }
    }

    // ----------------------------------------------------------------- input

    fun onDown(x: Float, y: Float) {
        downHit = hud.hitTest(this, x, y)
        lastMoveY = y
        flingVX = 0f
        dragMoved = false
        if (downHit?.zone == Zone.WHEEL_PICK && wheelCount > 0) {
            dragSpec = wheelSpec(hud.magnified(wheelCount))
            dragX = x
            dragY = y
        }
    }

    /** Absolute pointer updates, needed by the wheel and the drag-out. */
    fun onMove(x: Float, y: Float) {
        val dy = y - lastMoveY
        lastMoveY = y
        if (dragSpec >= 0) {
            if (abs(x - dragX) > 6f || abs(y - dragY) > 6f) dragMoved = true
            dragX = x
            dragY = y
            return
        }
        if (downHit?.zone == Zone.WHEEL) hud.rotateBy(dy, wheelCount)
    }

    fun onDrag(dxPx: Float) {
        if (dragSpec >= 0) return
        val h = downHit
        if (h != null && (h.zone != Zone.NONE)) return
        if (pickerOpen) return
        camFollow = false
        cam.x -= dxPx / cam.scale
        normalizeCam()
    }

    fun onUp(x: Float, y: Float, dragged: Boolean, vxPx: Float) {
        val h = downHit
        downHit = null
        if (dragSpec >= 0) {
            finishDragOut(x, y)
            return
        }
        if (h != null && h.zone == Zone.WHEEL) {
            hud.snap(wheelCount)
            return
        }
        if (h != null && h.zone != Zone.NONE) {
            if (!dragged) handleHud(h)
            return
        }
        if (dragged) {
            flingVX = -vxPx / cam.scale
            return
        }
        tapWorld(x, y)
    }

    private fun finishDragOut(sx: Float, sy: Float) {
        val spec = dragSpec
        dragSpec = -1
        if (spec < 0) return
        if (!hud.overWheelPlate(sx, sy)) {
            val wy = clamp(cam.worldYAt(sy), 0f, Character.DEPTH_BAND)
            spawnCharacter(spec, cam.worldXAt(sx), wy)
        } else if (!dragMoved) {
            // A plain tap on the magnified face drops them into the middle of view.
            spawnCharacter(spec, cam.x, 130f)
        } else {
            Sfx.play(Snd.UI)
        }
    }

    fun onBack(): Boolean {
        if (pickerOpen) {
            pickerOpen = false
            return true
        }
        return false
    }

    private fun handleHud(h: HudHit) {
        when (h.zone) {
            Zone.MAP -> {
                pickerOpen = true
                pickerT = 0f
                Sfx.play(Snd.UI)
            }
            Zone.MUTE -> {
                soundOn = !soundOn
                Sfx.enabled = soundOn
                Loops.enabled = soundOn
                com.bitiplay.world.audio.Music.enabled = soundOn
                Sfx.play(Snd.UI)
                saveState()
            }
            Zone.ACTION -> doAction()
            Zone.EXIT -> {
                val a = active()
                if (a != null) {
                    a.exitToWheel(scene)
                    toast("see you, ${a.spec.name}")
                }
            }
            Zone.WHEEL_PICK -> Unit
            Zone.WHEEL -> Unit
            Zone.DECK -> gotoDeck(h.index)
            Zone.CARD -> {
                pickerOpen = false
                goToScene(h.index)
            }
            Zone.SCRIM -> {
                pickerOpen = false
                Sfx.play(Snd.UI)
            }
            Zone.NONE -> Unit
        }
    }

    private fun tapWorld(sx: Float, sy: Float) {
        val a = active() ?: return
        val standDepth = clamp(cam.worldYAt(sy), 0f, Character.DEPTH_BAND)
        val e = Render.pick(scene, cam, sx, sy)
        if (e != null) {
            // Only playable friends switch control; NPCs get greeted instead.
            if (e is Character && e.playable) {
                if (e !== a) selectSpec(e.slot) else a.cheer(scene)
                return
            }
            camFollow = true
            if (e.onTapDirect(a, scene)) return
            toast(e.caption(a))
            val d = scene.delta(a.x, e.x)
            if (abs(d) <= e.useRange) {
                e.onUse(a, scene)
                return
            }
            val riding = a.riding
            val standoff = if (riding != null) {
                max(e.approachOffset(), riding.approachOffset())
            } else {
                max(e.approachOffset(), 0f)
            }
            val side = if (d > 0f) -1f else 1f
            val standX = scene.wrapX(e.x + side * standoff)
            val depth = clamp(e.standDepth(), 0f, Character.DEPTH_BAND)
            a.goTo(scene, standX, depth) { ch ->
                ch.faceToward(scene, e.x)
                e.onUse(ch, scene)
            }
            return
        }
        camFollow = true
        a.goTo(scene, scene.wrapX(cam.worldXAt(sx)), standDepth)
    }

    private fun selectSpec(i: Int) {
        if (i == activeSpec) return
        activeSpec = clampInt(i, 0, Roster.all.size - 1)
        camFollow = true
        flingVX = 0f
        Sfx.play(Snd.UI)
        toast(Roster.all[activeSpec].name)
    }

    private fun gotoDeck(i: Int) {
        val a = active() ?: return
        if (i == a.level) return
        a.stop()
        a.level = clampInt(i, 0, scene.levelCount - 1)
        camFollow = true
        Sfx.play(Snd.STEP)
        scene.fx.sparkles(a.x, -110f, a.level, 7, C.WHITE)
        toast(scene.levelNames.getOrElse(a.level) { "" })
    }

    fun goToScene(i: Int) {
        val idx = clampInt(i, 0, scenes.size - 1)
        if (idx == sceneIndex || fadeDir != 0) {
            Sfx.play(Snd.UI)
            return
        }
        pendingScene = idx
        fadeDir = 1
        Sfx.play(Snd.WHOOSH)
    }

    private fun switchNow() {
        if (pendingScene < 0) return
        sceneIndex = pendingScene
        pendingScene = -1
        prepare(scene)
        refreshWheel()
        val a = active()
        if (a != null) {
            cam.x = a.x
            cam.levelF = a.level.toFloat()
        }
        camFollow = true
        flingVX = 0f
        toast(scene.title)
        saveState()
    }

    private fun doAction() {
        val a = active() ?: return
        when {
            a.riding != null -> a.dismount(scene)
            a.carried != null -> a.throwCarried(scene, null)
            a.pushing != null -> {
                a.releasePush()
                Sfx.play(Snd.DROP)
            }
            else -> a.cheer(scene)
        }
    }

    fun actionKind(): Int {
        val a = active() ?: return ACT_JUMP
        return when {
            a.riding != null -> ACT_OFF
            a.carried != null -> ACT_THROW
            a.pushing != null -> ACT_RELEASE
            else -> ACT_JUMP
        }
    }

    fun toast(s: String?) {
        if (s.isNullOrEmpty()) return
        toastText = s
        toastT = 1.9f
    }

    // ---------------------------------------------------------------- update

    fun update(dt: Float) {
        time += dt
        // Cheap no-op once the scene's loop is already playing.
        com.bitiplay.world.audio.Music.play(scene.id)
        pickerT = if (pickerOpen) pickerT + dt else 0f
        if (toastT > 0f) toastT -= dt

        if (fadeDir > 0) {
            fade += dt * 3.6f
            if (fade >= 1f) {
                fade = 1f
                switchNow()
                fadeDir = -1
            }
        } else if (fadeDir < 0) {
            fade -= dt * 3.6f
            if (fade <= 0f) {
                fade = 0f
                fadeDir = 0
            }
        }

        scene.update(dt)
        refreshWheel()
        hud.clampWheel(wheelCount)

        // A character that walked off leaves control to whoever is still here.
        if (active() == null) {
            val next = firstPresent(scene)
            if (next != null) activeSpec = next.slot
        }

        if (!camFollow && abs(flingVX) > 2f) {
            cam.x += flingVX * dt
            flingVX *= exp(-2.6f * dt)
        }

        val a = active()
        if (a != null) {
            cam.levelF = damp(cam.levelF, a.level.toFloat(), 8f, dt)
            if (camFollow) {
                val d = scene.delta(cam.x, a.x)
                val dz = cam.visW * 0.11f
                val push = if (d > dz) d - dz else if (d < -dz) d + dz else 0f
                if (push != 0f) cam.x += push * (1f - exp(-5.5f * dt))
            }
        }
        cam.y = cam.restY
        normalizeCam()

        updateLoops(dt, a)
    }

    /**
     * Looping audio. Everything is re-asserted each frame, so anything that
     * stops applying (driving stops, scene changes) fades out by itself.
     */
    private fun updateLoops(dt: Float, a: Character?) {
        Loops.enabled = soundOn
        Loops.beginFrame()
        val v = a?.riding
        if (v != null && v.motorised && abs(v.vx) > 45f) {
            Loops.set("engine", "loop_car", 0.85f)
        }
        scene.ambient(cam)
        Loops.update(dt)
    }

    private fun normalizeCam() {
        if (scene.wraps) {
            // Kept modulo four scene widths so parallax layers never jump.
            cam.x = wrapPos(cam.x, scene.width * 4f)
        } else {
            cam.x = if (scene.width <= cam.visW) scene.width * 0.5f
            else clamp(cam.x, cam.visW * 0.5f, scene.width - cam.visW * 0.5f)
        }
    }

    // ------------------------------------------------------------------ draw

    fun draw(c: Canvas) {
        Render.draw(c, scene, cam)
        hud.draw(c, this)
        if (fade > 0f) {
            D.rect(c, 0f, 0f, cam.screenW, cam.screenH, withAlphaF(0xFF101820.toInt(), fade))
        }
    }

    companion object {
        const val ACT_JUMP = 0
        const val ACT_THROW = 1
        const val ACT_OFF = 2
        const val ACT_RELEASE = 3
        private const val PREFS = "bitiplay"
    }
}
