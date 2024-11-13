package io.getimpulse.player.core

import android.content.Context
import io.getimpulse.player.Navigator
import io.getimpulse.player.extension.requireActivity

/**
 * The native navigator simply determines the activity based on the context.
 */
internal class NativeNavigator(
    private val context: Context,
) : Navigator {

    override fun getCurrentActivity() = context.requireActivity()
}