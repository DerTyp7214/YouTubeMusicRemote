package de.dertyp7214.youtubemusicremote.core

import android.view.View
import android.view.ViewGroup.MarginLayoutParams

data class Margins(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

fun View.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
    if (layoutParams is MarginLayoutParams) {
        (layoutParams as MarginLayoutParams).setMargins(left, top, right, bottom)
        requestLayout()
    }
}

fun View.getMargins(): Margins {
    return if (layoutParams is MarginLayoutParams) {
        (layoutParams as MarginLayoutParams).let {
            Margins(it.leftMargin, it.topMargin, it.rightMargin, it.bottomMargin)
        }
    } else Margins(0, 0, 0, 0)
}