package io.getimpulse.player.core

import android.graphics.Rect
import io.getimpulse.player.model.Speed
import io.getimpulse.player.model.VideoKey
import io.getimpulse.player.model.VideoQuality
import java.util.UUID

internal sealed class Contracts {

    companion object {
        private val registered = mutableMapOf<String, Contracts>()

        fun <T: Contracts> get(key: String): T = registered[key] as T

        private fun register(contract: Contracts) {
            registered[contract.key] = contract
        }
    }

    val key = UUID.randomUUID().toString()

    data class Fullscreen(
        val videoKey: VideoKey,
    ) : Contracts() {
        init {
            register(this)
        }
    }

    data class SelectCast(
        val videoKey: VideoKey,
    ) : Contracts() {
        init {
            register(this)
        }
    }

    data class PictureInPicture(
        val videoKey: VideoKey,
        val srcRect: Rect,
    ) : Contracts() {
        init {
            register(this)
        }
    }

    data class SelectSpeed(
        val videoKey: VideoKey,
        val options: List<Speed>,
        val selectedKey: Int,
    ) : Contracts() {
        init {
            register(this)
        }
    }

    data class SelectVideoQuality(
        val videoKey: VideoKey,
        val options: List<VideoQuality>,
        val selectedKey: String,
    ) : Contracts() {
        init {
            register(this)
        }
    }
}
