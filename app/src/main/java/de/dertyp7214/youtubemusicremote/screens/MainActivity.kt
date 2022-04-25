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
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.components.CustomWebSocketListener
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.fragments.ControlsFragment
import de.dertyp7214.youtubemusicremote.fragments.CoverFragment
import de.dertyp7214.youtubemusicremote.fragments.YouTubeApiFragment
import de.dertyp7214.youtubemusicremote.services.MediaPlayer
import de.dertyp7214.youtubemusicremote.types.Action
import de.dertyp7214.youtubemusicremote.types.SendAction
import de.dertyp7214.youtubemusicremote.types.SongInfo
import de.dertyp7214.youtubemusicremote.types.VolumeData
import de.dertyp7214.youtubemusicremote.viewmodels.YouTubeViewModel
import dev.chrisbanes.insetter.applyInsetter
import java.lang.Float.max
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), OnTouchListener {

    companion object {
        var URL = ""
            set(value) {
                var url = value
                if (!url.startsWith("ws://") && url != "devUrl") url = "ws://$url"
                field = url
            }

        var currentSongInfo: MutableLiveData<SongInfo> = MutableLiveData(SongInfo())
    }

    private val gson = Gson().newBuilder().enableComplexMapKeySerialization().create()

    private var customWebSocketListener = CustomWebSocketListener()

    private lateinit var webSocket: CustomWebSocket

    private lateinit var volumePopup: MaterialCardView
    private lateinit var volumeSlider: VerticalSeekBar
    private lateinit var volumeWrapper: ConstraintLayout
    private lateinit var mainContent: ConstraintLayout

    private lateinit var youtubeSearchFrame: FrameLayout
    private lateinit var controlFrame: FrameLayout
    private lateinit var mainFrame: FrameLayout
    private lateinit var pageLayout: FrameLayout

    private var oldSongInfo: SongInfo = SongInfo()

    private val volumeHandler by lazy { Handler(mainLooper) }

    private val dp8 by lazy { 8.dpToPx(this) }
    private val dpN74 by lazy { (-74).dpToPx(this) }

    private val youTubeApiFragment by lazy {
        YouTubeApiFragment().resizeFragment(
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

    private val youtubeViewModel by lazy { ViewModelProvider(this)[YouTubeViewModel::class.java] }

    private var volumeOpen = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        youtubeSearchFrame = findViewById(R.id.youtubeSearch)
        controlFrame = findViewById(R.id.controlFrame)
        mainFrame = findViewById(R.id.mainFrame)
        pageLayout = findViewById(R.id.pageLayout)

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(youtubeSearchFrame.id, youTubeApiFragment)
            .replace(controlFrame.id, controlsFragment).replace(mainFrame.id, coverFragment)
            .commit()

        group.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }

        val pageHeight = resources.displayMetrics.heightPixels
        val youtubeTopMargin = getStatusBarHeight() + 16.dpToPx(this)
        youtubeSearchFrame.setHeight(pageHeight)
        youtubeSearchFrame.setMargins(0, (pageHeight - youtubeTopMargin).inv(), 0, 0)

        fun setMargins(open: Boolean) {
            animateInts(if (open) pageHeight else 0, if (open) 0 else pageHeight) { marginTop ->
                youtubeSearchFrame.setMargins(
                    0, marginTop.inv().let { if (open) it + youtubeTopMargin else it }, 0, 0
                )
                pageLayout.setMargins(0, 0, 0, marginTop - pageHeight)
            }
        }

        youtubeViewModel.observerSearchOpen(this) { open ->
            setMargins(open)
        }

        webSocket = if (CustomWebSocket.webSocketInstance == null)
            CustomWebSocket(URL, customWebSocketListener, gson = gson)
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
            val controlsColor = if (isDark(
                    coverData.background?.let {
                        if (group != null) {
                            val activity = this
                            val bitmap = it.toBitmap()
                            val screenHeight = activity.window.decorView.height.toFloat()
                            val groupHeight = group.height.toFloat() + group.getMargins().top
                            val ratio = bitmap.height / screenHeight
                            bitmap.resize(
                                0, 0, bitmap.width, (groupHeight * ratio).roundToInt()
                            ).toDrawable(activity)
                        } else it
                    }?.getDominantColor(coverData.parsedDominant) ?: coverData.parsedDominant
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

        customWebSocketListener.onFailure { _, throwable, _ ->
            throwable.printStackTrace()
        }

        currentSongInfo.observe(this) { songInfo ->
            songInfo.parseImageColorsAsync(this, oldSongInfo) {
                runOnUiThread {
                    setSongInfo(it)

                    youTubeApiFragment.setSongInfo(it)
                    controlsFragment.setSongInfo(it)
                    coverFragment.setSongInfo(it)

                    oldSongInfo = it
                }
            }
        }

        search.setOnClickListener {
            youtubeViewModel.setSearchOpen(true)
        }

        controlsFragment.passCallbacks(
            shuffle = { webSocket.send(SendAction(Action.SHUFFLE)) },
            previous = { webSocket.previous() },
            playPause = { webSocket.playPause() },
            next = { webSocket.next() },
            repeat = { webSocket.repeat() },
            like = { webSocket.like() },
            dislike = { webSocket.dislike() },
            seek = { webSocket.seek(it) },
            volume = { webSocket.send(SendAction(Action.VOLUME, VolumeData(it))) })
    }

    override fun onResume() {
        super.onResume()
        webSocket = CustomWebSocket(URL, customWebSocketListener, gson = gson)
        webSocket.setInstance()
    }

    override fun onBackPressed() {
        if (youtubeViewModel.getSearchOpen() == true) {
            youtubeViewModel.setSearchOpen(false)
            youtubeViewModel.setChannelId(null)
        } else super.onBackPressed()
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

    private fun showMenu(v: View, songInfo: SongInfo) {
        PopupMenu(this, v, Gravity.END, 0, R.style.Theme_YouTubeMusicRemote).apply {
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
                    R.id.add_new_url -> {
                        startActivity(
                            Intent(this@MainActivity, IntroActivity::class.java).putExtra(
                                "newUrl", true
                            )
                        )
                        finish()
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
        val color = (currentSongInfo.value?.coverData?.dominant ?: Color.BLACK).let {
            it.darkenColor(.7f * ColorUtils.calculateLuminance(it).toFloat())
        }
        val foregroundColor = ColorUtils.setAlphaComponent(
            color, (128f * max(ColorUtils.calculateLuminance(color).toFloat(), .5f)).roundToInt()
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
        sendBroadcast(Intent(this, MediaPlayer.Restarter::class.java))
        super.onDestroy()
    }
}