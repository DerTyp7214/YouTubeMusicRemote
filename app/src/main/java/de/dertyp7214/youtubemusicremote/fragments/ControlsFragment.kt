package de.dertyp7214.youtubemusicremote.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.types.RepeatMode
import de.dertyp7214.youtubemusicremote.types.SongInfo
import de.dertyp7214.youtubemusicremote.viewmodels.YouTubeViewModel
import kotlin.math.roundToInt

typealias Callback = () -> Unit

class ControlsFragment : Fragment() {
    private var currentSongInfo = SongInfo()

    private var shuffleCallback: Callback = {}
    private var previousCallback: Callback = {}
    private var playPauseCallback: Callback = {}
    private var nextCallback: Callback = {}
    private var repeatCallback: Callback = {}
    private var likeCallback: Callback = {}
    private var dislikeCallback: Callback = {}
    private var seekBarCallback: (seconds: Int) -> Unit = {}
    private var volumeCallback: (volume: Int) -> Unit = {}

    private var shuffle: ImageButton? = null
    private var previous: ImageButton? = null
    private var playPause: FloatingActionButton? = null
    private var next: ImageButton? = null
    private var repeat: ImageButton? = null

    private var like: ImageButton? = null
    private var dislike: ImageButton? = null

    private var title: TextView? = null
    private var artist: TextView? = null

    private var progress: TextView? = null
    private var duration: TextView? = null

    private var seekBar: SeekBar? = null

    private var layout: View? = null

    private var oldBackgroundTint = Color.WHITE

    private val youtubeViewModel by lazy { ViewModelProvider(requireActivity())[YouTubeViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_controls, container, false)

        shuffle = v.findViewById(R.id.shuffle)
        previous = v.findViewById(R.id.previous)
        playPause = v.findViewById(R.id.playPause)
        next = v.findViewById(R.id.next)
        repeat = v.findViewById(R.id.repeat)

        like = v.findViewById(R.id.like)
        dislike = v.findViewById(R.id.dislike)

        title = v.findViewById(R.id.title)
        artist = v.findViewById(R.id.artist)

        progress = v.findViewById(R.id.progress)
        duration = v.findViewById(R.id.duration)

        seekBar = v.findViewById(R.id.seekBar)

        layout = v.findViewById(R.id.layout)

        return v
    }

