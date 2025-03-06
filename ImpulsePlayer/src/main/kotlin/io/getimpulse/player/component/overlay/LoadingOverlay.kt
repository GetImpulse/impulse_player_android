package io.getimpulse.player.component.overlay

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ProgressBar
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.R
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.feature.cast.CastManager
import io.getimpulse.player.model.PlayerState
import io.getimpulse.player.model.VideoKey
import io.getimpulse.player.util.Logging
import io.getimpulse.player.util.extension.createColorStateList
import io.getimpulse.player.util.extension.setGone
import io.getimpulse.player.util.extension.setVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class LoadingOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val root by lazy {
        LayoutInflater.from(context).inflate(R.layout.overlay_loading, this, true)
    }
    private val loader by lazy { root.findViewById<ProgressBar>(R.id.loader) }

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
        setOnClickListener {
            // Hijack
        }
        viewScope.launch { collectAppearance() }
        viewScope.launch { collectState() }
    }

    private suspend fun collectAppearance() {
        ImpulsePlayer.getAppearance().collect {
            loader.indeterminateTintList = it.accentColor.createColorStateList()
        }
    }

    private suspend fun collectState() {
        combine(
            getSession().getState(),
            getSession().getCastState(),
        ) { sessionState, castState ->
            val visibleBySessionState = when (sessionState) {
                PlayerState.Loading -> true
                is PlayerState.Error,
                PlayerState.Ready -> false
            }
            val visibleByCastState = when (castState) {
                CastManager.State.Loading -> true
                CastManager.State.Initializing,
                CastManager.State.Inactive,
                CastManager.State.Active -> false
            }
            setVisible(visibleBySessionState || visibleByCastState)
        }.collect()
    }

    override fun onDetachedFromWindow() {
        viewScope.cancel("Detached")
        super.onDetachedFromWindow()
    }
}
