package de.dertyp7214.youtubemusicremote.components

import com.google.gson.Gson
import de.dertyp7214.youtubemusicremote.types.Action
import de.dertyp7214.youtubemusicremote.types.SeekData
import de.dertyp7214.youtubemusicremote.types.SendAction
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

class CustomWebSocket(
    url: String,
    val webSocketListener: CustomWebSocketListener,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
) {
    private val webSocket: WebSocket? =
        if (url == "devUrl") null else okHttpClient.newWebSocket(
            Request.Builder().url(url).build(),
            webSocketListener
        )

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

    fun close() {
        okHttpClient.dispatcher().executorService().shutdown()
        okHttpClient.connectionPool().evictAll()
        okHttpClient.cache()?.close()
    }

    fun setInstance() {
        webSocketInstance = this
    }

    companion object {
        var webSocketInstance: CustomWebSocket? = null
            private set
    }
}