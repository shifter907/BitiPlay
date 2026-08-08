package com.bitiplay.world.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

const val TAU = 6.2831855f
const val PI_F = 3.1415927f
const val DEG = 57.29578f

fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

fun clamp(v: Float, lo: Float, hi: Float): Float = max(lo, min(hi, v))

fun clamp01(v: Float): Float = clamp(v, 0f, 1f)

fun clampInt(v: Int, lo: Int, hi: Int): Int = max(lo, min(hi, v))

fun sgn(v: Float): Float = if (v > 0f) 1f else if (v < 0f) -1f else 0f

/** Moves [cur] toward [target] by at most [step]. */
fun approach(cur: Float, target: Float, step: Float): Float {
    val d = target - cur
    return if (abs(d) <= step) target else cur + sgn(d) * step
}

/** Frame-rate independent exponential smoothing. [rate] is roughly "fraction closed per second". */
fun damp(cur: Float, target: Float, rate: Float, dt: Float): Float {
    val t = 1f - kotlin.math.exp(-rate * dt)
    return cur + (target - cur) * t
}

/** Normalises [x] into `[0, w)`. */
fun wrapPos(x: Float, w: Float): Float {
    if (w <= 0f) return x
    var v = x % w
    if (v < 0f) v += w
    return v
}

/** Shortest signed distance from one point to another on a loop of circumference [w]. */
fun wrapDelta(d: Float, w: Float): Float {
    if (w <= 0f) return d
    var v = (d + w * 0.5f) % w
    if (v < 0f) v += w
    return v - w * 0.5f
}

fun smoothstep(t: Float): Float {
    val x = clamp01(t)
    return x * x * (3f - 2f * x)
}

/** Ease that overshoots slightly then settles - good for pop-in animations. */
fun easeBack(t: Float): Float {
    val x = clamp01(t)
    val c = 1.70158f
    val u = x - 1f
    return 1f + (c + 1f) * u * u * u + c * u * u
}

fun easeOutCubic(t: Float): Float {
    val u = 1f - clamp01(t)
    return 1f - u * u * u
}

fun rnd(a: Float, b: Float): Float = a + Random.nextFloat() * (b - a)

fun rndInt(a: Int, b: Int): Int = if (b <= a) a else a + Random.nextInt(b - a)

fun chance(p: Float): Boolean = Random.nextFloat() < p

fun <T> pick(list: List<T>): T = list[Random.nextInt(list.size)]

/**
 * Cheap smooth pseudo-noise in `[-1, 1]`, stable for a given [seed].
 * Used for idle sway so identical props do not move in lockstep.
 */
fun wobble(seed: Int, t: Float): Float {
    val a = (seed * 0.6180339f) % 1f * TAU
    val b = (seed * 0.2371f) % 1f * TAU
    return (sin(t * 1.7f + a) * 0.6f + sin(t * 2.9f + b) * 0.4f)
}
