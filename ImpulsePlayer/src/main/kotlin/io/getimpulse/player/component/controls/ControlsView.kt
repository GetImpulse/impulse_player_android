package io.getimpulse.player.component.controls

import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.R
import io.getimpulse.player.component.SeekSlider
import io.getimpulse.player.core.Contracts
import io.getimpulse.player.core.Formatter
import io.getimpulse.player.core.NativeNavigator
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.feature.cast.CastManager
import io.getimpulse.player.model.PlayerButton
import io.getimpulse.player.model.PlayerState
import io.getimpulse.player.model.VideoKey
import io.getimpulse.player.util.ImpulsePlayerNavigator
import io.getimpulse.player.util.Logging
import io.getimpulse.player.util.extension.createColorStateList
import io.getimpulse.player.util.extension.setFont
import io.getimpulse.player.util.extension.setGone
import io.getimpulse.player.util.extension.setVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class ControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    internal sealed interface Delegate {

        interface Embedded : Delegate {
            fun onEnterPictureInPicture()
            fun onEnterFullscreen()
        }

        interface PictureInPicture : Delegate {

        }

        interface Fullscreen : Delegate {
            fun onEnterPictureInPicture()
            fun onExitFullscreen()
        }
    }

    private val root by lazy {
        LayoutInflater.from(context).inflate(R.layout.view_controls, this, true)
    }
    private val topBar by lazy { root.findViewById<View>(R.id.top_bar) }
    private val topBarExtrasEnd by lazy { root.findViewById<LinearLayout>(R.id.top_bar_extras_end) }
    private val center by lazy { root.findViewById<View>(R.id.center) }
    private val bottomBar by lazy { root.findViewById<View>(R.id.bottom_bar) }
    private val bottomBarExtrasStart by lazy { root.findViewById<LinearLayout>(R.id.bottom_bar_extras_start) }
    private val bottomBarExtrasEnd by lazy { root.findViewById<LinearLayout>(R.id.bottom_bar_extras_end) }
    private val back by lazy { root.findViewById<View>(R.id.back) }
    private val title by lazy { root.findViewById<TextView>(R.id.title) }
    private val description by lazy { root.findViewById<TextView>(R.id.description) }
    private val cast by lazy { root.findViewById<CastButton>(R.id.cast) }
    private val pictureInPicture by lazy { root.findViewById<View>(R.id.picture_in_picture) }
    private val play by lazy { root.findViewById<View>(R.id.play) }
    private val pause by lazy { root.findViewById<View>(R.id.pause) }
    private val backward by lazy { root.findViewById<View>(R.id.backward) }
    private val forward by lazy { root.findViewById<View>(R.id.forward) }
    private val slider by lazy { root.findViewById<SeekSlider>(R.id.slider) }
    private val timeCurrent by lazy { root.findViewById<TextView>(R.id.time_current) }
    private val timeDuration by lazy { root.findViewById<TextView>(R.id.time_duration) }
    private val quality by lazy { root.findViewById<QualityButton>(R.id.quality) }
    private val speed by lazy { root.findViewById<SpeedButton>(R.id.speed) }
    private val expand by lazy { root.findViewById<View>(R.id.expand) }
    private val shrink by lazy { root.findViewById<View>(R.id.shrink) }

    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var videoKey: VideoKey
    private lateinit var delegate: Delegate
    private var navigator: ImpulsePlayerNavigator = NativeNavigator(context)
    private var jobVisibility: Job? = null
    private val requestShow = MutableStateFlow(false)
    private val castEnabled = MutableStateFlow(true)

    init {
        Logging.d("init")
        setGone()
    }

    private fun getSession() = SessionManager.require(videoKey)

    fun initialize(videoKey: VideoKey, delegate: Delegate) {
        Logging.d("initialize")
        this.videoKey = videoKey
        this.delegate = delegate

        // Setup
        setupListeners()
        setupCollects()

        // Forward
        cast.initialize(videoKey)
        quality.initialize(videoKey)
        speed.initialize(videoKey)
    }

    fun setNavigator(navigator: ImpulsePlayerNavigator?) {
        this.navigator = navigator ?: NativeNavigator(context)
    }

    fun setCastEnabled(enabled: Boolean) {
        castEnabled.value = enabled
    }

    fun show() {
        requestShow.value = true
        extendShowRequest()
    }

    private fun extendShowRequest() {
        jobVisibility?.cancel("Restarting")
        jobVisibility = viewScope.launch {
            delay(ImpulsePlayer.controlsTimeout)
            hide()
        }
    }

    fun hide() {
        jobVisibility?.cancel("Hiding")
        jobVisibility = null
        requestShow.value = false
    }

    private fun setupListeners() {
        setOnClickListener { hide() }
    }

    private fun setupCollects() {
        collectVisibilities()
    }

    private fun collectVisibilities() {
        viewScope.launch {
            combine(
                requestShow,
                getSession().getState(),
                getSession().isPlaying(),
                getSession().getCastState(),
            ) { request, sessionState, sessionPlaying, castState ->
                // Show when requested
                val visibleByRequest = request
                // Show when player is ready and we are paused
                val visibleBySessionState =
                    sessionState is PlayerState.Ready && sessionPlaying.not()
                // Show while casting
                val visibleByCastState = when (castState) {
                    CastManager.State.Initializing -> false
                    CastManager.State.Inactive -> false
                    CastManager.State.Loading -> false
                    CastManager.State.Active -> true
                }
                setVisible(visibleByRequest || visibleBySessionState || visibleByCastState)
            }.collect()
        }
        viewScope.launch {
            combine(
                ImpulsePlayer.getSettings(),
                getSession().getCastState(),
                castEnabled,
            ) { settings, castState, castEnabled ->
                val castVisibleBySettings = settings.castReceiverApplicationId.isNullOrBlank().not()
                val castVisibleByDelegate = when (delegate) {
                    is Delegate.Embedded,
                    is Delegate.Fullscreen -> true

                    is Delegate.PictureInPicture -> false
                }
                val castVisibleByCast = when (castState) {
                    CastManager.State.Initializing -> false
                    CastManager.State.Loading,
                    CastManager.State.Inactive,
                    CastManager.State.Active -> true
                }
                val castVisibleByView = castEnabled
                cast.setVisible(castVisibleBySettings && castVisibleByDelegate && castVisibleByCast && castVisibleByView)
            }.collect()
        }
        viewScope.launch {
            combine(
                ImpulsePlayer.getSettings(),
                getSession().getCastState(),
            ) { settings, castState ->
                val pipVisibleBySettings = settings.pictureInPictureEnabled
                val pipVisibleByDevice =
                    context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                val pipVisibleByDelegate = when (delegate) {
                    is Delegate.Embedded,
                    is Delegate.Fullscreen -> true

                    is Delegate.PictureInPicture -> false
                }
                val pipVisibleByCast = when (castState) {
                    CastManager.State.Initializing,
                    CastManager.State.Inactive -> true

                    CastManager.State.Loading,
                    CastManager.State.Active -> false
                }
                pictureInPicture.setVisible(pipVisibleBySettings && pipVisibleByDevice && pipVisibleByDelegate && pipVisibleByCast)
            }.collect()
        }
        viewScope.launch {
            getSession().isPlaying().collect { playing ->
                play.setGone(playing)
                pause.setVisible(playing)
            }
        }
    }

    override fun onDetachedFromWindow() {
        Logging.d("onDetachedFromWindow")
        viewScope.cancel("Detached")
        super.onDetachedFromWindow()
    }

    @OptIn(UnstableApi::class)
    fun attach(key: VideoKey) {
        Logging.d("attach ${videoKey.identifier}")
        SessionManager.attach(context, key)
        CastManager.attach(context)

        setupView()
        registerAppearance()
        registerCollects()
        registerListeners()
    }

    private fun setupView() {
        back.setVisible(
            when (delegate) {
                is Delegate.Embedded -> false
                is Delegate.Fullscreen -> true
                is Delegate.PictureInPicture -> false
            }
        )
        expand.setVisible(
            when (delegate) {
                is Delegate.Embedded -> true
                is Delegate.Fullscreen -> false
                is Delegate.PictureInPicture -> false
            }
        )
        shrink.setVisible(
            when (delegate) {
                is Delegate.Embedded -> false
                is Delegate.Fullscreen -> true
                is Delegate.PictureInPicture -> false
            }
        )
    }

    private fun registerAppearance() {
        viewScope.launch {
            ImpulsePlayer.getAppearance().collect {
                title.setFont(it.h4)
                description.setFont(it.s1)
                it.accentColor.createColorStateList().let {
                    slider.trackActiveTintList = it
                    slider.thumbTintList = it
                }
                timeCurrent.setFont(it.l7)
                timeDuration.setFont(it.l7)
            }
        }
    }

    private fun registerCollects() {
        Logging.d("Register jobs: $viewScope")
        viewScope.launch {
            getSession().getButtons().collect { buttons ->
                Logging.d("Show buttons: $buttons")
                topBarExtrasEnd.removeAllViews()
                bottomBarExtrasStart.removeAllViews()
                bottomBarExtrasEnd.removeAllViews()

                buttons.forEach { button ->
                    val view = ExtraButton(context)
                    view.setImageResource(button.value.icon)
                    view.contentDescription = button.value.title
                    view.setOnClickListener {
                        button.value.action()
                    }

                    when (button.value.position) {
                        PlayerButton.Position.TopEnd -> {
                            topBarExtrasEnd.addView(view)
                        }

                        PlayerButton.Position.BottomStart -> {
                            bottomBarExtrasStart.addView(view)
                        }

                        PlayerButton.Position.BottomEnd -> {
                            bottomBarExtrasEnd.addView(view)
                        }
                    }
                }
            }
        }
        viewScope.launch {
            getSession().getState().collect { state ->
                when (state) {
                    PlayerState.Loading -> {
                        topBar.setGone()
                        center.setGone()
                        bottomBar.setGone()
                    }

                    is PlayerState.Error -> {
                        topBar.setGone()
                        center.setGone()
                        bottomBar.setGone()
                    }

                    is PlayerState.Ready -> {
                        topBar.setVisible()
                        center.setVisible()
                        bottomBar.setVisible()
                    }
                }
            }
        }
        viewScope.launch {
            getSession().getVideo().collect { video ->
                renderTitle(video?.title)
                renderDescription(video?.description)
            }
        }
        viewScope.launch {
            getSession().getPlayerBuffer().collect { progress ->
                renderBuffer(progress)
            }
        }
        viewScope.launch {
            getSession().getProgress().collect { progress ->
//                Logging.d("Progress: $progress")
                renderProgress(progress)
            }
        }
        viewScope.launch {
            getSession().getDuration().collect { duration ->
//                Logging.d("Duration: $duration")
                renderDuration(duration)
            }
        }
    }

    private fun renderBuffer(progress: Long) {
        slider.setBuffer(progress.toFloat())
    }

    private fun renderProgress(progress: Long) {
        slider.value = progress.toFloat()
        timeCurrent.text = Formatter.time(progress.milliseconds)
    }

    private fun renderDuration(duration: Long) {
        slider.valueTo = if (duration > 0) {
            duration.toFloat()
        } else {
            1f
        }
        timeDuration.text = Formatter.time(duration.milliseconds)
    }

    private fun registerListeners() {
        cast.setOnClickListener {
            CastManager.open(navigator, videoKey, getSession().getVideo().value)
        }
        backward.setOnClickListener {
            getSession().onSeekBack()
            extendShowRequest()
        }
        play.setOnClickListener {
            getSession().onPlay()
            extendShowRequest()
        }
        pause.setOnClickListener {
            getSession().onPause()
            extendShowRequest()
        }
        forward.setOnClickListener {
            getSession().onSeekForward()
            extendShowRequest()
        }
        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                getSession().onSeek(value.toLong())
                extendShowRequest()
            }
        }
        quality.setOnClickListener {
            extendShowRequest()
            val qualities = getSession().getAvailableVideoQualities()
            val contract = Contracts.SelectVideoQuality(
                videoKey,
                qualities,
                getSession().getVideoQuality().value.key,
            )
            Navigation.openSelectVideoQuality(navigator, contract)
        }
        speed.setOnClickListener {
            extendShowRequest()
            val options = getSession().getAvailableSpeeds()
            val contract = Contracts.SelectSpeed(
                videoKey,
                options,
                getSession().getSpeed().value.key,
            )
            Navigation.openSelectSpeed(navigator, contract)
        }
        when (val delegate = delegate) {
            is Delegate.Embedded -> {
                back.setOnClickListener(null)
                pictureInPicture.setOnClickListener {
                    delegate.onEnterPictureInPicture()
                }
                shrink.setOnClickListener(null)
                expand.setOnClickListener {
                    delegate.onEnterFullscreen()
                }
            }

            is Delegate.PictureInPicture -> {

            }

            is Delegate.Fullscreen -> {
                back.setOnClickListener {
                    delegate.onExitFullscreen()
                }
                pictureInPicture.setOnClickListener {
                    delegate.onEnterPictureInPicture()
                }
                shrink.setOnClickListener {
                    delegate.onExitFullscreen()
                }
                expand.setOnClickListener(null)
            }
        }
    }

    fun detach(key: VideoKey) {
        CastManager.detach()
        SessionManager.detach(key)
    }

    private fun renderTitle(text: String?) {
        title.text = text
        title.setVisible(
            when (delegate) {
                is Delegate.Embedded -> false
                is Delegate.Fullscreen -> text.isNullOrBlank().not()
                is Delegate.PictureInPicture -> false
            }
        )
    }

    private fun renderDescription(text: String?) {
        description.text = text
        description.setVisible(
            when (delegate) {
                is Delegate.Embedded -> false
                is Delegate.Fullscreen -> text.isNullOrBlank().not()
                is Delegate.PictureInPicture -> false
            }
        )
    }
}
