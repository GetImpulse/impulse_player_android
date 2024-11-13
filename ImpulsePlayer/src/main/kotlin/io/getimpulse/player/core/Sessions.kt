package io.getimpulse.player.core

import android.content.Context
import androidx.media3.ui.PlayerView
import io.getimpulse.player.component.PictureInPictureActivity
import io.getimpulse.player.ImpulsePlayerView
import io.getimpulse.player.model.Video
import io.getimpulse.player.model.VideoKey

internal object Sessions {

    private val sessions by lazy { mutableMapOf<VideoKey, Session>() }

    fun load(context: Context, key: VideoKey, video: Video) {
        get(context, key).onLoad(video)
    }

    fun show(key: VideoKey, view: PlayerView) {
        get(view.context, key).onShow(view)
    }

    fun hide(key: VideoKey, view: PlayerView) {
        get(view.context, key).onHide(view)
    }

    private fun get(context: Context, key: VideoKey): Session {
        return sessions.getOrPut(key) {
            Session(context.applicationContext)
        }
    }

    fun getA(activity: PictureInPictureActivity, key: VideoKey): Session {
        return get(activity, key)
    }

    fun getV(view: ImpulsePlayerView, key: VideoKey): Session {
        return get(view.context, key)
    }

    fun attach(context: Context, key: VideoKey): Session {
        return get(context, key).apply {
            onAttach()
        }
    }

    fun detach(key: VideoKey) {
        val session = sessions[key] ?: return
        if (session.onDetach()) {
            // Clean up
            sessions.remove(key)
        }
    }
}