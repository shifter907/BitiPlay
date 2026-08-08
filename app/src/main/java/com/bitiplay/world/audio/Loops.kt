package com.bitiplay.world.audio

import android.content.Context
import android.media.MediaPlayer

/**
 * Continuously looping sounds whose volume follows the action - the car engine
 * while you drive, the carousel organ as you walk past it.
 *
 * Drive it from the frame loop: call [beginFrame], then [set] for whatever
 * should be audible right now, then [update]. Anything not refreshed this frame
 * fades out and stops on its own, so a loop can never survive a scene change.
 *
 * Clips are long, so these use MediaPlayer rather than SoundPool, and are
 * prepared off the game thread to avoid a hitch when one starts.
 */
object Loops {

    var enabled = true

    private class Loop(val resIds: IntArray) {
        // Written by the preparing worker, read by the frame loop.
        @Volatile
        var player: MediaPlayer? = null

        @Volatile
        var starting = false

        var target = 0f
        var current = 0f
    }

    private val loops = HashMap<String, Loop>()
    private val resCache = HashMap<String, IntArray>()

    @Volatile
    private var appCtx: Context? = null

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
    }

    /** Resolves `name`, plus `name_1..8`, from res/raw. */
    private fun resolve(name: String): IntArray {
        resCache[name]?.let { return it }
        val ctx = appCtx
        val out = ArrayList<Int>()
        if (ctx != null) {
            try {
                val res = ctx.resources
                val pkg = ctx.packageName
                @Suppress("DiscouragedApi")
                val single = res.getIdentifier(name, "raw", pkg)
                if (single != 0) out.add(single)
                for (i in 1..8) {
                    @Suppress("DiscouragedApi")
                    val id = res.getIdentifier(name + "_" + i, "raw", pkg)
                    if (id != 0) out.add(id)
                }
            } catch (_: Throwable) {
            }
        }
        val arr = out.toIntArray()
        resCache[name] = arr
        return arr
    }

    /** Marks every loop as silent; [set] calls this frame bring them back. */
    fun beginFrame() {
        for (l in loops.values) l.target = 0f
    }

    fun set(key: String, resName: String, volume: Float) {
        if (volume <= 0f) return
        val l = loops.getOrPut(key) { Loop(resolve(resName)) }
        if (l.resIds.isEmpty()) return
        if (volume > l.target) l.target = volume
    }

    fun update(dt: Float) {
        val ctx = appCtx ?: return
        val it = loops.entries.iterator()
        while (it.hasNext()) {
            val l = it.next().value
            val want = if (enabled) l.target else 0f
            // Ramp so starts and stops do not click.
            val rate = if (want > l.current) 2.4f else 1.8f
            l.current += (want - l.current) * (1f - kotlin.math.exp(-rate * dt))
            if (want <= 0f && l.current < 0.02f) l.current = 0f

            val p = l.player
            if (l.current > 0.02f) {
                if (p == null && !l.starting) start(ctx, l)
                else if (p != null) {
                    try {
                        p.setVolume(l.current, l.current)
                    } catch (_: Throwable) {
                    }
                }
            } else if (p != null) {
                try {
                    p.stop(); p.release()
                } catch (_: Throwable) {
                }
                l.player = null
            }
        }
    }

    private fun start(ctx: Context, l: Loop) {
        l.starting = true
        val res = l.resIds[kotlin.random.Random.nextInt(l.resIds.size)]
        Thread({
            var mp: MediaPlayer? = null
            try {
                mp = MediaPlayer.create(ctx, res)
                if (mp != null) {
                    mp.isLooping = true
                    synchronized(loops) {
                        if (l.current > 0.02f) {
                            mp.setVolume(l.current, l.current)
                            mp.start()
                            l.player = mp
                        } else {
                            mp.release()
                        }
                    }
                }
            } catch (_: Throwable) {
                try {
                    mp?.release()
                } catch (_: Throwable) {
                }
            } finally {
                l.starting = false
            }
        }, "bitiplay-loop").apply { isDaemon = true }.start()
    }

    fun stopAll() {
        for (l in loops.values) {
            l.target = 0f
            l.current = 0f
            try {
                l.player?.stop()
                l.player?.release()
            } catch (_: Throwable) {
            }
            l.player = null
        }
    }

    fun release() {
        stopAll()
        loops.clear()
    }
}
