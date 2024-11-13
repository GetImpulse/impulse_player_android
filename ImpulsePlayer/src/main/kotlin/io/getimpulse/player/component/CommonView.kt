package io.getimpulse.player.component

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.R
import io.getimpulse.player.core.Logging
import io.getimpulse.player.core.Session
import io.getimpulse.player.core.Sessions
import io.getimpulse.player.extension.asColorStateList
import io.getimpulse.player.extension.createColorStateList
import io.getimpulse.player.extension.setFont
import io.getimpulse.player.extension.setGone
import io.getimpulse.player.extension.setVisible
import io.getimpulse.player.model.PlayerState
import io.getimpulse.player.model.VideoKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class CommonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val root by lazy {
        LayoutInflater.from(context).inflate(R.layout.view_common, this, true)
    }
    private val loader by lazy { root.findViewById<ProgressBar>(R.id.loader) }
    private val errorLayout by lazy { root.findViewById<View>(R.id.error_layout) }
    private val errorTitle by lazy { root.findViewById<TextView>(R.id.error_title) }
    private val errorDescription by lazy { root.findViewById<TextView>(R.id.error_description) }
    private val retry by lazy { root.findViewById<Button>(R.id.retry) }

    private var job: Job? = null

    @OptIn(UnstableApi::class)
    fun attach(key: VideoKey) {
        job = Job()
        val viewScope = CoroutineScope(Dispatchers.Main + job!!)

        val session = Sessions.attach(context, key)
        setupView()
        registerAppearance(viewScope)
        registerJobs(viewScope, session)
        registerListeners(session)
    }

    private fun setupView() {

    }

    private fun registerAppearance(viewScope: CoroutineScope) {
        viewScope?.launch {
            ImpulsePlayer.getAppearance().collect {
                loader.indeterminateTintList = it.accentColor.createColorStateList()
                errorTitle.setFont(it.h3)
                errorDescription.setFont(it.l4)
                retry.setFont(it.h4)
            }
        }
    }

    private fun registerJobs(viewScope: CoroutineScope, session: Session) {
        Logging.d("Register jobs: $viewScope")
        viewScope?.launch {
            session.getState().collect { state ->
                when (state) {
                    PlayerState.Loading -> {
                        loader.setVisible()
                        errorLayout.setGone()
                    }

                    is PlayerState.Error -> {
                        loader.setGone()
                        errorLayout.setVisible()
                        errorDescription.text = state.message
                    }

                    is PlayerState.Ready -> {
                        loader.setGone()
                        errorLayout.setGone()
                    }
                }
            }
        }
    }

    private fun registerListeners(session: Session) {
        retry.setOnClickListener {
            session.onRetry()
        }
    }

    fun detach(key: VideoKey) {
        Sessions.detach(key)

        job?.cancel("Detached")
        job = null
    }
}
