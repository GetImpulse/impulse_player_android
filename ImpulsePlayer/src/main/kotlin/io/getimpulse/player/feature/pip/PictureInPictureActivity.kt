package io.getimpulse.player.feature.pip

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import io.getimpulse.player.R
import io.getimpulse.player.component.overlay.ErrorOverlay
import io.getimpulse.player.component.overlay.LoadingOverlay
import io.getimpulse.player.core.Contracts
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.feature.base.BaseActivity
import io.getimpulse.player.model.VideoKey
import io.getimpulse.player.util.Logging
import kotlinx.coroutines.launch

internal class PictureInPictureActivity : BaseActivity(R.layout.activity_picture_in_picture) {

    companion object {
        private const val ExtraContractKey = "contract_key"
        private const val ActionSeekBackward = "action_seek_backward"
        private const val ActionSeekForward = "action_seek_forward"
        private const val ActionPlay = "action_play"
        private const val ActionPause = "action_pause"

        fun createIntent(
            context: Context,
            contract: Contracts.PictureInPicture,
        ): Intent {
            return Intent(context, PictureInPictureActivity::class.java).apply {
                putExtra(ExtraContractKey, contract.key)
            }
        }
    }

    enum class State {
        Loading,
        PictureInPicture,
        Exit,
    }

    private val contract by lazy {
        val key = intent.getStringExtra(ExtraContractKey) ?: return@lazy null
        Contracts.get<Contracts.PictureInPicture>(key)
    }
    private val broadcastReceiver by lazy {
        BroadcastReceiver()
    }

    private val rootView by lazy { findViewById<View>(R.id.root) }
    private val playerView by lazy { findViewById<PlayerView>(R.id.player_view) }
    private val errorOverlay by lazy { findViewById<ErrorOverlay>(R.id.error_overlay) }
    private val loadingOverlay by lazy { findViewById<LoadingOverlay>(R.id.loading_overlay) }
    private val actionSeekBackward by lazy {
        createAction(
            R.drawable.ic_backward_10,
            R.string.controls_backward_10,
            ActionSeekBackward,
        )
    }
    private val actionSeekForward by lazy {
        createAction(
            R.drawable.ic_forward_10,
            R.string.controls_forward_10,
            ActionSeekForward,
        )
    }
    private val actionPlay by lazy {
        createAction(
            R.drawable.ic_play,
            R.string.controls_play,
            ActionPlay,
        )
    }
    private val actionPause by lazy {
        createAction(
            R.drawable.ic_pause,
            R.string.controls_pause,
            ActionPause,
        )
    }
    private var state = State.Loading

    private val pictureInPictureListener by lazy {
        object : PictureInPictureManager.Listener {
            override fun onActivated() {
                // Ignore
            }

            override fun onExited(exitedVideoKey: VideoKey) {
                Logging.d("Listener: onExited $isInPictureInPictureMode")
                if (isInPictureInPictureMode) {
                    Navigation.finish(this@PictureInPictureActivity)
                }
            }
        }
    }

