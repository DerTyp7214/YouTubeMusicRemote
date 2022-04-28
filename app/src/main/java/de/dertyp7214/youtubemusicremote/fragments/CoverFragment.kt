package de.dertyp7214.youtubemusicremote.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.animateColors
import de.dertyp7214.youtubemusicremote.core.fitToScreen
import de.dertyp7214.youtubemusicremote.core.getFallBackColor
import de.dertyp7214.youtubemusicremote.types.SongInfo

class CoverFragment : Fragment() {

    private var currentSongInfo = SongInfo()

    private var cover: ImageView? = null
    private var card: MaterialCardView? = null

    private var root: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_cover, container, false)

        cover = v.findViewById(R.id.cover)
        card = v.findViewById(R.id.card)

        root = requireActivity().findViewById(R.id.root)

        return v
    }

    fun setSongInfo(songInfo: SongInfo) {
        if (currentSongInfo == songInfo) return

        val coverData = songInfo.coverData ?: return

        val cardColor =
            getFallBackColor(coverData.vibrant, coverData.lightMuted, coverData.muted)
        animateColors(card?.strokeColorStateList?.defaultColor ?: Color.WHITE, cardColor) {
            card?.outlineAmbientShadowColor = it
            card?.outlineSpotShadowColor = it
            card?.strokeColor = it
        }

        cover?.setImageDrawable(coverData.cover)

        val activity = try {
            requireActivity()
        } catch (_: Exception) {
            null
        }
        if (activity != null) {
            val bitmap = coverData.background?.fitToScreen(activity)
            if (bitmap != null) root?.background = bitmap
        }

        currentSongInfo = songInfo
    }
}