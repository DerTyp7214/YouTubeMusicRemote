package de.dertyp7214.youtubemusicremote.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.View.OnTouchListener
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import com.google.gson.Gson
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.components.CustomWebSocketListener
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.fragments.ControlsFragment
import de.dertyp7214.youtubemusicremote.fragments.CoverFragment
import de.dertyp7214.youtubemusicremote.types.*

class MainActivity : AppCompatActivity(), OnTouchListener {

    companion object {
        var URL = ""
            set(value) {
                var url = value
                if (!url.startsWith("ws://")) url = "ws://$url"
                field = url
            }
    }

    private val gson = Gson().newBuilder().enableComplexMapKeySerialization().create()

    private lateinit var webSocket: CustomWebSocket
    private lateinit var customWebSocketListener: CustomWebSocketListener

    private var currentSongInfo: SongInfo = SongInfo()
    private var oldSongInfo: SongInfo = SongInfo()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val controlFrame = findViewById<FrameLayout>(R.id.controlFrame)
        val mainFrame = findViewById<FrameLayout>(R.id.mainFrame)

        val group = findViewById<LinearLayout>(R.id.group)
        val volume = findViewById<TextView>(R.id.volume)
        val search = findViewById<ImageButton>(R.id.search)
        val menuButton = findViewById<ImageButton>(R.id.menuButton)

        customWebSocketListener = CustomWebSocketListener()

        val controlsFragment = ControlsFragment()
        val coverFragment = CoverFragment()

        val controlFragmentTransaction = supportFragmentManager.beginTransaction()
        controlFragmentTransaction.add(controlFrame.id, controlsFragment).commit()

        val coverFragmentTransaction = supportFragmentManager.beginTransaction()
        coverFragmentTransaction.add(mainFrame.id, coverFragment).commit()

        group.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            setMargins(0, getStatusBarHeight() + 32.dpToPx(this@MainActivity), 0, 0)
        }

        webSocket = CustomWebSocket(URL, customWebSocketListener, gson = gson)
        webSocket.setInstance()

        fun setSongInfo(songInfo: SongInfo) {
            val controlsColor = songInfo.coverData?.controlsColor ?: Color.BLACK

            volume.changeText("${songInfo.volume}%")
            volume.animateTextColor(controlsColor)

            menuButton.animateImageTintList(controlsColor)

            search.animateImageTintList(controlsColor)

            menuButton.setOnClickListener {
                showMenu(menuButton, songInfo)
            }
        }

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
            currentSongInfo.parseImageColorsAsync(this, oldSongInfo) {
                runOnUiThread {
                    setSongInfo(it)

                    controlsFragment.setSongInfo(it)
                    coverFragment.setSongInfo(it)

                    oldSongInfo = it
                }
            }
        }

        customWebSocketListener.onFailure { _, throwable, _ ->
            throwable.printStackTrace()
        }

        search.setOnClickListener {
            startActivity(Intent(this, YouTubeSearchActivity::class.java))
        }

        controlsFragment.passCallbacks(
            shuffle = { webSocket.send(SendAction(Action.SHUFFLE)) },
            previous = { webSocket.send(SendAction(Action.PREVIOUS)) },
            playPause = { webSocket.send(SendAction(Action.PLAY_PAUSE)) },
            next = { webSocket.send(SendAction(Action.NEXT)) },
            repeat = { webSocket.send(SendAction(Action.SWITCH_REPEAT)) },
            like = { webSocket.send(SendAction(Action.LIKE)) },
            dislike = { webSocket.send(SendAction(Action.DISLIKE)) },
            seek = { webSocket.send(SendAction(Action.SEEK, SeekData(it))) },
            volume = { webSocket.send(SendAction(Action.VOLUME, VolumeData(it))) }
        )
    }

    override fun onResume() {
        super.onResume()
        webSocket = CustomWebSocket(URL, customWebSocketListener, gson = gson)
        webSocket.setInstance()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean = true

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                changeVolume(currentSongInfo.volume - 5)
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                changeVolume(currentSongInfo.volume + 5)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun showMenu(v: View, songInfo: SongInfo) {
        PopupMenu(this, v).apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_toggle_mute -> {
                        webSocket.send(SendAction(Action.MUTE_UNMUTE))
                        true
                    }
                    R.id.menu_share -> {
                        startActivity(Intent.createChooser(Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, songInfo.url)
                            type = "text/plain"
                        }, null))
                        true
                    }
                    else -> false
                }
            }
            inflate(R.menu.main_menu)
            menu.findItem(R.id.menu_toggle_mute)
                ?.setTitle(if (songInfo.isMuted) R.string.unmute else R.string.mute)
        }.show()
    }

    private fun changeVolume(volume: Int) {
        currentSongInfo.volume = volume
        webSocket.send(SendAction(Action.VOLUME, VolumeData(currentSongInfo.volume)))
    }
}