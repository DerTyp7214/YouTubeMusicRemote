package de.dertyp7214.youtubemusicremote.api

import android.graphics.drawable.Drawable

data class YouTubeSearchResponse(
    val kind: String,
    val etag: String,
    val nextPageToken: String,
    val regionCode: String,
    val pageInfo: PageInfo,
    val items: List<YouTubeSearchItem>
)

data class PageInfo(
    val totalResults: Long,
    val resultsPerPage: Int
)

data class YouTubeSearchItem(
    val kind: String,
    val etag: String,
    val id: YouTubeVideoId,
    val snippet: YouTubeVideoSnippet
)

data class YouTubeVideoId(
    val kind: String,
    val videoId: String
)

data class YouTubeVideoSnippet(
    val publishedAt: String,
    val channelId: String,
    val title: String,
    val description: String,
    val thumbnails: YouTubeThumbnails,
    val channelTitle: String,
    val liveBroadcastContent: String,
    val publishTime: String,
)

data class YouTubeThumbnails(
    val default: YouTubeThumbnail?,
    val medium: YouTubeThumbnail?,
    val high: YouTubeThumbnail?
)

data class YouTubeThumbnail(
    val url: String,
    val width: Int,
    val height: Int,

    @Transient
    var image: Drawable?
)