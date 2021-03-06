package de.dertyp7214.youtubemusicremote.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.slider.Slider
import de.dertyp7214.colorutilsc.ColorUtilsC
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.types.RepeatMode
import de.dertyp7214.youtubemusicremote.types.SongInfo
import de.dertyp7214.youtubemusicremote.viewmodels.SearchViewModel
import kotlin.math.roundToInt

fun interface Callback {
    operator fun invoke()
}

fun interface CallbackT<T> {
    operator fun invoke(data: T)
}

class ControlsFragment : Fragment() {
    private var currentSongInfo = SongInfo()

    private var shuffleCallback: Callback = Callback { }
    private var previousCallback: Callback = Callback {}
    private var playPauseCallback: Callback = Callback {}
    private var nextCallback: Callback = Callback {}
    private var repeatCallback: Callback = Callback {}
    private var likeCallback: Callback = Callback {}
    private var dislikeCallback: Callback = Callback {}
    private var queueCallback: Callback = Callback {}
    private var lyricsCallback: Callback = Callback {}
    private var seekBarCallback: CallbackT<Int> = CallbackT {}
    private var volumeCallback: CallbackT<Int> = CallbackT {}

    private lateinit var shuffle: ImageButton
    private lateinit var previous: ImageButton
    private lateinit var playPause: FloatingActionButton
    private lateinit var next: ImageButton
    private lateinit var repeat: ImageButton

    private lateinit var like: ImageButton
    private lateinit var dislike: ImageButton

    private lateinit var queue: ImageButton
    private lateinit var lyrics: ImageButton

    private lateinit var title: TextView
    private lateinit var artist: TextView

    private lateinit var progress: TextView
    private lateinit var duration: TextView

    private lateinit var seekBar: Slider

    private lateinit var layout: View

    private val initialized
        get() = ::shuffle.isInitialized &&
                ::previous.isInitialized &&
                ::playPause.isInitialized &&
                ::next.isInitialized &&
                ::repeat.isInitialized &&
                ::like.isInitialized &&
                ::dislike.isInitialized &&
                ::queue.isInitialized &&
                ::lyrics.isInitialized &&
                ::title.isInitialized &&
                ::artist.isInitialized &&
                ::progress.isInitialized &&
                ::duration.isInitialized &&
                ::seekBar.isInitialized &&
                ::layout.isInitialized

    private var oldBackgroundTint = Color.WHITE

    private var luminance = 0f
    private var vibrant = 0

    private val buttonRadius by lazy { resources.getDimension(R.dimen.innerRoundCorners) }
    private val buttonRadiusMax by lazy { 56.dpToPx(requireContext()).toFloat() }

    private val searchViewModel by lazy { ViewModelProvider(requireActivity())[SearchViewModel::class.java] }

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

        queue = v.findViewById(R.id.queueButton)
        lyrics = v.findViewById(R.id.lyricsButton)

        title = v.findViewById(R.id.title)
        artist = v.findViewById(R.id.artist)

        progress = v.findViewById(R.id.progress)
        duration = v.findViewById(R.id.duration)

        seekBar = v.findViewById(R.id.seekBar)

        layout = v.findViewById(R.id.layout)

