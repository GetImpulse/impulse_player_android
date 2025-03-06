package io.getimpulse.player

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import io.getimpulse.player.component.controls.ControlsView
import io.getimpulse.player.component.overlay.CastingOverlay
import io.getimpulse.player.component.overlay.ErrorOverlay
import io.getimpulse.player.component.overlay.LoadingOverlay
import io.getimpulse.player.component.overlay.PictureInPictureOverlay
import io.getimpulse.player.core.NativeNavigator
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.feature.fullscreen.FullscreenManager
import io.getimpulse.player.feature.pip.PictureInPictureManager
import io.getimpulse.player.model.PlayerButton
import io.getimpulse.player.model.PlayerDelegate
import io.getimpulse.player.model.Video
import io.getimpulse.player.model.VideoKey
import io.getimpulse.player.util.ImpulsePlayerNavigator
import io.getimpulse.player.util.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val errorOverlay by lazy { root.findViewById<ErrorOverlay>(R.id.error_overlay) }
    private val controlsView by lazy { root.findViewById<ControlsView>(R.id.controls_view) }
    private val castingOverlay by lazy { root.findViewById<CastingOverlay>(R.id.casting_overlay) }
    private val pictureInPictureOverlay by lazy { root.findViewById<PictureInPictureOverlay>(R.id.picture_in_picture_overlay) }
    private val loadingOverlay by lazy { root.findViewById<LoadingOverlay>(R.id.loading_overlay) }
    private val playerView by lazy { root.findViewById<PlayerView>(R.id.player_view) }
    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var navigator: ImpulsePlayerNavigator = NativeNavigator(context)
    private val fullscreenListener by lazy { FullScreenListener() }
    private val pictureInPictureListener by lazy { PictureInPictureListener() }

    init {
        Logging.d("init")
        SessionManager.create(this, videoKey)
        controlsView.initialize(videoKey, object : ControlsView.Delegate.Embedded {
            @OptIn(UnstableApi::class)
            override fun onEnterPictureInPicture() {
                enterPictureInPicture()
            }

            override fun onEnterFullscreen() {
                fullscreenListener.enter()
            }
        })
        castingOverlay.initialize(videoKey)
        pictureInPictureOverlay.initialize(videoKey)
        errorOverlay.initialize(videoKey)
        loadingOverlay.initialize(videoKey)
    }

    private fun getSession() = SessionManager.require(videoKey)

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        Logging.d("onVisibilityChanged")
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
        Logging.d("onAttachedToWindow")

        SessionManager.attach(context, videoKey)
        SessionManager.connect(videoKey, playerView)
        controlsView.attach(videoKey)
        playerView.setOnClickListener {
            controlsView.show()
        }
        pictureInPictureOverlay.setOnClickListener {
            PictureInPictureManager.exit()
        }
    }

    override fun onDetachedFromWindow() {
        Logging.d("onDetachedFromWindow")
        SessionManager.disconnect(videoKey, playerView)
        SessionManager.detach(videoKey)
        controlsView.detach(videoKey)

        viewScope.cancel("Detached")
        super.onDetachedFromWindow()
    }

    private fun enterPictureInPicture() {
        pictureInPictureListener.enter()
    }

    private fun onShow() {
        Logging.d("onShow")
        SessionManager.show(videoKey, context)
    }

    private fun onHide() {
        Logging.d("onHide")
        SessionManager.hide(videoKey, context)
    }

    private fun load(video: Video) {
        SessionManager.load(context, videoKey, video)
        checkPictureInPicture()
    }

    private fun checkPictureInPicture() {
        val pipVideoKey = when (val state = PictureInPictureManager.getState().value) {
            PictureInPictureManager.State.Inactive -> null
            is PictureInPictureManager.State.Loading -> state.key
            is PictureInPictureManager.State.Active -> state.key
        }
        if (pipVideoKey == null) {
            controlsView.hide()
        } else {
            val pipSession = SessionManager.require(pipVideoKey)
            val thisSession = getSession()
            Logging.d("Check pip: ${pipSession.getVideo().value} || ${thisSession.getVideo().value} == ${pipSession.getVideo().value == thisSession.getVideo().value}")
            if (pipSession.getVideo().value == thisSession.getVideo().value) {
                // It's the same, so sync thumbnail and show that we are in PIP
                SessionManager.sync(fromSession = pipSession, toSession = thisSession)
                PictureInPictureManager.register(pictureInPictureListener)
            } else {
                // Different video, so we are not in PIP and don't need to handle this
            }
        }
    }

    // External interface

    fun load(url: String) {
        load(Video(null, null, url))
    }

    fun load(title: String?, subtitle: String?, url: String) {
        load(Video(title, subtitle, url))
    }

    fun play() {
        getSession().onPlay()
    }

    fun pause() {
        getSession().onPause()
    }

    fun seek(time: Long) {
        getSession().onSeek(time)
    }

    fun getState() = getSession().getState()
    fun isPlaying() = getSession().isPlaying()
    fun getProgress() = getSession().getProgress()
    fun getDuration() = getSession().getDuration()
    fun getError() = getSession().getError()

    fun setDelegate(delegate: PlayerDelegate) {
        getSession().addDelegate(delegate)
    }

    fun setButton(key: String, button: PlayerButton) {
        getSession().setButton(key, button)
    }

    fun removeDelegate(delegate: PlayerDelegate) {
        getSession().removeDelegate(delegate)
    }

    fun removeButton(key: String) {
        getSession().removeButton(key)
    }

    // External interop
    internal fun setNavigator(navigator: ImpulsePlayerNavigator?) {
        this.navigator = navigator ?: NativeNavigator(context)
        controlsView.setNavigator(if (navigator is NativeNavigator) null else navigator)
    }

    internal fun externalAttach() {
        SessionManager.attach(context, videoKey)
    }

    internal fun externalDetach() {
        SessionManager.detach(videoKey)
    }

    // Inner classes
    internal inner class FullScreenListener : FullscreenManager.Listener {
        fun enter() {
            Logging.d("Fullscreen: - enter")
            SessionManager.disconnect(videoKey, playerView)

            FullscreenManager.enter(videoKey, navigator, this)
        }

        override fun onActivated() {
            Logging.d("Fullscreen: - onActivated")
        }

        override fun onExited(toPictureInPicture: Boolean) {
            Logging.d("Fullscreen: - onExited $toPictureInPicture")
            SessionManager.connect(videoKey, playerView)
            if (toPictureInPicture) {
                viewScope.launch {
                    delay(750.milliseconds)
                    enterPictureInPicture()
                }
            }
        }
    }

    internal inner class PictureInPictureListener : PictureInPictureManager.Listener {
        fun enter() {
            Logging.d("PictureInPicture: - enter")
            controlsView.hide()
            val ints = intArrayOf(0, 0)
            playerView.getLocationOnScreen(ints)
            val srcRect =
                Rect(ints[0], ints[1], ints[0] + playerView.width, ints[1] + playerView.height)

            SessionManager.disconnect(videoKey, playerView)

            PictureInPictureManager.enter(
                videoKey,
                navigator,
                srcRect,
                this
            )
        }

        override fun onActivated() {
            Logging.d("PictureInPicture: - onActivated")
        }

        override fun onExited(exitedVideoKey: VideoKey) {
            Logging.d("PictureInPicture: - onExited")
            if (exitedVideoKey == videoKey) {
                Logging.d("PictureInPicture: - Connect")
                SessionManager.connect(videoKey, playerView)
            } else {
                Logging.d("PictureInPicture: - Same video, other key")
                // Means that we used the same video as PIP was showing.
                val oldSession = SessionManager.require(exitedVideoKey)
                val thisSession = getSession()
                if (oldSession.getVideo().value == thisSession.getVideo().value) {
                    Logging.d("PictureInPicture: - Sync state")
                    // Video is still the same, sync state
                    SessionManager.sync(fromSession = oldSession, toSession = thisSession)

                    // Start playing if we were paused
                    if (oldSession.isPlaying().value && thisSession.isPlaying().value.not()) {
                        Logging.d("PictureInPicture: - Continue playing")
                        thisSession.onPlay()
                    }
                } else {
                    Logging.d("PictureInPicture: - Video changed: ${oldSession.getVideo().value} != ${thisSession.getVideo().value}")
                }
            }
        }
    }
}