    fun setSongInfo(songInfo: SongInfo) {
        if (currentSongInfo == songInfo) return

        shuffle?.setOnClickListener { shuffleCallback() }
        previous?.setOnClickListener { previousCallback() }
        playPause?.setOnClickListener { playPauseCallback() }
        next?.setOnClickListener { nextCallback() }
        repeat?.setOnClickListener { repeatCallback() }
        like?.setOnClickListener { likeCallback() }
        dislike?.setOnClickListener { dislikeCallback() }
        seekBar?.onProgressChanged { progress, userInput ->
            if (userInput) seekBarCallback(progress)
        }

        title?.changeText(songInfo.title)
        artist?.changeTextWithLinks(songInfo.artist, songInfo.fields) { field ->
            youtubeViewModel.setSearchOpen(true)
            youtubeViewModel.setChannelId(field.link.split("/").last())
        }

        progress?.changeText(songInfo.elapsedSeconds.toHumanReadable(true))
        duration?.changeText(songInfo.songDuration.toHumanReadable(true))

        title?.isSelected = true
        artist?.isSelected = true

        like?.setImageResource(if (songInfo.liked) R.drawable.ic_liked else R.drawable.ic_like)
        dislike?.setImageResource(if (songInfo.disliked) R.drawable.ic_disliked else R.drawable.ic_dislike)

        repeat?.setImageResource(
            when (songInfo.repeatMode) {
                RepeatMode.ALL -> R.drawable.ic_repeat
                RepeatMode.ONE -> R.drawable.ic_repeat_once
                else -> R.drawable.ic_repeat_off
            }
        )

        playPause?.let { playPause ->
            val context = try {
                requireContext()
            } catch (_: Exception) {
                null
            }

            if (currentSongInfo.isPaused != songInfo.isPaused && context != null) {
                val drawable = ContextCompat.getDrawable(
                    requireContext(),
                    if (songInfo.isPaused == false) R.drawable.ic_play_pause else R.drawable.ic_pause_play
                ) as AnimatedVectorDrawable
                playPause.setImageDrawable(drawable)
                drawable.start()
            }
        }

        val coverData = songInfo.coverData ?: return

        seekBar?.let { seekBar ->
            val duration = songInfo.songDuration.toInt()
            if (seekBar.max != duration) seekBar.max = duration
            seekBar.setProgress(songInfo.elapsedSeconds, true)
        }

        val playPauseColor = getFallBackColor(coverData.muted, coverData.vibrant)
        playPause?.let { playPause ->
            playPause.animateImageTintList(
                if (isDark(playPauseColor)) Color.WHITE else Color.BLACK,
                Color.BLACK
            )
            animateColors(
                playPause.backgroundTintList?.defaultColor ?: Color.WHITE,
                playPauseColor
            ) {
                playPause.backgroundTintList = ColorStateList.valueOf(it)
            }
        }

        val luminance = ColorUtils.calculateLuminance(
            coverData.background?.let {
                val self = layout
                val activity = try {
                    requireActivity()
                } catch (_: Exception) {
                    null
                }
                if (self != null && activity != null) {
                    val bitmap = it.toBitmap()
                    val screenHeight = activity.window.decorView.height.toFloat()
                    val selfHeight = self.height.toFloat()
                    val ratio = bitmap.height / screenHeight
                    val convertedSelfHeight = selfHeight * ratio
                    bitmap.resize(
                        0,
                        (bitmap.height - convertedSelfHeight).roundToInt(),
                        bitmap.width,
                        convertedSelfHeight.roundToInt()
                    ).toDrawable(activity)
                } else it
            }?.getDominantColor(coverData.dominant) ?: coverData.dominant
        ).toFloat()
        val seekColor = ColorUtils.blendARGB(
            getFallBackColor(coverData.muted, coverData.vibrant),
            if (luminance < .5) Color.WHITE else Color.BLACK,
            .6f * luminance
        )

        seekBar?.let { seekBar ->
            animateColors(seekBar.progressTintList?.defaultColor ?: Color.WHITE, seekColor) {
                seekBar.progressTintList = ColorStateList.valueOf(it)
            }
            animateColors(seekBar.thumbTintList?.defaultColor ?: Color.WHITE, seekColor) {
                seekBar.thumbTintList = ColorStateList.valueOf(it)
            }
        }

        val controlsColor = if (luminance < .5) Color.WHITE else Color.BLACK

        shuffle?.animateImageTintList(controlsColor, Color.BLACK)
        previous?.animateImageTintList(controlsColor, Color.BLACK)
        next?.animateImageTintList(controlsColor, Color.BLACK)
        repeat?.animateImageTintList(controlsColor, Color.BLACK)

        like?.animateImageTintList(controlsColor, Color.BLACK)
        dislike?.animateImageTintList(controlsColor, Color.BLACK)

        progress?.animateTextColor(controlsColor)
        duration?.animateTextColor(controlsColor)

        title?.animateTextColor(controlsColor)
        artist?.animateTextColor(controlsColor)

        val backgroundColor = ColorUtils.setAlphaComponent(coverData.dominant, 0)
        if (oldBackgroundTint != backgroundColor) {
            layout?.let { view ->
                animateColors(oldBackgroundTint, backgroundColor) {
                    view.background.setTint(it)
                }
                oldBackgroundTint = backgroundColor
            }
        }

        currentSongInfo = songInfo
    }

    fun passCallbacks(
        shuffle: Callback = {},
        previous: Callback = {},
        playPause: Callback = {},
        next: Callback = {},
        repeat: Callback = {},
        like: Callback = {},
        dislike: Callback = {},
        seek: (seconds: Int) -> Unit = {},
        volume: (volume: Int) -> Unit = {}
    ) {
        shuffleCallback = shuffle
        previousCallback = previous
        playPauseCallback = playPause
        nextCallback = next
        repeatCallback = repeat
        likeCallback = like
        dislikeCallback = dislike
        seekBarCallback = seek
        volumeCallback = volume
    }
}