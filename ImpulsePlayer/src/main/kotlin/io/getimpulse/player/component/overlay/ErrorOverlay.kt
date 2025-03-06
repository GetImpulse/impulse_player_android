package io.getimpulse.player.component.overlay

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.R
import io.getimpulse.player.util.Logging
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.util.extension.setFont
import io.getimpulse.player.util.extension.setGone
import io.getimpulse.player.util.extension.setVisible
import io.getimpulse.player.model.PlayerState
import io.getimpulse.player.model.VideoKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class ErrorOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val root by lazy {
        LayoutInflater.from(context).inflate(R.layout.overlay_error, this, true)
    }
    private val errorTitle by lazy { root.findViewById<TextView>(R.id.error_title) }
    private val errorDescription by lazy { root.findViewById<TextView>(R.id.error_description) }
    private val retry by lazy { root.findViewById<Button>(R.id.retry) }

    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var videoKey: VideoKey

    init {
        setGone()
    }

    private fun getSession() = SessionManager.require(videoKey)

    fun initialize(videoKey: VideoKey) {
        Logging.d("initialize")
        this.videoKey = videoKey

        viewScope.launch { collectAppearance() }
        viewScope.launch { collectState() }
        registerListeners()
    }

    private suspend fun collectAppearance() {
        ImpulsePlayer.getAppearance().collect {
            errorTitle.setFont(it.h3)
            errorDescription.setFont(it.l4)
            retry.setFont(it.h4)
        }
    }

    private suspend fun collectState() {
        getSession().getState().collect { state ->
            when (state) {
                PlayerState.Loading -> {
                    setGone()
                }

                is PlayerState.Error -> {
                    setVisible()
                    errorDescription.text = state.message
                }

                is PlayerState.Ready -> {
                    setGone()
                }
            }
        }
    }

    private fun registerListeners() {
        retry.setOnClickListener {
            getSession().onRetry()
        }
    }

    override fun onDetachedFromWindow() {
        viewScope.cancel("Detached")
        super.onDetachedFromWindow()
    }
}
