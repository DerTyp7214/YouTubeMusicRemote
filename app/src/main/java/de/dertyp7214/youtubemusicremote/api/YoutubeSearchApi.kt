package de.dertyp7214.youtubemusicremote.api

import com.google.gson.Gson
import de.dertyp7214.youtubemusicremote.core.doAsync
import java.net.URL
import java.net.URLEncoder

class YoutubeSearchApi(
    private val apiKey: String = "",
    private val gson: Gson = Gson()
) {
    fun search(
        q: String,
        maxResults: Int = 50,
        parts: List<String> = arrayListOf("id", "snippet"),
        safeSearch: String = "none",
        type: String = "video",
        videoCategory: String = "10",
        key: String = apiKey,
        filter: (YouTubeSearchItem) -> Boolean = { it.snippet.channelTitle.endsWith(" - Topic") }
    ): List<YouTubeSearchItem> {
        val builder = StringBuilder("https://youtube.googleapis.com/youtube/v3/search")
        parts.forEachIndexed { index, part ->
            builder.append("${if (index == 0) "?" else "&"}part=$part")
        }
        builder.append("&maxResults=$maxResults")
        builder.append("&q=${URLEncoder.encode(q, "utf-8")}")
        builder.append("&safeSearch=$safeSearch")
        builder.append("&type=$type")
        builder.append("&videoCategory=$videoCategory")
        builder.append("&key=$key")

        val response = URL(builder.toString()).readText()

        return try {
            val youtubeSearchResponse = gson.fromJson(response, YouTubeSearchResponse::class.java)
            youtubeSearchResponse.items.filter(filter).ifEmpty { youtubeSearchResponse.items }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf()
        }
    }

    fun searchAsync(
        q: String,
        maxResults: Int = 50,
        parts: List<String> = arrayListOf("id", "snippet"),
        safeSearch: String = "none",
        type: String = "video",
        videoCategory: String = "10",
        key: String = apiKey,
        callback: (List<YouTubeSearchItem>) -> Unit
    ) {
        doAsync({ search(q, maxResults, parts, safeSearch, type, videoCategory, key) }, callback)
    }
}