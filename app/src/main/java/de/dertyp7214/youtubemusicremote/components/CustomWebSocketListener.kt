package de.dertyp7214.youtubemusicremote.components

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

fun interface WebSocketCallbackA {
    operator fun invoke(webSocket: WebSocket, code: Int, reason: String)
}

fun interface WebSocketCallbackB {
    operator fun invoke(webSocket: WebSocket, throwable: Throwable, response: Response?)
}

fun interface WebSocketCallbackC {
    operator fun invoke(webSocket: WebSocket, bytes: ByteString)
}

fun interface WebSocketCallbackD {
    operator fun invoke(webSocket: WebSocket, text: String)
}

fun interface WebSocketCallbackE {
    operator fun invoke(webSocket: WebSocket, response: Response)
}

@Suppress("unused")
class CustomWebSocketListener : WebSocketListener() {
    private var onClosedCallback: ArrayList<WebSocketCallbackA> =
        arrayListOf()
    private var onClosingCallback: ArrayList<WebSocketCallbackA> =
        arrayListOf()
    private var onFailureCallback: ArrayList<WebSocketCallbackB> =
        arrayListOf()
    private var onMessageByteCallback: ArrayList<WebSocketCallbackC> = arrayListOf()
    private var onMessageCallback: ArrayList<WebSocketCallbackD> = arrayListOf()
    private var onOpenCallback: ArrayList<WebSocketCallbackE> = arrayListOf()

    private operator fun ArrayList<WebSocketCallbackA>.invoke(
        webSocket: WebSocket,
        code: Int,
        reason: String
    ) = forEach { it(webSocket, code, reason) }

    private operator fun ArrayList<WebSocketCallbackB>.invoke(
        webSocket: WebSocket,
        throwable: Throwable,
        response: Response?
    ) = forEach { it(webSocket, throwable, response) }

    private operator fun ArrayList<WebSocketCallbackC>.invoke(
        webSocket: WebSocket,
        bytes: ByteString
    ) = forEach { it(webSocket, bytes) }

    private operator fun ArrayList<WebSocketCallbackD>.invoke(
        webSocket: WebSocket,
        text: String
    ) = forEach { it(webSocket, text) }

    private operator fun ArrayList<WebSocketCallbackE>.invoke(
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

    fun onClosed(callback: WebSocketCallbackA) = onClosedCallback.add(callback)
    fun onClosing(callback: WebSocketCallbackA) = onClosingCallback.add(callback)
    fun onFailure(callback: WebSocketCallbackB) = onFailureCallback.add(callback)
    fun onMessageByte(callback: WebSocketCallbackC) = onMessageByteCallback.add(callback)
    fun onMessage(callback: WebSocketCallbackD) = onMessageCallback.add(callback)
    fun onOpen(callback: WebSocketCallbackE) = onOpenCallback.add(callback)
}