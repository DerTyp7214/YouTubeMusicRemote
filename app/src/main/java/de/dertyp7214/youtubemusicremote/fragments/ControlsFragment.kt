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
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.types.RepeatMode
import de.dertyp7214.youtubemusicremote.types.SongInfo

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

        val coverData = songInfo.coverData ?: return

        title?.changeText(songInfo.title)
        artist?.changeText(songInfo.artist)

        progress?.changeText(songInfo.elapsedSeconds.toHumanReadable(true))
        duration?.changeText(songInfo.songDuration.toHumanReadable(true))

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

            if (currentSongInfo.isPaused != songInfo.isPaused) {
                val drawable = ContextCompat.getDrawable(
                    requireContext(),
                    if (songInfo.isPaused == false) R.drawable.ic_play_pause else R.drawable.ic_pause_play
                ) as AnimatedVectorDrawable
                playPause.setImageDrawable(drawable)
                drawable.start()
            }
        }

        val seekColor = ColorUtils.blendARGB(
            getFallBackColor(coverData.muted, coverData.vibrant),
            Color.WHITE,
            .2f
        )

        seekBar?.let { seekBar ->
            animateColors(seekBar.progressTintList?.defaultColor ?: Color.WHITE, seekColor) {
                seekBar.progressTintList = ColorStateList.valueOf(it)
            }
            animateColors(seekBar.thumbTintList?.defaultColor ?: Color.WHITE, seekColor) {
                seekBar.thumbTintList = ColorStateList.valueOf(it)
            }
        }

        val controlsColor = if (isDark(
                ColorUtils.blendARGB(
                    coverData.dominant,
                    coverData.dominant.darkenColor(.5f),
                    .77f
                )
            )
        ) Color.WHITE else Color.BLACK
        shuffle?.animateImageTintList(controlsColor, Color.BLACK)
        previous?.animateImageTintList(controlsColor, Color.BLACK)
        next?.animateImageTintList(controlsColor, Color.BLACK)
        repeat?.animateImageTintList(controlsColor, Color.BLACK)

        like?.animateImageTintList(controlsColor, Color.BLACK)
        dislike?.animateImageTintList(controlsColor, Color.BLACK)

        like?.setImageResource(if (songInfo.liked) R.drawable.ic_liked else R.drawable.ic_like)
        dislike?.setImageResource(if (songInfo.disliked) R.drawable.ic_disliked else R.drawable.ic_dislike)

        repeat?.setImageResource(
            when (songInfo.repeatMode) {
                RepeatMode.ALL -> R.drawable.ic_repeat
                RepeatMode.ONE -> R.drawable.ic_repeat_once
                else -> R.drawable.ic_repeat_off
            }
        )

        progress?.animateTextColor(controlsColor)
        duration?.animateTextColor(controlsColor)

        title?.animateTextColor(controlsColor)
        artist?.animateTextColor(controlsColor)

        title?.isSelected = true
        artist?.isSelected = true

        val backgroundColor = ColorUtils.setAlphaComponent(coverData.dominant, 60)
        if (oldBackgroundTint != backgroundColor) {
            layout?.let { view ->
                animateColors(oldBackgroundTint, backgroundColor) {
                    view.background.setTint(it)
                }
                oldBackgroundTint = backgroundColor
            }
        }

        shuffle?.setOnClickListener { shuffleCallback() }
        previous?.setOnClickListener { previousCallback() }
        playPause?.setOnClickListener { playPauseCallback() }
        next?.setOnClickListener { nextCallback() }
        repeat?.setOnClickListener { repeatCallback() }
        like?.setOnClickListener { likeCallback() }
        dislike?.setOnClickListener { dislikeCallback() }
        seekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
            override fun onProgressChanged(bar: SeekBar?, progress: Int, userInput: Boolean) {
                if (userInput) seekBarCallback(progress)
            }
        })

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
        seek: (seconds: Int) -> Unit = {}
    ) {
        shuffleCallback = shuffle
        previousCallback = previous
        playPauseCallback = playPause
        nextCallback = next
        repeatCallback = repeat
        likeCallback = like
        dislikeCallback = dislike
        seekBarCallback = seek
    }
}