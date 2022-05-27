package de.dertyp7214.youtubemusicremote.types

data class Lyrics(
    val lyrics: String,
    val videoId: String,
    val title: String,
    val author: String
) {
    val isEmpty
        get() = this == Empty

    companion object {
        val Empty = Lyrics("", "", "", "")
    }
}
