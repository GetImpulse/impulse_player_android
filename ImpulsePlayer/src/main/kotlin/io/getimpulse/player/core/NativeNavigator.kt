package io.getimpulse.player.core

import android.content.Context
import io.getimpulse.player.util.ImpulsePlayerNavigator
import io.getimpulse.player.util.extension.requireActivity

/**
 * The native navigator simply determines the activity based on the context.
 */
internal class NativeNavigator(
    private val context: Context,
) : ImpulsePlayerNavigator {

    override fun getCurrentActivity() = context.requireActivity()
}