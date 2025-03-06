package io.getimpulse.player.component.controls

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import io.getimpulse.player.R
import io.getimpulse.player.util.Logging
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.model.Speed
import io.getimpulse.player.util.extension.setVisible
import io.getimpulse.player.model.VideoKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class SpeedButton @JvmOverloads constructor(
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

        viewScope.launch { collectAvailable() }
        viewScope.launch { collectSpeed() }
    }

    private suspend fun collectAvailable() {
        getSession().isSelectSpeedAvailable().collect { available ->
            setVisible(available)
        }
    }

    private suspend fun collectSpeed() {
        getSession().getSpeed().collect {
            setImageResource(
                when (it) {
                    Speed.x0_25 -> R.drawable.ic_speed_0_25
                    Speed.x0_50 -> R.drawable.ic_speed_0_50
                    Speed.x0_75 -> R.drawable.ic_speed_0_75
                    Speed.x1_00 -> R.drawable.ic_speed_1_00
                    Speed.x1_25 -> R.drawable.ic_speed_1_25
                    Speed.x1_50 -> R.drawable.ic_speed_1_50
                    Speed.x1_75 -> R.drawable.ic_speed_1_75
                    Speed.x2_00 -> R.drawable.ic_speed_2_00
                }
            )
        }
    }

    override fun onDetachedFromWindow() {
        viewScope.cancel("Detached")
        super.onDetachedFromWindow()
    }
}