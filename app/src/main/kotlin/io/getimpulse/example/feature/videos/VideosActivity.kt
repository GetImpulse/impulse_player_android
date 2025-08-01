package io.getimpulse.example.feature.videos

import android.graphics.Typeface
import android.graphics.fonts.FontVariationAxis
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import io.getimpulse.example.R
import io.getimpulse.example.feature.Settings
import io.getimpulse.example.feature.base.BaseActivity
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.ImpulsePlayerView
import io.getimpulse.player.model.ImpulsePlayerAppearance
import io.getimpulse.player.model.ImpulsePlayerFont
import io.getimpulse.player.model.PlayerButton
import io.getimpulse.player.model.PlayerDelegate

class VideosActivity : BaseActivity(R.layout.activity_videos) {

    private val impulsePlayer by lazy { findViewById<ImpulsePlayerView>(R.id.video_player_one) }
    private val videoPlayerTwo by lazy { findViewById<ImpulsePlayerView>(R.id.video_player_two) }
    private val videoPlayerThree by lazy { findViewById<ImpulsePlayerView>(R.id.video_player_three) }
    private val videoPlayerFour by lazy { findViewById<ImpulsePlayerView>(R.id.video_player_four) }

    override fun setupView() {
        val videoOne = Settings.Videos[0]
        impulsePlayer.load(
            videoOne.url,
            videoOne.title,
            videoOne.subtitle,
            videoOne.headers,
        )
        impulsePlayer.setCastEnabled(false)
        val videoTwo = Settings.Videos[1]
        videoPlayerTwo.load(
            videoTwo.url,
            videoTwo.title,
            videoTwo.subtitle,
            videoTwo.headers,
        )
//        lifecycleScope.launch {
//            delay(10.seconds)
//
//            val videoThree = Settings.Videos[2]
//            videoPlayerTwo.load(
//                videoThree.url,
//                videoThree.title,
//                videoThree.subtitle,
//                videoThree.headers,
//            )
//        }
        val videoThree = Settings.Videos[2]
        videoPlayerThree.load(
            videoThree.url,
            videoThree.title,
            videoThree.subtitle,
            videoThree.headers,
        )
        videoPlayerFour.load(
            ""
        )
//        commands()
//        getters()
//        setters()
        listeners()
        customization()
    }

    private fun commands() {
        impulsePlayer.load(
            "Title",
            "Subtitle",
            "url",
        )
        impulsePlayer.play()
        impulsePlayer.pause()
        impulsePlayer.seek(0)
    }

    private fun getters() {
        impulsePlayer.isPlaying() // StateFlow<Boolean>, default `false`
        impulsePlayer.getState() // StateFlow<PlayerState>, default `Loading`
        impulsePlayer.getProgress() // StateFlow<Long>, default `0`
        impulsePlayer.getDuration() // StateFlow<Long>, default `0`
        impulsePlayer.getError() // StateFlow<String?>, default `null`
    }

    private fun setters() {
        impulsePlayer.removeButton("autoplay")
        impulsePlayer.setButton(
            "autoplay",
            PlayerButton(
                PlayerButton.Position.TopEnd,
                R.drawable.ic_launcher_foreground,
                getString(R.string.app_name)
            ) {
                Log.d("ImpulsePlayer", "Auto play clicked")
            },
        )
    }

    private fun listeners() {
        val delegate = object : PlayerDelegate {
            override fun onReady() {
                Log.d("ImpulsePlayer", "onReady")
            }

            override fun onPlay() {
                Log.d("ImpulsePlayer", "onPlay")
            }

            override fun onPause() {
                Log.d("ImpulsePlayer", "onPause")
            }

            override fun onFinish() {
                // Note: View doesn't have to be active anymore (with PIP, the user could have closed the original screen).
                Log.d("ImpulsePlayer", "onFinish")
            }

            override fun onError(message: String) {
                Log.d("ImpulsePlayer", "onError: $message")
            }
        }
        impulsePlayer.removeDelegate(delegate)
        impulsePlayer.setDelegate(delegate)
    }

    private fun customization() {
        val weightRegular = FontVariationAxis("wght", 400f)
        val weightSemibold = FontVariationAxis("wght", 600f)
        val semibold = Typeface.Builder(assets, "fonts/Inter-Variable.ttf")
            .setFontVariationSettings(arrayOf(weightSemibold))
            .build()
        val regular = Typeface.Builder(assets, "fonts/Inter-Variable.ttf")
            .setFontVariationSettings(arrayOf(weightRegular))
            .build()
        ImpulsePlayer.setAppearance(
            ImpulsePlayerAppearance(
                h3 = ImpulsePlayerFont(
                    sizeSp = 16,
                    typeFace = semibold,
                ),
                h4 = ImpulsePlayerFont(
                    sizeSp = 14,
                    typeFace = semibold,
                ),
                s1 = ImpulsePlayerFont(
                    sizeSp = 12,
                    typeFace = regular,
                ),
                l4 = ImpulsePlayerFont(
                    sizeSp = 14,
                    typeFace = regular,
                ),
                l7 = ImpulsePlayerFont(
                    sizeSp = 10,
                    typeFace = regular,
                ),
                p1 = ImpulsePlayerFont(
                    sizeSp = 16,
                    typeFace = regular,
                ),
                p2 = ImpulsePlayerFont(
                    sizeSp = 14,
                    typeFace = regular,
                ),
                accentColor = ResourcesCompat.getColor(
                    resources,
                    R.color.impulse_player_accent,
                    null
                ),
            )
        )
    }
}