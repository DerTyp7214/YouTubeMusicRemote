package de.dertyp7214.youtubemusicremote.types

data class Playlists(
    val index: Int,
    val title: String,
    val subtitle: String,
    val thumbnails: List<Thumbnail>
)

data class PlaylistContent(
    val index: Int,
    val videoId: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
    val artist: String
)