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

    val trackColor = ColorUtilsC.blendARGB(color, Color.WHITE, .5f).let { newColor ->
        fun checkColor(trackColor: Int, oldColor: Int = color): Int {
            val diff = ColorUtilsC.calculateColorDifference(newColor, trackColor)
            return when {
                diff > 20 -> trackColor
                trackColor == oldColor -> ColorUtilsC.blendARGB(color, Color.BLACK, .7f)
                else -> checkColor(ColorUtilsC.blendARGB(trackColor, Color.WHITE, .5f), trackColor)
            }
        }
        checkColor(newColor)
    }

    val trackStates = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        ),
        intArrayOf(
            Color.LTGRAY,
            ColorUtilsC.setAlphaComponent(trackColor, 150),
            Color.TRANSPARENT
        )
    )
    trackTintList = trackStates
}