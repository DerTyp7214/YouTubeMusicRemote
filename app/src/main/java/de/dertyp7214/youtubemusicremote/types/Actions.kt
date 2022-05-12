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
    SEEK,

    @SerializedName("volume")
    VOLUME,

    @SerializedName("videoId")
    VIDEO_ID,

    @SerializedName("status")
    STATUS,

    @SerializedName("queue")
    QUEUE,

    @SerializedName("requestQueue")
    REQUEST_QUEUE,

    @SerializedName("queueVideoId")
    QUEUE_VIDEO_ID,

    @SerializedName("lyrics")
    LYRICS,

    @SerializedName("requestLyrics")
    REQUEST_LYRICS
}

@Suppress("unused")
enum class SongInfoAction {
    @SerializedName("default")
    DEFAULT,

    @SerializedName("video-src-changed")
    VIDEO_SRC_CHANGED,

    @SerializedName("playPaused")
    PLAY_PAUSED,

    @SerializedName("playerStatus")
    PLAYER_STATUS,

    @SerializedName("elapsedSecondsChanged")
    ELAPSED_SECONDS_CHANGED,

    @SerializedName("volumeChange")
    VOLUME_CHANGE
}

data class VideoIdData(
    val videoId: String
)

data class StatusData(
    val name: String
)

data class SeekData(
    val elapsedSeconds: Int
)

data class VolumeData(
    val volume: Int
)

data class LyricsData(
    val lyrics: String
)