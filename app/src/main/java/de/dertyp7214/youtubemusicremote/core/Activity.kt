package de.dertyp7214.youtubemusicremote.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import de.dertyp7214.youtubemusicremote.types.SongInfo

fun Activity.verifyInstallerId(): Boolean {
    val validInstallers: List<String> =
        listOf("com.android.vending", "com.google.android.feedback")

    val installer = packageManager.getInstallSourceInfo(packageName).installingPackageName

    return installer != null && validInstallers.contains(installer)
}

fun Activity.openUrl(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

inline val Activity.screenBounds
    get() = windowManager.currentWindowMetrics.bounds

data class ShareInfo(
    val title: String,
    val artist: String,
    val url: String
) {
    companion object {
        fun fromSongInfo(songInfo: SongInfo) =
            ShareInfo(songInfo.title, songInfo.artist, songInfo.url)
    }
}

fun Activity.share(shareInfo: ShareInfo) {
    startActivity(Intent.createChooser(Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(
            Intent.EXTRA_TEXT, """
                Listen to "${shareInfo.title}" by "${shareInfo.artist}"
                
                ${shareInfo.url}
            """.trimIndent()
        )
        type = "text/plain"
    }, null))
}