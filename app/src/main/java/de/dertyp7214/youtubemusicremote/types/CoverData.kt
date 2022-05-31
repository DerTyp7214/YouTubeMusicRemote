package de.dertyp7214.youtubemusicremote.types

import android.graphics.Color
import android.graphics.drawable.Drawable

data class CoverData(
    var background: Drawable? = null,
    var cover: Drawable? = null,
    var dominant: Int = Color.TRANSPARENT,
    var parsedDominant: Int = Color.TRANSPARENT,
    var vibrant: Int = Color.TRANSPARENT,
    var darkVibrant: Int = Color.TRANSPARENT,
    var lightVibrant: Int = Color.TRANSPARENT,
    var muted: Int = Color.TRANSPARENT,
    var darkMuted: Int = Color.TRANSPARENT,
    var lightMuted: Int = Color.TRANSPARENT
)