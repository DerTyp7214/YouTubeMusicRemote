package de.dertyp7214.youtubemusicremote.core

import androidx.core.graphics.ColorUtils
import com.google.android.material.progressindicator.LinearProgressIndicator

fun LinearProgressIndicator.setColor(color: Int) {
   val trackColor = ColorUtils.setAlphaComponent(color, 60)
   setIndicatorColor(color, color, color)
   setTrackColor(trackColor)
}