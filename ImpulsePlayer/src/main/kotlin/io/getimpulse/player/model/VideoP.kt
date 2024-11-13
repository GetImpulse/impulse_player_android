package io.getimpulse.player.model

internal enum class VideoP(
    val height: Int,
) {
    Standard360p(360),
    HigherStandard480p(480),
    HighDefinition720p(720),
    FullHighDefinition1080p(1080),
    UltraHighDefinition4k2160p(2160),
    UltraHighDefinition8k4320p(4320),
    ;
    companion object {
        fun from(height: Int) = entries.firstOrNull { it.height == height }
    }
}