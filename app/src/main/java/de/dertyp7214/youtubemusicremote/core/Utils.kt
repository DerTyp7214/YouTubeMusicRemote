package de.dertyp7214.youtubemusicremote.core

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.bumptech.glide.request.FutureTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


fun doInBackground(doInBackground: () -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        withContext(Dispatchers.Default) { doInBackground() }
    }
}

fun <T> doAsync(doInBackground: () -> T, getResult: (result: T) -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        withContext(Dispatchers.Default) {
            val result = doInBackground()
            CoroutineScope(Dispatchers.Main).launch {
                getResult(result)
            }
        }
    }
}

infix fun <T> (() -> T).asyncInto(into: (T) -> Unit) = doAsync(this, into)
infix fun <T> FutureTarget<T>.asyncInto(into: (T) -> Unit) = doAsync(::get, into)

fun isDark(color: Int): Boolean {
    return ColorUtils.calculateLuminance(color) < 0.5
}

fun getFallBackColor(vararg colors: Int): Int {
    colors.forEach { if (it != -1) return it }
    return -1
}

fun animateColors(
    colorA: Int,
    colorB: Int,
    duration: Long = 250,
    callback: (Int) -> Unit
) {
    if (colorA != colorB) {
        val animator = ValueAnimator.ofArgb(colorA, colorB)
        animator.duration = duration
        animator.addUpdateListener { callback(it.animatedValue as Int) }
        animator.start()
    }
}

fun ImageView.animateImageTintList(newColor: Int, defaultColor: Int = Color.WHITE) {
    animateColors(imageTintList?.defaultColor ?: defaultColor, newColor) {
        imageTintList = ColorStateList.valueOf(it)
    }
}

fun TextView.animateTextColor(newColor: Int) {
    animateColors(currentTextColor, newColor) { setTextColor(it) }
}

fun TextView.changeText(text: String) {
    if (this.text != text) this.text = text
}

fun Int.darkenColor(amount: Float): Int {
    return Color.HSVToColor(FloatArray(3).apply {
        Color.colorToHSV(this@darkenColor, this)
        this[2] *= amount
    })
}