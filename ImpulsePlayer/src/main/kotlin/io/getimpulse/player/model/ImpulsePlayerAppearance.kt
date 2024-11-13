package io.getimpulse.player.model

import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import io.getimpulse.player.R

data class ImpulsePlayerAppearance(
    val h3: ImpulsePlayerFont,
    val h4: ImpulsePlayerFont,
    val s1: ImpulsePlayerFont,
    val l4: ImpulsePlayerFont,
    val l7: ImpulsePlayerFont,
    val p1: ImpulsePlayerFont,
    val p2: ImpulsePlayerFont,
    @ColorInt val accentColor: Int,
) {

    companion object {
        internal fun default() = ImpulsePlayerAppearance(
            h3 = ImpulsePlayerFont(
                sizeSp = 16,
                typeFace = Typeface.DEFAULT_BOLD,
            ),
            h4 = ImpulsePlayerFont(
                sizeSp = 14,
                typeFace = Typeface.DEFAULT_BOLD,
            ),
            s1 = ImpulsePlayerFont(
                sizeSp = 12,
                typeFace = Typeface.DEFAULT,
            ),
            l4 = ImpulsePlayerFont(
                sizeSp = 14,
                typeFace = Typeface.DEFAULT,
            ),
            l7 = ImpulsePlayerFont(
                sizeSp = 10,
                typeFace = Typeface.DEFAULT,
            ),
            p1 = ImpulsePlayerFont(
                sizeSp = 16,
                typeFace = Typeface.DEFAULT,
            ),
            p2 = ImpulsePlayerFont(
                sizeSp = 14,
                typeFace = Typeface.DEFAULT,
            ),
            accentColor = 0x4945FF, // R.color.control_accent,
        )
    }
}
