package com.github.eprendre.tingshu

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import androidx.core.app.NotificationManagerCompat
import androidx.media.session.MediaButtonReceiver
import androidx.room.EmptyResultSetException
import com.github.eprendre.tingshu.db.AppDatabase
import com.github.eprendre.tingshu.sources.MyPlaybackPreparer
import com.github.eprendre.tingshu.sources.MyQueueNavigator
import com.github.eprendre.tingshu.ui.PlayerActivity
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.NOW_PLAYING_NOTIFICATION
import com.github.eprendre.tingshu.widget.NotificationBuilder
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class TingShuService : Service() {
    val myBinder = MyLocalBinder()
    private val compositeDisposable = CompositeDisposable()

    lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationBuilder
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private var isForegroundService = false
    val exoPlayer: SimpleExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this).apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
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
        becomingNoisyReceiver = BecomingNoisyReceiver(context = this, sessionToken = mediaSession.sessionToken)
        mediaSessionConnector = MediaSessionConnector(mediaSession).also {
            val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "tingshu"))

            val playbackPrepare = MyPlaybackPreparer(exoPlayer, dataSourceFactory)
            it.setPlayer(exoPlayer, playbackPrepare)
            it.setQueueNavigator(MyQueueNavigator(mediaSession))
        }
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> mediaController.transportControls.skipToNext()
                }
            }
        })
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
        Flowable.interval(1, TimeUnit.SECONDS)
            .take(seconds)
            .subscribeBy(onNext = {
                RxBus.post(RxEvent.TimerEvent("${DateUtils.formatElapsedTime(seconds - it)} 后关闭"))
            }, onComplete = {
                RxBus.post(RxEvent.TimerEvent("定时关闭"))
                mediaController.transportControls.pause()
            })
            .addTo(compositeDisposable)
    }

    fun resetTimer() {
        compositeDisposable.clear()
    }

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
            if (state.state == PlaybackStateCompat.STATE_PAUSED || state.state == PlaybackStateCompat.STATE_PLAYING) {
                storeCurrentPosition()
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
                    } else if (notification != null) {
                        notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                    }
                }
                else -> {
                    becomingNoisyReceiver.unregister()

                    if (isForegroundService) {
                        stopForeground(false)
                        isForegroundService = false

                        // If playback has ended, also stop the service.
                        if (updatedState == PlaybackStateCompat.STATE_NONE) {
                            stopSelf()
                        }

                        if (notification != null) {
                            notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                        } else {
                            //现在的跳转下一首的姿势比较非主流，会造成通知栏被关掉再打开，故备注这段代码
//                            removeNowPlayingNotification()
                        }
                    }
                }
            }
        }
    }

    private fun storeCurrentPosition() {
        if (exoPlayer.playbackState == Player.STATE_READY) {
            Prefs.currentEpisodePosition = exoPlayer.currentPosition
            AppDatabase.getInstance(this@TingShuService)
                .bookDao()
                .findByBookUrl(Prefs.currentBookUrl!!)
                .subscribeOn(Schedulers.io())
                .subscribeBy(onSuccess = { book ->
                    book.currentEpisodePosition = Prefs.currentEpisodePosition
                    book.currentEpisodeName = Prefs.currentEpisodeName
                    book.currentEpisodeUrl = Prefs.currentEpisodeUrl
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
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
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
            context.unregisterReceiver(this)
            registered = false
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            controller.transportControls.pause()
        }
    }
}