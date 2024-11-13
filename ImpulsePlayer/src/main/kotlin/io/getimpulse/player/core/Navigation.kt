package io.getimpulse.player.core

import androidx.appcompat.app.AppCompatActivity
import io.getimpulse.player.Navigator
import io.getimpulse.player.model.Speed
import io.getimpulse.player.model.VideoQuality
import io.getimpulse.player.sheet.Contract

internal object Navigation {

    private val contracts = mutableMapOf<String, Contract<*>>()

    fun <T : Contract<*>> getContract(key: String): T? = contracts[key] as? T

    suspend fun selectVideoQuality(
        navigator: Navigator,
        contract: Contract.VideoQualityContract,
    ): VideoQuality? {
        start(navigator, contract)
        return contract.getResult()
    }

    suspend fun selectSpeed(
        navigator: Navigator,
        contract: Contract.SpeedContract,
    ): Speed? {
        start(navigator, contract)
        return contract.getResult()
    }

    suspend fun openPictureInPicture(
        navigator: Navigator,
        contract: Contract.PictureInPictureContract,
    ) {
        start(navigator, contract)
        navigator.getCurrentActivity().overridePendingTransition(0, 0)
        contract.getResult()
    }

    suspend fun openFullscreen(
        navigator: Navigator,
        contract: Contract.FullscreenContract,
    ) {
        start(navigator, contract)
        contract.getResult()
    }

    private fun start(navigator: Navigator, contract: Contract<*>) {
        contracts[contract.key] = contract
        navigator.getCurrentActivity().startActivity(contract.createIntent())
    }

    fun finish(activity: AppCompatActivity) {
        activity.finish()
    }
}