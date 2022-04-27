package de.dertyp7214.youtubemusicremote.core

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.request.RequestOptions
import de.dertyp7214.youtubemusicremote.types.CoverData
import de.dertyp7214.youtubemusicremote.types.SongInfo
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.gpu.BrightnessFilterTransformation

fun SongInfo.parseImageColors(context: Context, currentSongInfo: SongInfo): SongInfo {
    if (currentSongInfo.imageSrc == imageSrc) {
        coverData = currentSongInfo.coverData
        return this
    }

    coverData = CoverData()
    val glide = Glide.with(context)
    glide.asBitmap().load(imageSrc).apply {
        try {
            val palette = Palette.Builder(submit().get()).maximumColorCount(32).generate()
            palette.dominantSwatch?.rgb?.let { coverData!!.dominant = it }
            palette.vibrantSwatch?.rgb?.let { coverData!!.vibrant = it }
            palette.darkVibrantSwatch?.rgb?.let { coverData!!.darkVibrant = it }
            palette.lightVibrantSwatch?.rgb?.let { coverData!!.lightVibrant = it }
            palette.mutedSwatch?.rgb?.let { coverData!!.muted = it }
            palette.darkMutedSwatch?.rgb?.let { coverData!!.darkMuted = it }
            palette.lightMutedSwatch?.rgb?.let { coverData!!.lightMuted = it }
        } catch (_: Exception) {
        }
    }

    val luminance = ColorUtils.calculateLuminance(
        coverData?.dominant ?: Color.WHITE
    ).toFloat()

    glide.asDrawable().load(imageSrc).apply {
        try {
            coverData!!.cover = submit().get()
            coverData!!.background = apply(
                RequestOptions.bitmapTransform(
                    MultiTransformation(
                        BrightnessFilterTransformation(
                            -(.5f * luminance)
                        ),
                        BlurTransformation(25, 5)
                    )
                )
            ).submit().get()
        } catch (_: Exception) {
        }
    }

    coverData!!.parsedDominant = coverData!!.dominant.darkenColor(.5f * luminance)

    return this
}

fun SongInfo.parseImageColorsAsync(
    context: Context,
    currentSongInfo: SongInfo,
    callback: (SongInfo) -> Unit
) = doAsync({ parseImageColors(context, currentSongInfo) }, callback)