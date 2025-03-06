package io.getimpulse.player.util.extension

import android.view.View

internal fun View.setVisible(visible: Boolean? = true) {
    visibility = if (visible == true) View.VISIBLE else View.GONE
}

internal fun View.setGone(gone: Boolean? = true) {
    visibility = if (gone == true) View.GONE else View.VISIBLE
}

internal fun View.setInvisible(invisible: Boolean? = true) {
    visibility = if (invisible == true) View.INVISIBLE else View.VISIBLE
}
