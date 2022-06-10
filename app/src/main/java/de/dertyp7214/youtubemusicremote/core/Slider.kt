package de.dertyp7214.youtubemusicremote.core

import android.content.res.ColorStateList
import com.google.android.material.slider.Slider

fun Slider.onProgressChanged(callback: (progress: Int, userInput: Boolean) -> Unit) {
    addOnChangeListener { _, value, fromUser ->
        callback(value.toInt(), fromUser)
    }
}

fun Slider.setColor(color: Int) {
    val stateList = ColorStateList.valueOf(color)

    thumbStrokeColor = stateList
    thumbTintList = stateList
    trackActiveTintList = stateList
    tickTintList = stateList
    haloTintList = stateList
    trackInactiveTintList = stateList.withAlpha(60)
}