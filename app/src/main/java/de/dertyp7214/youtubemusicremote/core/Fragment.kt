package de.dertyp7214.youtubemusicremote.core

import android.widget.FrameLayout
import androidx.fragment.app.Fragment

fun <T: Fragment> T.resizeFragment(newWidth: Int, newHeight: Int): T {
    view?.layoutParams = FrameLayout.LayoutParams(newWidth, newHeight)
    view?.requestLayout()
    return this
}