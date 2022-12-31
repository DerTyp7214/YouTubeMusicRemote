package de.dertyp7214.youtubemusicremote.screens

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson
import de.dertyp7214.audiovisualization.components.AudioVisualizerView
import de.dertyp7214.colorutilsc.ColorUtilsC
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.core.clamp
import de.dertyp7214.youtubemusicremote.core.customLockscreenBrightness
import de.dertyp7214.youtubemusicremote.core.customLockscreenVisualizeAudio
import de.dertyp7214.youtubemusicremote.core.customLockscreenVisualizeAudioSize
import de.dertyp7214.youtubemusicremote.core.dpToPx
import de.dertyp7214.youtubemusicremote.core.equalsIgnoreOrder
import de.dertyp7214.youtubemusicremote.core.getFallBackColor
import de.dertyp7214.youtubemusicremote.core.setMargins
import de.dertyp7214.youtubemusicremote.core.toHumanReadableTime
import de.dertyp7214.youtubemusicremote.services.NotificationService
import de.dertyp7214.youtubemusicremote.types.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

class LockScreenPlayer : AppCompatActivity(), SensorEventListener {
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

    private val pixelShift = MutableLiveData<Int>()

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

    private val getCoverData
        get() = coverData.value ?: CoverData()

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    private val notificationList = mutableListOf<StatusBarNotification>()
    private val adapter by lazy { NotificationAdapter(notificationList) }
    private val volumeSlider by lazy { findViewById<LinearProgressIndicator>(R.id.volume) }

