package io.getimpulse.player.sheet

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import io.getimpulse.player.component.FullscreenActivity
import io.getimpulse.player.component.PictureInPictureActivity
import io.getimpulse.player.core.Logging
import io.getimpulse.player.model.Speed
import io.getimpulse.player.model.VideoKey
import io.getimpulse.player.model.VideoQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.util.UUID

internal sealed class Contract<R>(val context: Context) {

    sealed class State {
        data object Pending : State()
        data object Result : State()
    }

    abstract fun createIntent(): Intent

    private val state = MutableStateFlow<State>(State.Pending)
    private var result: R? = null
    val key = UUID.randomUUID().toString()

    fun getState() = state.asStateFlow()

    class VideoQualityContract(
        context: Context,
        val options: List<VideoQuality>,
        val selected: Int?,
    ) : Contract<VideoQuality>(context) {

        override fun createIntent(): Intent {
            return VideoQualitySheet.createIntent(context, key)
        }
    }

    class SpeedContract(
        context: Context,
        val options: List<Speed>,
        val selected: Int?,
    ) : Contract<Speed>(context) {
        override fun createIntent(): Intent {
            return SpeedSheet.createIntent(context, key)
        }
    }

    class FullscreenContract(
        context: Context,
        val videoKey: VideoKey,
    ) : Contract<Unit>(context) {
        override fun createIntent(): Intent {
            return FullscreenActivity.createIntent(context, key)
        }
    }

    class PictureInPictureContract(
        context: Context,
        val videoKey: VideoKey,
        val srcRect: Rect,
    ) : Contract<Unit>(context) {
        override fun createIntent(): Intent {
            return PictureInPictureActivity.createIntent(context, key)
        }
    }

    fun onResult(data: R?) {
        result = data
        state.value = State.Result
    }

    suspend fun getResult(): R? {
        Logging.d("Waiting for result...")
        state.filter { it != State.Pending }.first()
        return result
    }

}