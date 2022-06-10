package de.dertyp7214.composecomponents

import android.content.Context
import android.graphics.Color.*
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.Color as CColor

class ComposeSwitch(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private var checkedChanged: (Boolean) -> Unit = {}
    private var checkedSetter: (Boolean) -> Unit = {}
    private var checkedGetter: () -> Boolean = { true }
    private var colorSetter: (Int) -> Unit = {}
    private var initialValue = true

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeAllViews()
        addView(ComposeView(context).apply {
            setContent {
                var checked by remember { mutableStateOf(initialValue) }
                val checkedTrackColor = remember { mutableStateOf(RED) }
                val lightColor = CColor(LTGRAY)
                val darkColor = CColor(ColorUtils.blendARGB(DKGRAY, GRAY, .5f))
                checkedSetter = { checked = it }
                checkedGetter = { checked }
                colorSetter = {
                    checkedTrackColor.value = it
                }
                Switch(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        checkedChanged(it)
                    },
                    colors = SwitchDefaults.colors(
                        uncheckedTrackColor = lightColor,
                        checkedTrackColor = CColor(checkedTrackColor.value),
                        uncheckedBorderColor = darkColor,
                        uncheckedThumbColor = darkColor
                    )
                )
            }
        })
    }

    fun setCheckedChangedListener(listener: (Boolean) -> Unit) {
        checkedChanged = listener
    }

    fun setColor(color: Int) = colorSetter(color)
    fun setChecked(value: Boolean, callListener: Boolean = true) {
        initialValue = value
        checkedSetter(value)
        if (callListener) checkedChanged(value)
    }

    fun getChecked() = checkedGetter()
}