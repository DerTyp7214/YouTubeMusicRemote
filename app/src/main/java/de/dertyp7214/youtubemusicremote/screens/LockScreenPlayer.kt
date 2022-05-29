package de.dertyp7214.youtubemusicremote.screens

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.BatteryManager
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
import de.dertyp7214.youtubemusicremote.core.preferences
import de.dertyp7214.youtubemusicremote.core.screenBounds
import de.dertyp7214.youtubemusicremote.types.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class LockScreenPlayer : AppCompatActivity() {

    private val coverData = MutableLiveData<CoverData>()
    private val songInfo = MutableLiveData<SongInfo>()
    private val timeData = MutableLiveData<String>()
    private val audioData = MutableLiveData<List<Short>>()

    private val gson = Gson().newBuilder().enableComplexMapKeySerialization().create()

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
        val bitmap = Bitmap.createBitmap(screenWidth, 50.dpToPx(this), Bitmap.Config.ARGB_8888)

        visualization.setImageBitmap(bitmap)

        audioData.observe(this) {
            drawOnBitmap(it.reversed() + it, bitmap)
            visualization.setImageDrawable(BitmapDrawable(resources, bitmap))
        }
    }

    private fun drawOnBitmap(audioData: List<Short>, bitmap: Bitmap) {
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        val barWidth = (width / audioData.size).roundToInt() - 2

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        canvas.drawRect(0f, 0f, 0f, 0f, Paint())

        val paint = Paint()
        paint.color = Color.WHITE
        paint.alpha = 128
        var i = 0
        while (i < audioData.size) {
            val barHeight = height / 256f * audioData[i]

            if (width - i.toFloat() * (barWidth + 2) > barWidth)
                canvas.drawRect(
                    i.toFloat() * (barWidth + 2),
                    height - barHeight,
                    width - (width - ((barWidth + 2) * i)) + barWidth,
                    height,
                    paint
                )

            i++
        }
    }
}