        return v
    }

    fun setSongInfo(songInfo: SongInfo) {
        if (currentSongInfo == songInfo || !initialized) return

        shuffle.setOnClickListener { shuffleCallback() }
        previous.setOnClickListener { previousCallback() }
        playPause.setOnClickListener { playPauseCallback() }
        next.setOnClickListener { nextCallback() }
        repeat.setOnClickListener { repeatCallback() }
        like.setOnClickListener { likeCallback() }
        dislike.setOnClickListener { dislikeCallback() }
        queue.setOnClickListener { queueCallback() }
        lyrics.setOnClickListener { lyricsCallback() }
        seekBar.onProgressChanged { progress, userInput ->
            if (userInput) seekBarCallback(progress)
        }

        progress.changeText(songInfo.elapsedSeconds.toHumanReadable(true))
        duration.changeText(songInfo.songDuration.toHumanReadable(true))

        like.setImageResource(if (songInfo.liked) R.drawable.ic_liked else R.drawable.ic_like)
        dislike.setImageResource(if (songInfo.disliked) R.drawable.ic_disliked else R.drawable.ic_dislike)

        repeat.setImageResource(
            when (songInfo.repeatMode) {
                RepeatMode.ALL -> R.drawable.ic_repeat
                RepeatMode.ONE -> R.drawable.ic_repeat_once
                else -> R.drawable.ic_repeat_off
            }
        )

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
            val start = if (songInfo.isPaused == false) buttonRadiusMax else buttonRadius
            val end = if (songInfo.isPaused == false) buttonRadius else buttonRadiusMax
            animateFloats(start, end) {
                playPause.shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(it)
            }
            drawable.start()
        }

        seekBar.let { seekBar ->
            val duration = songInfo.songDuration.toFloat()
            val current = songInfo.elapsedSeconds.toFloat()
            if (seekBar.value > duration) seekBar.value = 0f
            if (seekBar.valueTo != duration) seekBar.valueTo = duration
            if (seekBar.valueTo > current && current > 0f)
                seekBar.value = current
        }

        title.isSelected = true
        artist.isSelected = true

        val coverData = songInfo.coverData ?: return

        if (currentSongInfo almostEquals songInfo) {
            currentSongInfo = songInfo
            return
        }

        vibrant = coverData.vibrant
        var visualizerColor = vibrant
        luminance = ColorUtilsC.calculateLuminance(
            coverData.background?.let {
                try {
                    val activity = requireActivity()
                    val bitmap = it.toBitmap()
                    val screenHeight = activity.window.decorView.height.toFloat()
                    val selfHeight = layout.height.toFloat()
                    val ratio = bitmap.height / screenHeight
                    val convertedSelfHeight = selfHeight * ratio
                    val yB = (bitmap.height - convertedSelfHeight).roundToInt()
                    val resized = bitmap.resize(
                        0,
                        yB,
                        bitmap.width,
                        convertedSelfHeight.roundToInt()
                    )

                    val bitmap2 = bitmap.fitToScreen(activity)
                    val ratio2 = bitmap2.height / screenHeight
                    val location = intArrayOf(0, 0)
                    seekBar.getLocationOnScreen(location)

                    val x = (location[0] * ratio2).roundToInt()
                    val y = (location[1] * ratio2).roundToInt()
                    val seekbarBackground = bitmap2.resize(
                        x, y, bitmap2.width - (x * 2),
                        (seekBar.height * ratio2).roundToInt()
                    )
                    vibrant = calculateFallbackColor(
                        18,
                        seekbarBackground.dominantColor,
                        vibrant,
                        coverData.darkVibrant,
                        coverData.lightVibrant,
                        coverData.muted,
                        coverData.darkMuted,
                        coverData.lightMuted
                    )
                    val visualizerPart = bitmap.resize(
                        0, (bitmap.height * .88).roundToInt(), bitmap.width,
                        (bitmap.height * .12).roundToInt()
                    )
                    visualizerColor = calculateFallbackColor(
                        28,
                        visualizerPart.dominantColor,
                        visualizerColor,
                        coverData.darkVibrant,
                        coverData.lightVibrant,
                        coverData.muted,
                        coverData.darkMuted,
                        coverData.lightMuted
                    )
                    bitmap2.recycle()
                    seekbarBackground.recycle()
                    visualizerPart.recycle()
                    resized.dominantColor.also { resized.recycle() }
                } catch (_: Exception) {
                    it.dominantColor
                }
            } ?: coverData.dominant
        ).toFloat()

        CoverFragment.instance?.visualizeColor(
            getFallBackColor(
                visualizerColor,
                vibrant,
                coverData.muted
            )
        )

        val playPauseColor = getFallBackColor(vibrant, coverData.muted)
        playPause.animateImageTintList(
            if (isDark(playPauseColor)) Color.WHITE else Color.BLACK,
            -1
        )
        playPause.animateBackgroundTintList(playPauseColor, -1) {
            playPause.outlineSpotShadowColor = it
            playPause.outlineAmbientShadowColor = it
        }

        val seekColor = ColorUtilsC.blendARGB(
            getFallBackColor(vibrant, coverData.muted),
            if (luminance < .5) Color.WHITE else Color.BLACK,
            .6f * luminance
        )

        animateColors(
            seekBar.thumbStrokeColor?.defaultColor ?: Color.WHITE,
            seekColor,
            callback = seekBar::setColor
        )

        val controlsColor = if (luminance < .5) Color.WHITE else Color.BLACK

        title.changeText(songInfo.title)
        artist.changeTextWithLinks(songInfo.artist, songInfo.fields, controlsColor) {
            searchViewModel.setSearchOpen(true)
            searchViewModel.setQuery(it.text)
        }

        title.animateTextColor(controlsColor) {
            val tintList = ColorStateList.valueOf(it)

            shuffle.imageTintList = tintList
            previous.imageTintList = tintList
            next.imageTintList = tintList
            repeat.imageTintList = tintList

            like.imageTintList = tintList
            dislike.imageTintList = tintList

            queue.imageTintList = tintList
            lyrics.imageTintList = tintList

            progress.setTextColor(it)
            duration.setTextColor(it)
        }

        val backgroundColor = ColorUtils.setAlphaComponent(coverData.dominant, 0)
        if (oldBackgroundTint != backgroundColor) {
            animateColors(oldBackgroundTint, backgroundColor, callback = layout.background::setTint)
            oldBackgroundTint = backgroundColor
        }

        currentSongInfo = songInfo
    }

    fun passCallbacks(
        shuffle: Callback = Callback {},
        previous: Callback = Callback {},
        playPause: Callback = Callback {},
        next: Callback = Callback {},
        repeat: Callback = Callback {},
        like: Callback = Callback {},
        dislike: Callback = Callback {},
        queue: Callback = Callback {},
        lyrics: Callback = Callback {},
        seek: CallbackT<Int> = CallbackT {},
        volume: CallbackT<Int> = CallbackT {}
    ) {
        shuffleCallback = shuffle
        previousCallback = previous
        playPauseCallback = playPause
        nextCallback = next
        repeatCallback = repeat
        likeCallback = like
        dislikeCallback = dislike
        queueCallback = queue
        lyricsCallback = lyrics
        seekBarCallback = seek
        volumeCallback = volume
    }
}