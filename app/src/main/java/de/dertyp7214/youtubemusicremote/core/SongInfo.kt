package de.dertyp7214.youtubemusicremote.core

import android.app.Activity
import android.graphics.drawable.Drawable
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.request.RequestOptions
import de.dertyp7214.youtubemusicremote.types.SongInfo
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.gpu.BrightnessFilterTransformation

data class CoverData(
    var background: Drawable? = null,
    var cover: Drawable? = null,
    var dominant: Int = -1,
    var vibrant: Int = -1,
    var darkVibrant: Int = -1,
    var lightVibrant: Int = -1,
    var muted: Int = -1,
    var darkMuted: Int = -1,
    var lightMuted: Int = -1
)

fun SongInfo.parseImageColors(activity: Activity): SongInfo {
    coverData = CoverData()
    val glide = Glide.with(activity)
    glide.asBitmap().load(imageSrc).apply {
        val palette = Palette.Builder(submit().get()).maximumColorCount(32).generate()
        palette.dominantSwatch?.rgb?.let { coverData!!.dominant = it }
        palette.vibrantSwatch?.rgb?.let { coverData!!.vibrant = it }
        palette.darkVibrantSwatch?.rgb?.let { coverData!!.darkVibrant = it }
        palette.lightVibrantSwatch?.rgb?.let { coverData!!.lightVibrant = it }
        palette.mutedSwatch?.rgb?.let { coverData!!.muted = it }
        palette.darkMutedSwatch?.rgb?.let { coverData!!.darkMuted = it }
        palette.lightMutedSwatch?.rgb?.let { coverData!!.lightMuted = it }
    }
    glide.asDrawable().load(imageSrc).apply {
        coverData!!.cover = submit().get()
        coverData!!.background = apply(
            RequestOptions.bitmapTransform(
                MultiTransformation(
                    BrightnessFilterTransformation(-.5f),
                    BlurTransformation(25, 5)
                )
            )
        ).submit().get()
    }

    return this
}

fun SongInfo.parseImageColorsAsync(activity: Activity, callback: (SongInfo) -> Unit) =
    doAsync({ parseImageColors(activity) }, callback)