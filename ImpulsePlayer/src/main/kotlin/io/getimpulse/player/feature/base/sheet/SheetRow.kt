package io.getimpulse.player.feature.base.sheet

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.R
import io.getimpulse.player.feature.base.BaseRow
import io.getimpulse.player.util.extension.createColorStateList
import io.getimpulse.player.util.extension.setFont

internal class SheetRow(parent: ViewGroup) : BaseRow(R.layout.row_sheet, parent) {

    private val title by lazy { itemView.findViewById<TextView>(R.id.title) }
    private val indicator by lazy { itemView.findViewById<ImageView>(R.id.indicator) }

    fun render(row: SheetAdapter.Row) {
        val appearance = ImpulsePlayer.getAppearance().value
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