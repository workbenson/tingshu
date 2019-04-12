package com.github.eprendre.tingshu

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.github.eprendre.tingshu.extensions.title
import com.github.eprendre.tingshu.utils.Episode
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import com.google.android.exoplayer2.PlaybackParameters
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_player.*
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit


class PlayerActivity : AppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var myService: TingShuService
    var isBound = false
    private val listAdapter = EpisodeAdapter {
        Prefs.currentEpisodePosition = 0
        mediaController.transportControls.playFromUri(Uri.parse(it.url), null)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        volumeControlStream = AudioManager.STREAM_MUSIC

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        state_layout.showLoading()

        val intent = Intent(this, TingShuService::class.java)
        startService(intent)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private val myConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as TingShuService.MyLocalBinder
            myService = binder.getService()
            isBound = true

            mediaController = MediaControllerCompat(this@PlayerActivity, myService.mediaSession.sessionToken)
            mediaController.registerCallback(object : MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                    updateState(state)
                }
            })

            initViews()
            handleIntent()
        }
    }

    /**
     * 根据播放状态更新 "播放/暂停" 按钮的图标
     */
    private fun updateState(state: PlaybackStateCompat) {
        when (state.state) {
//            PlaybackStateCompat.STATE_BUFFERING,
            PlaybackStateCompat.STATE_PLAYING -> {
                supportActionBar?.title = mediaController.metadata.title
                button_play.setImageResource(R.drawable.exo_controls_pause)
                listAdapter.notifyDataSetChanged()
                recycler_view.scrollToPosition(App.currentEpisodeIndex())
                state_layout.showContent()
            }
            PlaybackStateCompat.STATE_PAUSED -> button_play.setImageResource(R.drawable.exo_controls_play)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    /**
     * 检查是否有新的书本要播放
     * 如果是则加载新书的播放列表
     * 如果不是则加载上一次听书的播放列表
     */
    private fun handleIntent() {
        val bookurl = intent.getStringExtra("bookurl")
        if (!bookurl.isNullOrBlank()) {
            Completable.fromCallable {
                val doc = Jsoup.connect(bookurl).get()
                val book = doc.getElementsByClass("list-ov-tw").first()
                val cover = book.getElementsByTag("img").first().attr("src")
                //下载封面
                val glideOptions = RequestOptions()
                    .fallback(R.drawable.default_art)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                App.coverBitmap = Glide.with(App.appContext)
                    .applyDefaultRequestOptions(glideOptions)
                    .asBitmap()
                    .load(cover)
                    .submit(144, 144)
                    .get()

                val bookInfos = book.getElementsByTag("span").map { it.text() }
                Prefs.artist = "${bookInfos[2]} ${bookInfos[3]}"

                val episodes = doc.getElementById("playlist")
                    .getElementsByTag("a")
                    .map {
                        Episode(it.text(), "http://m.ting56.com${it.attr("href")}")
                    }
                App.playList.apply {
                    clear()
                    addAll(episodes)
                }

                var index = App.currentEpisodeIndex()
                if (index < 0) {
                    index = 0
                    Prefs.currentEpisodePosition = 0
                }
                mediaController.transportControls.playFromUri(Uri.parse(App.playList[index].url), null)
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    infos.text = Prefs.artist
                }, { error ->
                    error.printStackTrace()
                    state_layout.showError()
                })
                .addTo(compositeDisposable)

        } else {
            infos.text = Prefs.artist
            updateState(mediaController.playbackState)
            state_layout.showContent()
        }
    }

    /**
     * 初始化控件，需要在serviceConnected之后
     */
    private fun initViews() {
        recycler_view.layoutManager = GridLayoutManager(this, 3)
        recycler_view.adapter = listAdapter
        listAdapter.submitList(App.playList)

        myService.exoPlayer.playbackParameters = PlaybackParameters(Prefs.speed)
        when (Prefs.speed) {
            0.75f -> radiogroup.check(R.id.button_speed_0_75)
            1f -> radiogroup.check(R.id.button_speed_1)
            1.25f -> radiogroup.check(R.id.button_speed_1_25)
            1.5f -> radiogroup.check(R.id.button_speed_1_5)
            2f -> radiogroup.check(R.id.button_speed_2)
            else -> radiogroup.check(R.id.button_speed_1)
        }

        //定时关闭
        timer_button.setOnClickListener {
            val list = arrayOf(
                "取消定时",
                "10分钟",
                "20分钟",
                "30分钟",
                "10秒钟(测试用)"
            )
            AlertDialog.Builder(this)
                .setItems(list) { dialog, which ->
                    myService.resetTimer()
                    when (which) {
                        0 -> timer_button.text = "定时关闭"
                        1 -> myService.setTimerSeconds(10 * 60)
                        2 -> myService.setTimerSeconds(20 * 60)
                        3 -> myService.setTimerSeconds(30 * 60)
                        4 -> myService.setTimerSeconds(10)
                    }
                }
                .create()
                .show()
        }

        RxBus.toFlowable(RxEvent.TimerEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                timer_button.text = it.msg
            }
            .addTo(compositeDisposable)

        //播放速度
        radiogroup.setOnCheckedChangeListener { group, checkedId ->
            val params = when (checkedId) {
                R.id.button_speed_0_75 -> PlaybackParameters(0.75f)
                R.id.button_speed_1 -> PlaybackParameters(1f)
                R.id.button_speed_1_25 -> PlaybackParameters(1.25f)
                R.id.button_speed_1_5 -> PlaybackParameters(1.5f)
                R.id.button_speed_2 -> PlaybackParameters(2f)
                else -> PlaybackParameters(1f)
            }
            myService.exoPlayer.playbackParameters = params
            Prefs.speed = params.speed
        }
        //进度条
        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {//拖动进度条的时候预计目标时间
                    val duration = myService.exoPlayer.duration
                    text_current.text = DateUtils.formatElapsedTime(duration * progress / 100 / 1000)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val duration = myService.exoPlayer.duration
                myService.exoPlayer.seekTo(duration * seekBar.progress / 100)
            }
        })
        //快退
        button_rewind.setOnClickListener {
            myService.exoPlayer.let { player ->
                var position = player.currentPosition - 10_000
                if (position < 0) {
                    position = 0
                }
                player.seekTo(position)
            }
        }
        //快进
        button_fastforward.setOnClickListener {
            myService.exoPlayer.let { player ->
                var position = player.currentPosition + 10_000
                val duration = player.duration
                if (position > duration) {
                    position = duration
                }
                player.seekTo(position)
            }
        }
        //上一首
        button_previous.setOnClickListener {
            mediaController.transportControls.skipToPrevious()
        }
        //下一首
        button_next.setOnClickListener {
            mediaController.transportControls.skipToNext()
        }
        //播放/暂停
        button_play.setOnClickListener {
            if (mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                mediaController.transportControls.pause()
            } else {
                mediaController.transportControls.play()
            }
        }
        //定时更新播放时长、进度条
        Flowable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                myService.exoPlayer.let { player ->
                    val bufferedPercentage = player.bufferedPercentage
                    val duration = player.duration
                    val position = player.currentPosition
                    val positionPercentage = (position * 100 / duration).toInt()

                    if (duration > 0) {
                        text_current.text = DateUtils.formatElapsedTime(position / 1000)
                        text_duration.text = DateUtils.formatElapsedTime(duration / 1000)
                        seekbar.secondaryProgress = bufferedPercentage
                        seekbar.progress = positionPercentage
                    }
                }
            }
            .addTo(compositeDisposable)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        unbindService(myConnection)
    }
}
