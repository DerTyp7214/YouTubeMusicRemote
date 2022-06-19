@file:Suppress("unused")

package de.dertyp7214.youtubemusicremote.core

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap


fun Drawable.fitToScreen(activity: Activity): Drawable {
    return toBitmap().fitToScreen(activity).toDrawable(activity)
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
        return toBitmap().dominantColor
    }

fun Drawable.blur(
    context: Context,
    radius: Int = 10,
    sampling: Int = 5,
    callback: (Drawable) -> Unit
) = toBitmap().blur(context, radius, sampling, callback)