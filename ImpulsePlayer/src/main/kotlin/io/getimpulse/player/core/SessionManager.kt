package io.getimpulse.player.core

import android.content.Context
import androidx.media3.ui.PlayerView
import io.getimpulse.player.ImpulsePlayerView
import io.getimpulse.player.feature.base.sheet.SheetActivity
import io.getimpulse.player.component.controls.CastButton
import io.getimpulse.player.component.controls.ControlsView
import io.getimpulse.player.feature.pip.PictureInPictureActivity
import io.getimpulse.player.model.Video
import io.getimpulse.player.model.VideoKey

internal object SessionManager {

    private val sessions by lazy { mutableMapOf<VideoKey, Session>() }

    fun create(view: ImpulsePlayerView, key: VideoKey): Session {
        return sessions.getOrPut(key) {
            Session(view.context.applicationContext)
        }
    }

    fun load(context: Context, key: VideoKey, video: Video) {
        get(context, key).onLoad(video)
    }

    fun connect(key: VideoKey, view: PlayerView) {
        get(view.context, key).onConnect(view)
    }

    fun disconnect(key: VideoKey, view: PlayerView) {
        get(view.context, key).onDisconnect(view)
    }

    fun show(key: VideoKey, context: Context) {
        get(context, key).onShow()
    }

    fun hide(key: VideoKey, context: Context) {
        get(context, key).onHide()
    }

    private fun get(context: Context, key: VideoKey): Session {
        return sessions.getOrPut(key) {
            Session(context.applicationContext)
        }
    }

    fun require(key: VideoKey): Session {
        return requireNotNull(sessions[key])
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

    fun sync(fromSession: Session, toSession: Session) {
        fromSession.onSync(toSession)
    }
}