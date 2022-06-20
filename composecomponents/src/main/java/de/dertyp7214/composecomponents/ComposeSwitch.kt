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
import de.dertyp7214.colorutilsc.ColorUtilsC
import androidx.compose.ui.graphics.Color as CColor

class ComposeSwitch(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private var checkedChanged: (Boolean) -> Unit = {}
    private var checkedSetter: (Boolean) -> Unit = {}
    private var checkedGetter: () -> Boolean = { true }
    private var borderSetter: (Boolean) -> Unit = {}
    private var colorSetter: (Int) -> Unit = {}
    private var initialValue = true
    private var initialColor = RED

    var hasBorder = true
        set(value) {
            field = value
            borderSetter(value)
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeAllViews()
        addView(ComposeView(context).apply {
            setContent {
                var checked by remember { mutableStateOf(initialValue) }
                val checkedTrackColor = remember { mutableStateOf(initialColor) }
                val lightColor = CColor(LTGRAY)
                val darkColor = CColor(ColorUtilsC.blendARGB(DKGRAY, GRAY, .5f))
                var hasBorder by remember { mutableStateOf(hasBorder) }
                checkedSetter = { checked = it }
                checkedGetter = { checked }
                colorSetter = { checkedTrackColor.value = it }
                borderSetter = { hasBorder = it }
                Switch(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        checkedChanged(it)
                    },
                    colors = SwitchDefaults.colors(
                        uncheckedTrackColor = if (hasBorder) lightColor else darkColor,
                        checkedTrackColor = CColor(checkedTrackColor.value),
                        uncheckedBorderColor = darkColor,
                        uncheckedThumbColor = if (hasBorder) darkColor else lightColor
                    )
                )
            }
        })
    }

    fun setCheckedChangedListener(listener: (Boolean) -> Unit) {
        checkedChanged = listener
    }

    fun setColor(color: Int) {
        initialColor = color
        colorSetter(color)
    }

    fun setChecked(value: Boolean, callListener: Boolean = true) {
        initialValue = value
        checkedSetter(value)
        if (callListener) checkedChanged(value)
    }

    fun getChecked() = checkedGetter()
}