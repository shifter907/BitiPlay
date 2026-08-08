package com.bitiplay.world.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generative background music. Each scene gets a four-bar loop built from a
 * bass line, soft chord pad, a plucked melody and light percussion, rendered
 * once on a worker thread and then looped by the hardware.
 *
 * Only one loop is resident at a time, so this costs about 400 KB.
 */
object Music {

    var enabled = true
        set(value) {
            field = value
            if (!value) stopTrack() else restart()
        }

    private const val RATE = 22050
    private const val VOLUME = 0.32f

    @Volatile
    private var track: AudioTrack? = null

    @Volatile
    private var currentId = ""

    private var wanted = ""
    private var worker: Thread? = null

    /** Starts (or switches to) the loop for a scene id. Safe to call every frame. */
    fun play(sceneId: String) {
        if (sceneId == wanted && (track != null || worker != null)) return
        wanted = sceneId
        stopTrack()
        if (!enabled) return
        val w = Thread({ renderAndStart(sceneId) }, "bitiplay-music")
        worker = w
        w.isDaemon = true
        w.start()
    }

    private fun restart() {
        val id = wanted
        if (id.isNotEmpty()) {
            currentId = ""
            play(id)
        }
    }

    private fun renderAndStart(sceneId: String) {
        try {
            val mood = moodFor(sceneId)
            val data = render(mood)
            // The player may have moved on while we were rendering.
            if (wanted != sceneId || !enabled) return
            val at = build(data) ?: return
            synchronized(this) {
                if (wanted != sceneId || !enabled) {
                    try {
                        at.release()
                    } catch (_: Throwable) {
                    }
                    return
                }
                stopTrack()
                at.setLoopPoints(0, data.size, -1)
                at.setVolume(VOLUME)
                at.play()
                track = at
                currentId = sceneId
            }
        } catch (_: Throwable) {
            // Music is optional; never let it take the game down.
        }
    }

    private fun stopTrack() {
        synchronized(this) {
            val t = track ?: return
            track = null
            currentId = ""
            try {
                t.stop(); t.release()
            } catch (_: Throwable) {
            }
        }
    }

    fun release() {
        wanted = ""
        stopTrack()
    }

