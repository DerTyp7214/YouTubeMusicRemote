package de.dertyp7214.youtubemusicremote

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class CustomWebSocketListener : WebSocketListener() {
    private var onClosedCallback: (WebSocket, code: Int, reason: String) -> Unit = { _, _, _ -> }
    private var onClosingCallback: (WebSocket, code: Int, reason: String) -> Unit = { _, _, _ -> }
    private var onFailureCallback: (WebSocket, Throwable, Response?) -> Unit = { _, _, _ -> }
    private var onMessageByteCallback: (WebSocket, ByteString) -> Unit = { _, _ -> }
    private var onMessageCallback: (WebSocket, text: String) -> Unit = { _, _ -> }
    private var onOpenCallback: (WebSocket, Response) -> Unit = { _, _ -> }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        onClosedCallback(webSocket, code, reason)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        onClosingCallback(webSocket, code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onFailureCallback(webSocket, t, response)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        onMessageByteCallback(webSocket, bytes)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        onMessageCallback(webSocket, text)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        onOpenCallback(webSocket, response)
    }

    fun onClosed(callback: (WebSocket, code: Int, reason: String) -> Unit) {
        onClosedCallback = callback
    }

    fun onClosing(callback: (WebSocket, code: Int, reason: String) -> Unit) {
        onClosingCallback = callback
    }

    fun onFailure(callback: (WebSocket, Throwable, Response?) -> Unit) {
        onFailureCallback = callback
    }

    fun onMessageByte(callback: (WebSocket, ByteString) -> Unit) {
        onMessageByteCallback = callback
    }

    fun onMessage(callback: (WebSocket, text: String) -> Unit) {
        onMessageCallback = callback
    }

    fun onOpen(callback: (WebSocket, Response) -> Unit) {
        onOpenCallback = callback
    }
}