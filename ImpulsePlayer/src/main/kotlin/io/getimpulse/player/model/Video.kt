package io.getimpulse.player.model

internal data class Video(
    val title: String?,
    val description: String?,
    val url: String,
    val headers: Map<String, String>,
)