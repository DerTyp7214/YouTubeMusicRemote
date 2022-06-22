package de.dertyp7214.youtubemusicremote.core

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import com.google.android.material.switchmaterial.SwitchMaterial

fun SwitchMaterial.setColor(color: Int) {
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
        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(
            Color.TRANSPARENT,
            Color.TRANSPARENT
        )
    )
    trackTintList = trackStates
    trackTintMode = PorterDuff.Mode.OVERLAY
}