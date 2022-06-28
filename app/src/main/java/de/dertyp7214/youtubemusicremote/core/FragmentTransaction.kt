package de.dertyp7214.youtubemusicremote.core

import androidx.fragment.app.FragmentTransaction
import de.dertyp7214.youtubemusicremote.R

fun FragmentTransaction.enterAnimations() {
    setCustomAnimations(
        R.anim.slide_in_bottom,
        R.anim.slide_out_top,
        R.anim.slide_in_bottom,
        R.anim.slide_out_top
    )
}

fun FragmentTransaction.exitAnimations() {
    setCustomAnimations(
        R.anim.slide_in_top,
        R.anim.slide_out_bottom,
        R.anim.slide_in_top,
        R.anim.slide_out_bottom
    )
}