package de.dertyp7214.youtubemusicremote.core

fun String.toMillis(): Long = try {
    toLong() * 1000
} catch (_: Exception) {
    -1
}