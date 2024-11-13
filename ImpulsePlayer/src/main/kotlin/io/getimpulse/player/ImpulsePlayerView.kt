package io.getimpulse.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import io.getimpulse.player.component.CommonView
import io.getimpulse.player.component.ControlsView
import io.getimpulse.player.core.Logging
import io.getimpulse.player.core.NativeNavigator
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.core.Sessions
import io.getimpulse.player.extension.setFont
import io.getimpulse.player.extension.setGone
import io.getimpulse.player.extension.setVisible
import io.getimpulse.player.model.PlayerButton
import io.getimpulse.player.model.PlayerDelegate
import io.getimpulse.player.model.Video
import io.getimpulse.player.model.VideoKey
import io.getimpulse.player.sheet.Contract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
class ImpulsePlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val videoKey by lazy { VideoKey.create(this) }

    // NOTE: Lazy root would result in the video surface view rendering wrong when using Flutter.
    private val root = LayoutInflater.from(context).inflate(
        R.layout.view_video_player,
        this,
        true,
    )
    private val commonView by lazy { root.findViewById<CommonView>(R.id.common_view) }
    private val controlView by lazy { playerView.findViewById<ControlsView>(R.id.controls) }
    private val pictureInPictureLayout by lazy { root.findViewById<View>(R.id.pip_layout) }
    private val pictureInPictureText by lazy { root.findViewById<TextView>(R.id.pip_text) }
    private val playerView by lazy {
        root.findViewById<PlayerView>(R.id.player_view).apply {
            @OptIn(UnstableApi::class)
            controllerShowTimeoutMs = ImpulsePlayer.controlsTimeout.inWholeMilliseconds.toInt()
        }
    }

    private var job: Job? = null
    private var navigator: Navigator = NativeNavigator(context)

    init {
        Logging.d("Init")
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        when (visibility) {
            GONE,
            INVISIBLE -> {
                onHide()
            }

            VISIBLE -> {
                onShow()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        job = Job()
        val viewScope = CoroutineScope(Dispatchers.Main + job!!)
        Logging.d("onAttachedToWindow $viewScope")
        registerAppearance(viewScope)

        val session = Sessions.attach(context, videoKey)
        commonView.attach(videoKey)
        controlView.attach(videoKey, object : ControlsView.Delegate.Embedded {
            @OptIn(UnstableApi::class)
            override fun onEnterPictureInPicture() {
                playerView.hideController()
                val ints = intArrayOf(0, 0)
                playerView.getLocationOnScreen(ints)
                val srcRect =
                    Rect(ints[0], ints[1], ints[0] + playerView.width, ints[1] + playerView.height)
                val contract = Contract.PictureInPictureContract(context, videoKey, srcRect)
                viewScope.launch {
                    playerView.player = null // Cleanup
                    pictureInPictureLayout.setVisible()
                    launch {
                        val current = navigator.getCurrentActivity()
                        Navigation.openPictureInPicture(navigator, contract)
                        pictureInPictureLayout.setGone()
                        session.onShow(playerView) // Attach again

                        val intent = Intent(context, current::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        context.startActivity(intent)
                    }
                }
            }

            override fun onEnterFullscreen() {
                val contract = Contract.FullscreenContract(context, videoKey)
                viewScope?.launch {
                    launch {
                        delay(300.milliseconds)
                        playerView.player = null // Cleanup
                    }
                    launch {
                        val currentOrientation = resources.configuration.orientation
                        Navigation.openFullscreen(navigator, contract)
                        session.onShow(playerView) // Attach again
                        navigator.getCurrentActivity().apply {
                            requestedOrientation = currentOrientation // Reset
                            requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED // Prevent force
                        }
                    }
                }
            }
        })
    }

    private fun registerAppearance(viewScope: CoroutineScope) {
        viewScope.launch {
            ImpulsePlayer.getAppearance().collect {
                pictureInPictureText.setFont(it.p2)
            }
        }
    }

    override fun onDetachedFromWindow() {
        Logging.d("onDetachedFromWindow")
        Sessions.detach(videoKey)
        commonView.detach(videoKey)
        controlView.detach(videoKey)

        job?.cancel("Detached")
        job = null
        super.onDetachedFromWindow()
    }

    private fun onShow() {
        Logging.d("onShow")
        Sessions.show(videoKey, playerView)
    }

    private fun onHide() {
        Logging.d("onHide")
        Sessions.hide(videoKey, playerView)
    }

    private fun load(video: Video) {
        Sessions.load(context, videoKey, video)
    }

    // External interface

    fun load(url: String) {
        load(Video(null, null, url))
    }

    fun load(title: String?, subtitle: String?, url: String) {
        load(Video(title, subtitle, url))
    }

    fun play() {
        Sessions.getV(this, videoKey).onPlay()
    }

    fun pause() {
        Sessions.getV(this, videoKey).onPause()
    }

    fun seek(time: Long) {
        Sessions.getV(this, videoKey).onSeek(time)
    }

    fun getState() = Sessions.getV(this, videoKey).getState()
    fun isPlaying() = Sessions.getV(this, videoKey).isPlaying()
    fun getProgress() = Sessions.getV(this, videoKey).getProgress()
    fun getDuration() = Sessions.getV(this, videoKey).getDuration()
    fun getError() = Sessions.getV(this, videoKey).getError()

    fun setDelegate(delegate: PlayerDelegate) {
        Sessions.getV(this, videoKey).addDelegate(delegate)
    }

    fun setButton(key: String, button: PlayerButton) {
        Sessions.getV(this, videoKey).setButton(key, button)
    }

    fun removeDelegate(delegate: PlayerDelegate) {
        Sessions.getV(this, videoKey).removeDelegate(delegate)
    }

    fun removeButton(key: String) {
        Sessions.getV(this, videoKey).removeButton(key)
    }

    // External interop
    internal fun setNavigator(navigator: Navigator?) {
        this.navigator = navigator ?: NativeNavigator(context)
        controlView.setNavigator(if (navigator is NativeNavigator) null else navigator)
    }

    internal fun externalAttach() {
        Sessions.attach(context, videoKey)
    }

    internal fun externalDetach() {
        Sessions.detach(videoKey)
    }
}
