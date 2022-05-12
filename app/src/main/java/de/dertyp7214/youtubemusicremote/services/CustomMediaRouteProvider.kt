package de.dertyp7214.youtubemusicremote.services

import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import androidx.mediarouter.media.MediaControlIntent.*
import androidx.mediarouter.media.MediaRouteDescriptor
import androidx.mediarouter.media.MediaRouteProvider
import androidx.mediarouter.media.MediaRouteProviderDescriptor
import androidx.mediarouter.media.MediaRouter
import de.dertyp7214.youtubemusicremote.R

class CustomMediaRouteProvider(context: Context) : MediaRouteProvider(context) {
    companion object {
        const val CATEGORY_CUSTOM_ROUTE = "de.dertyp7214.youtubemusicremote.CATEGORY_CUSTOM_ROUTE"

        val CONTROL_FILTERS_BASIC = listOf(
            IntentFilter().apply {
                addCategory(CATEGORY_CUSTOM_ROUTE)
                addAction(ACTION_SEEK)
                addAction(ACTION_GET_STATUS)
                addAction(ACTION_PAUSE)
                addAction(ACTION_RESUME)
                addAction(ACTION_STOP)
            }
        )
    }

    init {
        publishRoutes()
    }

    private fun publishRoutes() {
        val routeDescriptor = MediaRouteDescriptor.Builder(
            "customRoute",
            context.resources.getString(R.string.custom_route)
        )
            .setDescription(context.resources.getString(R.string.custom_route_description))
            .addControlFilters(CONTROL_FILTERS_BASIC)
            .setPlaybackStream(AudioManager.STREAM_MUSIC)
            .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
            .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
            .setVolumeMax(10)
            .build()

        val providerDescriptor = MediaRouteProviderDescriptor.Builder()
            .addRoute(routeDescriptor)
            .build()

        descriptor = providerDescriptor
    }

    override fun onCreateRouteController(routeId: String): RouteController {
        return CustomRouteController()
    }

    class CustomRouteController : RouteController()
}