package io.getimpulse.player.component

import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.Navigator
import io.getimpulse.player.R
import io.getimpulse.player.core.Formatter
import io.getimpulse.player.core.Logging
import io.getimpulse.player.core.NativeNavigator
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.core.Session
import io.getimpulse.player.core.Sessions
import io.getimpulse.player.extension.asColorStateList
import io.getimpulse.player.extension.createColorStateList
import io.getimpulse.player.extension.indexOfFirstOrNull
import io.getimpulse.player.extension.setFont
import io.getimpulse.player.extension.setGone
import io.getimpulse.player.extension.setVisible
import io.getimpulse.player.model.PlayerButton
import io.getimpulse.player.model.PlayerState
import io.getimpulse.player.model.Speed
import io.getimpulse.player.model.VideoKey
import io.getimpulse.player.model.VideoP
import io.getimpulse.player.model.VideoQuality
import io.getimpulse.player.sheet.Contract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class ControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    internal sealed interface Delegate {

        interface Embedded : Delegate {
            fun onEnterPictureInPicture()
            fun onEnterFullscreen()
        }

        interface PictureInPicture : Delegate {

        }

        interface Fullscreen : Delegate {
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
    private val pictureInPicture by lazy { root.findViewById<View>(R.id.picture_in_picture) }
    private val play by lazy { root.findViewById<View>(R.id.play) }
    private val pause by lazy { root.findViewById<View>(R.id.pause) }
    private val backward by lazy { root.findViewById<View>(R.id.backward) }
    private val forward by lazy { root.findViewById<View>(R.id.forward) }

    //    private val seek by lazy { root.findViewById<SeekBar>(R.id.seek) }
    private val slider by lazy { root.findViewById<SeekSlider>(R.id.slider) }
    private val timeCurrent by lazy { root.findViewById<TextView>(R.id.time_current) }
    private val timeDuration by lazy { root.findViewById<TextView>(R.id.time_duration) }
    private val quality by lazy { root.findViewById<BarButtonView>(R.id.quality) }
    private val speed by lazy { root.findViewById<BarButtonView>(R.id.speed) }
    private val expand by lazy { root.findViewById<View>(R.id.expand) }
    private val shrink by lazy { root.findViewById<View>(R.id.shrink) }

    private var job: Job? = null
    private var navigator: Navigator = NativeNavigator(context)

    private var delegate: Delegate? = null

    fun setNavigator(navigator: Navigator?) {
        this.navigator = navigator ?: NativeNavigator(context)
    }

    @OptIn(UnstableApi::class)
    fun attach(key: VideoKey, delegate: Delegate) {
        job = Job()
        val viewScope = CoroutineScope(Dispatchers.Main + job!!)

        this.delegate = delegate
        val session = Sessions.attach(context, key)
        setupView()
        registerAppearance(viewScope)
        registerSettings(viewScope)
        registerJobs(viewScope, session)
        registerListeners(viewScope, session, key)
    }

    private fun setupView() {
        back.setVisible(
            when (delegate) {
                is Delegate.Embedded -> false
                is Delegate.Fullscreen -> true
                is Delegate.PictureInPicture -> false
                null -> false
            }
        )
        expand.setVisible(
            when (delegate) {
                is Delegate.Embedded -> true
                is Delegate.Fullscreen -> false
                is Delegate.PictureInPicture -> false
                null -> false
            }
        )
        shrink.setVisible(
            when (delegate) {
                is Delegate.Embedded -> false
                is Delegate.Fullscreen -> true
                is Delegate.PictureInPicture -> false
                null -> false
            }
        )
    }

    private fun registerAppearance(viewScope: CoroutineScope) {
        viewScope?.launch {
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

    private fun registerSettings(viewScope: CoroutineScope) {
        viewScope?.launch {
            ImpulsePlayer.getSettings().collect { settings ->
                pictureInPicture.setVisible(
                    when (delegate) {
                        is Delegate.Embedded -> {
                            settings.pictureInPictureEnabled &&
                                    context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                        }

                        is Delegate.Fullscreen -> false
                        is Delegate.PictureInPicture -> false
                        null -> false
                    }
                )
            }
        }
    }

    private fun registerJobs(viewScope: CoroutineScope, session: Session) {
        Logging.d("Register jobs: $viewScope")
        viewScope?.launch {
            session.getButtons().collect { buttons ->
                Logging.d("Show buttons: $buttons")
                topBarExtrasEnd.removeAllViews()
                bottomBarExtrasStart.removeAllViews()
                bottomBarExtrasEnd.removeAllViews()

                buttons.forEach { button ->
                    val view = BarButtonView(context)
                    view.setIcon(button.value.icon)
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
        viewScope?.launch {
            session.getState().collect { state ->
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
        viewScope?.launch {
            session.isPlaying().collect { playing ->
                play.setGone(playing)
                pause.setVisible(playing)
            }
        }
        viewScope?.launch {
            session.getVideo().collect { video ->
                setTitle(video?.title)
                setDescription(video?.description)
            }
        }
        viewScope?.launch {
            session.getBuffer().collect { progress ->
                updateBuffer(progress)
            }
        }
        viewScope?.launch {
            session.getProgress().collect { progress ->
//                Logging.d("Progress: $progress")
                updateProgress(progress)
            }
        }
        viewScope?.launch {
            session.getDuration().collect { duration ->
                Logging.d("Duration: $duration")
                slider.valueTo = if (duration > 0) {
                    duration.toFloat()
                } else {
                    1f
                }
                timeDuration.text = Formatter.time(duration.milliseconds)
            }
        }
        viewScope?.launch {
            session.getVideoQuality().collect { videoQuality ->
                val icon = when (videoQuality) {
                    VideoQuality.Automatic -> R.drawable.ic_quality_automatic
                    is VideoQuality.Detected -> {
                        when (VideoP.from(videoQuality.height)) {
                            VideoP.Standard360p -> null
                            VideoP.HigherStandard480p -> R.drawable.ic_quality_sd
                            VideoP.HighDefinition720p -> R.drawable.ic_quality_hd
                            VideoP.FullHighDefinition1080p -> R.drawable.ic_quality_fhd
                            VideoP.UltraHighDefinition4k2160p -> R.drawable.ic_quality_2k
                            VideoP.UltraHighDefinition8k4320p -> R.drawable.ic_quality_4k
                            null -> null
                        }
                    }
                }

                quality.setIcon(icon)
                quality.setText(
                    if (icon == null) {
                        when (videoQuality) {
                            VideoQuality.Automatic -> null
                            is VideoQuality.Detected -> {
                                context.getString(R.string.quality_x_p, videoQuality.height)
                            }
                        }
                    } else null
                )
                session.onSetVideoQuality(videoQuality)
            }
        }
        viewScope?.launch {
            session.getSpeed().collect {
                speed.setIcon(
                    when (it) {
                        Speed.x0_25 -> R.drawable.ic_speed_0_25
                        Speed.x0_50 -> R.drawable.ic_speed_0_50
                        Speed.x0_75 -> R.drawable.ic_speed_0_75
                        Speed.x1_00 -> R.drawable.ic_speed_1_00
                        Speed.x1_25 -> R.drawable.ic_speed_1_25
                        Speed.x1_50 -> R.drawable.ic_speed_1_50
                        Speed.x1_75 -> R.drawable.ic_speed_1_75
                        Speed.x2_00 -> R.drawable.ic_speed_2_00
                    }
                )
            }
        }
    }

    private fun updateBuffer(progress: Long) {
        slider.setBuffer(progress.toFloat())
    }

    private fun updateProgress(progress: Long) {
        slider.value = progress.toFloat()
        timeCurrent.text = Formatter.time(progress.milliseconds)
    }

    private fun registerListeners(viewScope: CoroutineScope, session: Session, videoKey: VideoKey) {
        backward.setOnClickListener {
            session.onSeekBack()
        }
        play.setOnClickListener {
            session.onPlay()
        }
        pause.setOnClickListener {
            session.onPause()
        }
        forward.setOnClickListener {
            session.onSeekForward()
        }
        slider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                session.onSeek(value.toLong())
            }
        }
        quality.setOnClickListener {
            viewScope?.launch {
                val qualities = session.getAvailableVideoQualities()
                val contract = Contract.VideoQualityContract(
                    context,
                    qualities,
                    qualities.indexOfFirstOrNull { session.getVideoQuality().value.key == it.key },
                )
                Navigation.selectVideoQuality(navigator, contract)?.let { result ->
                    session.onSetVideoQuality(result)
                }
            }
        }
        speed.setOnClickListener {
            viewScope?.launch {
                val speeds = session.getAvailableSpeeds()
                val contract = Contract.SpeedContract(
                    context,
                    speeds,
                    speeds.indexOfFirstOrNull { session.getSpeed().value.key == it.key }
                )
                Navigation.selectSpeed(navigator, contract)?.let { result ->
                    session.onSetSpeed(result)
                }
            }
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
                pictureInPicture.setOnClickListener(null)
                shrink.setOnClickListener {
                    delegate.onExitFullscreen()
                }
                expand.setOnClickListener(null)
            }

            null -> {
                // Ignore
            }
        }
    }

    fun detach(key: VideoKey) {
        Sessions.detach(key)

        job?.cancel("Detached")
        job = null
    }

    private fun setTitle(text: String?) {
        title.text = text
        title.setVisible(
            when (delegate) {
                is Delegate.Embedded -> false
                is Delegate.Fullscreen -> text.isNullOrBlank().not()
                is Delegate.PictureInPicture -> false
                null -> false
            }
        )
    }

    private fun setDescription(text: String?) {
        description.text = text
        description.setVisible(
            when (delegate) {
                is Delegate.Embedded -> false
                is Delegate.Fullscreen -> text.isNullOrBlank().not()
                is Delegate.PictureInPicture -> false
                null -> false
            }
        )
    }
}
