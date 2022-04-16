package de.dertyp7214.youtubemusicremote.core

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
