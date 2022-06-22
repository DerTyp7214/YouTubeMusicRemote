@file:Suppress("unused", "UNUSED_PARAMETER")

package de.dertyp7214.youtubemusicremote.core

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.get
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlin.math.roundToInt

fun Bitmap.toDrawable(context: Context): Drawable = BitmapDrawable(context.resources, this)

val Bitmap.dominantColor: Int
    get() {
        val newBitmap = Bitmap.createScaledBitmap(this, 1, 1, true)
        val color = newBitmap[0, 0]
        newBitmap.recycle()
        return color
    }

fun Bitmap.fitToScreen(activity: Activity): Bitmap {
    val rootLayout = activity.window.decorView
    val aspectRatio1 = rootLayout.width.toFloat() / rootLayout.height.toFloat()
    val aspectRatio2 = rootLayout.height.toFloat() / rootLayout.width.toFloat()

    return if (aspectRatio1 <= 1) resize(
        ((width * (1 - aspectRatio1)) / 2).roundToInt(),
        0,
        (width * aspectRatio1).roundToInt(),
        height
    ) else resize(
        0,
        ((height * (1 - aspectRatio2)) / 2).roundToInt(),
        width,
        (height * aspectRatio2).roundToInt()
    )
}

fun Bitmap.resize(x: Int, y: Int, width: Int, height: Int): Bitmap =
    Bitmap.createBitmap(this, x, y, width, height)

fun Bitmap.darken(): Bitmap {
    val canvas = Canvas(this)
    val p = Paint(Color.RED)
    val filter: ColorFilter = LightingColorFilter(0xFF7F7F7F.toInt(), 0x00000000)
    p.colorFilter = filter
    canvas.drawBitmap(this, Matrix(), p)
    return this
}

fun Bitmap.blur(
    context: Context,
    radius: Int = 10,
    sampling: Int = 5,
    callback: (Drawable) -> Unit
) {
    doAsync(
        {
            Glide.with(context).asDrawable()
                .load(this).apply(
                    RequestOptions.bitmapTransform(
                        BlurTransformation(10, 5)
                    )
                ).submit().get()
        }, callback
    )
}