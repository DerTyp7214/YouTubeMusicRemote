package de.dertyp7214.youtubemusicremote.types

import com.google.gson.JsonElement

data class SocketResponse(
    val action: Action,
    val `data`: JsonElement?
)