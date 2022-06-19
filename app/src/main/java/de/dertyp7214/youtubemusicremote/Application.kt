package de.dertyp7214.youtubemusicremote

import android.app.Application as AndroidApplication

class Application : AndroidApplication() {
    companion object {
        init {
            System.loadLibrary("youtubemusicremote")
        }
    }
}

object Config {
    val PLAY_URL = { packageName: String ->
        "https://play.google.com/store/apps/details?id=$packageName"
    }
}