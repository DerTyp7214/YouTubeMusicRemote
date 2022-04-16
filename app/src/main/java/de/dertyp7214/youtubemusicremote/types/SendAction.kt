package de.dertyp7214.youtubemusicremote.types

data class SendAction(
    val action: Action,
    val `data`: Any? = null
)
