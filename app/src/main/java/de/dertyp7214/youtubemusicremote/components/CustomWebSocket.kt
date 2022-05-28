package de.dertyp7214.youtubemusicremote.components

import com.google.gson.Gson
import de.dertyp7214.youtubemusicremote.types.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

class CustomWebSocket(
    private val url: String,
    val webSocketListener: CustomWebSocketListener,
    private val gson: Gson = Gson()
) {
    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null

    fun setUp(): CustomWebSocket {
        okHttpClient = OkHttpClient.Builder().build()
        webSocket = if (url == "devUrl" || url.isBlank()) null else okHttpClient?.newWebSocket(
            Request.Builder().url(url).build(),
            webSocketListener
        )
        return this
    }

    fun reconnect(): CustomWebSocket {
        close()
        setUp()
        return this
    }

    fun send(data: Any) {
        webSocket?.send(gson.toJson(data))
    }

    fun previous() {
        send(SendAction(Action.PREVIOUS))
    }

    fun playPause() {
        send(SendAction(Action.PLAY_PAUSE))
    }

    fun next() {
        send(SendAction(Action.NEXT))
    }

    fun seek(elapsedSeconds: Int) {
        send(SendAction(Action.SEEK, SeekData(elapsedSeconds)))
    }

    fun like() {
        send(SendAction(Action.LIKE))
    }

    fun dislike() {
        send(SendAction(Action.DISLIKE))
    }

    fun repeat() {
        send(SendAction(Action.SWITCH_REPEAT))
    }

    fun shuffle() {
        send(SendAction(Action.SHUFFLE))
    }

    fun volume(volume: Int) {
        send(SendAction(Action.VOLUME, VolumeData(volume)))
    }

    fun startQueueItemRadio(videoId: String) {
        send(SendAction(Action.START_QUEUE_ITEM_RADIO, QueueData(videoId)))
    }

    fun playQueueItemNext(videoId: String) {
        send(SendAction(Action.PLAY_QUEUE_ITEM_NEXT, QueueData(videoId)))
    }

    fun addQueueItemToQueue(videoId: String) {
        send(SendAction(Action.ADD_QUEUE_ITEM_TO_QUEUE, QueueData(videoId)))
    }

    fun removeQueueItemFromQueue(videoId: String, position: Int) {
        send(SendAction(Action.REMOVE_QUEUE_ITEM_FROM_QUEUE, RemoveQueueData(videoId, position)))
    }

    fun search(query: String) {
        send(SendAction(Action.SEARCH, SearchData(query)))
    }

    fun close() {
        okHttpClient?.dispatcher?.executorService?.shutdown()
        okHttpClient?.connectionPool?.evictAll()
        okHttpClient?.cache?.close()
        webSocket?.cancel()
    }

    fun setInstance() {
        webSocketInstance?.close()
        webSocketInstance = this
    }

    companion object {
        var webSocketInstance: CustomWebSocket? = null
            private set
    }
}