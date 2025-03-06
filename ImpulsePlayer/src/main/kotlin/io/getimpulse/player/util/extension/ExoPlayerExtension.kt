package io.getimpulse.player.util.extension

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import io.getimpulse.player.util.Logging
import io.getimpulse.player.model.VideoQuality

@OptIn(UnstableApi::class)
internal fun ExoPlayer.getVideoQualities(): List<VideoQuality.Detected> {
    val result = mutableListOf<VideoQuality.Detected>()
    val tracks = currentTracks
    val groups = tracks.groups
    groups.forEachIndexed { groupIndex, group ->
        if (group.type == C.TRACK_TYPE_VIDEO) {
            for (i in 0 until group.mediaTrackGroup.length) {
                val selected = group.isTrackSelected(i)
                val format = group.mediaTrackGroup.getFormat(i)
                val width = format.width
                val height = format.height
                val bitrate = format.bitrate

                result.add(
                    VideoQuality.Detected(
                        groupIndex,
                        i,
                        selected,
                        width,
                        height,
                        bitrate,
                    )
                )
                Logging.d("Video Quality: Resolution: ${group.mediaTrackGroup.id} ${width}x$height, Bitrate: $bitrate, Selected: $selected")
            }
        }
    }
    return result.groupBy { "${it.width}x${it.height}" }
        .map { it.value.maxBy { it.bitrate } }
}

@OptIn(UnstableApi::class)
internal fun ExoPlayer.setVideoQuality(videoQuality: VideoQuality) {
    val selector = trackSelector ?: throw IllegalStateException("Missing tracks")
    val updatedParameters = selector.parameters.buildUpon()
    when (videoQuality) {
        VideoQuality.Automatic -> {
            updatedParameters.clearOverrides()
        }

        is VideoQuality.Detected -> {
            val group = currentTracks.groups[videoQuality.group].mediaTrackGroup
            updatedParameters.setOverrideForType(TrackSelectionOverride(group, videoQuality.index))
        }
    }
    trackSelector?.parameters = updatedParameters.build()
}