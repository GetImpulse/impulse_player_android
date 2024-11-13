package io.getimpulse.player

import io.getimpulse.player.model.ImpulsePlayerAppearance
import io.getimpulse.player.model.ImpulsePlayerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.seconds

object ImpulsePlayer {

    internal val controlsTimeout = 3.seconds

    private val appearance = MutableStateFlow(ImpulsePlayerAppearance.default())
    private val settings = MutableStateFlow(ImpulsePlayerSettings.default())

    internal fun getAppearance() = appearance.asStateFlow()
    internal fun getSettings() = settings.asStateFlow()

    fun setAppearance(appearance: ImpulsePlayerAppearance) {
        this.appearance.value = appearance
    }

    fun setSettings(settings: ImpulsePlayerSettings) {
        this.settings.value = settings
    }
}
