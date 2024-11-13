package io.getimpulse.player.extension

import android.content.Intent

internal fun Intent.getIntExtra(name: String): Int? {
    if (hasExtra(name).not()) return null
    return getIntExtra(name, -1)
}