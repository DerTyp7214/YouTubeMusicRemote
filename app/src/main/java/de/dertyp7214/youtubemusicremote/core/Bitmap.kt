package de.dertyp7214.youtubemusicremote.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

fun Bitmap.toDrawable(context: Context): Drawable = BitmapDrawable(context.resources, this)

fun Bitmap.resize(x: Int, y: Int, width: Int, height: Int) =
    Bitmap.createBitmap(this, x, y, width, height)