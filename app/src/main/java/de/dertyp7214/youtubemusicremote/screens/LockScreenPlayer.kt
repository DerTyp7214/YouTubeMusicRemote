package de.dertyp7214.youtubemusicremote.screens

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.types.CoverData
import de.dertyp7214.youtubemusicremote.types.SongInfo
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class LockScreenPlayer : AppCompatActivity() {

    private val coverData = MutableLiveData<CoverData>()
    private val songInfo = MutableLiveData<SongInfo>()
    private val timeData = MutableLiveData<String>()

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
}