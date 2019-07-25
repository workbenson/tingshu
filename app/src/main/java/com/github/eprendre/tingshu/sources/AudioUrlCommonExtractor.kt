package com.github.eprendre.tingshu.sources

import android.graphics.BitmapFactory
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.extensions.*
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Jsoup 就可搞定的音频提取用此类
 */
object AudioUrlCommonExtractor : AudioUrlExtractor {
    private val compositeDisposable by lazy { CompositeDisposable() }
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var parse: (Document) -> String

    fun setUp(exoPlayer: ExoPlayer, dataSourceFactory: DataSource.Factory, parse: (Document) -> String) {
        this.exoPlayer = exoPlayer
        this.dataSourceFactory = dataSourceFactory
        this.parse = parse
    }

    override fun extract(url: String) {
        compositeDisposable.clear()
        Single.fromCallable {
            return@fromCallable parse(Jsoup.connect(url).get())
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { audioUrl ->

                val bookname = Prefs.currentEpisodeName + " - " + Prefs.currentBookName

                val metadata = MediaMetadataCompat.Builder()
                    .apply {
                        title = bookname
                        artist = Prefs.artist
                        mediaUri = audioUrl

                        displayTitle = bookname
                        displaySubtitle = Prefs.artist
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
                if (Prefs.currentEpisodePosition > 0) {
                    exoPlayer.seekTo(Prefs.currentEpisodePosition)
                }
            }, onError = {
                RxBus.post(RxEvent.ParsingPlayUrlErrorEvent())
            }).addTo(compositeDisposable)
    }
}