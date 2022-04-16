package de.dertyp7214.youtubemusicremote.core

import de.dertyp7214.youtubemusicremote.api.YouTubeThumbnail
import de.dertyp7214.youtubemusicremote.api.YouTubeVideoSnippet

fun YouTubeVideoSnippet.getThumbnail(): YouTubeThumbnail? {
    return thumbnails.high ?: thumbnails.medium ?: thumbnails.default
}