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
    val fields: List<Field> = listOf(),
    val isMuted: Boolean = false,
    val action: SongInfoAction = SongInfoAction.DEFAULT,

    @Transient
    var coverData: CoverData? = null
) {
    infix fun almostEquals(other: Any?): Boolean {
        return other is SongInfo
                && other.fields == fields
                && other.title == title
                && other.artist == artist
                && other.coverData == coverData
                && other.isPaused == isPaused
                && other.album == album
                && other.liked == liked
                && other.disliked == disliked
                && other.repeatMode == repeatMode
                && other.videoId == videoId
    }
}

data class Field(
    val text: String,
    val link: String
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