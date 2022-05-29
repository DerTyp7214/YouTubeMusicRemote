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
    REQUEST_LYRICS,

    @SerializedName("startQueueItemRadio")
    START_QUEUE_ITEM_RADIO,

    @SerializedName("playQueueItemNext")
    PLAY_QUEUE_ITEM_NEXT,

    @SerializedName("addQueueItemToQueue")
    ADD_QUEUE_ITEM_TO_QUEUE,

    @SerializedName("removeQueueItemFromQueue")
    REMOVE_QUEUE_ITEM_FROM_QUEUE,

    @SerializedName("search")
    SEARCH,

    @SerializedName("audioData")
    AUDIO_DATA
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

data class QueueData(
    val videoId: String
)

class RemoveQueueData(
    val videoId: String,
    val position: Int
)

data class SearchData(
    val query: String
)

data class AudioDataData(
    val data: List<Short>
)