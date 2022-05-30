@file:Suppress("unused")

package de.dertyp7214.youtubemusicremote.core

import android.content.Context
import java.util.concurrent.TimeUnit
import kotlin.math.pow

fun String.toHumanReadable(isSeconds: Boolean = false): String {
    return try {
        toLong().toHumanReadable(isSeconds)
    } catch (_: Exception) {
        this
    }
}

fun Int.toHumanReadable(isSeconds: Boolean = false): String = toLong().toHumanReadable(isSeconds)
fun Long.toHumanReadable(isSeconds: Boolean = false): String {
    val seconds = if (isSeconds) this else TimeUnit.MILLISECONDS.toSeconds(this)
    val minutes = TimeUnit.SECONDS.toMinutes(seconds)
    val hours = TimeUnit.MINUTES.toHours(minutes)

    val builder = StringBuilder()
    if (hours > 0) builder.append("$hours:")
    builder.append("${(minutes % 60).let { if (it > 9) it else "0$it" }}:")
    builder.append("${(seconds % 60).let { if (it > 9) it else "0$it" }}")

    return builder.toString()
}

fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

fun Int.pxToDp(context: Context): Int {
    return (this / context.resources.displayMetrics.density).toInt()
}

fun Number.toMillis() = toString().toMillis()

fun Number.easeInQuad(x: Float) = toFloat() * (x * x)

fun Number.easeOutQuad(x: Float) = toFloat() * (1f - (1f - x) * (1f - x))

fun Number.easeInQuart(x: Float) = toFloat() * (x * x * x * x)

fun Number.easeOutQuart(x: Float) = toFloat() * (1f - (1f - x).pow(4))

fun Number.easeInExpo(x: Float) = if (x == 0f) toFloat()
else toFloat() * 2f.pow(10 * x - 10)

fun Number.easeOutExpo(x: Float) = if (x == 1f) toFloat()
else toFloat() * (1f - 2f.pow(-10 * x))

fun Number.easeInCubic(x: Float) = toFloat() * (x * x * x)

fun Number.easeOutCubic(x: Float) = toFloat() * (1f - (1f - x).pow(3))

fun Number.easeInBounce(x: Float) = toFloat() - easeOutBounce(1f - x)

fun Number.easeOutBounce(x: Float): Float {
    val n1 = 7.5625f
    val d1 = 2.75f

    val factor = if (x < 1 / d1) {
        n1 * x * x
    } else if (x < 2 / d1) {
        n1 * (x - 1.5f / d1) * (x - 1.5f) + 0.75f
    } else if (x < 2.5 / d1) {
        n1 * (x - 2.25f / d1) * (x - 2.25f) + 0.9375f
    } else {
        n1 * (x - 2.625f / d1) * (x - 2.625f) + 0.984375f
    }

    return toFloat() * factor
}

fun Float.minMax(min: Float, max: Float) = if (this > max) max else if (this < min) min else this