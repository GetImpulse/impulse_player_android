package io.getimpulse.player.core

import io.getimpulse.player.model.Speed
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal object Formatter {

    fun time(duration: Duration): String {
        val hours = duration.inWholeHours
        val minutes = (duration - hours.hours).inWholeMinutes
        val seconds = (duration - minutes.minutes).inWholeSeconds
        return when {
            hours > 0 -> "$hours:${formatLeading(minutes)}:${formatLeading(seconds)}"
            else -> "$minutes:${formatLeading(seconds)}"
        }
    }

    private fun formatLeading(value: Long): String {
        return if (value < 10) {
            "0$value"
        } else {
            value.toString()
        }
    }

    fun speed(speed: Speed): String {
        return when (speed) {
            Speed.x0_25 -> "0.25"
            Speed.x0_50 -> "0.5"
            Speed.x0_75 -> "0.75"
            Speed.x1_00 -> "1"
            Speed.x1_25 -> "1.25"
            Speed.x1_50 -> "1.5"
            Speed.x1_75 -> "1.75"
            Speed.x2_00 -> "2"
        } + "x"
    }
}