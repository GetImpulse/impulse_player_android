package io.getimpulse.player.component

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.addCallback
import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.R
import io.getimpulse.player.base.BaseActivity
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.core.Sessions
import io.getimpulse.player.sheet.Contract
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class FullscreenActivity : BaseActivity(R.layout.activity_fullscreen) {

    companion object {
        private const val ExtraContractKey = "contract_key"

        fun createIntent(
            context: Context,
            contractKey: String,
        ): Intent {
            return Intent(context, FullscreenActivity::class.java).apply {
                putExtra(ExtraContractKey, contractKey)
            }
        }
    }

    private val contract by lazy {
        val key = intent.getStringExtra(ExtraContractKey) ?: return@lazy null
        Navigation.getContract<Contract.FullscreenContract>(key)
    }

    private val commonView by lazy { findViewById<CommonView>(R.id.common_view) }
    private val playerView by lazy { findViewById<PlayerView>(R.id.player_view) }
    private val controlView by lazy { playerView.findViewById<ControlsView>(R.id.controls) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }

        // Allow both landscape and reverse landscape
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
    }

    override fun setupView() {
        @OptIn(UnstableApi::class)
        playerView.controllerShowTimeoutMs = ImpulsePlayer.controlsTimeout.inWholeMilliseconds.toInt()
    }

    override fun setupViewListeners() {
        onBackPressedDispatcher.addCallback {
            contract?.onResult(null)
            Navigation.finish(this@FullscreenActivity)
            lifecycleScope.launch {
                delay(300.milliseconds)
                playerView.player = null // Cleanup
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val videoKey = contract?.videoKey ?: return
        val session = Sessions.attach(this, videoKey)
        commonView.attach(videoKey)
        controlView.attach(videoKey, object : ControlsView.Delegate.Fullscreen {
            override fun onExitFullscreen() {
                onBackPressed()
            }
        })
        session.onShow(playerView) // Attach
    }

    override fun onStop() {
        super.onStop()
        val videoKey = contract?.videoKey ?: return
        Sessions.detach(videoKey)
        commonView.detach(videoKey)
        controlView.detach(videoKey)
    }
}