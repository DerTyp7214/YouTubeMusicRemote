package de.dertyp7214.youtubemusicremote.core

import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener

fun SeekBar.onProgressChanged(callback: (progress: Int, userInput: Boolean) -> Unit) {
    setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            callback(p1, p2)
        }
    })
}