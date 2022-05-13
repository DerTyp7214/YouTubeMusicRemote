@file:Suppress("unused")

package de.dertyp7214.youtubemusicremote.core

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
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

data class Size(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

fun Drawable.resize(
    context: Context,
    onResize: (width: Int, height: Int) -> Size
): Drawable {
    val bitmap = toBitmap()

    val size = onResize(bitmap.width, bitmap.height)
    return BitmapDrawable(
        context.resources,
        Bitmap.createBitmap(bitmap, size.x, size.y, size.width, size.height)
    )
}

val Drawable.dominantColor: Int
    get() {
        val newBitmap = Bitmap.createScaledBitmap(toBitmap(), 1, 1, true)
        val color = newBitmap.getPixel(0, 0)
        newBitmap.recycle()
        return color
    }

fun Drawable.blur(
    context: Context,
    radius: Int = 10,
    sampling: Int = 5,
    callback: (Drawable) -> Unit
) = toBitmap().blur(context, radius, sampling, callback)