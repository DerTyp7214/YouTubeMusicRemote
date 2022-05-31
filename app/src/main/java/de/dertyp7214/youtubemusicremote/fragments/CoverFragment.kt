package de.dertyp7214.youtubemusicremote.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.google.android.material.card.MaterialCardView
import de.dertyp7214.audiovisualization.components.AudioVisualizerView
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.types.SongInfo
import kotlin.math.pow
import kotlin.math.roundToInt

class CoverFragment : Fragment() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var instance: CoverFragment? = null
            private set
    }

    private val mutableAudioData = MutableLiveData<MutableLiveData<List<Short>>>()

    private var currentSongInfo = SongInfo()

    private lateinit var cover: ImageView
    private lateinit var card: MaterialCardView
    private lateinit var visualizerView: AudioVisualizerView

    private lateinit var root: View

    private val initialized
        get() = ::cover.isInitialized && ::card.isInitialized && ::root.isInitialized

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_cover, container, false)

        cover = v.findViewById(R.id.cover)
        card = v.findViewById(R.id.card)
        visualizerView = v.findViewById(R.id.visualizer)

        root = requireActivity().findViewById(R.id.root)

        visualizerView.alpha = .8f
        visualizerView.setBottomLeftCorner(AudioVisualizerView.Corner(card.radius.roundToInt()))
        visualizerView.setBottomRightCorner(AudioVisualizerView.Corner(card.radius.roundToInt()))

        requireActivity().let { activity ->
            visualizeSize(activity.visualizeSize)
            visualizeAudioPreferenceChanged(activity.visualizeAudio)
            mutableAudioData.observe(activity) {
                it.observe(activity) { audioData ->
                    visualizerView.setAudioData(audioData, true)
                }
            }
        }

        instance = this

        return v
    }

    fun visualizeAudioPreferenceChanged(visualize: Boolean) {
        visualizerView.visualize = visualize
    }

    fun visualizeColor(color: Int) {
        visualizerView.setColor(color)
    }

    fun visualizeSize(size: Int) {
        visualizerView.size = 2f.pow(size).roundToInt()
    }

    fun setAudioData(audioData: MutableLiveData<List<Short>>) =
        mutableAudioData.postValue(audioData)

    fun setSongInfo(songInfo: SongInfo) {
        if (currentSongInfo == songInfo || !initialized) return

        val coverData = songInfo.coverData ?: return

        if (currentSongInfo.coverData == coverData) return

        val cardColor =
            getFallBackColor(coverData.vibrant, coverData.lightMuted, coverData.muted)
        animateColors(card.strokeColorStateList?.defaultColor ?: Color.WHITE, cardColor) {
            card.outlineAmbientShadowColor = it
            card.outlineSpotShadowColor = it
            card.strokeColor = it
        }

        cover.setImageDrawable(coverData.cover)

        val activity = try {
            requireActivity()
        } catch (_: Exception) {
            null
        }
        if (activity != null) {
            val bitmap = coverData.background?.fitToScreen(activity)
            if (bitmap != null) root.background = bitmap
        }

        currentSongInfo = songInfo
    }
}