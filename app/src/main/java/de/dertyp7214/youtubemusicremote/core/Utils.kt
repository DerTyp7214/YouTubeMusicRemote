package de.dertyp7214.youtubemusicremote.core

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
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
    return ColorUtils.calculateLuminance(color) < .5
}

fun getFallBackColor(vararg colors: Int): Int {
    colors.forEach { if (it != -1) return it }
    return -1
}

fun animateInts(
    intA: Int,
    intB: Int,
    duration: Long = 250,
    callback: (Int) -> Unit
) {
    if (!((intA - 1)..(intA + 1)).contains(intB)) {
        val animator = ValueAnimator.ofInt(intA, intB)
        animator.duration = duration
        animator.addUpdateListener { callback(it.animatedValue as Int) }
        animator.start()
    }
}

fun View.animateRightMargin(from: Int, to: Int, duration: Long = 250) = animateInts(from, to, duration) {
    setMargins(
        marginLeft,
        marginTop,
        it,
        marginBottom
    )
}

fun animateColors(
    colorA: Int,
    colorB: Int,
    duration: Long = 250,
    animating: () -> Unit = {},
    callback: (Int) -> Unit
) {
    if (colorA != colorB) {
        animating()
        val animator = ValueAnimator.ofArgb(colorA, colorB)
        animator.duration = duration
        animator.addUpdateListener { callback(it.animatedValue as Int) }
        animator.start()
    }
}

fun ImageView.animateImageTintList(
    newColor: Int,
    defaultColor: Int = Color.BLACK,
    duration: Long = 250,
    animating: () -> Unit = {}
) {
    animateColors(imageTintList?.defaultColor ?: defaultColor, newColor, duration, animating) {
        imageTintList = ColorStateList.valueOf(it)
    }
}

fun View.animateForegroundTintList(
    newColor: Int,
    defaultColor: Int = Color.BLACK,
    duration: Long = 250,
    animating: () -> Unit = {}
) {
    animateColors(
        foregroundTintList?.defaultColor ?: defaultColor,
        newColor,
        duration,
        animating
    ) {
        foregroundTintList = ColorStateList.valueOf(it)
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

fun Context.getStatusBarHeight(): Int {
    var result = 0
    val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) result = resources.getDimensionPixelSize(resourceId)
    return result
}