package de.dertyp7214.youtubemusicremote

import de.dertyp7214.audiovisualization.components.AudioVisualizationC
import de.dertyp7214.colorutilsc.ColorUtilsC
import de.dertyp7214.mathc.MathC
import android.app.Application as AndroidApplication

class Application : AndroidApplication() {
    companion object {
        init {
            System.loadLibrary("youtubemusicremote")
            AudioVisualizationC.init()
            ColorUtilsC.init()
            MathC.init()
        }
    }
}

object Config {
    val PLAY_URL = { packageName: String ->
        "https://play.google.com/store/apps/details?id=$packageName"
    }
}