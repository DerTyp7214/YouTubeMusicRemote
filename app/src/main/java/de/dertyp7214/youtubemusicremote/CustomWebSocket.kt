package de.dertyp7214.youtubemusicremote

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class CustomWebSocket(
    url: String,
    webSocketListener: WebSocketListener,
    okHttpClient: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
) {
    private val webSocket: WebSocket =
        okHttpClient.newWebSocket(Request.Builder().url(url).build(), webSocketListener)

    fun send(data: Any) {
        webSocket.send(gson.toJson(data))
    }

    fun send(data: String) {
        webSocket.send(data)
    }
}