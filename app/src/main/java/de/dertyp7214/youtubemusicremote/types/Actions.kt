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

    @SerializedName("audioData")
    AUDIO_DATA,

    @SerializedName("requestPlaylists")
    REQUEST_PLAYLISTS,

    @SerializedName("requestPlaylist")
    REQUEST_PLAYLIST,

    @SerializedName("playlists")
    PLAYLISTS,

    @SerializedName("playlist")
    PLAYLIST,

    @SerializedName("playPlaylist")
    PLAY_PLAYLIST,

    @SerializedName("search")
    SEARCH,

    @SerializedName("searchMainResults")
    SEARCH_MAIN_RESULT,

    @SerializedName("showShelf")
    SHOW_SHELF,

    @SerializedName("showShelfResults")
    SHOW_SHELF_RESULTS,

    @SerializedName("playSearchSong")
    PLAY_SEARCH_SONG,

    @SerializedName("openPlayer")
    OPEN_PLAYER,

    @SerializedName("searchContextMenu")
    SEARCH_CONTEXT_MENU,

    @SerializedName("playlistContextMenu")
    PLAYLIST_CONTEXT_MENU,

    @SerializedName("selectSearchTab")
    SELECT_SEARCH_TAB
}

enum class ContextAction {
    @SerializedName("radio")
    RADIO,

    @SerializedName("next")
    NEXT,

    @SerializedName("queue")
    QUEUE
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

data class AudioDataData(
    val data: List<Short>
)

data class RequestPlaylistData(
    val index: Int
)

data class PlayPlaylistData(
    val shuffle: Boolean,
    val index: Int
)

data class SearchData(
    val query: String
)

data class SearchMainResultData(
    val index: Int,
    val title: String,
    val type: String,
    val entries: List<EntryData>,
    val showAll: Boolean
)

data class EntryData(
    val index: Int,
    val title: String,
    val type: String,
    val subTitle: List<String>,
    val thumbnails: List<Thumbnail>
)

data class ShowShelfData(
    val index: Int
)

data class ShowShelfResultData(
    val index: Int,
    val title: String,
    val subTitle: List<String>,
    val thumbnails: List<Thumbnail>
)

data class PlaySearchSongData(
    val index: Int,
    val shelf: Int?
)

data class Thumbnail(
    val url: String,
    val width: Int,
    val height: Int
)

data class SearchContextMenuData(
    val action: ContextAction,
    val index: Int,
    val shelf: Int?
)

data class PlaylistContextMenuData(
    val action: ContextAction,
    val index: Int,
    val song: Boolean
)

data class SelectSearchTabData(
    val index: Int
)