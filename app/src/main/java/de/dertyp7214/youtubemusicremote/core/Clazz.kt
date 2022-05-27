@file:Suppress("unused")

package de.dertyp7214.youtubemusicremote.core

fun <T, R> Class<T>.at(block: T.() -> R) = block(newInstance())