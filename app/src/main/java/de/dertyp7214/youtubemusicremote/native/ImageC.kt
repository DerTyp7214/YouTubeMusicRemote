package de.dertyp7214.youtubemusicremote.native

import android.graphics.Bitmap

internal object ImageC {
    external fun blurBitmap(bitmap: Bitmap?, radius: Int, unpin: Int, forceLess: Int): Int
}