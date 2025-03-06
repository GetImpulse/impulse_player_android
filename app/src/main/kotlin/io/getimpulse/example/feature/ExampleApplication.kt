package io.getimpulse.example

import android.app.Application
import io.getimpulse.player.model.ImpulsePlayerSettings

class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        settings()
    }

    private fun settings() {
        ImpulsePlayerSettings.Builder()
            .withPictureInPicture() // Default disabled
            .withCast("01128E51") // Default disabled
            .apply()
    }
}