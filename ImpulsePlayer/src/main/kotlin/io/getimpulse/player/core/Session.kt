package io.getimpulse.player.core

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import io.getimpulse.player.R
import io.getimpulse.player.extension.getVideoQualities
import io.getimpulse.player.extension.setVideoQuality
import io.getimpulse.player.model.PlayerButton
import io.getimpulse.player.model.PlayerDelegate
import io.getimpulse.player.model.PlayerState
import io.getimpulse.player.model.Speed
import io.getimpulse.player.model.Video
import io.getimpulse.player.model.VideoQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
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
    private var shows = 0

    private var player: ExoPlayer? = null

    private val state = MutableStateFlow<PlayerState>(PlayerState.Loading)
    private val playing = MutableStateFlow(false)
    private val currentVideo = MutableStateFlow<Video?>(null)
    private val currentBuffer = MutableStateFlow(0L)
    private val currentProgress = MutableStateFlow(0L)
    private val currentDuration = MutableStateFlow(0L)
    private val currentVideoQuality = MutableStateFlow<VideoQuality>(VideoQuality.Automatic)
    private val currentSpeed = MutableStateFlow(Speed.x1_00)
    private val jobsCommon = mutableListOf<Job>()
    private var jobProgress: Job? = null
    private val delegates = mutableListOf<PlayerDelegate>()
    private val buttons = MutableStateFlow(mapOf<String, PlayerButton>())

    fun getState() = state.asStateFlow()
    fun isPlaying() = playing.asStateFlow()
    fun getVideo() = currentVideo.asStateFlow()
    fun getBuffer() = currentBuffer.asStateFlow()
    fun getProgress() = currentProgress.asStateFlow()
    fun getDuration() = currentDuration.asStateFlow()
    fun getVideoQuality() = currentVideoQuality.asStateFlow()
    fun getSpeed() = currentSpeed.asStateFlow()
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
        reset()
        currentVideo.value = video
    }

    private fun getPlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context)
                .setSeekBackIncrementMs(SeekInterval.inWholeMilliseconds)
                .setSeekForwardIncrementMs(SeekInterval.inWholeMilliseconds)
                .build().apply {
                    registerListeners(this)
                }
        }
        return requireNotNull(player)
    }

    private fun prepare(video: Video?) {
        if (video == null) {
            getPlayer().clearMediaItems()
        } else {
            val uri = Uri.parse(video.url)
            val mediaItem = MediaItem.fromUri(uri)
            getPlayer().setMediaItem(mediaItem)
        }
        getPlayer().prepare()
    }

    private fun reset() {
        Logging.d("reset")
        currentVideo.value = null
        currentBuffer.value = 0
        currentProgress.value = 0
        currentVideoQuality.value = VideoQuality.Automatic
        currentSpeed.value = Speed.x1_00
    }

    fun onPlay() {
        when (getPlayer().playbackState) {
            Player.STATE_BUFFERING,
            Player.STATE_IDLE,
            Player.STATE_READY -> {
                // All good
            }

            Player.STATE_ENDED -> {
                onSeek(0)
            }
        }
        getPlayer().play()
    }

    fun onPause() {
        getPlayer().pause()
    }

    fun onSeekBack() {
        getPlayer().seekBack()
        updateProgress()
    }

    fun onSeekForward() {
        getPlayer().seekForward()
        updateProgress()
    }

    fun onSeek(time: Long) {
        getPlayer().seekTo(time)
        updateProgress()
    }

    fun onSetVideoQuality(videoQuality: VideoQuality) {
        currentVideoQuality.value = videoQuality
    }

    fun onSetSpeed(speed: Speed) {
        currentSpeed.value = speed
    }

    fun onRetry() {
        state.value = PlayerState.Loading
        getPlayer().prepare()
    }

    fun onAttach() {
        if (scope.isActive.not()) throw IllegalStateException("Already closed")
        attachs += 1
        Logging.d("Attachs now: $attachs")
    }

    fun onShow(playerView: PlayerView) {
        shows += 1
        Logging.d("Shows now: $shows")
        playerView.player = getPlayer()
        if (shows == 1) {
            showPlayer()
        }
    }

    fun onHide(playerView: PlayerView) {
        playerView.player = null
        shows -= 1
        Logging.d("Shows now: $shows")
        if (shows == 0) {
            hidePlayer()
        }
    }

    fun onDetach(): Boolean {
        attachs -= 1
        Logging.d("Attachs remaining: $attachs")
        return if (attachs == 0) {
            stopPlayer()
            true
        } else {
            false
        }
    }

    private fun showPlayer() {
        if (jobsCommon.isEmpty().not()) throw IllegalStateException("Already showed")
        registerCommonJobs()
    }

    private fun hidePlayer() {
        if (jobsCommon.isEmpty()) throw IllegalStateException("Already hidden")
        clearCommonJobs()
        clearProgressJob()
        getPlayer().release()
        player = null
    }

    private fun stopPlayer() {
        Logging.d("Released")
        scope.cancel()
    }

    private fun updateProgress() {
        currentBuffer.value = getPlayer().bufferedPosition
        currentProgress.value = getPlayer().currentPosition
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
                scope.launch {
                    state.collect { state ->
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
                    playing.collect { playing ->
                        if (playing) {
                            registerProgressJob()
                        } else {
                            clearProgressJob()
                        }
                    }
                },
                scope.launch {
                    currentVideo.collect { video ->
                        withContext(Dispatchers.Main) {
                            prepare(video)
                        }
                    }
                },
//                scope.launch {
//                    currentProgress.collect { progress ->
//                        Logging.d("Progress: ${progress}")
//                    }
//                },
                scope.launch {
                    currentVideoQuality.collect {
                        withContext(Dispatchers.Main) {
                            getPlayer().setVideoQuality(it)
                        }
                    }
                },
                scope.launch {
                    currentSpeed.collect {
                        withContext(Dispatchers.Main) {
                            getPlayer().setPlaybackSpeed(it.value)
                        }
                    }
                },
            )
        )
        scope.launch {
            val target = currentProgress.value
            Logging.d("Launch progress $target")
            state.first { it is PlayerState.Ready }
            if (target != 0L) {
                Logging.d("Seek!")
                withContext(Dispatchers.Main) {
                    onSeek(currentProgress.value)
                }
            }
        }
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
                        state.value = player.playerError?.let {
                            val text = context.getString(R.string.controls_error_x, it.errorCode)
                            delegates.forEach { it.onError(text) }
                            PlayerState.Error(text)
                        } ?: PlayerState.Loading
                    }

                    Player.STATE_BUFFERING -> {
                        if (state.value != PlayerState.Loading) {
                            state.value = PlayerState.Ready
                        }
                    }

                    Player.STATE_READY -> {
                        currentDuration.value = player.duration
                        playing.value = player.isPlaying
                        val triggerReady = state.value != PlayerState.Ready
                        state.value = PlayerState.Ready

                        if (currentSpeed.value.value != player.playbackParameters.speed) {
                            Logging.d("Applying remembered speed")
                            onSetSpeed(currentSpeed.value)
                        }
                        if (triggerReady) {
                            delegates.forEach { it.onReady() }
                        }
                    }

                    Player.STATE_ENDED -> {
                        playing.value = player.isPlaying
                        state.value = PlayerState.Ready
                        delegates.forEach { it.onFinish() }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing.value = isPlaying
                delegates.forEach {
                    if (isPlaying) it.onPlay() else it.onPause()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
            }
        })
    }
}