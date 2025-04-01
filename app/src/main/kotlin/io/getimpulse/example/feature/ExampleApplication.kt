package io.getimpulse.example.feature

import android.app.Application
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.model.ImpulsePlayerSettings

class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        settings()
    }

    private fun settings() {
        ImpulsePlayer.setSettings(
            ImpulsePlayerSettings(
                pictureInPictureEnabled = true, // Whether Picture-in-Picture is enabled; Default `false` (disabled)
                castReceiverApplicationId = "01128E51", // Cast receiver application id of the cast app; Default `null` (disabled)
            )
        )
    }
}