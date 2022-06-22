@file:Suppress("unused")

package de.dertyp7214.youtubemusicremote.core

import android.content.Context
import java.util.concurrent.TimeUnit

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

fun Float.minMax(min: Float, max: Float) = if (this > max) max else if (this < min) min else this