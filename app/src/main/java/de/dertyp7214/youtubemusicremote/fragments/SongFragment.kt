package de.dertyp7214.youtubemusicremote.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.*
import androidx.transition.TransitionInflater
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.screens.MainActivity
import de.dertyp7214.youtubemusicremote.types.SongInfo
import kotlin.math.roundToInt

class SongFragment : Fragment() {
    private var oldSongInfo = SongInfo()
    private lateinit var layout: View

    private val controlFrame: FrameLayout by lazy { findViewById(R.id.controlFrame) }
    private val mainFrame: FrameLayout by lazy { findViewById(R.id.mainFrame) }

    private val group by lazy { findViewById<LinearLayout>(R.id.group) }
    private val volume by lazy { findViewById<TextView>(R.id.volume) }
    private val search by lazy { findViewById<ImageButton>(R.id.search) }
    private val menuButton by lazy { findViewById<ImageButton>(R.id.menuButton) }

    private val songViewModel by lazy { ViewModelProvider(requireActivity())[SongViewModel::class.java] }

    private val onLayoutInitialized = arrayListOf<() -> Unit>()

    private val controlsFragment by lazy {
        ControlsFragment().resizeFragment(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
    }
    private val coverFragment by lazy {
        CoverFragment().resizeFragment(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
    }

    val coverImage = coverFragment.coverImage

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        TransitionInflater.from(requireContext()).apply {
            sharedElementReturnTransition = inflateTransition(R.transition.shared_image)
            sharedElementEnterTransition = inflateTransition(R.transition.shared_image)
        }
        layout = inflater.inflate(R.layout.fragment_song, container, false)

        group.setMargins(0, getStatusBarHeight(), 0, 0)

        requireActivity().supportFragmentManager.commit {
            add(controlFrame.id, controlsFragment)
            add(mainFrame.id, coverFragment)
            addToBackStack("songContent")
        }

        onLayoutInitialized.forEach { it.invoke() }

        coverFragment.setAudioData(songViewModel.getMutableAudioLiveData())

        return layout
    }

    private fun <T : View> findViewById(@IdRes id: Int): T = layout.findViewById(id)

    fun postAudioLiveData(audioLiveData: ShortArray) {
        songViewModel.getMutableAudioLiveData().postValue(audioLiveData)
    }

    fun onSearchClick(onClickListener: OnClickListener) {
        if (::layout.isInitialized) {
            search.setOnClickListener(onClickListener)
        } else onLayoutInitialized.add {
            search.setOnClickListener(onClickListener)
        }
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
    ) = controlsFragment.passCallbacks(
        shuffle, previous, playPause, next,
        repeat, like, dislike, queue,
        lyrics, seek, volume
    )

    fun setSongInfo(songInfo: SongInfo) {
        controlsFragment.setSongInfo(songInfo)
        coverFragment.setSongInfo(songInfo)

        volume.changeText("${songInfo.volume}%")

        menuButton.setOnClickListener {
            MainActivity.run {
                showMenu(menuButton, songInfo)
            }
        }

        val coverData = songInfo.coverData ?: return

        if (!songInfo.srcChanged(oldSongInfo) && !songInfo.playPaused) return

        val window = requireActivity().window
        val controlsColor = if (isDark(
                coverData.background?.let {
                    val screenHeight = window.decorView.height.toFloat()
                    val groupHeight = group.height.toFloat() + group.getMargins().top
                    val ratio = it.intrinsicHeight / screenHeight
                    it.resize(
                        requireContext(),
                        0,
                        0,
                        it.intrinsicWidth,
                        (groupHeight * ratio).roundToInt()
                    )
                }?.dominantColor ?: coverData.parsedDominant
            )
        ) {
            window.decorView.windowInsetsController?.setSystemBarsAppearance(
                0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
            Color.WHITE
        } else {
            window.decorView.windowInsetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
            Color.BLACK
        }

        volume.animateTextColor(controlsColor)
        menuButton.animateImageTintList(controlsColor)
        search.animateImageTintList(controlsColor)

        oldSongInfo = songInfo
    }

    class SongViewModel : ViewModel() {
        private val audioLiveData = MutableLiveData<ShortArray>()

        fun setAudioLiveData(audioLiveData: ShortArray) {
            this.audioLiveData.postValue(audioLiveData)
        }

        fun getAudioLiveData(): ShortArray {
            return this.audioLiveData.value ?: shortArrayOf()
        }

        fun getMutableAudioLiveData(): MutableLiveData<ShortArray> = this.audioLiveData

        fun observeAudioLiveData(lifecycleOwner: LifecycleOwner, observer: Observer<ShortArray>) {
            this.audioLiveData.observe(lifecycleOwner, observer)
        }
    }
}