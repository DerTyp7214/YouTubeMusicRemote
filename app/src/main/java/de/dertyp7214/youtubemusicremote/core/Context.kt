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

inline val Context.playlistColumns: Int
    get() = preferences.getInt("playlistColumns", 3)

inline val Context.mirrorBars: Boolean
    get() = preferences.getBoolean("mirrorBars", true)

fun <T> Context.defaultValue(id: String, default: T): T {
    return when (id) {
        "useRatingInNotification" -> useRatingInNotification.parse(default)
        "customLockscreenOnlyWhilePlaying" -> customLockscreenOnlyWhilePlaying.parse(default)
        "useCustomLockScreen" -> useCustomLockScreen.parse(default)
        "visualizeAudio" -> visualizeAudio.parse(default)
        "visualizeSize" -> visualizeSize.parse(default)
        "customLockscreenVisualizeAudioSize" -> customLockscreenVisualizeAudioSize.parse(default)
        "customLockscreenVisualizeAudio" -> customLockscreenVisualizeAudio.parse(default)
        "playlistColumns" -> playlistColumns.parse(default)
        "mirrorBars" -> mirrorBars.parse(default)
        else -> default
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.parse(default: T): T {
    return try {
        this as T
    } catch (e: Exception) {
        default
    }
}

tailrec fun Context?.getActivity(): Activity? {
    return if (this == null) null
    else if (this !is ContextWrapper) null
    else if (this is Activity) this
    else baseContext.getActivity()
}