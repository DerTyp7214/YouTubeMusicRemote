package de.dertyp7214.youtubemusicremote.screens

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Point
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.View.GONE
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import de.dertyp7214.audiovisualization.components.AudioVisualizerView
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.core.customLockscreenVisualizeAudio
import de.dertyp7214.youtubemusicremote.core.customLockscreenVisualizeAudioSize
import de.dertyp7214.youtubemusicremote.types.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

class LockScreenPlayer : AppCompatActivity() {

    private class MRoundedCorner(windowInsets: WindowInsets, position: Int) {
        val position: Int
        val radius: Int
        val center: Point

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val cornerRadius = windowInsets.getRoundedCorner(position)
                this.position = cornerRadius?.position ?: 0
                this.radius = cornerRadius?.radius ?: 0
                this.center = cornerRadius?.center ?: Point(0, 0)
            } else {
                this.position = 0
                this.radius = 0
                this.center = Point(0, 0)
            }
        }

        val corner: AudioVisualizerView.Corner
            get() = AudioVisualizerView.Corner(radius)

        override fun toString(): String {
            return "[position:$position,radius:$radius,center:$center]"
        }

        companion object {
            const val POSITION_BOTTOM_LEFT = 3
            const val POSITION_BOTTOM_RIGHT = 2
            const val POSITION_TOP_LEFT = 0
            const val POSITION_TOP_RIGHT = 1
        }
    }

    private val coverData = MutableLiveData<CoverData>()
    private val songInfo = MutableLiveData<SongInfo>()
    private val timeData = MutableLiveData<String>()

    private val gson = Gson().newBuilder().enableComplexMapKeySerialization().create()

    private lateinit var windowInsets: WindowInsets

    private val bottomLeftCorner by lazy {
        MRoundedCorner(
            windowInsets,
            MRoundedCorner.POSITION_BOTTOM_LEFT
        )
    }
    private val bottomRightCorner by lazy {
        MRoundedCorner(
            windowInsets,
            MRoundedCorner.POSITION_BOTTOM_RIGHT
        )
    }

    private val visualization by lazy { findViewById<AudioVisualizerView>(R.id.visualization) }

    private val getSongInfo
        get() = songInfo.value ?: SongInfo()

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_screen_player)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        window.insetsController?.hide(WindowInsets.Type.navigationBars())

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val cover: ImageView = findViewById(R.id.cover)
        val title: TextView = findViewById(R.id.title)
        val artist: TextView = findViewById(R.id.artist)
        val time: TextView = findViewById(R.id.time)
        val battery: TextView = findViewById(R.id.battery)

        val prevButton: ImageButton = findViewById(R.id.prevButton)
        val playPauseButton: ImageButton = findViewById(R.id.playPauseButton)
        val nextButton: ImageButton = findViewById(R.id.nextButton)

        prevButton.setOnClickListener {
            CustomWebSocket.webSocketInstance?.previous()
        }
        playPauseButton.setOnClickListener {
            CustomWebSocket.webSocketInstance?.playPause()
        }
        nextButton.setOnClickListener {
            CustomWebSocket.webSocketInstance?.next()
        }

        fun timeCheck() {
            val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            if (timeData.value != timeString) timeData.postValue(timeString)
        }

        val batteryReceiver = object : BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val percentage = (level * 100 / scale.toFloat()).roundToInt()
                battery.text = "$percentage%"
            }
        }

        val timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                timeCheck()
            }
        }

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        registerReceiver(timeReceiver, IntentFilter(Intent.ACTION_TIME_TICK))

        timeCheck()

        if (customLockscreenVisualizeAudio)
            CustomWebSocket.webSocketInstance?.webSocketListener?.apply {
                onMessage { _, text ->
                    try {
                        val socketResponse = gson.fromJson(text, SocketResponse::class.java)

                        when (socketResponse.action) {
                            Action.AUDIO_DATA -> {
                                val audioDataData = gson.fromJson(
                                    text, AudioDataData::class.java
                                )
                                val audioArray = audioDataData.data
                                visualization.setAudioData(audioArray, true)
                            }
                            else -> {}
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        else visualization.visibility = GONE

        MainActivity.currentSongInfo.observe(this) {
            if (coverData.value != it.coverData && it.coverData != null) coverData.postValue(it.coverData!!)
            if (
                it.title != getSongInfo.title ||
                it.artist != getSongInfo.artist ||
                it.isPaused != getSongInfo.isPaused
            ) songInfo.postValue(it)
        }

        songInfo.observe(this) {
            title.text = it.title
            artist.text = it.artist

            if (it.isPaused == true) playPauseButton.setImageResource(R.drawable.ic_play)
            else playPauseButton.setImageResource(R.drawable.ic_pause)
        }

        coverData.observe(this) {
            cover.setImageDrawable(it.cover)
        }

        timeData.observe(this) {
            time.text = it
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        windowInsets = window.decorView.rootWindowInsets

        visualization.size = 2f.pow(customLockscreenVisualizeAudioSize).roundToInt()
        visualization.setBottomLeftCorner(bottomLeftCorner.corner)
        visualization.setBottomRightCorner(bottomRightCorner.corner)
    }
}