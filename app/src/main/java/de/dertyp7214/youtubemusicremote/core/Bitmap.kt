@file:Suppress("unused", "UNUSED_PARAMETER")

package de.dertyp7214.youtubemusicremote.core

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation


fun Bitmap.toDrawable(context: Context): Drawable = BitmapDrawable(context.resources, this)

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