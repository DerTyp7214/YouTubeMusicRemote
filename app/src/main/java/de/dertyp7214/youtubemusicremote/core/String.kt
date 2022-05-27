package de.dertyp7214.youtubemusicremote.core

import kotlin.random.Random

fun String.toMillis(): Long = try {
    toLong() * 1000
} catch (_: Exception) {
    -1
}

@Suppress("unused")
fun String.Companion.random(maxLength: Int = 16): String {
    val randomStringBuilder = StringBuilder()
    val randomLength: Int = Random.nextInt(maxLength)
    for (i in 0 until randomLength) {
        randomStringBuilder.append((Random.nextInt(96) + 32))
    }
    return randomStringBuilder.toString()
}