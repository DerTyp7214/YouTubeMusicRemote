package de.dertyp7214.youtubemusicremote.components

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.dpToPx
import de.dertyp7214.youtubemusicremote.core.getFallBackColor
import de.dertyp7214.youtubemusicremote.core.isDark
import de.dertyp7214.youtubemusicremote.types.CoverData
import de.dertyp7214.youtubemusicremote.types.Lyrics

class LyricsBottomSheet : BaseBottomSheet() {
    private val lyricsLiveData: MutableLiveData<Lyrics> = MutableLiveData()
    private val coverDataLiveData: MutableLiveData<CoverData> = MutableLiveData()

    var lyrics: Lyrics
        get() = lyricsLiveData.value ?: Lyrics("", "", "")
        set(value) = lyricsLiveData.postValue(value)

    var coverData: CoverData
        get() = coverDataLiveData.value ?: CoverData()
        set(value) {
            if (coverData != value) coverDataLiveData.postValue(value)
        }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        return NestedScrollView(context).apply {
            val scrollView = this
            background = ContextCompat.getDrawable(context, R.drawable.top_round)
            isVerticalFadingEdgeEnabled = true

            setPadding(8.dpToPx(context))

            addView(TextView(context).apply {
                textSize = 16f
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER

                fun setLyrics(lyrics: String) {
                    text = lyrics.let { text ->
                        val lines = text.split('\n')
                        var spannable = SimpleSpanBuilder() + SimpleSpanBuilder.Span(
                            "${this@LyricsBottomSheet.lyrics.title}\n\n",
                            StyleSpan(Typeface.BOLD),
                            RelativeSizeSpan(1.8f)
                        )
                        lines.forEach { line ->
                            spannable += if (line.trim().startsWith("[")) SimpleSpanBuilder.Span(
                                "\n$line\n",
                                StyleSpan(Typeface.BOLD),
                                RelativeSizeSpan(1.2f)
                            )
                            else SimpleSpanBuilder.Span("$line\n")
                        }
                        spannable.build()
                    }
                }

                fun setColors(coverData: CoverData) {
                    val background =
                        getFallBackColor(coverData.darkVibrant, coverData.muted, coverData.vibrant)
                    scrollView.backgroundTintList = ColorStateList.valueOf(background)

                    if (isDark(background)) setTextColor(Color.WHITE)
                    else setTextColor(Color.BLACK)
                }

                lyricsLiveData.observe(this@LyricsBottomSheet) {
                    setLyrics(it.lyrics)
                    blurFunction(false)
                }

                coverDataLiveData.observe(this@LyricsBottomSheet) {
                    setColors(it)
                }

                state.observe(this@LyricsBottomSheet) { state ->
                    when (state) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            scrollView.background = ColorDrawable(Color.WHITE)
                            setColors(coverData)
                        }
                        else -> {
                            scrollView.background =
                                ContextCompat.getDrawable(requireContext(), R.drawable.top_round)
                            setColors(coverData)
                        }
                    }
                }

                setLyrics(lyrics.lyrics)
                setColors(coverData)
            })
        }
    }
}