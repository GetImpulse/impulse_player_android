package io.getimpulse.player.model

sealed class PlayerState {
    data object Loading : PlayerState()
    data class Error(val message: String) : PlayerState()
    data object Ready : PlayerState()
}