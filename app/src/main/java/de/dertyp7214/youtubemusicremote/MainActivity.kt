package de.dertyp7214.youtubemusicremote

import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import de.dertyp7214.youtubemusicremote.core.parseImageColorsAsync
import de.dertyp7214.youtubemusicremote.fragments.ControlsFragment
import de.dertyp7214.youtubemusicremote.fragments.CoverFragment
import de.dertyp7214.youtubemusicremote.types.*

class MainActivity : AppCompatActivity() {
    private val url = "ws://192.168.178.234:8080"

    private val gson = Gson().newBuilder().enableComplexMapKeySerialization().create()

    lateinit var webSocket: CustomWebSocket

    private var currentSongInfo: SongInfo = SongInfo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val controlFrame = findViewById<FrameLayout>(R.id.controlFrame)
        val mainFrame = findViewById<FrameLayout>(R.id.mainFrame)

        val customWebSocketListener = CustomWebSocketListener()

        val controlsFragment = ControlsFragment()
        val coverFragment = CoverFragment()

        val controlFragmentTransaction = supportFragmentManager.beginTransaction()
        controlFragmentTransaction.add(controlFrame.id, controlsFragment).commit()

        val coverFragmentTransaction = supportFragmentManager.beginTransaction()
        coverFragmentTransaction.add(mainFrame.id, coverFragment).commit()

        webSocket = CustomWebSocket(url, customWebSocketListener, gson = gson)

        customWebSocketListener.onMessage { _, text ->
            try {
                val socketResponse = gson.fromJson(text, SocketResponse::class.java)

                when (socketResponse.action) {
                    Action.SONG_INFO -> {
                        currentSongInfo = gson.fromJson(socketResponse.data, SongInfo::class.java)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            currentSongInfo.parseImageColorsAsync(this) {
                runOnUiThread {
                    controlsFragment.setSongInfo(it)
                    coverFragment.setSongInfo(it)
                }
            }
        }

        customWebSocketListener.onFailure { _, throwable, _ ->
            throwable.printStackTrace()
        }

        controlsFragment.passCallbacks(
            shuffle = { webSocket.send(SendAction(Action.SHUFFLE)) },
            previous = { webSocket.send(SendAction(Action.PREVIOUS)) },
            playPause = { webSocket.send(SendAction(Action.PLAY_PAUSE)) },
            next = { webSocket.send(SendAction(Action.NEXT)) },
            repeat = { webSocket.send(SendAction(Action.SWITCH_REPEAT)) },
            like = { webSocket.send(SendAction(Action.LIKE)) },
            dislike = { webSocket.send(SendAction(Action.DISLIKE)) },
            seek = { webSocket.send(SendAction(Action.SEEK, SeekData(it))) }
        )
    }
}