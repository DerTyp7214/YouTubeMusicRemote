package de.dertyp7214.youtubemusicremote.core

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.widget.SwitchCompat
import de.dertyp7214.colorutilsc.ColorUtilsC

fun SwitchCompat.setColor(color: Int) {
    val thumbStates = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        ),
        intArrayOf(
            Color.DKGRAY,
            color,
            Color.LTGRAY
        )
    )
    thumbTintList = thumbStates
    val trackStates = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        ),
        intArrayOf(
            Color.LTGRAY,
            ColorUtilsC.setAlphaComponent(ColorUtilsC.blendARGB(color, Color.DKGRAY, .7f), 150),
            Color.TRANSPARENT
        )
    )
    trackTintList = trackStates
}