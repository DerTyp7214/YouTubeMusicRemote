package de.dertyp7214.youtubemusicremote.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.*

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

fun View.setHeight(height: Int) {
    updateLayoutParams {
        this.height = height
    }
}

fun View.getMargins(): Margins {
    return if (layoutParams is MarginLayoutParams) {
        (layoutParams as MarginLayoutParams).let {
            Margins(it.leftMargin, it.topMargin, it.rightMargin, it.bottomMargin)
        }
    } else Margins(marginLeft, marginTop, marginRight, marginBottom)
}

fun View.getBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}