    private fun getSession() = contract?.let { SessionManager.require(it.videoKey) }
    private fun requireSession() = requireNotNull(getSession())

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        super.onCreate(savedInstanceState)
        initialize()
    }

    private fun initialize() {
        val videoKey = contract?.videoKey ?: return
        errorOverlay.initialize(videoKey)
        loadingOverlay.initialize(videoKey)
    }

    override fun setupView() {
        updatePadding()
    }

    override fun setupViewListeners() {
        PictureInPictureManager.register(pictureInPictureListener)
        addOnPictureInPictureModeChangedListener {
            Logging.d("Mode changed: $isInPictureInPictureMode")
            if (isInPictureInPictureMode.not()) {
                updatePadding()
                state = State.Exit
                PictureInPictureManager.exit()
            } else {
                PictureInPictureManager.active(contract!!.videoKey)
                updatePadding()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Logging.d("onResume")
        when (state) {
            State.Loading,
            State.PictureInPicture -> {
                // Ignore
            }

            State.Exit -> {
                Logging.d("- Exit")
                Navigation.finish(this@PictureInPictureActivity)
            }
        }
    }

    private fun updatePadding() {
        val contract = contract ?: return
        if (isInPictureInPictureMode) {
            rootView.setPadding(0, 0, 0, 0)
        } else {
            val sheight = resources.getDimensionPixelSize(
                resources.getIdentifier(
                    "status_bar_height",
                    "dimen",
                    "android"
                )
            )
            Logging.d(
                "${contract.srcRect} | ${contract.srcRect.right - contract.srcRect.width()} | ${contract.srcRect.bottom} | ${
                    Resources.getSystem().getDisplayMetrics().heightPixels
                } | ${sheight}"
            )
            val height = contract.srcRect.width() * 9 / 16
            rootView.setPadding(
                contract.srcRect.left,
                contract.srcRect.top,
                contract.srcRect.right - contract.srcRect.width(),
                Resources.getSystem().displayMetrics.heightPixels - contract.srcRect.top - height + sheight // contract.srcRect.bottom,// - contract.srcRect.height(),
            )
        }
    }

    override fun onStart() {
        super.onStart()
        Logging.d("onStart")
        val videoKey = contract?.videoKey ?: return
        val session = requireSession()
        val filter = IntentFilter()
        filter.addAction(ActionSeekBackward)
        filter.addAction(ActionSeekForward)
        filter.addAction(ActionPlay)
        filter.addAction(ActionPause)
        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        SessionManager.attach(this, videoKey)
        SessionManager.connect(videoKey, playerView)
        SessionManager.show(videoKey, this)
        lifecycleScope.launch {
            session.isPlaying().collect { playing ->
                setPictureInPictureParams(createPictureInPictureParameters(playing))
            }
        }
        val params = createPictureInPictureParameters(session.isPlaying().value)
        enterPictureInPictureMode(params)
        state = State.PictureInPicture
    }

    private fun createPictureInPictureParameters(playing: Boolean): PictureInPictureParams {
        val rect = Rect()
        playerView.getGlobalVisibleRect(rect)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setSourceRectHint(rect)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(false)
        }
        val actions = mutableListOf<RemoteAction>()
        if (maxNumPictureInPictureActions >= 3) {
            actions.add(actionSeekBackward)
            actions.add(if (playing) actionPause else actionPlay)
            actions.add(actionSeekForward)
        } else {
            actions.add(if (playing) actionPause else actionPlay)
        }
        builder.setActions(actions)
        return builder.build()
    }

    override fun onStop() {
        super.onStop()
        Logging.d("onStop")
        val videoKey = contract?.videoKey ?: return

        when (state) {
            State.Loading -> {
                // Ignore
            }

            State.PictureInPicture -> {
                val session = SessionManager.require(contract!!.videoKey)
                session.onPause()
            }

            State.Exit -> {
                // Just continue
            }
        }

        unregisterReceiver(broadcastReceiver)
        SessionManager.disconnect(videoKey, playerView)
        SessionManager.hide(videoKey, this)
        SessionManager.detach(videoKey)
    }

    private fun createAction(
        @DrawableRes icon: Int,
        @StringRes title: Int,
        action: String
    ) = RemoteAction(
        Icon.createWithResource(this, icon),
        getString(title),
        getString(title),
        PendingIntent.getBroadcast(
            this,
            action.hashCode(),
            Intent(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
    )

    inner class BroadcastReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val contract = contract ?: return
            Logging.d("onReceive: ${intent?.action}")
            val session = SessionManager.require(contract.videoKey)
            when (intent?.action) {
                ActionSeekBackward -> {
                    session.onSeekBack()
                }

                ActionSeekForward -> {
                    session.onSeekForward()
                }

                ActionPlay -> {
                    session.onPlay()
                }

                ActionPause -> {
                    session.onPause()
                }

                else -> {
                    Logging.d("Unhandled action: ${intent?.action}")
                }
            }
        }
    }
}