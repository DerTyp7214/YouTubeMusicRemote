package de.dertyp7214.youtubemusicremote.core

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlin.math.roundToInt

fun Drawable.fitToScreen(activity: Activity): Drawable {
    val bitmap = toBitmap()
    val rootLayout = activity.window.decorView
    val aspectRatio1 = rootLayout.width.toFloat() / rootLayout.height.toFloat()
    val aspectRatio2 = rootLayout.height.toFloat() / rootLayout.width.toFloat()

    return if (aspectRatio1 <= 1) resize(
        activity,
        ((bitmap.width * (1 - aspectRatio1)) / 2).roundToInt(),
        0,
        (bitmap.width * aspectRatio1).roundToInt(),
        bitmap.height
    ) else resize(
        activity,
        0,
        ((bitmap.height * (1 - aspectRatio2)) / 2).roundToInt(),
        bitmap.width,
        (bitmap.height * aspectRatio2).roundToInt()
    )
}

fun Drawable.resize(context: Context, x: Int, y: Int, width: Int, height: Int) =
    BitmapDrawable(context.resources, Bitmap.createBitmap(toBitmap(), x, y, width, height))

fun Drawable.resize(
    context: Context,
    onResize: (width: Int, height: Int, callback: (x: Int, y: Int, width: Int, height: Int) -> Unit) -> Unit,
    callback: (Drawable) -> Unit
) {
    val bitmap = toBitmap()

    onResize(bitmap.width, bitmap.height) { x, y, width, height ->
        callback(
            BitmapDrawable(
                context.resources,
                Bitmap.createBitmap(bitmap, x, y, width, height)
            )
        )
    }
}

fun Drawable.getDominantColor(fallback: Int = Color.BLACK) =
    Palette.Builder(toBitmap()).maximumColorCount(32).generate().dominantSwatch?.rgb ?: fallback