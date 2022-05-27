package de.dertyp7214.youtubemusicremote

import android.app.Application as AndroidApplication

class Application : AndroidApplication()

object Config {
    val PLAY_URL = { packageName: String ->
        "https://play.google.com/store/apps/details?id=$packageName"
    }
}