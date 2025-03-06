package io.getimpulse.player.feature.cast

import android.annotation.SuppressLint
import android.content.Context
import io.getimpulse.player.util.ImpulsePlayerNavigator
import io.getimpulse.player.core.Contracts
import io.getimpulse.player.util.Logging
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.model.Video
import io.getimpulse.player.model.VideoKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// https://android.googlesource.com/platform/frameworks/base.git/+/master/core/java/com/android/internal/app/MediaRouteChooserDialog.java
internal object CastManager {

    sealed class State {
        data object Initializing : State()
        data object Inactive : State()
        data object Loading : State()
        data object Active : State()
    }

    private var clients = 0
    @SuppressLint("StaticFieldLeak") // Is being handled
    private var castRouter: CastRouter? = null
    @SuppressLint("StaticFieldLeak") // Is being handled
    private var castInteractor: CastInteractor? = null
    private val state = MutableStateFlow<State>(State.Initializing)
    private val routes = MutableStateFlow(listOf<CastDisplay.Route>())
    private val video = MutableStateFlow<Video?>(null)
    private val playbackPlaying = MutableStateFlow(false)
    private val playbackProgress = MutableStateFlow(0L)
    private val playbackDuration = MutableStateFlow(0L)

    fun getRoutes() = routes.asStateFlow()
    fun getState() = state.asStateFlow()
    fun getPlaybackPlaying() = playbackPlaying.asStateFlow()
    fun getPlaybackProgress() = playbackProgress.asStateFlow()
    fun getPlaybackDuration() = playbackDuration.asStateFlow()
    fun getVideo() = video.asStateFlow()

    fun attach(context: Context) {
        clients += 1
        Logging.d("Attach $clients")
        if (castInteractor == null) {
            val castRouterListener = object : CastRouter.Listener {
                override fun onRoutesChanged(routes: List<CastDisplay.Route>) {
                    this@CastManager.routes.value = routes
                }
            }
            val castInteractorListener = object : CastInteractor.Listener {
                override fun onStateChanged(state: State) {
                    this@CastManager.state.value = state
                }

                override fun onMediaLoaded(video: Video?, playing: Boolean) {
                    this@CastManager.video.value = video
                    playbackPlaying.value = playing
                }

                override fun onEnded() {
                    video.value = null
                }

                override fun onPlaybackStarted() {
                    playbackPlaying.value = true
                }

                override fun onPlaybackPaused() {
                    playbackPlaying.value = false
                }

                override fun onPlaybackProgress(progress: Long, duration: Long) {
                    playbackDuration.value = duration
                    playbackProgress.value = progress
                }
            }
            castRouter = CastRouter(context.applicationContext, castRouterListener)
            castInteractor = CastInteractor(context.applicationContext, castInteractorListener)
        }
    }

    fun detach() {
        clients -= 1
        Logging.d("Detach $clients")
        if (clients == 0) {
            castRouter?.dispose()
            castInteractor?.dispose()

            castRouter = null
            castInteractor = null
            state.value = State.Initializing
            routes.value = listOf()
        }
    }

    fun open(navigator: ImpulsePlayerNavigator, videoKey: VideoKey, video: Video?) {
        when (state.value) {
            State.Initializing -> throw IllegalStateException("Cannot open yet")
            State.Loading -> {
                Logging.d("Loading, replacing video")
                replace(video)
            }

            State.Inactive -> {
                Logging.d("Inactive, opening select sheet")
                openSelect(navigator, videoKey)
            }

            State.Active -> {
                if (video == this.video.value) {
                    Logging.d("Active, opening select sheet")
                    openSelect(navigator, videoKey)
                } else {
                    Logging.d("Active, replacing video")
                    replace(video)
                }
            }
        }
    }

    private fun openSelect(navigator: ImpulsePlayerNavigator, videoKey: VideoKey) {
        val contract = Contracts.SelectCast(videoKey)
        Navigation.openSelectCast(navigator, contract)
    }

    fun isSelected(routeId: String): Boolean {
        val castRouter = castRouter ?: throw IllegalStateException("Not attached")
        return castRouter.isSelected(routeId)
    }

    fun select(route: CastDisplay.Route, castVideo: Video?) {
        val castRouter = castRouter ?: throw IllegalStateException("Not attached")
        when {
            castRouter.isSelected(route.id) -> {
                Logging.d("Route already selected, ignoring!")
            }

            castRouter.isDefault(route.id) -> {
                Logging.d("Default route, stop casting")
                video.value = null
                stopCasting()
            }

            else -> {
                Logging.d("Changed route, casting $castVideo")
                stopCasting() // Changed route, stop casting there
                video.value = castVideo
                castRouter.select(route.id) // Selecting new route
                replace(castVideo) // Start casting on new route if possible
            }
        }
    }

    fun play() {
        val castInteractor = castInteractor ?: throw IllegalStateException("Not attached")
        Logging.d("play")
        castInteractor.doPlay()
    }

    fun pause() {
        val castInteractor = castInteractor ?: throw IllegalStateException("Not attached")
        Logging.d("pause")
        castInteractor.doPause()
    }

    fun seek(time: Long) {
        val castInteractor = castInteractor ?: throw IllegalStateException("Not attached")
        Logging.d("forward")
        castInteractor.seek(time)
    }

    private fun replace(castVideo: Video?) {
        video.value = castVideo
        if (castVideo == null) {
            stopCasting()
        } else {
            startCasting(castVideo)
        }
    }

    private fun startCasting(castVideo: Video) {
        val castInteractor = castInteractor ?: throw IllegalStateException("Not attached")
        castInteractor.start(castVideo)
    }

    private fun stopCasting() {
        val castInteractor = castInteractor ?: throw IllegalStateException("Not attached")
        castInteractor.stop()
    }
}