package io.getimpulse.player.feature.fullscreen

import io.getimpulse.player.util.ImpulsePlayerNavigator
import io.getimpulse.player.core.Contracts
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.model.VideoKey
import kotlinx.coroutines.flow.MutableStateFlow

internal object FullscreenManager {

    sealed class State {
        data object Inactive : State()
        data class Loading(val key: VideoKey) : State()
        data class Active(val key: VideoKey) : State()
    }

    interface Listener {
        fun onActivated()
        fun onExited(toPictureInPicture: Boolean)
    }

    private var currentVideoKey: VideoKey? = null
    private val listeners = mutableListOf<Listener>()
    private val state = MutableStateFlow<State>(State.Inactive)

    private fun isActive(videoKey: VideoKey) = currentVideoKey == videoKey

    fun register(listener: Listener) {
        listeners.add(listener)
    }

    fun enter(videoKey: VideoKey, navigator: ImpulsePlayerNavigator, listener: Listener) {
        if (this.currentVideoKey != null) throw IllegalStateException("Already in fullscreen")
        listeners.add(listener)
        currentVideoKey = videoKey

        state.value = State.Loading(videoKey)
        Navigation.openFullscreen(navigator, Contracts.Fullscreen(videoKey))
    }

    fun active(videoKey: VideoKey) {
        state.value = State.Active(videoKey)

        listeners.forEach { it.onActivated() }
    }

    fun exit(toPictureInPicture: Boolean) {
        currentVideoKey = null
        state.value = State.Inactive

        listeners.forEach { it.onExited(toPictureInPicture) }
        listeners.clear()
    }
}
