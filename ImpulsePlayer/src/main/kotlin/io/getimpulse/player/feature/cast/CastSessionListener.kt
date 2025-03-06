package io.getimpulse.player.feature.cast

import com.google.android.gms.cast.CastStatusCodes
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.Session
import com.google.android.gms.cast.framework.SessionManagerListener
import io.getimpulse.player.util.Logging

internal class CastSessionListener(
    private val listener: Listener,
) : SessionManagerListener<Session> {

    interface Listener {
        fun onStarted()
        fun onResumed()
        fun onEnded()
    }

    override fun onSessionEnded(p0: Session, p1: Int) {
        // 2055: Stopped by movie ending + waited for session to close
        // 2161: Stopped by our code (directly ending current session)
        Logging.d("onSessionEnded $p1: ${CastStatusCodes.getStatusCodeString(p1)}")
        listener.onEnded()
    }

    override fun onSessionEnding(p0: Session) {
        Logging.d("onSessionEnding")
    }

    override fun onSessionResumeFailed(p0: Session, p1: Int) {
        Logging.d("onSessionResumeFailed $p1: ${CastStatusCodes.getStatusCodeString(p1)}")
    }

    override fun onSessionResumed(session: Session, wasSuspended: Boolean) {
        Logging.d("onSessionResumed $wasSuspended")

        if (session is CastSession) {
            listener.onResumed()
        }
    }

    override fun onSessionResuming(p0: Session, p1: String) {
        Logging.d("onSessionResuming $p1")
    }

    override fun onSessionStartFailed(p0: Session, p1: Int) {
        Logging.d("onSessionStartFailed $p1: ${CastStatusCodes.getStatusCodeString(p1)}")
    }

    override fun onSessionStarted(p0: Session, p1: String) {
        Logging.d("onSessionStarted $p1")
        listener.onStarted()
    }

    override fun onSessionStarting(p0: Session) {
        Logging.d("onSessionStarting")
    }

    override fun onSessionSuspended(p0: Session, p1: Int) {
        Logging.d("onSessionSuspended $p1: ${CastStatusCodes.getStatusCodeString(p1)}")
    }
}