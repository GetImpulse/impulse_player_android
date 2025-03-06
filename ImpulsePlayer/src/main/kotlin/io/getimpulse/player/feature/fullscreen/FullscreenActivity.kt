package io.getimpulse.player.feature.fullscreen

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.addCallback
import androidx.media3.ui.PlayerView
import io.getimpulse.player.R
import io.getimpulse.player.component.controls.ControlsView
import io.getimpulse.player.component.overlay.CastingOverlay
import io.getimpulse.player.component.overlay.ErrorOverlay
import io.getimpulse.player.component.overlay.LoadingOverlay
import io.getimpulse.player.core.Contracts
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.feature.base.BaseActivity
import io.getimpulse.player.util.extension.setVisible

internal class FullscreenActivity : BaseActivity(R.layout.activity_fullscreen) {

    companion object {
        private const val ExtraContractKey = "contract_key"

        fun createIntent(
            context: Context,
            contract: Contracts.Fullscreen,
        ): Intent {
            return Intent(context, FullscreenActivity::class.java).apply {
                putExtra(ExtraContractKey, contract.key)
            }
        }
    }

    private val contract by lazy {
        val key = intent.getStringExtra(ExtraContractKey) ?: return@lazy null
        Contracts.get<Contracts.Fullscreen>(key)
    }

    private val errorOverlay by lazy { findViewById<ErrorOverlay>(R.id.error_overlay) }
    private val playerView by lazy { findViewById<PlayerView>(R.id.player_view) }
    private val controlView by lazy { findViewById<ControlsView>(R.id.controls_view) }
    private val castingOverlay by lazy { findViewById<CastingOverlay>(R.id.casting_overlay) }
    private val loadingOverlay by lazy { findViewById<LoadingOverlay>(R.id.loading_overlay) }
    private var previousOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var exitToPictureInPicture = false

    private val fullscreenListener by lazy {
        object : FullscreenManager.Listener {
            override fun onActivated() {
                // Ignore
            }

            override fun onExited(toPictureInPicture: Boolean) {
                Navigation.finish(this@FullscreenActivity)
            }
        }
    }


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
        previousOrientation = resources.configuration.orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE

        initialize()
    }

    private fun initialize() {
        val videoKey = contract?.videoKey ?: return
        controlView.initialize(videoKey, object : ControlsView.Delegate.Fullscreen {
            override fun onEnterPictureInPicture() {
                exitToPictureInPicture = true
                onBackPressed()
            }

            override fun onExitFullscreen() {
                onBackPressed()
            }
        })
        castingOverlay.initialize(videoKey)
        errorOverlay.initialize(videoKey)
        loadingOverlay.initialize(videoKey)
    }

    override fun setupView() {
        playerView.setOnClickListener {
            controlView.setVisible()
        }
    }

    override fun setupViewListeners() {
        FullscreenManager.register(fullscreenListener)

        onBackPressedDispatcher.addCallback {
            requestedOrientation = previousOrientation // Reset orientation
            FullscreenManager.exit(exitToPictureInPicture)
        }
    }

    override fun onStart() {
        super.onStart()
        val videoKey = contract?.videoKey ?: return
        SessionManager.attach(this, videoKey)
        controlView.attach(videoKey)
        SessionManager.connect(videoKey, playerView)
        SessionManager.show(videoKey, this)

        FullscreenManager.active(videoKey)
    }

    override fun onStop() {
        super.onStop()
        val videoKey = contract?.videoKey ?: return
        SessionManager.disconnect(videoKey, playerView)
        SessionManager.hide(videoKey, this)
        SessionManager.detach(videoKey)
        controlView.detach(videoKey)
    }
}
