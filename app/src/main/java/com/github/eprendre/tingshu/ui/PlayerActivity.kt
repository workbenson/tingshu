package com.github.eprendre.tingshu.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.graphics.Outline
import android.graphics.PorterDuff
import android.graphics.drawable.RippleDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.GridLayoutManager
import androidx.room.EmptyResultSetException
import com.bumptech.glide.request.RequestOptions
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.TingShuService
import com.github.eprendre.tingshu.db.AppDatabase
import com.github.eprendre.tingshu.extensions.md5
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.ui.adapters.EpisodeAdapter
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.GlideApp
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.dialog_countdown.view.*
import kotlinx.android.synthetic.main.dialog_episodes.view.*
import org.jetbrains.anko.*
import java.io.File
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit


class PlayerActivity : AppCompatActivity(), AnkoLogger {
    private val compositeDisposable = CompositeDisposable()
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var myService: TingShuService
    private var isBound = false
    private var bodyTextColor: Int? = null //spinner每次选择后需要重新染色
    private var toolbarIconColor: Int? = null
    //    private var isFavorite = false
    private var favoriteBook: Book? = null
    private val dialogEpisodes: BottomSheetDialog by lazy {
        BottomSheetDialog(this).apply {
            setContentView(dialogView)
        }
    }
    private val dialogView by lazy {
        layoutInflater.inflate(R.layout.dialog_episodes, null).apply {
            recycler_view.layoutManager = GridLayoutManager(this@PlayerActivity, 3)
            recycler_view.adapter = listAdapter
        }
    }
    private val listAdapter = EpisodeAdapter {
        if (it.isFree) {
            Prefs.currentBook = Prefs.currentBook?.apply {
                this.currentEpisodePosition = 0
            }
            myService.getAudioUrl(it.url)
        } else {
            longToast("此章节是收费章节，请去原网站收听")
        }
        dialogEpisodes.dismiss()
    }
    private val dialogCountDown: BottomSheetDialog by lazy {
        BottomSheetDialog(this).apply {
            setContentView(countDownView)
        }
    }
    private val countDownView by lazy {
        layoutInflater.inflate(R.layout.dialog_countdown, null).apply {
            button_countdown_cancel.setOnClickListener {
                timer_button.text = "定时关闭"
                myService.resetTimer()
                dialogCountDown.dismiss()
            }
            button_countdown_ok.setOnClickListener {
                myService.resetTimer()
                val hours = np_hour.value
                val minutesIndex = np_minute.value
                val seconds = hours * 60 * 60 + minutesIndex * 5 * 60
                myService.setTimerSeconds(seconds.toLong())
                dialogCountDown.dismiss()
            }
            button_pause_by1.setOnClickListener {
                myService.setPauseCount(1)
                dialogCountDown.dismiss()
            }
            button_pause_by2.setOnClickListener {
                myService.setPauseCount(2)
                dialogCountDown.dismiss()
            }
            button_pause_by3.setOnClickListener {
                myService.setPauseCount(3)
                dialogCountDown.dismiss()
            }
            //formatter 默认值有 bug，于是采用 displayedValues
            np_hour.minValue = 0
            np_hour.maxValue = 23
            np_hour.displayedValues = (0..23).map { "$it 小时" }.toTypedArray()
            val minuteArray = (0..59).filter { it % 5 == 0 }.map { "$it 分钟" }.toTypedArray()
            np_minute.minValue = 0
            np_minute.maxValue = 11
            np_minute.displayedValues = minuteArray
            np_minute.value = 4
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (Prefs.currentTheme) {
            0 -> setTheme(R.style.AppTheme_Player)
            1 -> setTheme(R.style.DarkTheme_Player)
            2 -> setTheme(R.style.BlueTheme_Player)
        }
        setContentView(R.layout.activity_player)
        setSupportActionBar(toolbar)
        volumeControlStream = AudioManager.STREAM_MUSIC
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        state_layout.showLoading()
    }

    override fun onStart() {
        super.onStart()
        timer_button.text = "定时关闭"
        startAndBindService()
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

            mediaController =
                MediaControllerCompat(this@PlayerActivity, myService.mediaSession.sessionToken)
            mediaController.registerCallback(object : MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                    updateState(state)
                }
            })

