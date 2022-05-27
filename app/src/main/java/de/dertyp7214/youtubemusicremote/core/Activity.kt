package de.dertyp7214.youtubemusicremote.core

import android.app.Activity
import android.content.Intent
import android.net.Uri

fun Activity.verifyInstallerId(): Boolean {
    val validInstallers: List<String> =
        listOf("com.android.vending", "com.google.android.feedback")

    val installer = packageManager.getInstallSourceInfo(packageName).installingPackageName

    return installer != null && validInstallers.contains(installer)
}

fun Activity.openUrl(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))