package com.bitiplay.world.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class Snd {
    // handling
    POP, PICK, DROP, THROW, SPLASH, DIG, EAT, HAPPY, STEP, UI, PAGE, BUILD, SIZZLE,
    WHOOSH, CHIME, CLICK, THUD, SPLAT, CREAK, POUR, MAGIC, SPRING,
    // machines
    ENGINE, HORN, BOAT_HORN, WHISTLE, CASH, BELL, RING, WIN,
    // animals
    MOO, BAA, OINK, CLUCK, QUACK, WOOF, MEOW, ROAR, TRUMPET, CHIRP, SQUEAK, HONK, NEIGH,
    MONKEY, PARROT, SEAGULL, PIGEON
}

/**
 * Procedural sound bank. Everything is synthesised at 44.1 kHz on a worker
 * thread, so the app still ships with no audio assets.
 */
object Sfx {

    var enabled = true

    const val RATE = 44100

    /** How many `sfx_<name>_1..N` variants are looked for per cue. */
    private const val MAX_VARIANTS = 8

    @Volatile
    private var tracks: Map<Snd, AudioTrack> = emptyMap()

    @Volatile
    private var samples: Map<Snd, IntArray> = emptyMap()

    @Volatile
    private var pool: SoundPool? = null

    private val loadedIds = HashSet<Int>()

    @Volatile
    private var ready = false

    /**
     * Recorded clips in `res/raw` win; the synthesised bank stays as a per-cue
     * fallback, so a missing file just means that one sound is still generated.
     */
    fun init(ctx: Context) {
        if (ready) return
        ready = true
        loadSamples(ctx)
        buildSynth()
    }

