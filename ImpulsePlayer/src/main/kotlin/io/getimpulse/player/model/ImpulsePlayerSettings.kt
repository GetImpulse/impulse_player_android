package io.getimpulse.player.model

class ImpulsePlayerSettings(
    internal val pictureInPictureEnabled: Boolean,
    internal val castReceiverApplicationId: String?,
) {

    companion object {
        internal fun default() = ImpulsePlayerSettings(
            pictureInPictureEnabled = false,
            castReceiverApplicationId = null,
        )
    }

    fun isCastEnabled() = castReceiverApplicationId != null
}
