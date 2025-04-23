package io.getimpulse.player.core

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import io.getimpulse.player.R
import io.getimpulse.player.feature.cast.CastManager
import io.getimpulse.player.model.PlayerButton
import io.getimpulse.player.model.PlayerDelegate
import io.getimpulse.player.model.PlayerState
import io.getimpulse.player.model.Speed
import io.getimpulse.player.model.Video
import io.getimpulse.player.model.VideoQuality
import io.getimpulse.player.util.Logging
import io.getimpulse.player.util.extension.getVideoQualities
import io.getimpulse.player.util.extension.setVideoQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SuppressLint("UnsafeOptInUsageError")
internal class Session(
    private val context: Context,
) {

    companion object {
        private val SeekInterval = 10.seconds
    }

    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private var attachs = 0
    private var connects = 0
    private var shows = 0

    private val currentVideo = MutableStateFlow<Video?>(null)
    private val jobsCommon = mutableListOf<Job>()
    private var jobProgress: Job? = null
    private val delegates = mutableListOf<PlayerDelegate>()
    private val buttons = MutableStateFlow(mapOf<String, PlayerButton>())

    // Common
    private val pictureInPictureAvailable = MutableStateFlow(false)
    private val playing = MutableStateFlow(false)
    private val selectQualityAvailable = MutableStateFlow(false)
    private val selectSpeedAvailable = MutableStateFlow(false)
    private val progress = MutableStateFlow(0L)
    private val duration = MutableStateFlow(0L)

    // Player
    private var player: ExoPlayer? = null
    private val playerState = MutableStateFlow<PlayerState>(PlayerState.Loading)
    private val playerPlaying = MutableStateFlow(false)
    private val playerVideoQuality = MutableStateFlow<VideoQuality>(VideoQuality.Automatic)
    private val playerSpeed = MutableStateFlow(Speed.x1_00)
    private val playerBuffer = MutableStateFlow(0L)
    private val playerProgress = MutableStateFlow(0L)
    private val playerDuration = MutableStateFlow(0L)

    // Cast
    private val castState = MutableStateFlow<CastManager.State>(CastManager.State.Initializing)
    private val castPlaying = MutableStateFlow(false)
    private val castProgress = MutableStateFlow(0L)
    private val castDuration = MutableStateFlow(0L)

    // Picture in Picture
//    private val pipState = MutableStateFlow(PictureInPictureState.Initializing)

    fun getState() = playerState.asStateFlow()
    fun isPlaying() = playing.asStateFlow()
    fun getVideo() = currentVideo.asStateFlow()
    fun getPlayerBuffer() = playerBuffer.asStateFlow()
    fun getProgress() = progress.asStateFlow()
    fun getDuration() = duration.asStateFlow()
    fun getVideoQuality() = playerVideoQuality.asStateFlow()
    fun getSpeed() = playerSpeed.asStateFlow()
    fun getCastState() = castState.asStateFlow()
    fun isPictureInPictureAvailable() = pictureInPictureAvailable.asStateFlow()
    fun isSelectQualityAvailable() = selectQualityAvailable.asStateFlow()
    fun isSelectSpeedAvailable() = selectSpeedAvailable.asStateFlow()
    fun getError() = getState().map { (it as? PlayerState.Error)?.message }
        .stateIn(scope, SharingStarted.Lazily, null)

    fun getButtons() = buttons.asStateFlow()

    fun getAvailableVideoQualities(): List<VideoQuality> {
        val detected = getPlayer().getVideoQualities().sortedByDescending { it.height }
        return listOf(VideoQuality.Automatic) + detected
    }

    fun getAvailableSpeeds() = Speed.all()

    fun addDelegate(delegate: PlayerDelegate) {
        delegates.add(delegate)
    }

    fun setButton(key: String, button: PlayerButton) {
        val updated = buttons.value.toMutableMap()
        updated[key] = button
        buttons.value = updated
    }

    fun removeDelegate(delegate: PlayerDelegate) {
        delegates.remove(delegate)
    }

    fun removeButton(key: String) {
        val updated = buttons.value.toMutableMap()
        updated.remove(key)
        buttons.value = updated
    }

    fun onLoad(video: Video) {
        Logging.d("onLoad: $video")
        reset()
        currentVideo.value = video
    }

    private fun getPlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context)
                .setSeekBackIncrementMs(SeekInterval.inWholeMilliseconds)
                .setSeekForwardIncrementMs(SeekInterval.inWholeMilliseconds)
                .build()
                .apply {
                    registerListeners(this)
                }
        }
        return requireNotNull(player)
    }

    private fun prepare(video: Video?) {
        if (video == null) {
            getPlayer().clearMediaItems()
        } else {
            val uri = video.url.toUri()
            val mediaItem = MediaItem.fromUri(uri)
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(video.headers)
            val source = DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(mediaItem)
            getPlayer().setMediaSource(source)
        }
        getPlayer().prepare()
    }

    private fun reset() {
        Logging.d("reset")
        currentVideo.value = null
        playerBuffer.value = 0
        playerProgress.value = 0
        playerVideoQuality.value = VideoQuality.Automatic
        playerSpeed.value = Speed.x1_00
    }

    private fun isCasting() = when (castState.value) {
        CastManager.State.Initializing,
        CastManager.State.Inactive -> false
        CastManager.State.Loading,
        CastManager.State.Active -> true
    }

    fun onPlay() {
        Logging.d("onPlay")
        if (isCasting()) {
            CastManager.play()
        } else {
            when (getPlayer().playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_IDLE,
                Player.STATE_READY -> {
                    // All good
                }

                Player.STATE_ENDED -> {
                    onSeek(0) // Reset to start when was ended
                }
            }
            getPlayer().play()
        }
    }

    fun onPause() {
        Logging.d("onPause")
        if (isCasting()) {
            CastManager.pause()
        } else {
            getPlayer().pause()
        }
    }

    fun onSeekBack() {
        Logging.d("onSeekBack")
        if (isCasting()) {
            val updated = castProgress.value - 10.seconds.inWholeMilliseconds
            if (updated > castDuration.value) {
                CastManager.seek(castDuration.value)
            } else {
                CastManager.seek(updated)
            }
        } else {
            getPlayer().seekBack()
            updateProgress()
        }
    }

    fun onSeekForward() {
        Logging.d("onSeekForward")
        if (isCasting()) {
            val updated = castProgress.value + 10.seconds.inWholeMilliseconds
            if (updated > castDuration.value) {
                CastManager.seek(castDuration.value)
            } else {
                CastManager.seek(updated)
            }
        } else {
            getPlayer().seekForward()
            updateProgress()
        }
    }

    fun onSeek(time: Long) {
        Logging.d("onSeek: $time")
        if (isCasting()) {
            CastManager.seek(time)
        } else {
            getPlayer().seekTo(time)
        }
        updateProgress()
    }

    fun onSetVideoQuality(videoQuality: VideoQuality) {
        Logging.d("onSetVideoQuality: $videoQuality")
        playerVideoQuality.value = videoQuality
    }

    fun onSetSpeed(speed: Speed) {
        Logging.d("onSetSpeed: $speed")
        playerSpeed.value = speed
    }

    fun onRetry() {
        Logging.d("onRetry")
        playerState.value = PlayerState.Loading
        getPlayer().prepare()
    }

    fun onAttach() {
        if (scope.isActive.not()) throw IllegalStateException("Already closed")
        attachs += 1
        Logging.d("onAttach: $attachs")
        if (attachs == 1) {
            startListening()
        }
    }

    fun onConnect(playerView: PlayerView) {
        connects += 1
        Logging.d("onConnect: $connects")
        playerView.player = getPlayer()
    }

    fun onDisconnect(playerView: PlayerView) {
        connects -= 1
        Logging.d("onDisconnect: $connects")
        playerView.player = null
    }

    fun onShow() {
        shows += 1
        Logging.d("onShow: $shows")
    }

    fun onHide() {
        shows -= 1
        Logging.d("onHide: $shows")
        if (shows == 0) {
            getPlayer().pause()
        }
    }

    fun onDetach(): Boolean {
        attachs -= 1
        Logging.d("onDetach: $attachs")
        if (attachs == 0) {
            stopListening()
            getPlayer().release()
            player = null
        }
        return if (attachs == 0) {
            stopPlayer()
            true
        } else {
            false
        }
    }

    private fun startListening() {
        if (jobsCommon.isEmpty().not()) throw IllegalStateException("Already listening")
        registerCommonJobs()
    }

    private fun stopListening() {
        if (jobsCommon.isEmpty()) throw IllegalStateException("Already not listening")
        clearCommonJobs()
        clearProgressJob()
    }

    private fun stopPlayer() {
        Logging.d("Released")
        scope.cancel()
    }

    private fun updateProgress() {
        playerBuffer.value = getPlayer().bufferedPosition
        playerProgress.value = getPlayer().currentPosition
    }

    private fun clearCommonJobs() {
        jobsCommon.forEach {
            it.cancel("Cleaning up")
        }
        jobsCommon.clear()
    }

    private fun clearProgressJob() {
        jobProgress?.cancel("Cleaning up")
        jobProgress = null
    }

    private fun registerCommonJobs() {
        jobsCommon.addAll(
            listOf(
                // Player syncing
                scope.launch {
                    playerVideoQuality.collect {
                        withContext(Dispatchers.Main) {
                            getPlayer().setVideoQuality(it)
                        }
                    }
                },
                scope.launch {
                    playerSpeed.collect {
                        withContext(Dispatchers.Main) {
                            getPlayer().setPlaybackSpeed(it.value)
                        }
                    }
                },
                // Note: Doesn't work yet
//                scope.launch {
//                    var previous = castState.value
//                    castState.collect { castState ->
//                        withContext(Dispatchers.Main) {
//                            when {
//                                castState !is CastManager.State.Active && previous is CastManager.State.Active -> {
//                                    // Cast progess is already reset to 0
//                                    Logging.d("To player ${castProgress.value}")
//                                    onSeek(castProgress.value)
//                                }
//
//                                castState is CastManager.State.Active && previous is CastManager.State.Loading -> {
//                                    // Cast is not ready to seek yet
//                                    Logging.d("To cast ${playerProgress.value}")
//                                    onSeek(playerProgress.value)
//                                }
//                            }
//                        }
//                        previous = castState
//                    }
//                },
                // Player properties
                scope.launch {
                    currentVideo.collect { video ->
                        withContext(Dispatchers.Main) {
                            prepare(video)
                        }
                    }
                },
                scope.launch {
                    playerState.collect { state ->
                        when (state) {
                            PlayerState.Loading -> {
                                clearProgressJob()
                            }

                            is PlayerState.Error -> {
                                clearProgressJob()
                            }

                            is PlayerState.Ready -> {
                                // Ignore
                            }
                        }
                    }
                },
                scope.launch {
                    playerPlaying.collect { playing ->
                        if (playing) {
                            registerProgressJob()
                        } else {
                            clearProgressJob()
                        }
                    }
                },
                // Cast properties
                scope.launch {
                    combine(
                        CastManager.getState(),
                        CastManager.getVideo(),
                        getVideo(),
                    ) { castState, castVideo, sessionVideo ->
                        when (castState) {
                            CastManager.State.Initializing -> {
                                CastManager.State.Initializing
                            }

                            CastManager.State.Inactive -> {
                                CastManager.State.Inactive
                            }

                            CastManager.State.Loading -> {
                                if (castVideo == sessionVideo) {
                                    CastManager.State.Loading
                                } else {
                                    CastManager.State.Inactive
                                }
                            }

                            CastManager.State.Active -> {
                                if (castVideo == sessionVideo) {
                                    CastManager.State.Active
                                } else {
                                    CastManager.State.Inactive
                                }
                            }
                        }
                    }.collect { state ->
                        castState.value = state
                    }
                },
                scope.launch {
                    combine(castState, CastManager.getPlaybackPlaying()) { state, playing ->
                        castPlaying.value = state is CastManager.State.Active && playing
                    }.collect()
                },
                scope.launch {
                    combine(castState, CastManager.getPlaybackProgress()) { state, progress ->
                        castProgress.value = if (state is CastManager.State.Active) {
                            progress
                        } else {
                            0L
                        }
                    }.collect()
                },
                scope.launch {
                    combine(castState, CastManager.getPlaybackDuration()) { state, duration ->
                        castDuration.value = if (state is CastManager.State.Active) {
                            duration
                        } else {
                            0L
                        }
                    }.collect()
                },
                // Combined properties
                scope.launch {
                    combine(playerState, castState) { player, cast ->
                        val onlyForPlayer = if (cast is CastManager.State.Inactive) {
                            player is PlayerState.Ready
                        } else {
                            false
                        }
                        pictureInPictureAvailable.value = onlyForPlayer
                        selectSpeedAvailable.value = onlyForPlayer
                        selectQualityAvailable.value = onlyForPlayer
                    }.collect()
                },
                scope.launch {
                    combine(
                        playerPlaying,
                        castPlaying,
                    ) { playerPlaying, castPlaying ->
                        playing.value = playerPlaying || castPlaying
                    }.collect()
                },
                scope.launch {
                    combine(
                        playerProgress,
                        castState,
                        castProgress,
                    ) { playerProgress, castState, castProgress ->
                        progress.value = if (castState is CastManager.State.Active) {
                            castProgress
                        } else {
                            playerProgress
                        }
                    }.collect()
                },
                scope.launch {
                    combine(
                        playerDuration,
                        castState,
                        castDuration,
                    ) { playerDuration, castState, castDuration ->
                        duration.value = if (castState is CastManager.State.Active) {
                            castDuration
                        } else {
                            playerDuration
                        }
                    }.collect()
                },
            )
        )
        // Note: Revisit
//        scope.launch {
//            val target = playerProgress.value
//            Logging.d("Launch progress $target")
//            playerState.first { it is PlayerState.Ready }
//            if (target != 0L) {
//                Logging.d("Seek!")
//                withContext(Dispatchers.Main) {
//                    onSeek(playerProgress.value)
//                }
//            }
//        }
    }

    private fun registerProgressJob() {
        clearProgressJob()
        jobProgress = scope.launch {
            while (true) {
                delay(100.milliseconds)
                withContext(Dispatchers.Main) {
                    updateProgress()
                }
            }
        }
    }

    private fun registerListeners(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Logging.d("onPlaybackStateChanged: $playbackState")
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        playerState.value = player.playerError?.let {
                            val text = context.getString(R.string.controls_error_x, it.errorCode)
                            delegates.forEach { it.onError(text) }
                            PlayerState.Error(text)
                        } ?: PlayerState.Loading
                    }

                    Player.STATE_BUFFERING -> {
                        if (playerState.value != PlayerState.Loading) {
                            playerState.value = PlayerState.Ready
                        }
                    }

                    Player.STATE_READY -> {
                        playerDuration.value = player.duration
                        playerPlaying.value = player.isPlaying
                        val triggerReady = playerState.value != PlayerState.Ready
                        playerState.value = PlayerState.Ready

                        if (playerSpeed.value.value != player.playbackParameters.speed) {
                            Logging.d("Applying remembered speed")
                            onSetSpeed(playerSpeed.value)
                        }
                        if (triggerReady) {
                            delegates.forEach { it.onReady() }
                        }
                    }

                    Player.STATE_ENDED -> {
                        if (playerState.value != PlayerState.Loading) {
                            playerPlaying.value = player.isPlaying
                            playerState.value = PlayerState.Ready
                            delegates.forEach { it.onFinish() }
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playerPlaying.value = isPlaying
                delegates.forEach {
                    if (isPlaying) it.onPlay() else it.onPause()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
            }
        })
    }

    fun onSync(toSession: Session) {
        require(getVideo().value == toSession.getVideo().value) { "Cannot sync unequal videos: ${getVideo().value} != ${toSession.getVideo().value}" }
        toSession.onSeek(getProgress().value)
    }
}