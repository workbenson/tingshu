package com.github.eprendre.tingshu.sources

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.os.ResultReceiver
import android.support.v4.media.session.PlaybackStateCompat
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DataSource

/**
 * 在这里获取真实的播放地址
 */
class MyPlaybackPreparer(
    private val exoPlayer: ExoPlayer,
    private val dataSourceFactory: DataSource.Factory
) : MediaSessionConnector.PlaybackPreparer {
    var wakeLock: PowerManager.WakeLock? = null
    init {
        RxBus.toFlowable(RxEvent.ReleaseWakeLockEvent::class.java)
            .subscribe {
                releaseWakeLock()
            }
    }

    override fun onCommand(
        player: Player?,
        controlDispatcher: ControlDispatcher?,
        command: String?,
        extras: Bundle?,
        cb: ResultReceiver?
    ): Boolean {
        return false
    }

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
    }

    override fun getSupportedPrepareActions(): Long {
        return PlaybackStateCompat.ACTION_PLAY_FROM_URI
    }

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
        if (uri == null) {
            return
        }
        val url = uri.toString()
        Prefs.currentEpisodeUrl = url
        Prefs.currentEpisodeName = App.currentEpisode().title
        RxBus.post(RxEvent.ParsingPlayUrlEvent())

        releaseWakeLock()
        wakeLock =
            (App.appContext.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                    acquire(30000)
                }
            }
        TingShuSourceHandler.getAudioUrlExtractor(url, exoPlayer, dataSourceFactory).extract(url)
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onPrepare() {
    }
}