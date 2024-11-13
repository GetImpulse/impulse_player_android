package io.getimpulse.player.extension

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

internal fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

internal fun Context.requireActivity() = requireNotNull(
    getActivity(),
    { "Context is of class: ${this::class.java}" }
)