            initViews()
            handleIntent()
            isBound = true
        }
    }

    /**
     * 根据播放状态更新 "播放/暂停" 按钮的图标
     */
    private fun updateState(state: PlaybackStateCompat) {
        val book = Prefs.currentBook!!
        artist_text.text = "${book.artist}"
        episode_text.text = "当前章节：${book.currentEpisodeName ?: ""}"
        listAdapter.notifyDataSetChanged()//更新当前正在播放的item颜色
        when (state.state) {
            PlaybackStateCompat.STATE_ERROR -> {
                play_progress.visibility = View.GONE
                button_play.setImageResource(R.drawable.exo_controls_play)
                button_play.contentDescription = "播放"
                toast("播放出错了(如果多次报错此地址可能已失效)")
            }
            PlaybackStateCompat.STATE_STOPPED -> {
                play_progress.visibility = View.GONE
                button_play.setImageResource(R.drawable.exo_controls_play)
                button_play.contentDescription = "播放"
            }
            PlaybackStateCompat.STATE_PLAYING -> {
                button_play.setImageResource(R.drawable.exo_controls_pause)
                button_play.contentDescription = "暂停"
                play_progress.visibility = View.GONE
                if (App.currentEpisodeIndex() < Prefs.playList.size - 1) {
                    val episodeUrl = Prefs.playList[App.currentEpisodeIndex() + 1].url
                    val file = File(externalCacheDir, episodeUrl.md5())
                    if (file.exists()) {//如果缓存文件已存在，则忽略
                        val size = Formatter.formatShortFileSize(this, file.length())
                        cache_text.text = "下集缓存成功\n大小: $size"
                    } else {
                        cache_text.text = ""
                    }
                }
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                button_play.setImageResource(R.drawable.exo_controls_play)
                button_play.contentDescription = "播放"
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        state_layout.showLoading()
        if (!::myService.isInitialized) {
            startAndBindService()
        } else {
            handleIntent()
        }
    }

    private fun startAndBindService() {
        Handler().postDelayed({
            //https://stackoverflow.com/questions/52013545/android-9-0-not-allowed-to-start-service-app-is-in-background-after-onresume
            val intent = Intent(this, TingShuService::class.java)
            startService(intent)
            bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
        }, 300)
    }

    /**
     * 检查是否有新的书本要播放
     * 如果是则加载新书的播放列表
     * 如果不是则加载上一次听书的播放列表
     */
    private fun handleIntent() {
        val bookurl = intent.getStringExtra(ARG_BOOKURL)
        if (!bookurl.isNullOrBlank() && bookurl != Prefs.currentBookUrl) {//需要换书
            Prefs.currentBookUrl = bookurl
            if (myService.exoPlayer.playbackState == Player.STATE_READY) {
                mediaController.transportControls.stop()
            }
            playFromBookUrl(bookurl)
            invalidateOptionsMenu()
        } else {//不需要换书
            if (myService.exoPlayer.playbackState == Player.STATE_IDLE) {
                //此状态代表通知栏被关闭，导致播放器移除了当前播放曲目，需要重新加载链接
                Prefs.currentBookUrl?.apply { playFromBookUrl(this) }
            } else {//继续播放
                val book = Prefs.currentBook!!
                artist_text.text = "${book.artist}"
                episode_text.text = "当前章节：${book.currentEpisodeName ?: ""}"
                supportActionBar?.title = book.title
                listAdapter.submitList(Prefs.playList)
                updateState(mediaController.playbackState)
                state_layout.showContent()
                tintColor()
                if (Prefs.isAutoPlay && mediaController.playbackState.state != PlaybackStateCompat.STATE_PLAYING) {
                    mediaController.transportControls.play()
                }
            }
        }
    }

    /**
     * 根据传入的地址抓取书籍信息
     */
    private fun playFromBookUrl(bookUrl: String) {
        TingShuSourceHandler.playFromBookUrl(bookUrl)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val book = Prefs.currentBook!!
                artist_text.text = "${book.artist}"
                episode_text.text = "当前章节：${book.currentEpisodeName ?: ""}"
                invalidateOptionsMenu()
                state_layout.showContent()
                tintColor()

                //开始请求播放
                supportActionBar?.title = book.title
                listAdapter.submitList(Prefs.playList)
                val episode = Prefs.playList.firstOrNull { !it.isFree }
                if (episode != null) {
                    longToast("本书章节第${episode.title}开始是收费内容，请去原网站收听。")
                    charge_text.visibility = View.VISIBLE
                } else {
                    charge_text.visibility = View.GONE
                }
                myService.getAudioUrl(Prefs.playList[App.currentEpisodeIndex()].url, Prefs.isAutoPlay)
            }, { error ->
                error.printStackTrace()
                state_layout.showError()
            })
            .addTo(compositeDisposable)
    }

    /**
     * 使用 Palette 提取封面颜色，并给相关控件染色
     */
    private fun tintColor() {
        GlideApp.with(this)
            .load(Prefs.currentBook!!.coverUrl)
            .error(//不能直接往error里面扔resource id, 否则transformation就不会应用
                GlideApp.with(this@PlayerActivity)
                    .load(R.drawable.default_art)
                    .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3)))
            )
            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3)))
            .into(cover_image)//背景封面太提前加载不好看，所以放到这里去加载。
        App.coverBitmap?.let { cover ->
            Palette.from(cover).generate { palette ->
                if (palette == null) {
                    return@generate
                }

                palette.dominantSwatch?.let { swatch ->
                    val bgColor = ColorUtils.setAlphaComponent(swatch.rgb, 204)
                    toolbar.setBackgroundColor(swatch.rgb)
                    //如果actionbar的背景颜色太亮，则修改toolbar, statusbar的文字、图标为深色
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                        window.statusBarColor = swatch.rgb
                        if (ColorUtils.calculateLuminance(swatch.rgb) > 0.5) {
                            val backArrow = ContextCompat.getDrawable(this, R.drawable.back)
                            backArrow?.setColorFilter(
                                swatch.bodyTextColor,
                                PorterDuff.Mode.SRC_ATOP
                            )
                            supportActionBar?.setHomeAsUpIndicator(backArrow)
                            toolbar.setTitleTextColor(swatch.bodyTextColor)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                window.decorView.systemUiVisibility =
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            }
                            toolbarIconColor = swatch.bodyTextColor
                            invalidateOptionsMenu()
                        }
                    }
                    artist_text.setTextColor(swatch.titleTextColor)
                    artist_text.setShadowLayer( 24f, 0f, 0f, swatch.rgb)//加上阴影可以解决字体颜色和附近背景颜色接近时不能识别的情况
                    episode_text.setTextColor(swatch.titleTextColor)
                    episode_text.setShadowLayer(24f, 0f, 0f, swatch.rgb)
                    charge_text.setTextColor(swatch.titleTextColor)
                    charge_text.setShadowLayer(24f, 0f, 0f, swatch.rgb)
                    cache_text.setTextColor(swatch.titleTextColor)
                    cache_text.setShadowLayer(24f, 0f, 0f, swatch.rgb)
                    control_panel.setBackgroundColor(bgColor)
                    timer_button.setTextColor(swatch.bodyTextColor)
                    playlist_button.setColorFilter(swatch.bodyTextColor)
                    speed_button.setTextColor(swatch.bodyTextColor)
                    text_current.setTextColor(swatch.bodyTextColor)
                    seekbar.progressDrawable.setColorFilter(
                        swatch.bodyTextColor,
                        PorterDuff.Mode.SRC_ATOP
                    )
                    seekbar.thumb.setColorFilter(swatch.bodyTextColor, PorterDuff.Mode.SRC_ATOP)
                    text_duration.setTextColor(swatch.bodyTextColor)
                    play_progress.indeterminateDrawable.setColorFilter(
                        swatch.bodyTextColor,
                        PorterDuff.Mode.SRC_ATOP
                    )
                    bodyTextColor = swatch.bodyTextColor
                    listOf(
                        button_fastforward,
                        button_play,
                        button_rewind,
                        button_previous,
                        button_next
                    )
                        .forEach {
                            it.setColorFilter(swatch.bodyTextColor)
                            val background = it.background
                            if (background is RippleDrawable) {
                                background.setColor(
                                    ColorStateList.valueOf(
                                        ColorUtils.setAlphaComponent(
                                            swatch.bodyTextColor,
                                            50
                                        )
                                    )
                                )
                            }
                        }
                }
            }
        }
    }


    /**
     * 初始化控件，需要在serviceConnected之后
     */
    private fun initViews() {
        GlideApp.with(this)
            .load(Prefs.currentBook!!.coverUrl)
            .error(
                GlideApp.with(this@PlayerActivity)
                    .load(R.drawable.default_art)
                    .circleCrop()
            )
            .circleCrop()
            .into(cover_round_image)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //给中间封面添加一圈圆形的阴影
            val offset = dip(12)
            cover_round_image.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val top = (view.height - view.width) / 2
                    val bottom = top + view.width
                    return outline.setOval(
                        offset,
                        top + offset,
                        view.width - offset,
                        bottom - offset
                    )
                }
            }
            cover_round_image.clipToOutline = true
            cover_round_image.elevation = offset.toFloat()
        }
        //报错时的点击重试
        state_layout.setErrorListener {
            state_layout.showLoading()
            playFromBookUrl(Prefs.currentBookUrl!!)
        }
        state_layout.setErrorText("当前网址解析出错了, 点击重试(有时候需要多试几次才能成功）")


        //定时关闭
        timer_button.setOnClickListener {
            dialogCountDown.show()
        }

        //书籍页面解析成功 -> 开始解析播放地址
        RxBus.toFlowable(RxEvent.ParsingPlayUrlEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                when (it.status) {
                    0 -> play_progress.visibility = View.VISIBLE
                    1, 3 -> play_progress.visibility = View.GONE
                    2 -> {
                        play_progress.visibility = View.GONE
                        button_play.setImageResource(R.drawable.exo_controls_play)
                        toast("播放地址解析出错了，请重试")
                    }
                }

            }
            .addTo(compositeDisposable)
        RxBus.toFlowable(RxEvent.CacheEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                when (it.status) {
                    0 -> cache_text.text = "下集缓存中..."
                    1 -> cache_text.text = "下集缓存成功\n大小: ${it.msg}"
                    2 -> cache_text.text = "下集缓存失败${it.msg}"
                    3 -> cache_text.text = "下集缓存中 ${it.progress}%${it.msg}"
                }

            }
            .addTo(compositeDisposable)
        cache_text.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("是否取消下集缓存?")
                .setPositiveButton("是") { dialog, which ->
                    myService.cancelDownloadCache()
                }
                .setNegativeButton("否", null)
                .show()
        }
        //播放速度
        myService.exoPlayer.playbackParameters = PlaybackParameters(Prefs.speed)//初始化播放器的速度
        speed_button.text = "${Prefs.speed} x"
        speed_button.contentDescription = "播放速度 ${Prefs.speed} 倍"
        speed_button.setOnClickListener {
            val updateSpeedButton = {
                myService.exoPlayer.playbackParameters = PlaybackParameters(Prefs.speed)
                speed_button.text = "${Prefs.speed} x"
                speed_button.contentDescription = "播放速度 ${Prefs.speed} 倍"
            }
            val dialog = AlertDialog.Builder(this)
                .setView(R.layout.speed_dialog)
                .show()

            dialog.setCanceledOnTouchOutside(true)
            val seekBar = dialog.find<SeekBar>(R.id.seekbar_speed)
            val speedText = dialog.find<TextView>(R.id.text_speed)
            val speedDownButton = dialog.find<ImageButton>(R.id.button_speed_down)
            val speedUpButton = dialog.find<ImageButton>(R.id.button_speed_up)
            seekBar.progress = (Prefs.speed * 10 - 1).toInt()
            speedText.text = "播放速度: ${Prefs.speed} 倍"
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val speed = (progress + 1) / 10f
                    speedText.text = "播放速度: ${speed} 倍"
                    Prefs.speed = speed
                    updateSpeedButton()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
            speedDownButton.setOnClickListener {
                seekBar.progress = seekBar.progress - 1
                updateSpeedButton()
            }
            speedUpButton.setOnClickListener {
                seekBar.progress = seekBar.progress + 1
                updateSpeedButton()
            }
        }
        //进度条
        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {//拖动进度条的时候预计目标时间
                    val duration = myService.exoPlayer.duration
                    text_current.text =
                        DateUtils.formatElapsedTime(duration * progress / 100 / 1000)
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
            when (mediaController.playbackState.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    mediaController.transportControls.pause()
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    mediaController.transportControls.play()
                }
                PlaybackStateCompat.STATE_ERROR,
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.STATE_NONE -> {
                    if (!Prefs.currentAudioUrl.isNullOrEmpty()) {
                        mediaController.transportControls.playFromUri(
                            Uri.parse(Prefs.currentAudioUrl!!), null
                        )
                    } else {
                        Prefs.currentBookUrl?.apply { playFromBookUrl(this) }
                    }
                }
            }
        }
        playlist_button.setOnClickListener { openEpisodesDialog() }
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
                    //顺便更新倒计时
                    if (myService.getPauseCount() > 0) {
                        timer_button.text = "播完 ${myService.getPauseCount()} 集关闭"
                    } else if (myService.timeToPause > SystemClock.elapsedRealtime()) {
                        val seconds = (myService.timeToPause - SystemClock.elapsedRealtime()) / 1000
                        timer_button.text = "${DateUtils.formatElapsedTime(seconds)} 后关闭"
                    } else {
                        timer_button.text = "定时关闭"
                    }

                }
            }
            .addTo(compositeDisposable)
    }

    private fun openEpisodesDialog() {
        dialogView.recycler_view.scrollToPosition(App.currentEpisodeIndex())
        dialogEpisodes.show()
    }

    override fun onResume() {
        super.onResume()
        App.isRetry = false
    }

    override fun onPause() {
        super.onPause()
        App.isRetry = true
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
        try {
            if (isBound) {
                unbindService(myConnection)
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    /**
     * 注册按键，方便咱使用黑莓key2的键盘
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_SPACE -> button_play.performClick()
            KeyEvent.KEYCODE_F -> button_fastforward.performClick()
            KeyEvent.KEYCODE_R -> button_rewind.performClick()
            KeyEvent.KEYCODE_N -> button_next.performClick()
            KeyEvent.KEYCODE_P -> button_previous.performClick()
            else -> super.onKeyUp(keyCode, event)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.player_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
//        val linkItem = menu.findItem(R.id.link)
        toolbarIconColor?.let { color ->
            toolbar.overflowIcon?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
        val favoriteItem = menu.findItem(R.id.favorite)
        Prefs.currentBookUrl?.let {
            AppDatabase.getInstance(this).bookDao().findByBookUrl(it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = { book ->
                    //如果已收藏设置图标为实心
                    favoriteItem.setIcon(R.drawable.ic_favorite)
                    favoriteItem.title = "取消收藏"
                    toolbarIconColor?.let { color ->
                        favoriteItem.icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                    }
                    favoriteBook = book
                }, onError = { e ->
                    //如果未收藏设置图标为空心
                    if (e is EmptyResultSetException) {
                        favoriteItem.setIcon(R.drawable.ic_favorite_border)
                        favoriteItem.title = "添加收藏"
                        toolbarIconColor?.let { color ->
                            favoriteItem.icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                        }
                        favoriteBook = null
                    }
                    e.printStackTrace()
                })
                .addTo(compositeDisposable)
        }
        val descItem = menu.findItem(R.id.desc)
        toolbarIconColor?.let { color ->
            descItem.icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
        descItem.isVisible = !Prefs.currentIntro.isNullOrBlank()

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.link -> {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(Prefs.currentBook!!.currentEpisodeUrl)
                startActivity(i)
            }
            R.id.favorite -> {
                if (favoriteBook != null) {
                    //取消收藏
                    AppDatabase.getInstance(this).bookDao()
                        .deleteBooks(favoriteBook!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(onSuccess = {
                            invalidateOptionsMenu()
                        }, onError = {
                            it.printStackTrace()
                        })
                        .addTo(compositeDisposable)

                } else {
                    //添加收藏
//                    val book = Book(
//                        Prefs.currentCover!!,
//                        Prefs.currentBookUrl!!,
//                        Prefs.currentBookName!!,
//                        Prefs.author!!,
//                        Prefs.artist!!
//                    ).apply {
//                        this.currentEpisodeUrl = Prefs.currentEpisodeUrl
//                        this.currentEpisodeName = Prefs.currentEpisodeName
//                        this.currentEpisodePosition = Prefs.currentEpisodePosition
//                    }
                    AppDatabase.getInstance(this).bookDao()
                        .insertBooks(Prefs.currentBook!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(onComplete = {
                            invalidateOptionsMenu()
                        }, onError = {
                            it.printStackTrace()
                        })
                        .addTo(compositeDisposable)
                }
            }
            R.id.desc -> {
                Prefs.currentIntro?.let {
                    AlertDialog.Builder(this)
                        .setMessage(it)
                        .setCancelable(true)
                        .show()
                }
            }
            R.id.skip -> {
                val currentBook = Prefs.currentBook!!
                val dialog = AlertDialog.Builder(this)
                    .setView(R.layout.dialog_skip)
                    .setPositiveButton("确定") { dialog, which ->
                        Prefs.currentBook = currentBook
                        storeSkipPosition(currentBook)
                    }
                    .setNegativeButton("取消", null)
                    .show()
                val textSkipBeginning = dialog.find<TextView>(R.id.text_skip_beginning)
                val seekBarSkipBeginning = dialog.find<SeekBar>(R.id.seekbar_skip_beginning)
                val textSkipEnd = dialog.find<TextView>(R.id.text_skip_end)
                val seekBarSkipEnd = dialog.find<SeekBar>(R.id.seekbar_skip_end)
                val resetButton = dialog.find<Button>(R.id.reset_skipping)
                textSkipBeginning.text = "跳过片头 ${currentBook.skipBeginning / 1000} 秒"
                textSkipEnd.text = "跳过片尾 ${currentBook.skipEnd / 1000} 秒"
                seekBarSkipBeginning.progress = (currentBook.skipBeginning / 1000).toInt()
                seekBarSkipEnd.progress = (currentBook.skipEnd / 1000).toInt()
                seekBarSkipBeginning.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        textSkipBeginning.text = "跳过片头 ${progress} 秒"
                        currentBook.skipBeginning = seekBar.progress * 1000L
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                    }
                })
                seekBarSkipEnd.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        textSkipEnd.text = "跳过片尾 ${progress} 秒"
                        currentBook.skipEnd = seekBar.progress * 1000L
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                    }
                })
                resetButton.setOnClickListener {
                    seekBarSkipBeginning.progress = 0
                    seekBarSkipEnd.progress = 0
                    currentBook.skipBeginning = 0
                    currentBook.skipEnd = 0
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("CheckResult")
    private fun storeSkipPosition(book: Book) {
        Prefs.storeSkipPosition(book)
        AppDatabase.getInstance(this)
            .bookDao()
            .findByBookUrl(book.bookUrl)
            .subscribeOn(Schedulers.io())
            .subscribeBy(onSuccess = {
                it.skipBeginning = book.skipBeginning
                it.skipEnd = book.skipEnd
                AppDatabase.getInstance(this)
                    .bookDao()
                    .updateBooks(it)
                    .subscribeOn(Schedulers.io())
                    .subscribeBy(onComplete = {}, onError = {})
            }, onError = {
                if (it is EmptyResultSetException) {
                    //数据库没有,忽略
                } else {
                    it.printStackTrace()
                }
            })
    }

    companion object {
        const val ARG_BOOKURL = "bookurl"
    }
}
