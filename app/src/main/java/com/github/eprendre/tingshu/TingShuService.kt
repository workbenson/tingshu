package com.github.eprendre.tingshu

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.Formatter
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.media.session.MediaButtonReceiver
import androidx.room.EmptyResultSetException
import com.github.eprendre.tingshu.db.AppDatabase
import com.github.eprendre.tingshu.extensions.md5
import com.github.eprendre.tingshu.sources.MyPlaybackPreparer
import com.github.eprendre.tingshu.sources.MyQueueNavigator
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.ui.PlayerActivity
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.requests.CancellableRequest
import com.github.kittinunf.result.Result
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.File
import java.lang.Exception
import java.util.concurrent.TimeUnit

class TingShuService : Service(), AnkoLogger {
    private val myBinder = MyLocalBinder()
    private val busDisposables = CompositeDisposable()
    private var tickDisposables = CompositeDisposable()
    private var retryCount = 0
    private var pauseCount = -1
    private var isSkipping = false
    private var isRetrying = false

    lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationBuilder
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var closeReciver: CloseBroadcastReceiver
    private val timerHandler by lazy { Handler() }
    private val timerRunnable by lazy {
        Runnable {
            mediaController.transportControls.pause()
        }
    }
    var timeToPause = 0L
    private var downloadRequest: CancellableRequest? = null
    private var downloadingEpisodeUrl: String? = null
    private var downloadProgress = 0L

