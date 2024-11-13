package io.getimpulse.player.extension

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.util.TypedValue
import androidx.core.content.ContextCompat

internal fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
internal fun Int.spToPx(): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_SP,
    this.toFloat(),
    Resources.getSystem().displayMetrics,
).toInt()

internal fun Int.asColorStateList(context: Context) = ContextCompat.getColorStateList(context, this)
internal fun Int.createColorStateList() = ColorStateList(
    arrayOf(IntArray(0)), // Default state (no specific state)
    intArrayOf(this) // Color for the default state
)
internal fun Int.asColor(context: Context) = ContextCompat.getColor(context, this)
internal fun Int.asInt(context: Context): Int = context.resources.getDimensionPixelSize(this)

internal fun Int.sp() = spToPx()
