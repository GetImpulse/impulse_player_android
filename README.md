# Impulse Player

[![](https://jitpack.io/v/GetImpulse/impulse_player_android.svg)](https://jitpack.io/#GetImpulse/impulse_player_android)

The Impulse Player makes using a video player in Android easy. Under the hood, the Impulse Player uses [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer) for video playback.

Additionally the Impulse Player contains features such as Fullscreen handling and Picture-in-Picture mode out of the box.

Features:

- Single view to show and handle the video player.
- Playback quality selection.
- Playback speed selection.
- Fullscreen handling.
- Picture-in-Picture handling.
- Support for casting.

## Installing

In root `build.gradle`:

```kotlin
allprojects {
    repositories {
        // ...
        maven { url("https://jitpack.io") }
    }
}
```

In `app/build.gradle`:

```kotlin
implementation("com.github.GetImpulse:impulse_player_android:0.2.1")
```

## Usage

Add the `ImpulsePlayerView` to the XML view:

```xml
<io.getimpulse.player.ImpulsePlayerView
    android:id="@+id/impulse_player"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Get the reference to the view:

```kotlin
val impulsePlayer = findViewById<ImpulsePlayerView>(R.id.impulse_player)
```

### Commands

The main commands to use the player:

```kotlin
impulsePlayer.load(
    "Title",
    "Subtitle",
    "url",
)
impulsePlayer.play()
impulsePlayer.pause()
impulsePlayer.seek(0)

```

### Getters

The values exposed by the player are flows, which allows to observe the specific value if needed. 

```kotlin
impulsePlayer.isPlaying() // StateFlow<Boolean>, default `false`
impulsePlayer.getState() // StateFlow<PlayerState>, default `Loading`
impulsePlayer.getProgress() // StateFlow<Long>, default `0`
impulsePlayer.getDuration() // StateFlow<Long>, default `0`
impulsePlayer.getError() // StateFlow<String?>, default `null`
```

### Delegate

Listening to events from the player.

```kotlin
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
```

### Buttons

The player can be further extended by adding custom buttons. These will be attached to the given position.

```kotlin
impulsePlayer.removeButton("autoplay")
impulsePlayer.setButton(
    "autoplay",
    PlayerButton(
        PlayerButton.Position.TopEnd,
        R.drawable.ic_launcher_foreground,
        getString(R.string.app_name),
    ) {
        Log.d("ImpulsePlayer", "Auto play clicked")
    },
)
```

### Settings

Features can be enabled or disabled based on the settings. The defaults can be changed as follows:

```kotlin
ImpulsePlayer.setSettings(
    ImpulsePlayerSettings(
        pictureInPictureEnabled = true, // Whether Picture-in-Picture is enabled; Default `false` (disabled)
        castReceiverApplicationId = "01128E51", // Cast receiver application id of the cast app; Default `null` (disabled)
    )
)
```

### Customization

Apply a custom appearance to customize the look of the player.

```kotlin
val weightRegular = FontVariationAxis("wght", 400f)
val weightSemibold = FontVariationAxis("wght", 600f)
val semibold = Typeface.Builder(assets, "fonts/Inter-Variable.ttf")
    .setFontVariationSettings(arrayOf(weightSemibold))
    .build()
val regular = Typeface.Builder(assets, "fonts/Inter-Variable.ttf")
    .setFontVariationSettings(arrayOf(weightRegular))
    .build()
ImpulsePlayer.setAppearance(
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
            accentColor = ResourcesCompat.getColor(resources, R.color.impulse_player_accent, null),
        )
    )
)
```