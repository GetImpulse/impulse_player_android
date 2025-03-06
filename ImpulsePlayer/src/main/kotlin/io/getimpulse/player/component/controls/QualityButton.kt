package io.getimpulse.player.component.controls

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import io.getimpulse.player.R
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.model.VideoKey
import io.getimpulse.player.model.VideoQuality
import io.getimpulse.player.model.VideoQualityType
import io.getimpulse.player.util.Logging
import io.getimpulse.player.util.extension.setVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class QualityButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val root by lazy {
        LayoutInflater.from(context).inflate(R.layout.button_quality, this, true)
    }
    private val icon by lazy { root.findViewById<ImageView>(R.id.icon) }
    private val text by lazy { root.findViewById<TextView>(R.id.text) }

    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var videoKey: VideoKey
    private fun getSession() = SessionManager.require(videoKey)

    fun initialize(videoKey: VideoKey) {
        Logging.d("initialize")
        this.videoKey = videoKey

        viewScope.launch { collectAvailable() }
        viewScope.launch { collectQuality() }
    }

    private suspend fun collectAvailable() {
        getSession().isSelectQualityAvailable().collect { available ->
            setVisible(available)
        }
    }

    private suspend fun collectQuality() {
        getSession().getVideoQuality().collect { videoQuality ->
            val icon = when (videoQuality) {
                VideoQuality.Automatic -> R.drawable.ic_quality_automatic
                is VideoQuality.Detected -> {
                    when (VideoQualityType.from(videoQuality.height)) {
                        VideoQualityType.Standard360p -> null
                        VideoQualityType.HigherStandard480p -> R.drawable.ic_quality_sd
                        VideoQualityType.HighDefinition720p -> R.drawable.ic_quality_hd
                        VideoQualityType.FullHighDefinition1080p -> R.drawable.ic_quality_fhd
                        VideoQualityType.UltraHighDefinition4k2160p -> R.drawable.ic_quality_2k
                        VideoQualityType.UltraHighDefinition8k4320p -> R.drawable.ic_quality_4k
                        null -> null
                    }
                }
            }

            setIcon(icon)
            text.text = if (icon == null) {
                when (videoQuality) {
                    VideoQuality.Automatic -> null
                    is VideoQuality.Detected -> {
                        context.getString(R.string.quality_x_p, videoQuality.height)
                    }
                }
            } else null
        }
    }

    private fun setIcon(@DrawableRes resource: Int?) {
        if (resource == null) {
            icon.setImageDrawable(null)
        } else {
            icon.setImageResource(resource)
        }
    }
}
