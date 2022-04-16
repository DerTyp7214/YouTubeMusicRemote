package de.dertyp7214.youtubemusicremote.types

import com.google.gson.annotations.SerializedName
import de.dertyp7214.youtubemusicremote.core.CoverData

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