    private val sensorManager by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val proximitySensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) }

    private val isClose = MutableLiveData<Boolean>()
    private val getIsClose
        get() = isClose.value ?: false

    @SuppressLint("NotifyDataSetChanged")
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

        val container = findViewById<View>(R.id.container)

        val recyclerViewNotifications = findViewById<RecyclerView>(R.id.notifications)

        val cover: ImageView = findViewById(R.id.cover)
        val title: TextView = findViewById(R.id.title)
        val artist: TextView = findViewById(R.id.artist)
        val time: TextView = findViewById(R.id.time)
        val battery: TextView = findViewById(R.id.battery)

        val prevButton: ImageButton = findViewById(R.id.prevButton)
        val playPauseButton: ImageButton = findViewById(R.id.playPauseButton)
        val nextButton: ImageButton = findViewById(R.id.nextButton)
        val likeButton: ImageButton = findViewById(R.id.like)
        val dislikeButton: ImageButton = findViewById(R.id.dislike)

        val card = cover.parent as? CardView

        container.alpha = customLockscreenBrightness / 100f

        recyclerViewNotifications.adapter = adapter

        cover.setOnClickListener {
            finish()
        }
        prevButton.setOnClickListener {
            CustomWebSocket.webSocketInstance?.previous()
        }
        playPauseButton.setOnClickListener {
            CustomWebSocket.webSocketInstance?.playPause()
        }
        nextButton.setOnClickListener {
            CustomWebSocket.webSocketInstance?.next()
        }
        likeButton.setOnClickListener {
            CustomWebSocket.webSocketInstance?.like()
        }
        dislikeButton.setOnClickListener {
            CustomWebSocket.webSocketInstance?.dislike()
        }

        pixelShift.observe(this) {
            val dp = 1.dpToPx(this)
            val top = if (it == 0) dp else 0
            val right = if (it == 1) dp else 0
            val bottom = if (it == 2) dp else 0
            val left = if (it == 3) dp else 0

            container.setMargins(top, right, bottom, left)
        }

        fun timeCheck() {
            val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            if (timeData.value != timeString) {
                timeData.postValue(timeString)
                pixelShift.postValue(pixelShift.value.let {
                    if (it == null || it > 2) 0 else it + 1
                })
            }
        }

        val batteryReceiver = object : BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                val percentage = (level * 100 / scale.toFloat()).roundToInt()

                val batteryManager = getSystemService(BatteryManager::class.java)
                val status = batteryManager?.computeChargeTimeRemaining()

                val current =
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                val chargingStatus =
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)

                val isPlugged = plugged != 0
                val isFull = chargingStatus == BatteryManager.BATTERY_STATUS_FULL
                val isCharging = current > 0
                val isChargingFast = current > 1.6e+6

                val charging = when {
                    isFull -> getString(R.string.full)
                    isChargingFast -> getString(R.string.charging_rapidly)
                    isCharging -> getString(R.string.charging)
                    isPlugged -> getString(R.string.plugged)
                    else -> ""
                }

                battery.text = "$percentage%".let {
                    if (charging.isNotEmpty()) "$it ・ $charging" else it
                }.let {
                    if (!isFull && isCharging && status != null && status > 0) "$it ・ ${getString(R.string.full_in)} ${status.toHumanReadableTime()}"
                    else if (!isFull && isPlugged && current < 0) "$it ・ ${getString(R.string.discharging)}"
                    else it
                }
            }
        }

        val timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                timeCheck()
            }
        }

        if (Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
                .contains(packageName)
        )
            NotificationService.notifications.observe(this)
            {
                val list = it.filter { notification ->
                    notification.packageName != packageName &&
                            notification.packageName != "android" &&
                            notification.packageName != "com.android.systemui"
                }
                if (!notificationList.equalsIgnoreOrder(list)) {
                    notificationList.clear()
                    notificationList.addAll(list)
                    adapter.notifyDataSetChanged()
                }
            }
        else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            startActivity(this)
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
                                if (!getIsClose && customLockscreenVisualizeAudio) {
                                    val audioDataData = gson.fromJson(
                                        text, AudioDataData::class.java
                                    )
                                    val audioArray = audioDataData.data
                                    visualization.setAudioData(audioArray, true)
                                }
                            }

                            else -> {}
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        else visualization.visibility = GONE

        MainActivity.currentSongInfo.observe(this)
        {
            if (coverData.value != it.coverData && it.coverData != null) coverData.postValue(it.coverData!!)
            if (
                it.title != getSongInfo.title ||
                it.artist != getSongInfo.artist ||
                it.isPaused != getSongInfo.isPaused ||
                it.liked != getSongInfo.liked ||
                it.disliked != getSongInfo.disliked ||
                it.volume != getSongInfo.volume
            ) songInfo.postValue(it)
        }

        songInfo.observe(this)
        {
            title.text = it.title
            artist.text = it.artist

            volumeSlider.setIndicatorColor(
                getFallBackColor(
                    getCoverData.vibrant,
                    getCoverData.darkMuted,
                    getCoverData.darkVibrant,
                    Color.WHITE
                )
            )

            volumeSlider.setProgress(it.volume.clamp(0, 100), true)

            if (it.isPaused == true) playPauseButton.setImageResource(R.drawable.ic_play)
            else playPauseButton.setImageResource(R.drawable.ic_pause)

            if (it.liked) likeButton.setImageResource(R.drawable.ic_liked)
            else likeButton.setImageResource(R.drawable.ic_like)

            if (it.disliked) dislikeButton.setImageResource(R.drawable.ic_disliked)
            else dislikeButton.setImageResource(R.drawable.ic_dislike)
        }

        coverData.observe(this)
        {
            val coverImage = it.cover
            if (coverImage != null) {
                val luminance = ColorUtilsC.calculateBitmapLuminance(coverImage.toBitmap())
                val alpha = 1f * .4f * (1f - luminance.toFloat()) + .4f

                card?.alpha = alpha
                cover.setImageDrawable(coverImage)
            }
        }

        timeData.observe(this)
        {
            time.text = it
        }

        isClose.observe(this)
        {
            if (it) container
                .animate()
                .alpha(0f)
                .setDuration(200)
                .start()
            else container
                .animate()
                .alpha(customLockscreenBrightness / 100f)
                .setDuration(200)
                .start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        windowInsets = window.decorView.rootWindowInsets

        visualization.size = 2f.pow(customLockscreenVisualizeAudioSize).roundToInt()
        visualization.setBottomLeftCorner(bottomLeftCorner.corner)
        visualization.setBottomRightCorner(bottomRightCorner.corner)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return MainActivity.currentSongInfo.value?.let { songInfo ->
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    changeVolume(songInfo.volume - 5)
                    true
                }

                KeyEvent.KEYCODE_VOLUME_UP -> {
                    changeVolume(songInfo.volume + 5)
                    true
                }

                else -> super.onKeyDown(keyCode, event)
            }
        } ?: super.onKeyDown(keyCode, event)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return getIsClose || super.dispatchTouchEvent(ev)
    }

    override fun onSensorChanged(event: SensorEvent) {
        isClose.postValue(event.values[0].let {
            !(it == event.sensor.maximumRange || it > 5)
        })
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()

        sensorManager.registerListener(
            this,
            proximitySensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()

        sensorManager.unregisterListener(this)
    }

    private fun changeVolume(volume: Int) {
        CustomWebSocket.webSocketInstance?.send(
            SendAction(
                Action.VOLUME,
                VolumeData(volume)
            )
        )
    }

    private class NotificationAdapter(
        private val notifications: List<StatusBarNotification>
    ) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        init {
            setHasStableIds(true)
        }

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.icon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.notification_item, parent, false)
        )

        override fun getItemCount() = notifications.size

        override fun getItemId(position: Int) = notifications[position].id.toLong()

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val notification = notifications[position]

            holder.icon.setImageDrawable(notification.notification.smallIcon.loadDrawable(holder.icon.context))
        }
    }
}