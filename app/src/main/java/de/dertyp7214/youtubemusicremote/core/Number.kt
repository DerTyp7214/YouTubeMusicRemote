@file:Suppress("unused")

package de.dertyp7214.youtubemusicremote.core

import android.content.Context
import de.dertyp7214.youtubemusicremote.R
import java.util.concurrent.TimeUnit

fun String.toHumanReadable(isSeconds: Boolean = false): String {
    return try {
        toLong().toHumanReadable(isSeconds)
    } catch (_: Exception) {
        this
    }
}

fun Long.toHumanReadableTime(context: Context? = null, includeSeconds: Boolean = false): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(hours)
    val seconds =
        TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(
            hours
        )

    val hrsSuffix = if (hours == 1L) context?.getString(R.string.hr) ?: "hr"
    else context?.getString(R.string.hrs) ?: "hrs"
    val minSuffix = if (minutes == 1L) context?.getString(R.string.min) ?: "min"
    else context?.getString(R.string.mins) ?: "mins"
    val secSuffix = context?.getString(R.string.s) ?: "s"

    return if (includeSeconds) {
        if (hours > 0) "$hours$hrsSuffix ${
            minutes.let {
                if (it == 0L) "" else "$it$minSuffix"
            }
        } ${
            seconds.let {
                if (it == 0L) "" else "it$secSuffix"
            }
        }"
        else if (minutes > 0) "$minutes$minSuffix ${
            seconds.let {
                if (it == 0L) "" else "$it$secSuffix"
            }
        }"
        else "$seconds$secSuffix"
    } else {
        if (hours > 0) "$hours$hrsSuffix ${
            minutes.let {
                if (it == 0L) "" else "$it$minSuffix"
            }
        }"
        else if (minutes > 0) "$minutes$minSuffix"
        else "$seconds$secSuffix"
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

fun <T: Number> T.clamp(min: T, max: T) =
    if (this.toFloat() > max.toFloat()) max else if (this.toFloat() < min.toFloat()) min else this