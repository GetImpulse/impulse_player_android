package io.getimpulse.player.model

import io.getimpulse.player.ImpulsePlayer

class ImpulsePlayerSettings private constructor(
    internal val pictureInPictureEnabled: Boolean,
    internal val castReceiverApplicationId: String?,
) {

    companion object {
        internal fun default() = ImpulsePlayerSettings(
            pictureInPictureEnabled = false,
            castReceiverApplicationId = null,
        )
    }

    class Builder {
        private var pictureInPicture: Boolean = false
        private var castReceiverApplicationId: String? = null

        fun withPictureInPicture(): Builder {
            pictureInPicture = true
            return this
        }

        fun withCast(receiverApplicationId: String): Builder {
            castReceiverApplicationId = receiverApplicationId
            return this
        }

        fun apply() {
            ImpulsePlayer.setSettings(
                ImpulsePlayerSettings(
                    pictureInPicture,
                    castReceiverApplicationId,
                )
            )
        }
    }
}
