package io.getimpulse.player.util

import io.getimpulse.player.ImpulsePlayerView

/**
 * Hooks for the Flutter package to use the Impulse Player
 */
object ImpulsePlayerFlutter {

    fun setNavigator(view: ImpulsePlayerView, navigator: ImpulsePlayerNavigator?) {
        view.setNavigator(navigator)
    }

    fun externalAttach(view: ImpulsePlayerView) {
        view.externalAttach()
    }

    fun externalDetach(view: ImpulsePlayerView) {
        view.externalDetach()
    }
}