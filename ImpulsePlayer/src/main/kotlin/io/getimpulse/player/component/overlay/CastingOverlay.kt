package io.getimpulse.player.component.overlay

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.R
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.feature.cast.CastManager
import io.getimpulse.player.model.VideoKey
import io.getimpulse.player.util.Logging
import io.getimpulse.player.util.extension.setFont
import io.getimpulse.player.util.extension.setGone
import io.getimpulse.player.util.extension.setVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class CastingOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val root by lazy {
        LayoutInflater.from(context).inflate(R.layout.overlay_casting, this, true)
    }
    private val text by lazy { root.findViewById<TextView>(R.id.text) }

    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var videoKey: VideoKey

    init {
        setGone()
    }

    private fun getSession() = SessionManager.require(videoKey)

    fun initialize(videoKey: VideoKey) {
        Logging.d("initialize")
        this.videoKey = videoKey

        // Setup
        setupListeners()
        setupCollects()
    }

    private fun setupListeners() {

    }

    private fun setupCollects() {
        viewScope.launch {
            ImpulsePlayer.getAppearance().collect { appearance ->
                text.setFont(appearance.p2)
            }
        }
        viewScope.launch {
            getSession().getCastState().collect { castState ->
                val visible = when (castState) {
                    CastManager.State.Initializing -> false
                    CastManager.State.Inactive -> false
                    CastManager.State.Loading -> true
                    CastManager.State.Active -> true
                }
                setVisible(visible)
            }
        }
    }

    override fun onDetachedFromWindow() {
        viewScope.cancel("Detached")
        super.onDetachedFromWindow()
    }
}
