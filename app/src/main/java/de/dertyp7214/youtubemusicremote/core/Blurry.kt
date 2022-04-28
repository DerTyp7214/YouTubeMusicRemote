package de.dertyp7214.youtubemusicremote.core

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation

fun liveBlur(
    context: Context,
    view: View,
    fps: Int = 20,
    active: () -> Boolean = { true },
    callback: (Drawable?) -> Unit
) {
    fun loop(skipTimeout: Boolean = false) {
        doAsync(
            {
                if (!skipTimeout) Thread.sleep((1000 / fps).toLong())
                Glide.with(context).asDrawable()
                    .load(view.getBitmap()).apply(
                        RequestOptions.bitmapTransform(
                            BlurTransformation(10, 5)
                        )
                    ).submit().get()
            }
        ) {
            callback(it)
            if (active()) loop()
            else callback(null)
        }
    }
    loop(true)
}