package de.dertyp7214.youtubemusicremote.core

import de.dertyp7214.youtubemusicremote.types.PlaylistContent
import de.dertyp7214.youtubemusicremote.types.ShowShelfResultData

fun ShowShelfResultData.toPlaylistContent() = PlaylistContent(
    index,
    "",
    thumbnails,
    title,
    subTitle.joinToString(" • ")
)