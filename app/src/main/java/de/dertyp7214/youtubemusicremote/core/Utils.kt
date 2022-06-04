package de.dertyp7214.youtubemusicremote.core

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
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
import androidx.core.graphics.ColorUtils
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.components.CustomWebSocketListener
import de.dertyp7214.youtubemusicremote.types.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

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
    return ColorUtils.calculateLuminance(color) < .5
}

fun getFallBackColor(vararg colors: Int): Int {
    colors.forEach { if (it != Color.TRANSPARENT) return it }
    return Color.TRANSPARENT
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
        this.text = spannableString
        movementMethod = LinkMovementMethod.getInstance()
        animateTextColor(textColor)
    }
}

fun Int.darkenColor(amount: Float): Int {
    return Color.HSVToColor(FloatArray(3).apply {
        Color.colorToHSV(this@darkenColor, this)
        this[2] *= amount
    })
}

@SuppressLint("InternalInsetResource", "DiscouragedApi")
fun Context.getStatusBarHeight(): Int {
    var result = 0
    val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) result = resources.getDimensionPixelSize(resourceId)
    return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2) result * 2 else result + 8.dpToPx(
        this
    )
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

fun getColorDifference(a: Int, b: Int): Double {
    val lab1 = doubleArrayOf(.0, .0, .0)
    val lab2 = doubleArrayOf(.0, .0, .0)
    ColorUtils.colorToLAB(a, lab1)
    ColorUtils.colorToLAB(b, lab2)
    return sqrt(
        (lab2[0] - lab1[0]).pow(2.0) + (lab2[1] - lab1[1]).pow(2.0) + (lab2[2] - lab1[2]).pow(2.0)
    )
}

fun invertColor(color: Int): Int {
    val a = Color.alpha(color)
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)
    return Color.argb(a, a - r, a - g, a - b)
}