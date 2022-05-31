package de.dertyp7214.youtubemusicremote.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

val Context.preferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

inline val Context.useRatingInNotification: Boolean
    get() = preferences.getBoolean("useRatingInNotification", false)

inline val Context.customLockscreenOnlyWhilePlaying: Boolean
    get() = preferences.getBoolean("customLockscreenOnlyWhilePlaying", false)

inline val Context.useCustomLockScreen: Boolean
    get() = preferences.getBoolean("useCustomLockScreen", false)

inline val Context.visualizeAudio: Boolean
    get() = preferences.getBoolean("visualizeAudio", false)

inline val Context.visualizeSize: Int
    get() = preferences.getInt("visualizeSize", 6)

inline val Context.customLockscreenVisualizeAudioSize: Int
    get() = preferences.getInt("customLockscreenVisualizeAudioSize", 7)

inline val Context.customLockscreenVisualizeAudio: Boolean
    get() = visualizeAudio && preferences.getBoolean("customLockscreenVisualizeAudio", false)

tailrec fun Context?.getActivity(): Activity? {
    return if (this == null) null
    else if (this !is ContextWrapper) null
    else if (this is Activity) this
    else baseContext.getActivity()
}