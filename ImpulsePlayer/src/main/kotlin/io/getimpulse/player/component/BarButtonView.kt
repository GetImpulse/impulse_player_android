package io.getimpulse.player.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import io.getimpulse.player.R

internal class BarButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val root by lazy {
        LayoutInflater.from(context).inflate(R.layout.view_bar_button, this, true)
    }
    private val icon by lazy { root.findViewById<ImageView>(R.id.icon) }
    private val text by lazy { root.findViewById<TextView>(R.id.text) }

    fun setIcon(@DrawableRes resource: Int?) {
        if (resource == null) {
            icon.setImageDrawable(null)
        } else {
            icon.setImageResource(resource)
        }
    }

    fun setText(text: String?) {
        this.text.text = text
    }
}
