package de.dertyp7214.youtubemusicremote.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.animateColors
import de.dertyp7214.youtubemusicremote.core.getFallBackColor
import de.dertyp7214.youtubemusicremote.types.SongInfo
import kotlin.math.roundToInt

class CoverFragment : Fragment() {

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

        root = requireActivity().window.decorView

        return v
    }

    fun setSongInfo(songInfo: SongInfo) {
        val coverData = songInfo.coverData ?: return

        val cardColor =
            getFallBackColor(coverData.vibrant, coverData.lightMuted, coverData.muted)
        animateColors(card?.strokeColorStateList?.defaultColor ?: Color.WHITE, cardColor) {
            card?.outlineAmbientShadowColor = it
            card?.outlineSpotShadowColor = it
            card?.strokeColor = it
        }

        cover?.setImageDrawable(coverData.cover)

        val bitmap = coverData.background?.toBitmap()
        val rootLayout = root
        if (bitmap != null && rootLayout != null) {
            val aspectRatio = rootLayout.width.toFloat() / rootLayout.height.toFloat()

            val image = Bitmap.createBitmap(
                bitmap,
                ((bitmap.width * aspectRatio) * .6).roundToInt(),
                0,
                (bitmap.width * aspectRatio).roundToInt(),
                bitmap.height
            )
            rootLayout.background = BitmapDrawable(resources, image)
        }
    }
}