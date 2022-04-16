package de.dertyp7214.youtubemusicremote.types

import com.google.gson.annotations.SerializedName

enum class Action {
    @SerializedName("songInfo")
    SONG_INFO,

    @SerializedName("playPause")
    PLAY_PAUSE,

    @SerializedName("next")
    NEXT,

    @SerializedName("previous")
    PREVIOUS,

    @SerializedName("like")
    LIKE,

    @SerializedName("dislike")
    DISLIKE,

    @SerializedName("muteUnmute")
    MUTE_UNMUTE,

    @SerializedName("switchRepeat")
    SWITCH_REPEAT,

    @SerializedName("shuffle")
    SHUFFLE,

    @SerializedName("seek")
    SEEK
}

data class SeekData(
    val elapsedSeconds: Int
)