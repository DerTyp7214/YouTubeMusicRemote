package de.dertyp7214.youtubemusicremote.components

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CustomFloatingActionButton(context: Context, attrs: AttributeSet?) :
    FloatingActionButton(context, attrs) {
    @DrawableRes
    var animatedImageSource: Int = -1
        set(value) {
            if (true || field != value) {
                field = value
                ContextCompat.getDrawable(context, value)?.let {
                    try {
                        val animatedImage = it as AnimatedVectorDrawable
                        setImageDrawable(animatedImage)
                        animatedImage.start()
                    } catch (_: Exception) {
                    }
                }
            }
        }
}