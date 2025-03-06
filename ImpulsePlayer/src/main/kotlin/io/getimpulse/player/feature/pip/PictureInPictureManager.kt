package io.getimpulse.player.feature.pip

import android.graphics.Rect
import io.getimpulse.player.util.ImpulsePlayerNavigator
import io.getimpulse.player.core.Contracts
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.model.VideoKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object PictureInPictureManager {

    sealed class State {
        data object Inactive : State()
        data class Loading(val key: VideoKey) : State()
        data class Active(val key: VideoKey) : State()
    }

    interface Listener {
        fun onActivated()
        fun onExited(exitedVideoKey: VideoKey)
    }

    private var currentVideoKey: VideoKey? = null
    private val listeners = mutableListOf<Listener>()
    private val state = MutableStateFlow<State>(State.Inactive)

    fun getState() = state.asStateFlow()

    fun register(listener: Listener) {
        listeners.add(listener)
    }

    fun enter(videoKey: VideoKey, navigator: ImpulsePlayerNavigator, srcRect: Rect, listener: Listener) {
        if (this.currentVideoKey != null) {
            exit() // Exit the existing session when creating a new one
        }
        listeners.add(listener)
        currentVideoKey = videoKey

        state.value = State.Loading(videoKey)
        Navigation.openPictureInPicture(navigator, Contracts.PictureInPicture(videoKey, srcRect))
    }

    fun active(videoKey: VideoKey) {
        state.value = State.Active(videoKey)

        listeners.forEach { it.onActivated() }
    }

    fun exit() {
        val exitedVideoKey = requireNotNull(currentVideoKey)
        currentVideoKey = null
        state.value = State.Inactive

        listeners.forEach { it.onExited(exitedVideoKey) }
        listeners.clear()
    }
}