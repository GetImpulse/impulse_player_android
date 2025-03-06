package io.getimpulse.player.core

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.getimpulse.player.util.ImpulsePlayerNavigator
import io.getimpulse.player.feature.SpeedSheet
import io.getimpulse.player.feature.VideoQualitySheet
import io.getimpulse.player.feature.cast.CastSheet
import io.getimpulse.player.feature.fullscreen.FullscreenActivity
import io.getimpulse.player.feature.pip.PictureInPictureActivity

internal object Navigation {

    fun openSelectCast(
        navigator: ImpulsePlayerNavigator,
        contract: Contracts.SelectCast,
    ) {
        start(navigator, CastSheet.createIntent(navigator.getCurrentActivity(), contract))
    }

    fun openFullscreen(
        navigator: ImpulsePlayerNavigator,
        contract: Contracts.Fullscreen,
    ) {
        start(navigator, FullscreenActivity.createIntent(navigator.getCurrentActivity(), contract))
    }

    fun openPictureInPicture(
        navigator: ImpulsePlayerNavigator,
        contract: Contracts.PictureInPicture,
    ) {
        start(
            navigator,
            PictureInPictureActivity.createIntent(navigator.getCurrentActivity(), contract),
        )
        navigator.getCurrentActivity().overridePendingTransition(0, 0)
    }

    fun openSelectSpeed(
        navigator: ImpulsePlayerNavigator,
        contract: Contracts.SelectSpeed,
    ) {
        start(navigator, SpeedSheet.createIntent(navigator.getCurrentActivity(), contract))
    }

    fun openSelectVideoQuality(
        navigator: ImpulsePlayerNavigator,
        contract: Contracts.SelectVideoQuality,
    ) {
        start(navigator, VideoQualitySheet.createIntent(navigator.getCurrentActivity(), contract))
    }

    private fun start(navigator: ImpulsePlayerNavigator, intent: Intent) {
        navigator.getCurrentActivity().startActivity(intent)
    }

    fun finish(activity: AppCompatActivity) {
        activity.finish()
    }
}