    private fun loadSamples(ctx: Context) {
        try {
            val sp = SoundPool.Builder()
                .setMaxStreams(12)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            sp.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) synchronized(loadedIds) { loadedIds.add(sampleId) }
            }
            val res = ctx.resources
            val pkg = ctx.packageName
            val map = HashMap<Snd, IntArray>()
            for (s in Snd.entries) {
                val base = "sfx_" + s.name.lowercase()
                val ids = ArrayList<Int>()
                @Suppress("DiscouragedApi")
                val single = res.getIdentifier(base, "raw", pkg)
                if (single != 0) ids.add(sp.load(ctx, single, 1))
                for (i in 1..MAX_VARIANTS) {
                    @Suppress("DiscouragedApi")
                    val id = res.getIdentifier(base + "_" + i, "raw", pkg)
                    if (id != 0) ids.add(sp.load(ctx, id, 1))
                }
                if (ids.isNotEmpty()) map[s] = ids.toIntArray()
            }
            pool = sp
            samples = map
        } catch (_: Throwable) {
        }
    }

    private fun buildSynth() {
        try {
            val built = HashMap<Snd, AudioTrack>()
            for (s in Snd.entries) {
                val data = render(s) ?: continue
                val track = build(data) ?: continue
                built[s] = track
            }
            tracks = built
        } catch (_: Throwable) {
            // Audio is a nicety; never let it take the game down.
        }
    }

    /** [pitch] multiplies playback speed; a little jitter stops repeats grating. */
    fun play(s: Snd, pitch: Float = 1f) {
        if (!enabled) return
        val p = pitch * (0.97f + Random.nextFloat() * 0.06f)
        if (playSample(s, p)) return
        playSynth(s, p)
    }

    /** Picks a random loaded variant. Returns false if none is ready yet. */
    private fun playSample(s: Snd, pitch: Float): Boolean {
        val sp = pool ?: return false
        val ids = samples[s] ?: return false
        var count = 0
        synchronized(loadedIds) {
            for (id in ids) if (loadedIds.contains(id)) count++
        }
        if (count == 0) return false
        var pick = Random.nextInt(count)
        for (id in ids) {
            val ok = synchronized(loadedIds) { loadedIds.contains(id) }
            if (!ok) continue
            if (pick == 0) {
                return try {
                    sp.play(id, 0.95f, 0.95f, 1, 0, pitch.coerceIn(0.5f, 2f)) != 0
                } catch (_: Throwable) {
                    false
                }
            }
            pick--
        }
        return false
    }

    private fun playSynth(s: Snd, pitch: Float) {
        val t = tracks[s] ?: return
        try {
            if (t.playState != AudioTrack.PLAYSTATE_STOPPED) t.stop()
            t.reloadStaticData()
            t.playbackRate = (RATE * pitch).toInt().coerceIn(8000, 96000)
            t.play()
        } catch (_: Throwable) {
        }
    }

    fun release() {
        val current = tracks
        tracks = emptyMap()
        for (t in current.values) {
            try {
                t.stop(); t.release()
            } catch (_: Throwable) {
            }
        }
        try {
            pool?.release()
        } catch (_: Throwable) {
        }
        pool = null
        samples = emptyMap()
        synchronized(loadedIds) { loadedIds.clear() }
        ready = false
    }

    private fun build(data: ShortArray): AudioTrack? = try {
        val at = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
        at.setVolume(0.6f)
        at
    } catch (_: Throwable) {
        null
    }

    // ------------------------------------------------------------- synthesis

    /** 0 sine, 1 triangle, 2 square, 3 saw. */
    private fun wave(shape: Int, phase: Double): Double {
        val p = phase % (2.0 * PI)
        return when (shape) {
            1 -> {
                val x = p / PI
                if (x < 1.0) -1.0 + 2.0 * x else 3.0 - 2.0 * x
            }
            2 -> if (sin(p) >= 0.0) 1.0 else -1.0
            3 -> (p / PI) - 1.0
            else -> sin(p)
        }
    }

    class Buf(seconds: Float) {
        val n = (RATE * seconds).toInt().coerceAtLeast(64)
        val d = FloatArray(n)

        /**
         * A pitched voice. [f0] to [f1] glides over the span, [attack] and
         * [decay] are in seconds, [vib] adds vibrato depth in semitone-ish units.
         */
        fun tone(
            f0: Float, f1: Float, amp: Float,
            attack: Float = 0.004f, decay: Float = 0.25f,
            shape: Int = 0, vib: Float = 0f, vibRate: Float = 6f,
            from: Float = 0f, to: Float = 1f, curve: Float = 1f
        ) {
            val a = (from * n).toInt().coerceIn(0, n)
            val b = min(n, (to * n).toInt())
            if (b <= a) return
            val span = (b - a).toFloat()
            val atkN = (attack * RATE).coerceAtLeast(1f)
            var phase = 0.0
            for (i in a until b) {
                val k = (i - a).toFloat()
                val u = k / span
                val g = Math.pow(u.toDouble(), curve.toDouble()).toFloat()
                var f = f0 + (f1 - f0) * g
                if (vib > 0f) f *= (1f + vib * sin(k / RATE * vibRate * 6.2831855).toFloat())
                phase += 2.0 * PI * f / RATE
                val atk = if (k < atkN) k / atkN else 1f
                val dec = exp(-(k / RATE) / decay.coerceAtLeast(0.001f))
                d[i] += (wave(shape, phase) * (amp * atk * dec)).toFloat()
            }
        }

        /** Filtered noise. [cut] near 1 is bright hiss, near 0 is a dull rumble. */
        fun noise(
            amp: Float, decay: Float, cut: Float,
            from: Float = 0f, to: Float = 1f, attack: Float = 0.002f, cutEnd: Float = -1f
        ) {
            val a = (from * n).toInt().coerceIn(0, n)
            val b = min(n, (to * n).toInt())
            if (b <= a) return
            val span = (b - a).toFloat()
            val atkN = (attack * RATE).coerceAtLeast(1f)
            var lp = 0f
            var lp2 = 0f
            for (i in a until b) {
                val k = (i - a).toFloat()
                val u = k / span
                val cc = if (cutEnd >= 0f) cut + (cutEnd - cut) * u else cut
                val r = Random.nextFloat() * 2f - 1f
                lp += (r - lp) * cc
                lp2 += (lp - lp2) * cc
                val env = if (k < atkN) k / atkN else 1f
                val dec = exp(-(k / RATE) / decay.coerceAtLeast(0.001f)).toFloat()
                d[i] += lp2 * amp * env * dec
            }
        }

        /** Adds a short pitched blip, the building block for chatter. */
        fun blip(at: Float, len: Float, f0: Float, f1: Float, amp: Float, shape: Int = 0) {
            val from = at / (n.toFloat() / RATE)
            val to = (at + len) / (n.toFloat() / RATE)
            tone(f0, f1, amp, 0.003f, len * 0.6f, shape, from = from, to = to)
        }

        fun toShorts(): ShortArray {
            var peak = 0f
            for (v in d) {
                val a = kotlin.math.abs(v)
                if (a > peak) peak = a
            }
            val g = if (peak > 0.0001f) 0.88f / peak else 1f
            val out = ShortArray(n)
            val fade = (RATE * 0.004f).toInt().coerceAtLeast(1)
            for (i in 0 until n) {
                var v = d[i] * g
                if (i < fade) v *= i.toFloat() / fade
                if (i > n - fade) v *= (n - i).toFloat() / fade
                // gentle soft-clip keeps the loud cues from sounding brittle
                v = (v - v * v * v / 3f) * 1.5f
                out[i] = (v * 30000f).toInt().coerceIn(-32767, 32767).toShort()
            }
            return out
        }
    }

    private fun render(s: Snd): ShortArray? = when (s) {

        // ---- handling ------------------------------------------------------
        Snd.POP -> Buf(0.16f).apply {
            tone(380f, 940f, 0.9f, 0.002f, 0.045f, 0, curve = 0.5f)
            tone(760f, 1880f, 0.25f, 0.002f, 0.03f, 0, curve = 0.5f)
        }.toShorts()

        Snd.PICK -> Buf(0.18f).apply {
            tone(620f, 1320f, 0.8f, 0.002f, 0.06f, 1, curve = 0.6f)
            tone(1860f, 2640f, 0.18f, 0.002f, 0.035f)
        }.toShorts()

        Snd.DROP -> Buf(0.20f).apply {
            tone(680f, 220f, 0.85f, 0.002f, 0.07f, 1, curve = 1.6f)
            noise(0.2f, 0.05f, 0.25f)
        }.toShorts()

        Snd.THROW -> Buf(0.30f).apply {
            noise(0.55f, 0.11f, 0.16f, cutEnd = 0.5f, attack = 0.03f)
            tone(240f, 900f, 0.3f, 0.02f, 0.12f, 1, curve = 0.7f)
        }.toShorts()

        Snd.SPLASH -> Buf(0.46f).apply {
            noise(0.9f, 0.10f, 0.55f, cutEnd = 0.12f)
            tone(1200f, 340f, 0.22f, 0.003f, 0.07f, 0, curve = 1.4f)
            noise(0.3f, 0.28f, 0.35f, from = 0.15f)
        }.toShorts()

        Snd.DIG -> Buf(0.26f).apply {
            noise(0.85f, 0.06f, 0.10f, cutEnd = 0.03f)
            tone(150f, 80f, 0.35f, 0.002f, 0.05f, 1)
        }.toShorts()

        Snd.EAT -> Buf(0.30f).apply {
            blip(0.00f, 0.09f, 330f, 210f, 0.7f, 1)
            blip(0.12f, 0.09f, 380f, 230f, 0.65f, 1)
            noise(0.18f, 0.05f, 0.4f, from = 0.0f, to = 0.35f)
        }.toShorts()

        Snd.HAPPY -> Buf(0.62f).apply {
            tone(523f, 523f, 0.55f, 0.005f, 0.16f, 1, from = 0f, to = 0.3f)
            tone(659f, 659f, 0.55f, 0.005f, 0.16f, 1, from = 0.28f, to = 0.58f)
            tone(784f, 784f, 0.55f, 0.005f, 0.2f, 1, from = 0.56f, to = 0.84f)
            tone(1046f, 1046f, 0.45f, 0.005f, 0.3f, 1, from = 0.8f, to = 1f)
        }.toShorts()

        Snd.STEP -> Buf(0.10f).apply {
            noise(0.5f, 0.022f, 0.30f, cutEnd = 0.1f)
            tone(180f, 120f, 0.28f, 0.001f, 0.02f, 1)
        }.toShorts()

        Snd.UI -> Buf(0.13f).apply {
            tone(880f, 1240f, 0.5f, 0.002f, 0.05f, 1, curve = 0.5f)
        }.toShorts()

        Snd.PAGE -> Buf(0.30f).apply {
            noise(0.45f, 0.09f, 0.62f, cutEnd = 0.2f, attack = 0.01f)
            tone(520f, 900f, 0.16f, 0.01f, 0.09f)
        }.toShorts()

        Snd.BUILD -> Buf(0.22f).apply {
            noise(0.6f, 0.035f, 0.2f)
            tone(300f, 170f, 0.6f, 0.001f, 0.05f, 1)
            tone(600f, 340f, 0.2f, 0.001f, 0.04f)
        }.toShorts()

        Snd.SIZZLE -> Buf(0.70f).apply {
            noise(0.4f, 0.55f, 0.86f, attack = 0.06f)
            noise(0.15f, 0.4f, 0.5f, from = 0.1f)
        }.toShorts()

        Snd.WHOOSH -> Buf(0.34f).apply {
            noise(0.7f, 0.13f, 0.10f, cutEnd = 0.62f, attack = 0.05f)
        }.toShorts()

        Snd.CHIME -> Buf(0.90f).apply {
            tone(1046f, 1046f, 0.5f, 0.002f, 0.30f)
            tone(1568f, 1568f, 0.26f, 0.002f, 0.22f)
            tone(2093f, 2093f, 0.14f, 0.002f, 0.16f)
            tone(3136f, 3136f, 0.06f, 0.002f, 0.10f)
        }.toShorts()

        Snd.CLICK -> Buf(0.06f).apply {
            noise(0.6f, 0.008f, 0.8f)
            tone(1400f, 900f, 0.3f, 0.001f, 0.012f)
        }.toShorts()

        Snd.THUD -> Buf(0.26f).apply {
            tone(120f, 62f, 0.9f, 0.002f, 0.09f, 1, curve = 1.5f)
            noise(0.25f, 0.05f, 0.08f)
        }.toShorts()

        Snd.SPLAT -> Buf(0.26f).apply {
            noise(0.8f, 0.05f, 0.45f, cutEnd = 0.08f)
            tone(400f, 90f, 0.4f, 0.002f, 0.06f, 1, curve = 2f)
        }.toShorts()

        Snd.CREAK -> Buf(0.55f).apply {
            tone(180f, 320f, 0.35f, 0.06f, 0.4f, 3, vib = 0.05f, vibRate = 11f, curve = 1.3f)
            noise(0.10f, 0.3f, 0.25f)
        }.toShorts()

        Snd.POUR -> Buf(0.85f).apply {
            noise(0.42f, 0.7f, 0.55f, attack = 0.08f, cutEnd = 0.35f)
            for (i in 0 until 7) {
                blip(0.06f + i * 0.10f, 0.05f, 900f + i * 60f, 620f, 0.16f)
            }
        }.toShorts()

        Snd.MAGIC -> Buf(0.70f).apply {
            for (i in 0 until 6) {
                blip(i * 0.075f, 0.14f, 700f + i * 220f, 1000f + i * 300f, 0.32f, 1)
            }
            tone(2400f, 3600f, 0.10f, 0.05f, 0.3f)
        }.toShorts()

        Snd.SPRING -> Buf(0.45f).apply {
            tone(260f, 1000f, 0.55f, 0.003f, 0.22f, 2, vib = 0.10f, vibRate = 22f, curve = 0.6f)
        }.toShorts()

        // ---- machines ------------------------------------------------------
        Snd.ENGINE -> Buf(0.55f).apply {
            tone(64f, 92f, 0.55f, 0.03f, 0.45f, 2, vib = 0.03f, vibRate = 19f)
            tone(128f, 184f, 0.18f, 0.03f, 0.35f, 3)
            noise(0.22f, 0.3f, 0.06f)
        }.toShorts()

        Snd.HORN -> Buf(0.38f).apply {
            tone(392f, 392f, 0.42f, 0.01f, 0.3f, 2)
            tone(494f, 494f, 0.34f, 0.01f, 0.3f, 2)
            tone(784f, 784f, 0.10f, 0.01f, 0.2f)
        }.toShorts()

        Snd.BOAT_HORN -> Buf(1.10f).apply {
            tone(110f, 104f, 0.6f, 0.10f, 0.7f, 2)
            tone(165f, 156f, 0.3f, 0.12f, 0.6f, 1)
            tone(55f, 52f, 0.35f, 0.10f, 0.8f)
        }.toShorts()

        Snd.WHISTLE -> Buf(0.85f).apply {
            tone(880f, 840f, 0.45f, 0.05f, 0.5f, 1, vib = 0.012f, vibRate = 7f)
            tone(1320f, 1260f, 0.30f, 0.05f, 0.45f, 1)
            noise(0.10f, 0.4f, 0.8f, attack = 0.05f)
        }.toShorts()

        Snd.CASH -> Buf(0.70f).apply {
            tone(2093f, 2093f, 0.45f, 0.002f, 0.22f)
            tone(2637f, 2637f, 0.30f, 0.002f, 0.18f)
            noise(0.25f, 0.03f, 0.7f, from = 0.35f)
            tone(160f, 110f, 0.3f, 0.002f, 0.05f, 1, from = 0.4f)
        }.toShorts()

        Snd.BELL -> Buf(1.20f).apply {
            tone(1760f, 1760f, 0.5f, 0.001f, 0.5f)
            tone(2640f, 2640f, 0.22f, 0.001f, 0.35f)
            tone(3520f, 3520f, 0.12f, 0.001f, 0.22f)
            tone(880f, 880f, 0.18f, 0.001f, 0.6f)
        }.toShorts()

        Snd.RING -> Buf(0.5f).apply {
            tone(1200f, 1180f, 0.4f, 0.001f, 0.18f, 1)
            tone(1800f, 1770f, 0.22f, 0.001f, 0.12f)
            noise(0.15f, 0.02f, 0.9f)
        }.toShorts()

        Snd.WIN -> Buf(1.05f).apply {
            val notes = floatArrayOf(523f, 659f, 784f, 1046f, 1318f)
            for (i in notes.indices) {
                blip(i * 0.11f, 0.22f, notes[i], notes[i], 0.45f, 1)
            }
            tone(1568f, 1568f, 0.35f, 0.01f, 0.45f, 1, from = 0.55f)
            tone(2093f, 2093f, 0.22f, 0.01f, 0.4f, from = 0.55f)
        }.toShorts()

        // ---- animals -------------------------------------------------------
        Snd.MOO -> Buf(0.85f).apply {
            tone(150f, 112f, 0.7f, 0.06f, 0.55f, 1, vib = 0.02f, vibRate = 5f, curve = 1.5f)
            tone(300f, 224f, 0.22f, 0.06f, 0.4f, 0, curve = 1.5f)
            noise(0.06f, 0.3f, 0.05f)
        }.toShorts()

        Snd.BAA -> Buf(0.70f).apply {
            tone(430f, 380f, 0.55f, 0.03f, 0.4f, 1, vib = 0.09f, vibRate = 17f)
            tone(860f, 760f, 0.16f, 0.03f, 0.3f, 0, vib = 0.09f, vibRate = 17f)
        }.toShorts()

        Snd.OINK -> Buf(0.45f).apply {
            tone(300f, 170f, 0.55f, 0.01f, 0.09f, 3, from = 0f, to = 0.42f, curve = 1.6f)
            tone(320f, 180f, 0.5f, 0.01f, 0.09f, 3, from = 0.5f, to = 1f, curve = 1.6f)
            noise(0.2f, 0.06f, 0.2f)
        }.toShorts()

        Snd.CLUCK -> Buf(0.42f).apply {
            blip(0.00f, 0.06f, 900f, 640f, 0.5f, 2)
            blip(0.10f, 0.05f, 1050f, 720f, 0.45f, 2)
            blip(0.19f, 0.08f, 780f, 1200f, 0.4f, 1)
            noise(0.12f, 0.04f, 0.6f)
        }.toShorts()

        Snd.QUACK -> Buf(0.35f).apply {
            tone(620f, 380f, 0.6f, 0.004f, 0.12f, 3, vib = 0.05f, vibRate = 30f, curve = 1.4f)
            noise(0.12f, 0.05f, 0.5f)
        }.toShorts()

        Snd.WOOF -> Buf(0.36f).apply {
            noise(0.5f, 0.05f, 0.28f, cutEnd = 0.1f)
            tone(340f, 180f, 0.7f, 0.004f, 0.09f, 1, curve = 1.7f)
            tone(680f, 360f, 0.2f, 0.004f, 0.06f)
        }.toShorts()

        Snd.MEOW -> Buf(0.75f).apply {
            tone(560f, 820f, 0.5f, 0.05f, 0.5f, 1, vib = 0.03f, vibRate = 9f,
                from = 0f, to = 0.45f, curve = 0.7f)
            tone(820f, 480f, 0.5f, 0.02f, 0.35f, 1, vib = 0.03f, vibRate = 9f,
                from = 0.42f, to = 1f, curve = 1.3f)
        }.toShorts()

        Snd.ROAR -> Buf(1.00f).apply {
            tone(120f, 86f, 0.6f, 0.12f, 0.6f, 3, vib = 0.05f, vibRate = 13f, curve = 1.4f)
            noise(0.45f, 0.5f, 0.14f, attack = 0.1f, cutEnd = 0.05f)
            tone(60f, 44f, 0.3f, 0.12f, 0.7f)
        }.toShorts()

        Snd.TRUMPET -> Buf(0.95f).apply {
            tone(300f, 620f, 0.55f, 0.06f, 0.45f, 3, vib = 0.03f, vibRate = 8f, curve = 0.6f)
            tone(600f, 1240f, 0.2f, 0.06f, 0.3f, 2, curve = 0.6f)
            noise(0.12f, 0.3f, 0.3f, attack = 0.08f)
        }.toShorts()

        Snd.CHIRP -> Buf(0.35f).apply {
            blip(0.00f, 0.07f, 2600f, 3600f, 0.42f)
            blip(0.11f, 0.06f, 2900f, 3900f, 0.36f)
            blip(0.21f, 0.08f, 2400f, 3300f, 0.30f)
        }.toShorts()

        Snd.SQUEAK -> Buf(0.24f).apply {
            tone(1800f, 2900f, 0.4f, 0.004f, 0.09f, 1, curve = 0.6f)
            tone(2700f, 1900f, 0.25f, 0.004f, 0.07f, 1, from = 0.45f)
        }.toShorts()

        Snd.HONK -> Buf(0.45f).apply {
            tone(420f, 360f, 0.55f, 0.02f, 0.2f, 3, vib = 0.04f, vibRate = 14f)
            tone(840f, 720f, 0.18f, 0.02f, 0.15f, 2)
        }.toShorts()

        Snd.NEIGH -> Buf(0.90f).apply {
            tone(760f, 420f, 0.5f, 0.02f, 0.45f, 3, vib = 0.11f, vibRate = 26f, curve = 1.4f)
            tone(380f, 210f, 0.28f, 0.02f, 0.4f, 1, vib = 0.11f, vibRate = 26f, curve = 1.4f)
            noise(0.14f, 0.3f, 0.35f, attack = 0.05f)
        }.toShorts()

        Snd.MONKEY -> Buf(0.50f).apply {
            for (i in 0 until 5) {
                blip(i * 0.085f, 0.06f, 1500f + i * 130f, 1050f + i * 90f, 0.4f, 2)
            }
        }.toShorts()

        Snd.PARROT -> Buf(0.45f).apply {
            tone(900f, 1500f, 0.5f, 0.006f, 0.12f, 3, vib = 0.08f, vibRate = 28f, curve = 0.6f)
            tone(1400f, 800f, 0.35f, 0.006f, 0.14f, 3, from = 0.45f, curve = 1.4f)
        }.toShorts()

        Snd.SEAGULL -> Buf(0.80f).apply {
            tone(1300f, 1900f, 0.45f, 0.03f, 0.16f, 1, vib = 0.05f, vibRate = 14f,
                from = 0f, to = 0.4f, curve = 0.6f)
            tone(1750f, 1150f, 0.4f, 0.02f, 0.22f, 1, from = 0.42f, curve = 1.3f)
        }.toShorts()

        Snd.PIGEON -> Buf(0.75f).apply {
            tone(430f, 380f, 0.42f, 0.06f, 0.22f, 0, vib = 0.05f, vibRate = 12f, from = 0f, to = 0.45f)
            tone(360f, 330f, 0.34f, 0.05f, 0.26f, 0, vib = 0.04f, vibRate = 10f, from = 0.45f)
            noise(0.05f, 0.2f, 0.2f)
        }.toShorts()
    }
}
