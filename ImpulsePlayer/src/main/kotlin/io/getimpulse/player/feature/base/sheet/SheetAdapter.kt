package io.getimpulse.player.feature.base.sheet

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

internal class SheetAdapter(
    private val listener: Listener,
) : RecyclerView.Adapter<SheetRow>() {

    interface Listener {
        fun onSelected(key: Int)
    }

    data class Row(
        val key: Int,
        val title: String,
        val selected: Boolean,
    )

    private val items = mutableListOf<Row>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SheetRow {
        return SheetRow(parent)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: SheetRow, position: Int) {
        val item = items[position]
        holder.render(item)
        holder.itemView.setOnClickListener {
            listener.onSelected(item.key)
        }
    }

    fun render(rows: List<Row>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }
}