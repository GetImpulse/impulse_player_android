package io.getimpulse.player.model

import androidx.annotation.DrawableRes

data class PlayerButton(
    val position: Position,
    @DrawableRes val icon: Int,
    val title: String,
    val action: () -> Unit,
) {

    enum class Position {
        TopEnd,
        BottomStart,
        BottomEnd,
    }
}