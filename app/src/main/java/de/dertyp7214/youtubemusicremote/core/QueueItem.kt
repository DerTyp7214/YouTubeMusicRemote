package de.dertyp7214.youtubemusicremote.core

import de.dertyp7214.youtubemusicremote.types.QueueItem

fun QueueItem.clone(
    image: String? = null,
    title: String? = null,
    artist: String? = null,
    duration: String? = null,
    videoId: String? = null,
    position: String? = null,
    currentlyPlaying: Boolean? = null
) = QueueItem(
    image ?: this.image,
    title ?: this.title,
    artist ?: this.artist,
    duration ?: this.duration,
    videoId ?: this.videoId,
    position ?: this.position,
    currentlyPlaying ?: this.currentlyPlaying,
)