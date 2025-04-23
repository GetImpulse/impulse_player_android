package io.getimpulse.player.component.controls

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import io.getimpulse.player.R
import io.getimpulse.player.util.Logging
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.feature.cast.CastManager
import io.getimpulse.player.util.extension.setGone
import io.getimpulse.player.util.extension.setVisible
import io.getimpulse.player.model.VideoKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class CastButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var videoKey: VideoKey
    private fun getSession() = SessionManager.require(videoKey)

    fun initialize(videoKey: VideoKey) {
        Logging.d("initialize")
        this.videoKey = videoKey

        viewScope.launch { listenCastState() }
    }

    private suspend fun listenCastState() {
        suspend fun showCastLoading() {
            while (true) {
                setImageResource(R.drawable.ic_cast_loading_1)
                delay(250.milliseconds)
                setImageResource(R.drawable.ic_cast_loading_2)
                delay(250.milliseconds)
                setImageResource(R.drawable.ic_cast_loading_3)
                delay(250.milliseconds)
            }
        }
        getSession().getCastState().collectLatest { state ->
            when (state) {
                CastManager.State.Initializing -> {
                    // Ignore
                }

                CastManager.State.Loading -> {
                    showCastLoading()
                }

                CastManager.State.Inactive -> {
                    setImageResource(R.drawable.ic_cast)
                }

                CastManager.State.Active -> {
                    setImageResource(R.drawable.ic_cast_connected)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        viewScope.cancel("Detached")
        super.onDetachedFromWindow()
    }
}