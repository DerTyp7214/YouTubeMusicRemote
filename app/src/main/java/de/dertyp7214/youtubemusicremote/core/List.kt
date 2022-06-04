package de.dertyp7214.youtubemusicremote.core

import kotlin.math.roundToInt

fun List<Short>.changeSize(newSize: Int): List<Short> {
    val tmp = ArrayList<Short>()
    val factor = newSize.toFloat() / size
    if (factor > 1f) {
        forEach {
            for (i in 0 until factor.roundToInt()) if (tmp.isNotEmpty()) tmp.add((tmp.last() + it / 2).toShort())
            else tmp.add(it)
        }
    } else {
        val f1 = (size.toFloat() / newSize).roundToInt()
        forEachIndexed { index, _ ->
            if (index % f1 == 0) tmp.add(get(index))
        }
    }
    return tmp
}