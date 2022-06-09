package de.dertyp7214.youtubemusicremote.types

data class Playlists(
    val playlists: List<LibraryItem>,
    val recentActivity: List<LibraryItem>
)

data class LibraryItem(
    val index: Int,
    val title: String,
    val subtitle: String,
    val thumbnails: List<Thumbnail>,
    val playable: Boolean,

    @Transient
    var isPlaylist: Boolean = false
)

data class PlaylistContent(
    val index: Int,
    val videoId: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
    val artist: String
)