package com.github.eprendre.tingshu.widget

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class AudioPlayer(val context: Context) {
    private val bandwidthMeter by lazy { DefaultBandwidthMeter() }

    val player: SimpleExoPlayer by lazy {
        val trackSelectionFactory = AdaptiveTrackSelection.Factory()
        val trackSelector = DefaultTrackSelector(trackSelectionFactory)
        ExoPlayerFactory.newSimpleInstance(context, trackSelector)
    }

    private val dataSourceFactory by lazy {
        DefaultDataSourceFactory(context, Util.getUserAgent(context, "exoplayer"), bandwidthMeter)
    }

    init {
        player.addListener(object : Player.EventListener {
            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                println("$playWhenReady  $playbackState")
                if (!playWhenReady || playbackState == Player.STATE_ENDED) {
//					App.bus.send(CalendarContract.Events.HideMusic())
                    return
                }
                if (playWhenReady && (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY)) {
//					App.bus.send(Events.ShowMusic())
                }
            }

            override fun onLoadingChanged(isLoading: Boolean) {
            }
        })
    }

    fun play(uri: Uri) {
        stop()
        val source = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
        player.prepare(source)
        player.playWhenReady = true
    }

    fun pause() {
        player.playWhenReady = false
    }

    fun stop() {
        player.stop(true)
    }

    fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
    }

    fun release() {
        player.release()
    }
}