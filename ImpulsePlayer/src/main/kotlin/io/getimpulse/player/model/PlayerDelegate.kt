package io.getimpulse.player.model

interface PlayerDelegate {
    fun onReady() {}
    fun onPlay() {}
    fun onPause() {}
    fun onFinish() {}
    fun onError(message: String) {}
}