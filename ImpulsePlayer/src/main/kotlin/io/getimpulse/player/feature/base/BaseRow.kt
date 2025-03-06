package io.getimpulse.player.feature.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

internal abstract class BaseRow(
    @LayoutRes layoutId: Int,
    parent: ViewGroup,
): RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
) {
}