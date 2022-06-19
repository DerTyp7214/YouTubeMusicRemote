package de.dertyp7214.youtubemusicremote.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.View.OnTouchListener
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.widget.*
import android.widget.FrameLayout.LayoutParams
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.components.CustomWebSocketListener
import de.dertyp7214.youtubemusicremote.components.LyricsBottomSheet
import de.dertyp7214.youtubemusicremote.components.QueueBottomSheet
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.fragments.ControlsFragment
import de.dertyp7214.youtubemusicremote.fragments.CoverFragment
import de.dertyp7214.youtubemusicremote.fragments.SearchFragment
import de.dertyp7214.youtubemusicremote.services.MediaPlayer
import de.dertyp7214.youtubemusicremote.types.*
import de.dertyp7214.youtubemusicremote.viewmodels.SearchViewModel
import dev.chrisbanes.insetter.applyInsetter
import java.lang.Float.max
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), OnTouchListener {

    companion object {
        var currentSongInfo: MutableLiveData<SongInfo> = MutableLiveData()
        var currentLyrics: MutableLiveData<Lyrics> = MutableLiveData()
    }

    private val getSongInfo: SongInfo
        get() {
            return currentSongInfo.value ?: SongInfo()
        }

    private val gson = Gson().newBuilder().enableComplexMapKeySerialization().create()

    private val queueBottomSheet =
        QueueBottomSheet(onLongPress = { queueBottomSheet, view, queueItem ->
            showMenu(
                view,
                SongInfo(
                    title = queueItem.title,
                    artist = queueItem.artist,
                    videoId = queueItem.videoId,
                    volume = queueBottomSheet.queueItems.filter { it.videoId == queueItem.videoId }
                        .indexOf(queueItem),
                    url = "https://music.youtube.com/watch?v=${queueItem.videoId}"
                ),
                R.menu.queue_menu
            )
        }) { dialogFragment, queueItem ->
            webSocket.send(SendAction(Action.QUEUE_VIDEO_ID, VideoIdData(queueItem.videoId)))
            dialogFragment.dismiss()
        }

    private val lyricsBottomSheet = LyricsBottomSheet()

    private var customWebSocketListener = CustomWebSocketListener()

    private val queueItems: MutableLiveData<List<QueueItem>> = MutableLiveData()

    private lateinit var webSocket: CustomWebSocket

    private lateinit var volumePopup: MaterialCardView
    private lateinit var volumeSlider: VerticalSeekBar
    private lateinit var volumeWrapper: ConstraintLayout
    private lateinit var mainContent: ConstraintLayout

    private lateinit var searchFrame: FrameLayout
    private lateinit var controlFrame: FrameLayout
    private lateinit var mainFrame: FrameLayout
    private lateinit var pageLayout: FrameLayout

    private var oldSongInfo: SongInfo = SongInfo()

    private val volumeHandler by lazy { Handler(mainLooper) }

    private val dp8 by lazy { 8.dpToPx(this) }
    private val dpN74 by lazy { (-74).dpToPx(this) }

    private val searchFragment by lazy {
        SearchFragment().resizeFragment(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        )
    }
    private val controlsFragment by lazy {
        ControlsFragment().resizeFragment(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        )
    }
    private val coverFragment by lazy {
        CoverFragment().resizeFragment(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        )
    }

    private val searchViewModel by lazy { ViewModelProvider(this)[SearchViewModel::class.java] }

    private var volumeOpen = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
        setContentView(R.layout.activity_main)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        if (CustomWebSocket.webSocketInstance == null)
            startForegroundService(Intent(this, MediaPlayer::class.java))

        val group = findViewById<LinearLayout>(R.id.group)
        val volume = findViewById<TextView>(R.id.volume)
        val search = findViewById<ImageButton>(R.id.search)
        val menuButton = findViewById<ImageButton>(R.id.menuButton)

        volumePopup = findViewById(R.id.volumePopup)
        volumeSlider = findViewById(R.id.volumeSlider)
        volumeWrapper = findViewById(R.id.volumeWrapper)
        mainContent = findViewById(R.id.mainContent)

        searchFrame = findViewById(R.id.searchLayout)
        controlFrame = findViewById(R.id.controlFrame)
        mainFrame = findViewById(R.id.mainFrame)
        pageLayout = findViewById(R.id.pageLayout)

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(searchFrame.id, searchFragment)
            .replace(controlFrame.id, controlsFragment).replace(mainFrame.id, coverFragment)
            .commit()

        group.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }

        val pageHeight = resources.displayMetrics.heightPixels + getStatusBarHeight()
        searchFrame.setHeight(pageHeight)
        searchFrame.setMargins(0, pageHeight.inv(), 0, 0)

        @Suppress("NAME_SHADOWING")
        fun setMargins(open: Boolean) {
            val pageHeight = resources.displayMetrics.heightPixels + getStatusBarHeight()
            searchFrame.setHeight(pageHeight)
            animateInts(if (open) pageHeight else 0, if (open) 0 else pageHeight) { marginTop ->
                searchFrame.setMargins(
                    0, marginTop.inv(), 0, 0
                )
                pageLayout.setMargins(0, 0, 0, marginTop - pageHeight)
            }
        }

        val audioLiveData = MutableLiveData<List<Short>>()
        coverFragment.setAudioData(audioLiveData)

        searchViewModel.observerSearchOpen(this) { open ->
            setMargins(open)
        }

        webSocket = if (CustomWebSocket.webSocketInstance == null)
            CustomWebSocket(MediaPlayer.URL, customWebSocketListener, gson = gson)
        else CustomWebSocket.webSocketInstance!!.also {
            customWebSocketListener = it.webSocketListener
        }
        webSocket.setInstance()

        volumeSlider.onProgressChanged { progress, userInput ->
            if (userInput) changeVolume(progress)
        }

        fun setSongInfo(songInfo: SongInfo) {
            volumeSlider.setProgress(songInfo.volume, true)

            volume.changeText("${songInfo.volume}%")

            menuButton.setOnClickListener {
                showMenu(menuButton, songInfo)
            }

            val coverData = songInfo.coverData ?: return

            if (!songInfo.srcChanged(oldSongInfo) && !songInfo.playPaused) return

            val controlsColor = if (isDark(
                    coverData.background?.let {
                        if (group != null) {
                            val screenHeight = window.decorView.height.toFloat()
                            val groupHeight = group.height.toFloat() + group.getMargins().top
                            val ratio = it.intrinsicHeight / screenHeight
                            it.resize(
                                this, 0, 0, it.intrinsicWidth, (groupHeight * ratio).roundToInt()
                            )
                        } else it
                    }?.dominantColor ?: coverData.parsedDominant
                )
            ) {
                window.decorView.windowInsetsController?.setSystemBarsAppearance(
                    0, APPEARANCE_LIGHT_STATUS_BARS
                )
                Color.WHITE
            } else {
                window.decorView.windowInsetsController?.setSystemBarsAppearance(
                    APPEARANCE_LIGHT_STATUS_BARS, APPEARANCE_LIGHT_STATUS_BARS
                )
                Color.BLACK
            }

            val luminance = ColorUtils.calculateLuminance(coverData.dominant).toFloat()
            val sliderColor = ColorUtils.blendARGB(
                getFallBackColor(coverData.muted, coverData.vibrant),
                if (luminance < .5) Color.BLACK else Color.WHITE,
                .6f * luminance
            )

            volume.animateTextColor(controlsColor)
            menuButton.animateImageTintList(controlsColor)
            search.animateImageTintList(controlsColor)

            animateColors(
                volumePopup.strokeColorStateList?.defaultColor ?: Color.WHITE,
                getFallBackColor(coverData.vibrant, coverData.lightMuted, coverData.muted)
            ) {
                volumePopup.strokeColor = it
            }

            animateColors(volumeSlider.progressTintList?.defaultColor ?: Color.WHITE, sliderColor) {
                volumeSlider.progressTintList = ColorStateList.valueOf(it)
            }
            animateColors(volumeSlider.thumbTintList?.defaultColor ?: Color.WHITE, sliderColor) {
                volumeSlider.thumbTintList = ColorStateList.valueOf(it)
            }
        }

        customWebSocketListener.onMessage { _, text ->
            try {
                val socketResponse = gson.fromJson(text, SocketResponse::class.java)

                when (socketResponse.action) {
                    Action.QUEUE -> {
                        val queue: List<QueueItem> = gson.fromJson(
                            socketResponse.data,
                            object : TypeToken<List<QueueItem>>() {}.type
                        )

                        queueItems.postValue(queue)
                    }
                    Action.LYRICS -> {
                        val lyrics = gson.fromJson(socketResponse.data, LyricsData::class.java)

                        currentLyrics.postValue(
                            Lyrics(
                                lyrics.lyrics,
                                getSongInfo.videoId,
                                getSongInfo.title,
                                getSongInfo.artist
                            )
                        )
                    }
                    Action.AUDIO_DATA -> {
                        val audioDataData = gson.fromJson(
                            text, AudioDataData::class.java
                        )
                        audioLiveData.postValue(audioDataData.data)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        customWebSocketListener.onFailure { _, throwable, _ ->
            throwable.printStackTrace()
            webSocket.reconnect()
        }

        queueItems.observe(this) {
            if (!it.isNullOrEmpty()) queueBottomSheet.apply {
                queueItems = it
                coverData = getSongInfo.coverData ?: CoverData()
            }
        }

        currentLyrics.observe(this) {
            if (it.lyrics.isNotBlank()) lyricsBottomSheet.apply {
                lyrics = it
                coverData = getSongInfo.coverData ?: CoverData()
            }
        }

        currentSongInfo.observe(this) { songInfo ->
            if (lyricsBottomSheet.isShowing && (currentLyrics.value?.videoId
                    ?: "") != songInfo.videoId
            ) webSocket.send(SendAction(Action.REQUEST_LYRICS))
            songInfo.parseImageColorsAsync(this, oldSongInfo) {
                runOnUiThread {
                    setSongInfo(it)

                    queueBottomSheet.coverData = it.coverData ?: CoverData()

                    searchFragment.setSongInfo(it)
                    controlsFragment.setSongInfo(it)
                    coverFragment.setSongInfo(it)

                    oldSongInfo = it
                }
            }
        }

        search.setOnClickListener {
            searchViewModel.setSearchOpen(true)
        }

        controlsFragment.passCallbacks(
            shuffle = { webSocket.shuffle() },
            previous = { webSocket.previous() },
            playPause = { webSocket.playPause() },
            next = { webSocket.next() },
            repeat = { webSocket.repeat() },
            like = { webSocket.like() },
            dislike = { webSocket.dislike() },
            queue = {
                webSocket.send(SendAction(Action.REQUEST_QUEUE))
                queueBottomSheet.showWithBlur(
                    this@MainActivity,
                    findViewById(R.id.root),
                    window.decorView
                )
            },
            lyrics = {
                webSocket.send(SendAction(Action.REQUEST_LYRICS))
                if (lyricsBottomSheet.lyrics.videoId != getSongInfo.videoId) lyricsBottomSheet.lyrics =
                    Lyrics.Empty
                getSongInfo.coverData?.let { lyricsBottomSheet.coverData = it }
                lyricsBottomSheet.showWithBlur(
                    this@MainActivity,
                    findViewById(R.id.root),
                    window.decorView
                )
            },
            seek = { webSocket.seek(it) },
            volume = { webSocket.volume(it) }
        )

        onBackPressedDispatcher.addCallback(this, true) {
            if (!searchFragment.handleBack()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean = true

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return currentSongInfo.value?.let { songInfo ->
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

    private fun showMenu(
        v: View,
        songInfo: SongInfo,
        menuLayout: Int = R.menu.main_menu
    ) {
        PopupMenu(this, v, Gravity.END, 0, R.style.Theme_YouTubeMusicRemote).apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_settings -> {
                        startActivity(Intent(this@MainActivity, Settings::class.java))
                        true
                    }
                    R.id.menu_toggle_mute -> {
                        webSocket.send(SendAction(Action.MUTE_UNMUTE))
                        true
                    }
                    R.id.menu_share -> {
                        share(ShareInfo.fromSongInfo(songInfo))
                        true
                    }
                    R.id.add_new_url -> {
                        startActivity(
                            Intent(this@MainActivity, IntroActivity::class.java).putExtra(
                                "newUrl", true
                            )
                        )
                        true
                    }
                    R.id.menu_queue -> {
                        webSocket.send(SendAction(Action.REQUEST_QUEUE))
                        queueBottomSheet.showWithBlur(
                            this@MainActivity,
                            findViewById(R.id.root),
                            window.decorView
                        )
                        true
                    }
                    R.id.menu_lyrics -> {
                        webSocket.send(SendAction(Action.REQUEST_LYRICS))
                        lyricsBottomSheet.showWithBlur(
                            this@MainActivity,
                            findViewById(R.id.root),
                            window.decorView
                        )
                        true
                    }
                    R.id.menu_start_radio -> {
                        webSocket.startQueueItemRadio(songInfo.videoId)
                        true
                    }
                    R.id.menu_play_next -> {
                        webSocket.playQueueItemNext(songInfo.videoId)
                        true
                    }
                    R.id.menu_add_to_queue -> {
                        webSocket.addQueueItemToQueue(songInfo.videoId)
                        true
                    }
                    R.id.menu_remove_from_queue -> {
                        webSocket.removeQueueItemFromQueue(songInfo.videoId, songInfo.volume)
                        true
                    }
                    else -> false
                }
            }
            inflate(menuLayout)
            menu.findItem(R.id.menu_toggle_mute)
                ?.setTitle(if (songInfo.isMuted) R.string.unmute else R.string.mute)
        }.show()
    }

    private fun changeVolume(volume: Int) {
        if (!::mainContent.isInitialized || !::volumeWrapper.isInitialized) return

        val color = (currentSongInfo.value?.coverData?.dominant ?: Color.BLACK).let {
            it.darkenColor(.7f * ColorUtils.calculateLuminance(it).toFloat())
        }
        val foregroundColor = ColorUtils.setAlphaComponent(
            color,
            (128f * max(ColorUtils.calculateLuminance(color).toFloat(), .5f)).roundToInt()
        )

        volumeHandler.removeCallbacksAndMessages(null)
        volumeHandler.postDelayed({
            volumeOpen = false
            mainContent.animateForegroundTintList(Color.TRANSPARENT, foregroundColor, 120) {
                volumeWrapper.animateRightMargin(dp8, dpN74, 120)
            }
        }, 1200)

        if (!volumeOpen) {
            volumeOpen = true
            mainContent.animateForegroundTintList(foregroundColor, Color.TRANSPARENT, 120) {
                volumeWrapper.animateRightMargin(dpN74, dp8, 120)
            }
        }

        currentSongInfo.value?.let { songInfo ->
            songInfo.volume = volume
            webSocket.send(SendAction(Action.VOLUME, VolumeData(volume)))
        }
    }

    override fun onDestroy() {
        startService(
            Intent(this, MediaPlayer::class.java).setAction(MediaPlayer.ACTION_STOP)
        )
        super.onDestroy()
    }
}