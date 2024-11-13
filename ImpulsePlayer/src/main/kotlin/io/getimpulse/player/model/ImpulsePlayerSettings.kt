package io.getimpulse.player.model

data class ImpulsePlayerSettings(
    val pictureInPictureEnabled: Boolean,
) {

    companion object {
        internal fun default() = ImpulsePlayerSettings(
            pictureInPictureEnabled = false,
        )
    }
}
