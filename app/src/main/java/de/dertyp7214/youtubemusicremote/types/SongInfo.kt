package de.dertyp7214.youtubemusicremote.types

import com.google.gson.annotations.SerializedName

data class SongInfo(
    val title: String = "",
    val artist: String = "",
    val views: String = "",
    val uploadDate: String = "",
    val imageSrc: String = "",
    val image: Image = Image(),
    val isPaused: Boolean? = null,
    val songDuration: String = "",
    val elapsedSeconds: Int = 0,
    val url: String = "",
    val album: String = "",
    val videoId: String = "",
    val playlistId: String = "",
    val liked: Boolean = false,
    val disliked: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    var volume: Int = 0,
    val isMuted: Boolean = false,

    @Transient
    var coverData: CoverData? = null
)

data class Image(
    val isMacTemplate: Boolean = false
)

enum class RepeatMode {
    @SerializedName("NONE")
    NONE,

    @SerializedName("ALL")
    ALL,

    @SerializedName("ONE")
    ONE
}