package io.getimpulse.example.feature

import io.getimpulse.example.model.Video

object Settings {

    val Videos = listOf(
        Video(
            "Big Buck Bunny",
            "Blender Foundation",
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            mapOf(),
        ),
        Video(
            "Sintel",
            "Blender Foundation",
            "https://origin.broadpeak.io/bpk-vod/voddemo/hlsv4/5min/sintel/index.m3u8",
            mapOf(),
        ),
        Video(
            "Tears of Steel",
            "Blender Foundation",
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            mapOf(),
        ),
    )
}