package de.dertyp7214.youtubemusicremote.components

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

@Suppress("unused")
class CustomWebSocketListener : WebSocketListener() {
    private var onClosedCallback: ArrayList<(WebSocket, code: Int, reason: String) -> Unit> =
        arrayListOf()
    private var onClosingCallback: ArrayList<(WebSocket, code: Int, reason: String) -> Unit> =
        arrayListOf()
    private var onFailureCallback: ArrayList<(WebSocket, Throwable, Response?) -> Unit> =
        arrayListOf()
    private var onMessageByteCallback: ArrayList<(WebSocket, ByteString) -> Unit> = arrayListOf()
    private var onMessageCallback: ArrayList<(WebSocket, text: String) -> Unit> = arrayListOf()
    private var onOpenCallback: ArrayList<(WebSocket, Response) -> Unit> = arrayListOf()

    private operator fun ArrayList<(WebSocket, Int, String) -> Unit>.invoke(
        webSocket: WebSocket,
        code: Int,
        reason: String
    ) = forEach { it(webSocket, code, reason) }

    private operator fun ArrayList<(WebSocket, Throwable, Response?) -> Unit>.invoke(
        webSocket: WebSocket,
        throwable: Throwable,
        response: Response?
    ) = forEach { it(webSocket, throwable, response) }

    private operator fun ArrayList<(WebSocket, ByteString) -> Unit>.invoke(
        webSocket: WebSocket,
        bytes: ByteString
    ) = forEach { it(webSocket, bytes) }

    private operator fun ArrayList<(WebSocket, String) -> Unit>.invoke(
        webSocket: WebSocket,
        text: String
    ) = forEach { it(webSocket, text) }

    private operator fun ArrayList<(WebSocket, Response) -> Unit>.invoke(
        webSocket: WebSocket,
        response: Response
    ) = forEach { it(webSocket, response) }

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
        onClosedCallback.add(callback)
    }

    fun onClosing(callback: (WebSocket, code: Int, reason: String) -> Unit) {
        onClosingCallback.add(callback)
    }

    fun onFailure(callback: (WebSocket, Throwable, Response?) -> Unit) {
        onFailureCallback.add(callback)
    }

    fun onMessageByte(callback: (WebSocket, ByteString) -> Unit) {
        onMessageByteCallback.add(callback)
    }

    fun onMessage(callback: (WebSocket, text: String) -> Unit) {
        onMessageCallback.add(callback)
    }

    fun onOpen(callback: (WebSocket, Response) -> Unit) {
        onOpenCallback.add(callback)
    }
}