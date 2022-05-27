package de.dertyp7214.youtubemusicremote.core

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

val Context.preferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

inline val Context.useRatingInNotification: Boolean
    get() = preferences.getBoolean("useRatingInNotification", false)