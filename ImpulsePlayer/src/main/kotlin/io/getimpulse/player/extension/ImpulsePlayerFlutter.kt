package io.getimpulse.player.extension

import io.getimpulse.player.ImpulsePlayerView
import io.getimpulse.player.Navigator

object ImpulsePlayerFlutter {

    fun setNavigator(view: ImpulsePlayerView, navigator: Navigator?) {
        view.setNavigator(navigator)
    }

    fun externalAttach(view: ImpulsePlayerView) {
        view.externalAttach()
    }

    fun externalDetach(view: ImpulsePlayerView) {
        view.externalDetach()
    }
}