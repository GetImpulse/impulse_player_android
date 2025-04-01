package io.getimpulse.player.feature.base.sheet

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.R
import io.getimpulse.player.feature.base.BaseRow
import io.getimpulse.player.util.extension.asColorStateList
import io.getimpulse.player.util.extension.createColorStateList
import io.getimpulse.player.util.extension.setFont
import io.getimpulse.player.util.extension.setGone
import io.getimpulse.player.util.extension.setVisible
import kotlin.math.roundToInt

internal class SheetRow(
    parent: ViewGroup,
    private val showSelected: Boolean,
) : BaseRow(R.layout.row_sheet, parent) {

    private val icon by lazy { itemView.findViewById<ImageView>(R.id.icon) }
    private val title by lazy { itemView.findViewById<TextView>(R.id.title) }
    private val indicator by lazy { itemView.findViewById<ImageView>(R.id.indicator) }

    init {
        indicator.setVisible(showSelected)
    }

    fun render(row: SheetAdapter.Row) {
        val appearance = ImpulsePlayer.getAppearance().value

        val background =
            ColorUtils.setAlphaComponent(appearance.accentColor, (0.1f * 255).roundToInt())
        icon.backgroundTintList = background.createColorStateList()
        icon.imageTintList = appearance.accentColor.createColorStateList()

        if (row.icon == null) {
            icon.setImageDrawable(null)
        } else {
            icon.setImageResource(row.icon)
        }
        icon.setGone(row.icon == null)

        title.setFont(appearance.p1)

        indicator.imageTintList = if (row.selected) {
            appearance.accentColor.createColorStateList()
        } else null

        title.text = row.title
        indicator.setImageResource(
            if (row.selected) R.drawable.ic_radio_active else R.drawable.ic_radio_default
        )
    }
}