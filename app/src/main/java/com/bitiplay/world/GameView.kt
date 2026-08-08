package com.bitiplay.world

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.VelocityTracker
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * Renders on a dedicated thread. Input arrives on the UI thread, so every touch
 * into [Game] is taken under the same lock the frame loop holds.
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    val game = Game(context)

    private val lock = Any()
    private var thread: Thread? = null

    @Volatile
    private var running = false

    private var lastNanos = 0L
    private var sized = false

    private var downX = 0f
    private var lastX = 0f
    private var dragging = false
    private var tracker: VelocityTracker? = null
    private val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    init {
        holder.addCallback(this)
        isFocusable = true
        keepScreenOn = true
    }

    // ---------------------------------------------------------------- surface

    override fun surfaceCreated(h: SurfaceHolder) = Unit

    override fun surfaceChanged(h: SurfaceHolder, format: Int, width: Int, height: Int) {
        synchronized(lock) {
            game.resize(width.toFloat(), height.toFloat())
            sized = true
        }
        resume()
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        pause()
    }

    fun resume() {
        if (running || !holder.surface.isValid) return
        running = true
        lastNanos = 0L
        thread = Thread(this, "bitiplay-render").also { it.start() }
    }

    fun pause() {
        if (!running) return
        running = false
        try {
            thread?.join(900)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        thread = null
        synchronized(lock) { game.saveState() }
        com.bitiplay.world.audio.Loops.stopAll()
        com.bitiplay.world.audio.Music.release()
    }

    // ------------------------------------------------------------------- loop

    override fun run() {
        while (running) {
            val now = System.nanoTime()
            var dt = if (lastNanos == 0L) 1f / 60f else (now - lastNanos) / 1_000_000_000f
            lastNanos = now
            // A long stall (GC, app switch) must not teleport the world.
            if (dt > 0.05f) dt = 0.05f
            if (dt < 0f) dt = 0f

            if (!sized) {
                try {
                    Thread.sleep(8)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                continue
            }

            val h = holder
            if (!h.surface.isValid) {
                try {
                    Thread.sleep(8)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                continue
            }

            val canvas = try {
                h.lockCanvas()
            } catch (_: Throwable) {
                null
            }
            if (canvas == null) {
                try {
                    Thread.sleep(8)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                continue
            }
            try {
                synchronized(lock) {
                    game.update(dt)
                    game.draw(canvas)
                }
            } finally {
                try {
                    h.unlockCanvasAndPost(canvas)
                } catch (_: Throwable) {
                }
            }
        }
    }

    // ------------------------------------------------------------------ input

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                lastX = e.x
                dragging = false
                tracker?.recycle()
                tracker = VelocityTracker.obtain().also { it.addMovement(e) }
                synchronized(lock) { game.onDown(e.x, e.y) }
            }

            MotionEvent.ACTION_MOVE -> {
                tracker?.addMovement(e)
                // Absolute position first: the switcher wheel and the drag-out
                // need it before the camera-pan slop is exceeded.
                synchronized(lock) { game.onMove(e.x, e.y) }
                if (!dragging && abs(e.x - downX) > slop) {
                    dragging = true
                    // Fold in the slop we swallowed so the world does not jump.
                    val d = e.x - downX
                    synchronized(lock) { game.onDrag(d) }
                    lastX = e.x
                } else if (dragging) {
                    val d = e.x - lastX
                    lastX = e.x
                    synchronized(lock) { game.onDrag(d) }
                }
            }

            MotionEvent.ACTION_UP -> {
                var vx = 0f
                tracker?.let {
                    it.addMovement(e)
                    it.computeCurrentVelocity(1000)
                    vx = it.xVelocity
                    it.recycle()
                }
                tracker = null
                synchronized(lock) { game.onUp(e.x, e.y, dragging, vx) }
                dragging = false
            }

            MotionEvent.ACTION_CANCEL -> {
                tracker?.recycle()
                tracker = null
                dragging = false
            }
        }
        return true
    }

    fun onBackPressedHandled(): Boolean = synchronized(lock) { game.onBack() }
}
