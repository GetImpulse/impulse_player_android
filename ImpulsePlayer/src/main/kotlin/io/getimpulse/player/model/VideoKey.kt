package io.getimpulse.player.model

import io.getimpulse.player.ImpulsePlayerView
import java.util.UUID

internal data class VideoKey private constructor(
    val identifier: String,
) {

    companion object {
        fun create(playerView: ImpulsePlayerView) = VideoKey(UUID.randomUUID().toString())
    }
}