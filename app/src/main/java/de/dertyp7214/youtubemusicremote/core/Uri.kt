package de.dertyp7214.youtubemusicremote.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

fun Uri.toBitmap(context: Context): Bitmap {
    val parcelFileDescriptor = context.contentResolver.openFileDescriptor(this, "r")
    val fileDescriptor = parcelFileDescriptor?.fileDescriptor
    val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
    parcelFileDescriptor?.close()
    return image
}