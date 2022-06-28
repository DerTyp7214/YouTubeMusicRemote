package de.dertyp7214.youtubemusicremote.core

import android.content.SharedPreferences
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager

fun <T: Fragment> T.resizeFragment(newWidth: Int, newHeight: Int): T {
    view?.layoutParams = FrameLayout.LayoutParams(newWidth, newHeight)
    view?.requestLayout()
    return this
}
val <T: Fragment> T.preferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(requireContext())

inline val <T: Fragment> T.useRatingInNotification: Boolean
    get() = preferences.getBoolean("useRatingInNotification", false)

inline val <T: Fragment> T.customLockscreenOnlyWhilePlaying: Boolean
    get() = preferences.getBoolean("customLockscreenOnlyWhilePlaying", false)

inline val <T: Fragment> T.useCustomLockScreen: Boolean
    get() = preferences.getBoolean("useCustomLockScreen", false)

inline val <T: Fragment> T.visualizeAudio: Boolean
    get() = preferences.getBoolean("visualizeAudio", false)

inline val <T: Fragment> T.visualizeSize: Int
    get() = preferences.getInt("visualizeSize", 6)

inline val <T: Fragment> T.customLockscreenVisualizeAudioSize: Int
    get() = preferences.getInt("customLockscreenVisualizeAudioSize", 7)

inline val <T: Fragment> T.customLockscreenVisualizeAudio: Boolean
    get() = visualizeAudio && preferences.getBoolean("customLockscreenVisualizeAudio", false)

inline val <T: Fragment> T.playlistColumns: Int
    get() = preferences.getInt("playlistColumns", 3)