    private fun build(data: ShortArray): AudioTrack? = try {
        val at = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(data.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        at.write(data, 0, data.size)
        at
    } catch (_: Throwable) {
        null
    }

    // -------------------------------------------------------------- material

    private class Mood(
        val tempo: Float,
        val root: Float,
        /** Scale degrees in semitones from the root. */
        val scale: IntArray,
        /** Chord roots per bar, as scale-independent semitone offsets. */
        val chords: IntArray,
        val leadShape: Int,
        val padShape: Int,
        val swing: Float,
        val drums: Float,
        val seed: Int
    )

    private val MAJOR = intArrayOf(0, 2, 4, 5, 7, 9, 11)
    private val MINOR = intArrayOf(0, 2, 3, 5, 7, 8, 10)
    private val DORIAN = intArrayOf(0, 2, 3, 5, 7, 9, 10)
    private val PENTA = intArrayOf(0, 2, 4, 7, 9)

    private fun moodFor(id: String): Mood = when (id) {
        "hood" -> Mood(102f, 261.63f, MAJOR, intArrayOf(0, 7, 9, 5), 1, 0, 0.10f, 0.5f, 11)
        "beach" -> Mood(92f, 174.61f, MAJOR, intArrayOf(0, 5, 7, 5), 0, 0, 0.16f, 0.3f, 23)
        "farm" -> Mood(114f, 196.00f, MAJOR, intArrayOf(0, 5, 7, 0), 2, 1, 0.20f, 0.6f, 37)
        "park" -> Mood(126f, 293.66f, MAJOR, intArrayOf(0, 5, 7, 7), 2, 2, 0.06f, 0.75f, 41)
        "cruise" -> Mood(88f, 233.08f, MAJOR, intArrayOf(0, 9, 5, 7), 0, 0, 0.18f, 0.25f, 53)
        "city" -> Mood(112f, 220.00f, MINOR, intArrayOf(0, 5, 3, 7), 1, 1, 0.14f, 0.55f, 67)
        "zoo" -> Mood(106f, 164.81f, DORIAN, intArrayOf(0, 5, 3, 5), 1, 0, 0.12f, 0.6f, 71)
        "shop" -> Mood(98f, 261.63f, PENTA, intArrayOf(0, 5, 7, 5), 0, 1, 0.10f, 0.35f, 83)
        else -> Mood(100f, 261.63f, MAJOR, intArrayOf(0, 7, 9, 5), 1, 0, 0.1f, 0.5f, 7)
    }

    private fun semis(root: Float, n: Int): Float =
        root * Math.pow(2.0, n / 12.0).toFloat()

    private fun wave(shape: Int, phase: Double): Double {
        val p = phase % (2.0 * PI)
        return when (shape) {
            1 -> {
                val x = p / PI
                if (x < 1.0) -1.0 + 2.0 * x else 3.0 - 2.0 * x
            }
            2 -> if (sin(p) >= 0.0) 0.7 else -0.7
            3 -> (p / PI) - 1.0
            else -> sin(p)
        }
    }

    private fun render(m: Mood): ShortArray {
        val beat = 60f / m.tempo
        val bars = 4
        val total = bars * 4 * beat
        val n = (total * RATE).toInt()
        val buf = FloatArray(n)
        val rnd = Random(m.seed)

        fun note(
            atSec: Float, durSec: Float, freq: Float, amp: Float,
            shape: Int, attack: Float = 0.01f, release: Float = 0.18f
        ) {
            val start = (atSec * RATE).toInt()
            val len = (durSec * RATE).toInt()
            if (start >= n || len <= 0) return
            val atkN = (attack * RATE).coerceAtLeast(1f)
            var phase = 0.0
            for (i in 0 until len) {
                val idx = start + i
                if (idx >= n) break
                phase += 2.0 * PI * freq / RATE
                val k = i.toFloat()
                val atk = if (k < atkN) k / atkN else 1f
                val dec = exp(-(k / RATE) / release)
                buf[idx] += (wave(shape, phase) * (amp * atk * dec)).toFloat()
            }
        }

        fun perc(atSec: Float, durSec: Float, amp: Float, cut: Float, low: Boolean) {
            val start = (atSec * RATE).toInt()
            val len = (durSec * RATE).toInt()
            var lp = 0f
            for (i in 0 until len) {
                val idx = start + i
                if (idx >= n) break
                val r = rnd.nextFloat() * 2f - 1f
                lp += (r - lp) * cut
                val dec = exp(-(i.toFloat() / RATE) / (durSec * 0.35f))
                var v = lp * amp * dec
                if (low) {
                    // a soft kick body under the noise
                    val f = 90f * exp(-(i.toFloat() / RATE) / 0.05f)
                    v += (sin(2.0 * PI * f * i / RATE) * amp * 1.6 * dec).toFloat()
                }
                buf[idx] += v
            }
        }

        for (bar in 0 until bars) {
            val barT = bar * 4 * beat
            val chord = m.chords[bar % m.chords.size]
            val bass = semis(m.root, chord - 24)

            // bass on 1 and 3, with a lift into the next bar
            note(barT, beat * 0.9f, bass, 0.34f, 0, 0.006f, 0.30f)
            note(barT + beat * 2f, beat * 0.9f, bass, 0.30f, 0, 0.006f, 0.28f)
            note(barT + beat * 3.5f, beat * 0.4f, semis(m.root, chord - 12), 0.18f, 0, 0.006f, 0.14f)

            // pad: root, third, fifth held across the bar
            val third = if (m.scale === MINOR) 3 else 4
            note(barT, beat * 3.8f, semis(m.root, chord - 12), 0.10f, m.padShape, 0.15f, 2.2f)
            note(barT, beat * 3.8f, semis(m.root, chord - 12 + third), 0.085f, m.padShape, 0.18f, 2.2f)
            note(barT, beat * 3.8f, semis(m.root, chord - 12 + 7), 0.075f, m.padShape, 0.20f, 2.2f)

            // melody: one phrase per bar, eighth notes on a scale
            var deg = rnd.nextInt(m.scale.size)
            for (e in 0 until 8) {
                if (rnd.nextFloat() < 0.28f) continue
                val swingOff = if (e % 2 == 1) m.swing * beat * 0.5f else 0f
                val t = barT + e * beat * 0.5f + swingOff
                deg = (deg + rnd.nextInt(-2, 3)).coerceIn(0, m.scale.size - 1)
                val oct = if (rnd.nextFloat() < 0.22f) 12 else 0
                val f = semis(m.root, chord + m.scale[deg] + oct)
                note(t, beat * 0.46f, f, 0.15f, m.leadShape, 0.008f, 0.16f)
                // a quiet octave shimmer above
                note(t, beat * 0.3f, f * 2f, 0.045f, 0, 0.008f, 0.10f)
            }

            // percussion
            if (m.drums > 0.01f) {
                for (e in 0 until 8) {
                    val t = barT + e * beat * 0.5f
                    perc(t, 0.06f, 0.05f * m.drums, 0.9f, false)
                }
                perc(barT, 0.22f, 0.10f * m.drums, 0.25f, true)
                perc(barT + beat * 2f, 0.22f, 0.09f * m.drums, 0.25f, true)
            }
        }

        // Normalise, then a short crossfade at the seam so the loop is seamless.
        var peak = 0f
        for (v in buf) {
            val a = kotlin.math.abs(v)
            if (a > peak) peak = a
        }
        val g = if (peak > 0.0001f) 0.82f / peak else 1f
        val out = ShortArray(n)
        val xf = min(n / 8, (RATE * 0.12f).toInt())
        for (i in 0 until n) {
            var v = buf[i] * g
            if (i < xf) {
                // blend the tail into the head so the join is inaudible
                val u = i.toFloat() / xf
                v = v * u + buf[n - xf + i] * g * (1f - u)
            }
            out[i] = (v * 30000f).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }
}
