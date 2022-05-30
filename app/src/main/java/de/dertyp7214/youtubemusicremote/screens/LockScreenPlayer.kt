package de.dertyp7214.youtubemusicremote.screens

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
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
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.core.dpToPx
import de.dertyp7214.youtubemusicremote.core.easeInQuad
import de.dertyp7214.youtubemusicremote.core.preferences
import de.dertyp7214.youtubemusicremote.core.screenBounds
import de.dertyp7214.youtubemusicremote.types.*
import java.text.SimpleDateFormat
import java.util.*
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
    private val audioData = MutableLiveData<List<Short>>()

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

        val visualization: ImageView = findViewById(R.id.visualization)

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

        if (preferences.getBoolean("visualizeAudio", false))
            CustomWebSocket.webSocketInstance?.webSocketListener?.apply {
                onMessage { _, text ->
                    try {
                        val socketResponse = gson.fromJson(text, SocketResponse::class.java)

                        when (socketResponse.action) {
                            Action.AUDIO_DATA -> {
                                val audioDataData = gson.fromJson(
                                    text, AudioDataData::class.java
                                )
                                audioData.postValue(audioDataData.data)
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

        val screenWidth = screenBounds.width()
        val bitmap =
            Bitmap.createBitmap(screenWidth, 150.dpToPx(this), Bitmap.Config.ARGB_8888)

        visualization.setImageBitmap(bitmap)

        audioData.observe(this) {
            drawOnBitmap(it, bitmap)
            visualization.setImageDrawable(BitmapDrawable(resources, bitmap))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        windowInsets = window.decorView.rootWindowInsets
    }

    private fun drawOnBitmap(audioData: List<Short>, bitmap: Bitmap) {
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        val barWidth = (width / audioData.size).roundToInt() - 1

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        canvas.drawRect(0f, 0f, 0f, 0f, Paint())

        val paint = Paint()
        paint.color = Color.WHITE
        paint.alpha = 128
        var i = 0
        while (i < audioData.size) {
            val barHeight = height / 256f * (audioData[i] * .2f)

            val x = i.toFloat() * (barWidth + 2)

            val cornerBottomSpace = if (::windowInsets.isInitialized && barHeight != 0f) {
                val leftRadius = bottomLeftCorner.radius * 1.1f
                val rightRadius = bottomRightCorner.radius * 1.1f
                if (x + barWidth < bottomLeftCorner.center.x + bottomLeftCorner.radius * .1f) {
                    val point = leftRadius - x
                    point.easeInQuad(1f / leftRadius * point)
                } else if (x - barWidth > (bottomRightCorner.center.x - bottomRightCorner.radius * .1f)) {
                    val point = x - bottomRightCorner.center.x + bottomRightCorner.radius * .1f
                    point.easeInQuad(1f / rightRadius * point)
                } else 0f
            } else 0f

            canvas.drawRect(
                x,
                height - barHeight - cornerBottomSpace,
                width - (width - ((barWidth + 2) * i)) + barWidth,
                height,
                paint
            )

            i++
        }
    }
}