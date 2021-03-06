package de.dertyp7214.youtubemusicremote.core

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import de.dertyp7214.colorutilsc.ColorUtilsC
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.components.CustomWebSocketListener
import de.dertyp7214.youtubemusicremote.types.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

fun isDark(color: Int): Boolean {
    return ColorUtilsC.calculateLuminance(color) < .5
}

fun getFallBackColor(vararg colors: Int): Int {
    colors.forEach { if (it != Color.TRANSPARENT) return it }
    return Color.TRANSPARENT
}

fun calculateFallbackColor(threshold: Int = 28, diffColor: Int, vararg colors: Int): Int {
    colors.forEach {
        if (it != Color.TRANSPARENT && ColorUtilsC.calculateColorDifference(
                diffColor,
                it
            ) >= threshold
        ) return it
    }
    return ColorUtilsC.invertColor(diffColor)
}

fun animateInts(
    intA: Int, intB: Int, duration: Long = 250, callback: (Int) -> Unit
) {
    if (!((intA - 1)..(intA + 1)).contains(intB)) {
        val animator = ValueAnimator.ofInt(intA, intB)
        animator.duration = duration
        animator.addUpdateListener { callback(it.animatedValue as Int) }
        animator.start()
    }
}

fun animateFloats(
    floatA: Float, floatB: Float, duration: Long = 250, callback: (Float) -> Unit
) {
    if (!((floatA - 1)..(floatA + 1)).contains(floatB)) {
        val animator = ValueAnimator.ofFloat(floatA, floatB)
        animator.duration = duration
        animator.addUpdateListener { callback(it.animatedValue as Float) }
        animator.start()
    }
}

fun View.animateRightMargin(from: Int, to: Int, duration: Long = 250) =
    animateInts(from, to, duration) {
        setMargins(
            marginLeft, marginTop, it, marginBottom
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
    newColor: Int, defaultColor: Int = Color.BLACK, duration: Long = 250, animating: () -> Unit = {}
) {
    animateColors(imageTintList?.defaultColor ?: defaultColor, newColor, duration, animating) {
        imageTintList = ColorStateList.valueOf(it)
    }
}

fun View.animateBackgroundTintList(
    newColor: Int,
    defaultColor: Int = Color.WHITE,
    duration: Long = 250,
    animating: (Int) -> Unit = {}
) {
    animateColors(
        backgroundTintList?.defaultColor ?: defaultColor, newColor, duration
    ) {
        animating(it)
        backgroundTintList = ColorStateList.valueOf(it)
    }
}

fun View.animateForegroundTintList(
    newColor: Int, defaultColor: Int = Color.BLACK, duration: Long = 250, animating: () -> Unit = {}
) {
    animateColors(
        foregroundTintList?.defaultColor ?: defaultColor, newColor, duration, animating
    ) {
        foregroundTintList = ColorStateList.valueOf(it)
    }
}

fun TextView.animateTextColor(newColor: Int, animating: (Int) -> Unit = {}) {
    animateColors(currentTextColor, newColor) { setTextColor(it); animating(it) }
}

fun TextView.changeText(text: String) {
    if (this.text != text) this.text = text
}

fun TextView.changeTextWithLinks(
    text: String,
    fields: List<Field>,
    textColor: Int = textColors.defaultColor,
    callback: (field: Field) -> Unit = {}
) {
    val spannableString = SpannableString(text)
    fields.forEach { field ->
        if (text.contains(field.text)) {
            val startIndex = text.indexOf(field.text)
            val endIndex = startIndex + field.text.length
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(view: View) {
                        callback(field)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = ds.linkColor
                        ds.isUnderlineText = false
                    }
                }, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                ForegroundColorSpan(textColor), startIndex, endIndex, 0
            )
        }
    }
    if (("${this.text}" != "$spannableString" && (fields.isEmpty() || fields.any { text.contains(it.text) })) || textColor != textColors.defaultColor || this.text != text) {
        setOnClickListener(null)
        this.text = spannableString
        movementMethod = LinkMovementMethod.getInstance()
        animateTextColor(textColor)
    }
    if (fields.isEmpty()) setOnClickListener { callback(Field(this.text.toString(), "")) }
}

fun Int.darkenColor(amount: Float): Int {
    return ColorUtilsC.HSLToColor(FloatArray(3).apply {
        ColorUtilsC.colorToHSL(this@darkenColor, this)
        this[2] *= amount
    })
}

@SuppressLint("InternalInsetResource", "DiscouragedApi")
fun Context.getStatusBarHeight(): Int {
    var result = 0
    val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) result = resources.getDimensionPixelSize(resourceId)
    return result + 8.dpToPx(this)
}

fun Fragment.getStatusBarHeight() = requireContext().getStatusBarHeight()

fun checkWebSocket(url: String, gson: Gson, callback: (Boolean, String?) -> Unit) {
    val customWebSocketListener = CustomWebSocketListener()

    try {
        val webSocket = CustomWebSocket(
            if (url.startsWith("ws://")) url else "ws://$url", customWebSocketListener
        )

        customWebSocketListener.onMessage { _, text ->
            try {
                val socketResponse = gson.fromJson(text, SocketResponse::class.java)

                when (socketResponse.action) {
                    Action.STATUS -> {
                        val statusData = gson.fromJson(socketResponse.data, StatusData::class.java)

                        if (statusData.name == "ytmd") callback(true, null)
                        else callback(false, "Invalid name")
                        webSocket.close()
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false, e.localizedMessage)
                webSocket.close()
            }
        }

        customWebSocketListener.onFailure { _, throwable, _ ->
            throwable.printStackTrace()
            callback(false, throwable.localizedMessage)
        }

        webSocket.send(SendAction(Action.STATUS))
    } catch (e: Exception) {
        e.printStackTrace()
        callback(false, e.localizedMessage)
    }
}

fun delayed(timeout: Long = 200, callback: () -> Unit) =
    Handler(Looper.getMainLooper()).postDelayed(callback, timeout)

fun runOnMainThread(block: () -> Unit) = Handler(Looper.getMainLooper()).post(block)