package com.github.eprendre.tingshu.sources

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.extensions.*
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
        RxBus.post(RxEvent.ParsingPlayUrlEvent(0))//借用这个event，让播放按钮外面的圈圈显示
        val book = Prefs.currentBook!!
        val bookname = book.currentEpisodeName + " - " + book.title

        val metadata = MediaMetadataCompat.Builder()
            .apply {
                title = bookname
                artist = book.artist
                mediaUri = uri.toString()

                displayTitle = bookname
                displaySubtitle = book.artist
                downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED

                if (Prefs.showAlbumInLockScreen) {
                    var art = App.coverBitmap
                    if (art == null) {
                        art = BitmapFactory.decodeResource(App.appContext.resources, R.drawable.ic_notification)
                    }
                    albumArt = art
                }
            }
            .build()

        val source = metadata.toMediaSource(dataSourceFactory)
        exoPlayer.prepare(source)
        if (book.currentEpisodePosition > 0) {
            exoPlayer.seekTo(book.currentEpisodePosition)
        }
    }

    override fun onPrepare() {
    }
}