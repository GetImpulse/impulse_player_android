package io.getimpulse.player.util.extension

import android.util.TypedValue
import android.widget.TextView
import io.getimpulse.player.model.ImpulsePlayerFont

internal fun TextView.setFont(font: ImpulsePlayerFont) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, font.sizeSp.toFloat())
    setTypeface(font.typeFace)
}