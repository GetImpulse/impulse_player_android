package io.getimpulse.player.feature.cast

import android.content.Context
import com.google.android.gms.cast.MediaError
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import io.getimpulse.player.util.Logging
import io.getimpulse.player.model.Video
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class CastInteractor(
    private val context: Context,
    private val listener: Listener,
) : CastSessionListener.Listener {

    interface Listener {
        fun onStateChanged(state: CastManager.State)
        fun onMediaLoaded(video: Video?, playing: Boolean)
        fun onEnded()
        fun onPlaybackStarted()
        fun onPlaybackPaused()
        fun onPlaybackProgress(progress: Long, duration: Long)
    }

    private val castContext = CastContext.getSharedInstance(context)
    private val castStateListener = CastStateListener()
    private val castSessionListener = CastSessionListener(this)
    private val remoteMediaClientCallback = RemoteMediaClientCallback()
    private var currentVideo: Video? = null

    private val progressListener = RemoteMediaClient.ProgressListener { progress, duration ->
//        Logging.d("onProgressUpdated: $progress $duration")
        listener.onPlaybackProgress(progress, duration)
    }

    init {
        castContext.addCastStateListener(castStateListener)
        castContext.sessionManager.addSessionManagerListener(castSessionListener)

        recoverInitialState()
    }

    fun dispose() {
        castContext.sessionManager.removeSessionManagerListener(castSessionListener)
        castContext.removeCastStateListener(castStateListener)
    }

    fun start(video: Video) {
        currentVideo = video
        val remoteMediaClient = getRemoteMediaClient()
        if (remoteMediaClient == null) {
            Logging.d("No remote client yet, still starting the cast session")
        } else {
            play(remoteMediaClient)
        }
    }

    fun stop() {
        castContext.sessionManager.endCurrentSession(true)
    }

    fun doPlay() {
        val client = getRemoteMediaClient() ?: throw IllegalStateException("Missing client")
        client.play()
    }

    fun doPause() {
        val client = getRemoteMediaClient() ?: throw IllegalStateException("Missing client")
        client.pause()
    }

    fun seek(time: Long) {
        val client = getRemoteMediaClient() ?: throw IllegalStateException("Missing client")
        client.seek(MediaSeekOptions.Builder().setPosition(time).build())
    }

    private fun getRemoteMediaClient(): RemoteMediaClient? {
        return castContext.sessionManager.currentCastSession?.remoteMediaClient
    }

    private fun recoverInitialState() {
        when (castContext.castState) {
            CastState.CONNECTING -> {
                listener.onStateChanged(CastManager.State.Loading)
            }
            CastState.CONNECTED -> {
                listener.onStateChanged(CastManager.State.Active)
            }
            else -> {
                Logging.d("Initial cast state: ${castContext.castState}")
            }
        }
    }

    private fun play(client: RemoteMediaClient) {
        val video = currentVideo ?: throw IllegalStateException("Cannot play without video")
        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        video.title?.let {
            mediaMetadata.putString(MediaMetadata.KEY_TITLE, it)
        }
        video.description?.let {
            mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, it)
        }

        val contentId = UUID.randomUUID().toString()
        val mediaInfo = MediaInfo.Builder(contentId)
            .setContentUrl(video.url)
//            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED) // .mp4
//            .setContentType("video/mp4") // .mp4
//            .setStreamType(MediaInfo.STREAM_TYPE_LIVE) // .m3u8
//            .setContentType("application/x-mpegURL") // .m3u8
            .setMetadata(mediaMetadata)
            .build()

        client.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .build()
        )
        client.play()
    }

    override fun onStarted() {
        val client = getRemoteMediaClient() ?: throw IllegalStateException("Missing client")
        client.registerCallback(remoteMediaClientCallback)
        client.addProgressListener(
            progressListener,
            250.milliseconds.inWholeMilliseconds,
        )
        val video = currentVideo
        if (video == null) {
            stop()
        } else {
            start(video)
        }
    }

    override fun onResumed() {
        val client = getRemoteMediaClient() ?: throw IllegalStateException("Missing client")
        client.registerCallback(remoteMediaClientCallback)
        client.addProgressListener(
            progressListener,
            250.milliseconds.inWholeMilliseconds,
        )
    }

    override fun onEnded() {
        currentVideo = null
        getRemoteMediaClient()?.removeProgressListener(progressListener)
        getRemoteMediaClient()?.unregisterCallback(remoteMediaClientCallback)
        listener.onEnded()
    }

    private inner class CastStateListener :
        com.google.android.gms.cast.framework.CastStateListener {
        override fun onCastStateChanged(state: Int) {
            when (state) {
                CastState.NO_DEVICES_AVAILABLE -> {
                    listener.onStateChanged(CastManager.State.Inactive)
                }

                CastState.NOT_CONNECTED -> {
                    listener.onStateChanged(CastManager.State.Inactive)
                }

                CastState.CONNECTING -> {
                    listener.onStateChanged(CastManager.State.Loading)
                }

                CastState.CONNECTED -> {
                    listener.onStateChanged(CastManager.State.Active)
                }

                else -> {
                    Logging.e("onCastStateChanged: $state unhandled")
                }
            }
        }
    }

    private inner class RemoteMediaClientCallback : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            val client = getRemoteMediaClient()
            val state = client?.mediaStatus?.playerState
            Logging.d("onStatusUpdated ${state}")
            when (state) {
                MediaStatus.PLAYER_STATE_UNKNOWN -> {

                }

                MediaStatus.PLAYER_STATE_IDLE -> {
                    // The player turns idle when
                    // - The video has ended (but we catch this callback already)
                    // - The video couldn't play on the device (failed to load for some reason)
                    stop()
                }

                MediaStatus.PLAYER_STATE_PLAYING -> {
                    listener.onPlaybackStarted()
                }

                MediaStatus.PLAYER_STATE_PAUSED -> {
                    listener.onPlaybackPaused()
                }

                MediaStatus.PLAYER_STATE_BUFFERING -> {

                }

                MediaStatus.PLAYER_STATE_LOADING -> {

                }

                null -> {

                }

                else -> {
                    Logging.e("Unhandled player state: $state")
                }
            }
            val mediaInfo = client?.mediaInfo
            Logging.d("- ${mediaInfo?.contentId} - ${mediaInfo?.contentUrl}")
            val title = mediaInfo?.metadata?.getString(MediaMetadata.KEY_TITLE)
            val subtitle = mediaInfo?.metadata?.getString(MediaMetadata.KEY_SUBTITLE)
            val url = mediaInfo?.contentUrl
            if (mediaInfo != null) {
                val playing = state == RemoteMediaClient.RESUME_STATE_PAUSE
                if (url == null) {
                    currentVideo = null
                    listener.onMediaLoaded(null, playing)
                } else {
                    val video = Video(title, subtitle, url)
                    currentVideo = video
                    listener.onMediaLoaded(video, playing)
                }
            }
        }

        override fun onAdBreakStatusUpdated() {
            Logging.d("onAdBreakStatusUpdated")
        }

        override fun onMediaError(p0: MediaError) {
            Logging.d("onMediaError $p0")
        }

        override fun onMetadataUpdated() {
            Logging.d("onMetadataUpdated")
        }

        override fun onPreloadStatusUpdated() {
            Logging.d("onPreloadStatusUpdated")
        }

        override fun onQueueStatusUpdated() {
            Logging.d("onQueueStatusUpdated")
        }

        override fun onSendingRemoteMediaRequest() {
            Logging.d("onSendingRemoteMediaRequest")
        }
    }
}
