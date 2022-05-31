package de.dertyp7214.audiovisualization.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import kotlin.math.roundToInt

class AudioVisualizerView(context: Context, attrs: AttributeSet? = null) :
    AppCompatImageView(context, attrs) {
    private val mutableAudioData = MutableLiveData<List<Short>>()
    private val mutableBottomLeftCorner = MutableLiveData(Corner(0))
    private val mutableBottomRightCorner = MutableLiveData(Corner(0))
    private val mutableColor = MutableLiveData(Color.WHITE)

    private val audioData
        get() = mutableAudioData.value ?: listOf()
    private val bottomLeftCorner
        get() = mutableBottomLeftCorner.value ?: Corner(0)
    private val bottomRightCorner
        get() = mutableBottomRightCorner.value ?: Corner(0)
    private val color
        get() = mutableColor.value ?: Color.WHITE

    var size: Int = 64
    var visualize: Boolean = true

    fun setColor(color: Int) = this.mutableColor.postValue(color)
    fun setAudioData(audioData: List<Short>, mirrored: Boolean = false) =
        this.mutableAudioData.postValue(audioData.let {
            if (mirrored) {
                val data = audioData.changeSize(size / 2)
                data + data.reversed()
            } else it.changeSize(size)
        })

    fun setBottomLeftCorner(corner: Corner) = this.mutableBottomLeftCorner.postValue(corner)
    fun setBottomRightCorner(corner: Corner) = this.mutableBottomRightCorner.postValue(corner)

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        viewTreeObserver.addOnGlobalLayoutListener {
            context.getActivity()?.let { activity ->
                if (activity is FragmentActivity) {
                    mutableAudioData.observe(activity) {
                        invalidate()
                    }

                    mutableColor.observe(activity) {
                        invalidate()
                    }
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let { c -> drawOnBitmap(audioData, c) }
    }

    private fun drawOnBitmap(audioData: List<Short>, canvas: Canvas) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        val space = (width / size) / 10
        val barWidth = width / size - space

        canvas.drawRect(0f, 0f, 0f, 0f, Paint().apply {
            color = Color.TRANSPARENT
        })
        if (!visualize) return

        val paint = Paint()
        paint.color = color
        var i = 0
        var x = space / 2
        while (i < audioData.size) {
            val barHeight = height / 256f * (audioData[i] * .2f)

            val cornerBottomSpace = if (barHeight != 0f) {
                val leftRadius = bottomLeftCorner.radius * 1.1f
                val rightRadius = bottomRightCorner.radius * 1.1f
                if (x + barWidth < bottomLeftCorner.radius * 1.1f) {
                    val point = leftRadius - x
                    point.easeInQuad(1f / leftRadius * point)
                } else if (x - barWidth > (width - bottomRightCorner.radius * 1.1f)) {
                    val point = bottomRightCorner.radius - (width - x)
                    point.easeInQuad(1f / rightRadius * point)
                } else 0f
            } else 0f

            canvas.drawRect(
                x,
                height - barHeight - cornerBottomSpace,
                width - (width - x) + barWidth,
                height,
                paint
            )

            i++
            x += barWidth + space
        }
    }

    private fun Number.easeInQuad(x: Float) = toFloat() * (x * x)

    private fun List<Short>.changeSize(newSize: Int): List<Short> {
        val tmp = ArrayList<Short>()
        val factor = newSize.toFloat() / size
        if (factor > 1f) {
            forEach {
                for (i in 0 until factor.roundToInt()) if (tmp.isNotEmpty()) tmp.add((tmp.last() + it / 2).toShort())
                else tmp.add(it)
            }
        } else {
            val f1 = (size.toFloat() / newSize).roundToInt()
            forEachIndexed { index, _ ->
                if (index % f1 == 0) tmp.add(get(index))
            }
        }
        return tmp
    }

    private tailrec fun Context?.getActivity(): Activity? {
        return if (this == null) null
        else if (this !is ContextWrapper) null
        else if (this is Activity) this
        else baseContext.getActivity()
    }

    class AudioStream(initialData: List<Short> = listOf()) {
        private val audioData = MutableLiveData(initialData)

        fun setNewAudioData(data: List<Short>) = audioData.postValue(data)

        fun observeAudioData(activity: FragmentActivity, callback: (List<Short>) -> Unit) =
            audioData.observe(activity, callback)
    }

    data class Corner(val radius: Int)
}