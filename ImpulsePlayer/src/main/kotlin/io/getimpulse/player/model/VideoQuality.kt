package io.getimpulse.player.model

internal sealed class VideoQuality(
    val key: String,
) {

    data object Automatic : VideoQuality("automatic")

    data class Detected(
        val group: Int,
        val index: Int,
        val selected: Boolean,
        val width: Int,
        val height: Int,
        val bitrate: Int,
    ) : VideoQuality(height.toString())
}
