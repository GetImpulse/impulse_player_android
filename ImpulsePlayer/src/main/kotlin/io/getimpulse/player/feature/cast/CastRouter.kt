package io.getimpulse.player.feature.cast

import android.content.Context
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouter.RouteInfo
import io.getimpulse.player.util.Logging

internal class CastRouter(
    private val context: Context,
    private val listener: Listener,
) {
    interface Listener {
        fun onRoutesChanged(routes: List<CastDisplay.Route>)
    }

    companion object {
        private const val DefaultRouteId = "DEFAULT_ROUTE" // As defined by the Android OS
    }

    private val router = MediaRouter.getInstance(context)
    private val callback = MediaRouterCallback()

    init {
        router.addCallback(
            createMediaSelector(),
            callback,
            MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY, // Better when not active
//                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN // When foreground?
        )
    }

    fun dispose() {
        router.removeCallback(callback)
    }

    fun isDefault(routeId: String): Boolean {
        return routeId == DefaultRouteId
    }

    fun isSelected(routeId: String): Boolean {
        val route = router.routes.firstOrNull {
            it.id == routeId
        } ?: return false
        return router.selectedRoute.id == route.id
    }

    fun select(routeId: String) {
        val route = router.routes.firstOrNull {
            it.id == routeId
        } ?: throw IllegalStateException("Missing route")

        Logging.d("Selected route (from ${router.selectedRoute.name}) to: ${route.name}")
        route.select()
    }

    private fun notifyRoutesChanged() {
        val routes = router.routes
            .asSequence()
            .map { route ->
                Logging.d("- ${route.id}: [${route.deviceType}] ${route.name} - ${route.description} | selected:${route.isSelected} enabled:${route.isEnabled}")
                route
            }
            .groupBy { it.connectionState }
            .flatMap { (state, routes) ->
                when (state) {
                    RouteInfo.CONNECTION_STATE_DISCONNECTED -> {
                        // Show the available routes
                        routes
                    }

                    RouteInfo.CONNECTION_STATE_CONNECTING -> {
                        // Only show the selected one
                        routes.firstOrNull { it.isSelected }?.let {
                            listOf(it)
                        } ?: routes
                    }

                    RouteInfo.CONNECTION_STATE_CONNECTED -> {
                        // Only show the selected one
                        routes.firstOrNull { it.isSelected }?.let {
                            listOf(it)
                        } ?: routes
                    }

                    else -> throw IllegalStateException("Unhandled state: $state")
                }
            }
            .sortedWith { routeLeft, routeRight ->
                when {
                    routeLeft.id == "DEFAULT_ROUTE" -> -1 // left should be first
                    routeRight.id == "DEFAULT_ROUTE" -> 1  // right should be first
                    else -> routeLeft.name.compareTo(routeRight.name)
                }
            }
            .map {
                CastDisplay.Route(it.id, it.name)
            }
        listener.onRoutesChanged(routes)
    }

    private fun createMediaSelector(): MediaRouteSelector {
        return MediaRouteSelector.Builder()
            .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
            .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
            .build()
    }

    private inner class MediaRouterCallback : MediaRouter.Callback() {

        override fun onRouteAdded(router: MediaRouter, route: RouteInfo) {
            Logging.d("onRouteAdded")
            notifyRoutesChanged()
        }

        override fun onRouteRemoved(router: MediaRouter, route: RouteInfo) {
            Logging.d("onRouteRemoved")
            notifyRoutesChanged()
        }

        override fun onRouteChanged(router: MediaRouter, route: RouteInfo) {
            Logging.d("onRouteChanged")
            notifyRoutesChanged()
        }

        override fun onProviderAdded(router: MediaRouter, provider: MediaRouter.ProviderInfo) {
            Logging.d("onProviderAdded")
            notifyRoutesChanged()
        }

        override fun onProviderRemoved(router: MediaRouter, provider: MediaRouter.ProviderInfo) {
            Logging.d("onProviderRemoved")
            notifyRoutesChanged()
        }

        override fun onProviderChanged(router: MediaRouter, provider: MediaRouter.ProviderInfo) {
            Logging.d("onProviderChanged")
            notifyRoutesChanged()
        }
    }
}