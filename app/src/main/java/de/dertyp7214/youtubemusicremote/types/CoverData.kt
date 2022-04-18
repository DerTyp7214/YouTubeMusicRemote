package de.dertyp7214.youtubemusicremote.types

import android.graphics.drawable.Drawable

data class CoverData(
    var background: Drawable? = null,
    var cover: Drawable? = null,
    var dominant: Int = -1,
    var parsedDominant: Int = -1,
    var vibrant: Int = -1,
    var darkVibrant: Int = -1,
    var lightVibrant: Int = -1,
    var muted: Int = -1,
    var darkMuted: Int = -1,
    var lightMuted: Int = -1
)