    private var isForegroundService = false
    val exoPlayer: SimpleExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this).apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                !Prefs.ignoreFocus
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Build a PendingIntent that can be used to launch the UI.
//        val sessionIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val sessionIntent = Intent(this, PlayerActivity::class.java)
        val sessionActivityPendingIntent = PendingIntent.getActivity(this, 0, sessionIntent, 0)

        // Create a new MediaSession.
        mediaSession = MediaSessionCompat(this, "TingShuService")
            .apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true
            }
        mediaController = MediaControllerCompat(this, mediaSession).also {
            it.registerCallback(MediaControllerCallback())
        }

        notificationBuilder = NotificationBuilder(this)
        notificationManager = NotificationManagerCompat.from(this)
        becomingNoisyReceiver =
            BecomingNoisyReceiver(context = this, sessionToken = mediaSession.sessionToken)
        closeReciver = CloseBroadcastReceiver(this)
        mediaSessionConnector = MediaSessionConnector(mediaSession).also {
            val httpDataSourceFactory = DefaultHttpDataSourceFactory(
                Util.getUserAgent(this, "tingshu"),
                null,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true
            )
            val dataSourceFactory = DefaultDataSourceFactory(this, null, httpDataSourceFactory)

            val playbackPrepare = MyPlaybackPreparer(exoPlayer, dataSourceFactory)
            it.setPlayer(exoPlayer)
            it.setPlaybackPreparer(playbackPrepare)
            it.setQueueNavigator(MyQueueNavigator(mediaSession, this))
        }
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        if (exoPlayer.duration > 10000) {//静听网会播放一个访问过快的音频，造成不停地跳转下一集
                            if (isSkipping) return
                            if (pauseCount > 0) {
                                pauseCount -= 1
                            }
                            isSkipping = true
                            mediaController.transportControls.skipToNext()
                        } else {
                            Prefs.currentBook = Prefs.currentBook?.apply {
                                this.currentEpisodePosition = 0
                            }
                        }
                    }
                    Player.STATE_READY -> {
                        retryCount = 0
                        isSkipping = false
                        Prefs.currentAudioUrl = null
                        val currentBook = Prefs.currentBook ?: return
                        if (!exoPlayer.playWhenReady) return

                        if (pauseCount == 0) {//检测按集关闭
                            mediaController.transportControls.pause()
                            pauseCount = -1
                            return
                        }

                        if ((currentBook.skipBeginning + currentBook.skipEnd) > exoPlayer.duration) return//若设置的片头片尾大于音频总长度则忽略
                        if (exoPlayer.currentPosition < currentBook.skipBeginning) {//跳过片头
                            exoPlayer.seekTo(currentBook.skipBeginning)
                            return
                        }
                    }
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                retryOnError()
            }
        })
        RxBus.toFlowable(RxEvent.ParsingPlayUrlEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                when (it.status) {
                    3 -> {
                        Prefs.currentAudioUrl?.let { url ->
                            mediaController.transportControls.playFromUri(
                                Uri.parse(url),null)
                        }
                    }
                    2 -> {
                        retryOnError()
                    }
                }
            }
            .addTo(busDisposables)
        RxBus.toFlowable(RxEvent.StorePositionEvent::class.java)
            .subscribe {
                if (exoPlayer.playWhenReady) {
                    storeCurrentPosition(it.book)
                }
            }
            .addTo(busDisposables)
        RxBus.toFlowable(RxEvent.CacheEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                when (it.status) {
                    0 -> {
                        downloadRequest?.cancel() //先取消上一个任务

                        val tmpFile = File(externalCacheDir, it.episodeUrl.md5() + ".tmp")
                        downloadRequest = Fuel.download(it.audioUrl)
                            .fileDestination { response, request -> tmpFile }
                            .progress { readBytes, totalBytes ->
                                val progress = 100 * readBytes / totalBytes
                                if (downloadProgress != progress && progress % 5 == 0L) {
                                    downloadProgress = progress
                                    val read = Formatter.formatShortFileSize(this, readBytes)
                                    val total = Formatter.formatShortFileSize(this, totalBytes)
                                    val event = RxEvent.CacheEvent(it.episodeUrl, it.audioUrl, 3)
                                    event.progress = progress
                                    event.msg = "\n$read/$total"
                                    RxBus.post(event)
                                }
                            }
                            .response { result ->
                                downloadingEpisodeUrl = null
                                when (result) {
                                    is Result.Failure -> {
                                        RxBus.post(RxEvent.CacheEvent(it.episodeUrl, it.audioUrl, 2))
                                    }
                                    is Result.Success -> {
                                        val length = tmpFile.length()
                                        if (length < 100 * 1024) { //小于100KB，说明是访问过快的音频文件
                                            tmpFile.delete()
                                            val event = RxEvent.CacheEvent(it.episodeUrl, it.audioUrl, 2).apply {
                                                msg = "\n您访问过快"
                                            }
                                            RxBus.post(event)
                                        } else {
                                            val fileSize = Formatter.formatShortFileSize(this, tmpFile.length())
                                            val event = RxEvent.CacheEvent(it.episodeUrl, it.audioUrl, 1).apply {
                                                msg = fileSize
                                            }
                                            RxBus.post(event)
                                            tmpFile.renameTo(File(externalCacheDir, it.episodeUrl.md5()))
                                        }
                                    }
                                }
                            }

                    }
                }
            }
            .addTo(busDisposables)
    }

    fun cancelDownloadCache() {
        downloadRequest?.cancel()
        if (App.currentEpisodeIndex() < Prefs.playList.size - 1) {
            val episodeUrl = Prefs.playList[App.currentEpisodeIndex() + 1].url
            val file = File(externalCacheDir, episodeUrl.md5())
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun retryOnError() {
        if (isRetrying) {
            return
        }
        isRetrying = true
        if (Prefs.audioOnError) {
            val player = MediaPlayer.create(applicationContext, R.raw.play_failed)
            player.setOnCompletionListener {
                if (App.isRetry && retryCount < 3) {
                    MediaPlayer.create(applicationContext, R.raw.retry).start()
                    Handler().postDelayed({
                        mediaController.transportControls.playFromUri(
                            Uri.parse(Prefs.currentBook!!.currentEpisodeUrl),
                            null
                        )
                        isRetrying = false
                    }, 1000)
                    retryCount += 1
                }
            }
            player.start()
        } else {
            if (App.isRetry && retryCount < 3) {
                Handler().postDelayed({
                    mediaController.transportControls.playFromUri(
                        Uri.parse(Prefs.currentBook!!.currentEpisodeUrl),
                        null
                    )
                    isRetrying = false
                }, 1000)
                retryCount += 1
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return myBinder
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): TingShuService {
            return this@TingShuService
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    fun setTimerSeconds(seconds: Long) {
        pauseCount = -1
        timeToPause = SystemClock.elapsedRealtime() + seconds * 1000
        timerHandler.postDelayed(timerRunnable, seconds * 1000)
    }

    fun resetTimer() {
        pauseCount = -1
        timeToPause = 0
        timerHandler.removeCallbacks(timerRunnable)
    }

    fun setPauseCount(count: Int) {
        resetTimer()//如果有定时的话需要先重置
        pauseCount = count
    }

    fun getPauseCount() = pauseCount


    /**
     * Removes the [NOW_PLAYING_NOTIFICATION] notification.
     *
     * Since `stopForeground(false)` was already called (see
     * [MediaControllerCallback.onPlaybackStateChanged], it's possible to cancel the notification
     * with `notificationManager.cancel(NOW_PLAYING_NOTIFICATION)` if minSdkVersion is >=
     * [Build.VERSION_CODES.LOLLIPOP].
     *
     * Prior to [Build.VERSION_CODES.LOLLIPOP], notifications associated with a foreground
     * service remained marked as "ongoing" even after calling [Service.stopForeground],
     * and cannot be cancelled normally.
     *
     * Fortunately, it's possible to simply call [Service.stopForeground] a second time, this
     * time with `true`. This won't change anything about the service's state, but will simply
     * remove the notification.
     */
    private fun removeNowPlayingNotification() {
        stopForeground(true)
    }

    /**
     * Class to receive callbacks about state changes to the [MediaSessionCompat]. In response
     * to those callbacks, this class:
     *
     * - Build/update the service's notification.
     * - Register/unregister a broadcast receiver for [AudioManager.ACTION_AUDIO_BECOMING_NOISY].
     * - Calls [Service.startForeground] and [Service.stopForeground].
     */
    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            mediaController.playbackState?.let { updateNotification(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (state == null) {
                return
            }
            updateNotification(state)
            when (state.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    tickDisposables.clear()
                    Flowable.interval(1, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            val currentBook = Prefs.currentBook ?: return@subscribe
                            if (it % 30 == 0L) {//每30秒保存一次位置
                                storeCurrentPosition()
                            }
                            if (exoPlayer.playbackState != Player.STATE_READY || !exoPlayer.playWhenReady) return@subscribe//如果没有在播放状态则不检测
                            if (exoPlayer.duration == C.TIME_UNSET) return@subscribe
                            if ((currentBook.skipBeginning + currentBook.skipEnd) > exoPlayer.duration) return@subscribe//若设置的片头片尾大于音频总长度则忽略
                            if (exoPlayer.currentPosition + currentBook.skipEnd > exoPlayer.duration) {//跳过片尾
                                if (isSkipping) return@subscribe
                                if (pauseCount > 0) {
                                    pauseCount -= 1
                                }
                                isSkipping = true
                                mediaController.transportControls.skipToNext()
                            }
                        }.addTo(tickDisposables)
                    storeCurrentPosition()

                    cacheAudioUrl()
                }
                PlaybackStateCompat.STATE_ERROR,
                PlaybackStateCompat.STATE_PAUSED -> {
                    tickDisposables.clear()
                    storeCurrentPosition(ignorePaused = true)
                }
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.STATE_NONE -> {
                    tickDisposables.clear()
                }
            }
        }

        private fun updateNotification(state: PlaybackStateCompat) {
            val updatedState = state.state
            if (mediaController.metadata == null) {
                return
            }

            // Skip building a notification when state is "none".
            val notification = if (updatedState != PlaybackStateCompat.STATE_NONE) {
                notificationBuilder.buildNotification(mediaSession.sessionToken)
            } else {
                null
            }

            when (updatedState) {
                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING -> {
                    becomingNoisyReceiver.register()

                    /**
                     * This may look strange, but the documentation for [Service.startForeground]
                     * notes that "calling this method does *not* put the service in the started
                     * state itself, even though the name sounds like it."
                     */
                    if (!isForegroundService) {
                        startService(Intent(applicationContext, this@TingShuService.javaClass))
                        startForeground(NOW_PLAYING_NOTIFICATION, notification)
                        isForegroundService = true
                        closeReciver.register()
                    } else if (notification != null) {
                        notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                    }
                }
                else -> {
                    becomingNoisyReceiver.unregister()

                    if (isForegroundService) {
//                        stopForeground(false)
//                        isForegroundService = false
//
//                        // If playback has ended, also stop the service.
////                        if (updatedState == PlaybackStateCompat.STATE_NONE) {
////                            stopSelf()
////                        }
//
                        if (notification != null) {
                            notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
//                        } else {
//                            //现在的跳转下一首的姿势比较非主流，会造成通知栏被关掉再打开，故备注这段代码
////                            removeNowPlayingNotification()
                        }
                    }
                }
            }
        }
    }

    fun exit() {
        mediaController.transportControls.pause()
        if (isForegroundService) {
            closeReciver.unregister()
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }
    }

    fun getAudioUrl(episodeUrl: String, autoPlay: Boolean = true) {
        val book = Prefs.currentBook!!
        book.currentEpisodeUrl = episodeUrl
        book.currentEpisodeName = Prefs.playList.first { it.url == episodeUrl }.title
        Prefs.currentBook = book
        val file = File(externalCacheDir, episodeUrl.md5())
        if (file.exists()) {
            mediaController.transportControls.playFromUri(file.toUri(),null)
        } else {
            RxBus.post(RxEvent.ParsingPlayUrlEvent(0))
            TingShuSourceHandler.getAudioUrlExtractor(episodeUrl).extract(episodeUrl, autoPlay)
        }
    }

    private fun cacheAudioUrl() {
        if (!Prefs.isCacheNextEpisode) {
            return
        }
        if (App.currentEpisodeIndex() < Prefs.playList.size - 1) {
            val episodeUrl = Prefs.playList[App.currentEpisodeIndex() + 1].url
            if (episodeUrl.startsWith(TingShuSourceHandler.SOURCE_URL_JINGTINGWANG)) {
                return //静听网不能频繁加载，故不做缓存。
            }
            //audiourl可能每次都不一样，所以用episodeUrl来判断
            if (downloadingEpisodeUrl == episodeUrl) {//如果正在下载，则忽略
                return
            }
            if (File(externalCacheDir, episodeUrl.md5()).exists()) {//如果缓存文件已存在，则忽略
                return
            }
            downloadingEpisodeUrl = episodeUrl
            TingShuSourceHandler.getAudioUrlExtractor(episodeUrl).extract(episodeUrl,
                autoPlay = false,
                isCache = true
            )
        }
    }

    @SuppressLint("CheckResult")
    private fun storeCurrentPosition(b: Book? = null, ignorePaused: Boolean = false) {
        if (!ignorePaused && mediaController.playbackState.state != PlaybackStateCompat.STATE_PLAYING) return//如果不在播放就不保存
        val currentBook: Book = b ?: Prefs.currentBook ?: return
        currentBook.currentEpisodePosition = exoPlayer.currentPosition
        if (currentBook.currentEpisodePosition == 0L) return

        Prefs.storeHistoryPosition(currentBook)
        Prefs.currentBook = currentBook
        AppDatabase.getInstance(this@TingShuService)
            .bookDao()
            .findByBookUrl(currentBook.bookUrl)
            .subscribeOn(Schedulers.io())
            .subscribeBy(onSuccess = { book ->
                book.currentEpisodePosition = currentBook.currentEpisodePosition
                book.currentEpisodeName = currentBook.currentEpisodeName
                book.currentEpisodeUrl = currentBook.currentEpisodeUrl
                AppDatabase.getInstance(this@TingShuService)
                    .bookDao()
                    .updateBooks(book)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onComplete = {}, onError = {})
            }, onError = {
                if (it is EmptyResultSetException) {
                    //数据库没有,忽略
                } else {
                    it.printStackTrace()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.stop(true)
        busDisposables.clear()
    }
}

/**
 * Helper class for listening for when headphones are unplugged (or the audio
 * will otherwise cause playback to become "noisy").
 */
private class BecomingNoisyReceiver(
    private val context: Context,
    sessionToken: MediaSessionCompat.Token
) : BroadcastReceiver() {

    private val noisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val controller = MediaControllerCompat(context, sessionToken)

    private var registered = false

    fun register() {
        if (!registered) {
            context.registerReceiver(this, noisyIntentFilter)
            registered = true
        }
    }

    fun unregister() {
        if (registered) {
            try {
                context.unregisterReceiver(this)
            } catch (e: Exception) {
            }
            registered = false
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            controller.transportControls.pause()
        }
    }
}