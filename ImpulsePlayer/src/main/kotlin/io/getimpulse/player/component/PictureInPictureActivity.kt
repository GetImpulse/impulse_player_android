package io.getimpulse.player.component

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
import androidx.activity.addCallback
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import io.getimpulse.player.R
import io.getimpulse.player.base.BaseActivity
import io.getimpulse.player.core.Logging
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.core.Sessions
import io.getimpulse.player.extension.setGone
import io.getimpulse.player.sheet.Contract
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class PictureInPictureActivity : BaseActivity(R.layout.activity_picture_in_picture) {

    companion object {
        private const val ExtraContractKey = "contract_key"
        private const val ActionSeekBackward = "action_seek_backward"
        private const val ActionSeekForward = "action_seek_forward"
        private const val ActionPlay = "action_play"
        private const val ActionPause = "action_pause"

        fun createIntent(
            context: Context,
            contractKey: String,
        ): Intent {
            return Intent(context, PictureInPictureActivity::class.java).apply {
                putExtra(ExtraContractKey, contractKey)
            }
        }
    }

    private val contract by lazy {
        val key = intent.getStringExtra(ExtraContractKey) ?: return@lazy null
        Navigation.getContract<Contract.PictureInPictureContract>(key)
    }
    private val broadcastReceiver by lazy {
        BroadcastReceiver()
    }

    private val rootView by lazy { findViewById<View>(R.id.root) }
    private val commonView by lazy { findViewById<CommonView>(R.id.common_view) }
    private val playerView by lazy { findViewById<PlayerView>(R.id.player_view) }
    private val controlView by lazy { playerView.findViewById<ControlsView>(R.id.controls) }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        super.onCreate(savedInstanceState)
    }

    @OptIn(UnstableApi::class)
    override fun setupView() {
        val contract = contract ?: return

        // Picture in Picture is not interactable, only with System provided buttons
        controlView.setGone()

        updatePadding()
    }

    override fun setupViewListeners() {
        val contract = contract ?: return
        onBackPressedDispatcher.addCallback {
            contract.onResult(null)
            Navigation.finish(this@PictureInPictureActivity)
            lifecycleScope.launch {
                delay(200.milliseconds)
                playerView.player = null // Cleanup
            }
        }
        addOnPictureInPictureModeChangedListener {
            Logging.d("PIP: $isInPictureInPictureMode")
            if (isInPictureInPictureMode.not()) {
                updatePadding()
                onBackPressedDispatcher.onBackPressed()
            } else {
                updatePadding()
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
        val videoKey = contract?.videoKey ?: return
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
        val session = Sessions.attach(this, videoKey)
        commonView.attach(videoKey)
        controlView.attach(videoKey, object : ControlsView.Delegate.PictureInPicture {

        })
        session.onShow(playerView)
        lifecycleScope.launch {
            session.isPlaying().collect { playing ->
                setPictureInPictureParams(createPictureInPictureParameters(playing))
            }
        }
        val params = createPictureInPictureParameters(session.isPlaying().value)
//        lifecycleScope.launch {
//            delay(3.seconds)
            enterPictureInPictureMode(params)
//        }
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
        val videoKey = contract?.videoKey ?: return
        unregisterReceiver(broadcastReceiver)
        Sessions.detach(videoKey)
        commonView.detach(videoKey)
        controlView.detach(videoKey)
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
            val session = Sessions.getA(this@PictureInPictureActivity, contract.videoKey)
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