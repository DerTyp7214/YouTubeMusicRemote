package de.dertyp7214.audiovisualization.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import de.dertyp7214.audiovisualization.R
import de.dertyp7214.mathc.AudioVisualization
import kotlin.math.roundToInt
import kotlin.random.Random

class AudioVisualizerView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    View(context, attrs) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

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

    init {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.AudioVisualizerView, defStyle, 0)

        size = typedArray.getInt(R.styleable.AudioVisualizerView_size, size)
        if (typedArray.getBoolean(R.styleable.AudioVisualizerView_testInput, false))
            mutableAudioData.value = ArrayList<Short>().apply {
                for (i in 0 until size) add(Random.nextInt(255).toShort())
            }

        typedArray.recycle()
    }

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

            val cornerBottomSpace = AudioVisualization.calculateBottomSpace(
                x,
                width,
                bottomLeftCorner.radius,
                bottomRightCorner.radius,
                barWidth,
                barHeight
            )

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

    private fun <T> filledList(entry: T, amount: Int): List<T> {
        val tmp = ArrayList<T>()
        for (i in 0 until amount) tmp.add(entry)
        return tmp
    }

    class AudioStream(initialData: List<Short> = listOf()) {
        private val audioData = MutableLiveData(initialData)

        fun setNewAudioData(data: List<Short>) = audioData.postValue(data)

        fun observeAudioData(activity: FragmentActivity, callback: (List<Short>) -> Unit) =
            audioData.observe(activity, callback)
    }

    data class Corner(val radius: Int)
}