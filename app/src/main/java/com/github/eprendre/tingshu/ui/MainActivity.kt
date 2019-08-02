package com.github.eprendre.tingshu.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.SearchRecentSuggestions
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.github.eprendre.tingshu.BuildConfig
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.TingShuService
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.ui.adapters.CategoryPagerAdapter
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.utils.CategoryTab
import com.github.eprendre.tingshu.widget.GlideApp
import com.github.eprendre.tingshu.widget.MySuggestionProvider
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), AnkoLogger {
    var isBound = false
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var myService: TingShuService
    private val headerView by lazy { nav_view.getHeaderView(0) }
    private lateinit var categoryPagerAdapter: CategoryPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        volumeControlStream = AudioManager.STREAM_MUSIC

        updateTitle()
        initViews()

        val intent = Intent(this, TingShuService::class.java)
        startService(intent)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
        checkUpdate()
    }

    private fun initViews() {
        nav_view.setCheckedItem(R.id.nav_home)
        initCategoryAdapter(TingShuSourceHandler.getMainCategories())
        tabs.setupWithViewPager(view_pager)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    initCategoryAdapter(TingShuSourceHandler.getMainCategories())
                }
                R.id.nav_other -> {
                    initCategoryAdapter(TingShuSourceHandler.getOtherCategories())
                }
                R.id.nav_favorite -> {
                    startActivity<FavoriteActivity>()
                }
                R.id.nav_settings -> {
                    startActivity<SettingsActivity>()
                }
                R.id.nav_clear_history -> {
                    SearchRecentSuggestions(this, MySuggestionProvider.AUTHORITY, MySuggestionProvider.MODE)
                        .clearHistory()
                }
                R.id.nav_update -> {
                    if (System.currentTimeMillis() - Prefs.lastUpdate <
                        TimeUnit.MILLISECONDS.convert(1L, TimeUnit.MINUTES)
                    ) {
                        toast("检查更新不能太频繁")
                        return@setNavigationItemSelectedListener true
                    }
                    Prefs.lastUpdate = System.currentTimeMillis()
                    checkUpdate(true)
                }
                R.id.nav_about -> {
                    val message = "当前版本: ${BuildConfig.VERSION_NAME}\n\n" +
                            "部分资源来自:\nhttps://unsplash.com\n\n" +
                            "查看源码:\nhttps://github.com/eprendre/tingshu"
                    val s = SpannableString(message)
                    Linkify.addLinks(s, Linkify.ALL)
                    val dialog = AlertDialog.Builder(this)
                        .setTitle("关于")
                        .setMessage(s)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    dialog.findViewById<TextView>(android.R.id.message)?.movementMethod =
                        LinkMovementMethod.getInstance()
                }
            }
            drawer_layout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }

        fab.setOnClickListener {
            if (mediaController.playbackState.state == PlaybackStateCompat.STATE_NONE) {
                startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to Prefs.currentBookUrl)
            } else {
                startActivity<PlayerActivity>()
            }
        }
    }

    private fun updatePlayerInfo() {
        if (Prefs.currentBookUrl.isNullOrBlank()) {
            headerView.container.setOnClickListener(null)
            fab.hide()
        } else {
            GlideApp.with(this).load(Prefs.currentCover).into(headerView.cover_image)
            headerView.book_name_text.text = Prefs.currentBookName
            headerView.author_text.text = Prefs.author
            headerView.artist_text.text = Prefs.artist
            headerView.episode_text.text = Prefs.currentEpisodeName
            headerView.container.setOnClickListener {
                drawer_layout.closeDrawer(GravityCompat.START)
                Handler().postDelayed({
                    if (mediaController.playbackState.state == PlaybackStateCompat.STATE_NONE) {
                        startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to Prefs.currentBookUrl)
                    } else {
                        startActivity<PlayerActivity>()
                    }
                }, 250)
            }
            fab.show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isBound) {
            updatePlayerInfo()
        }
        when (nav_view.checkedItem!!.itemId) {
            R.id.nav_home -> {
                if (categoryPagerAdapter.categories != TingShuSourceHandler.getMainCategories()) {
                    initCategoryAdapter(TingShuSourceHandler.getMainCategories())
                    updateTitle()
                }
            }
            R.id.nav_other -> {
                if (categoryPagerAdapter.categories != TingShuSourceHandler.getOtherCategories()) {
                    initCategoryAdapter(TingShuSourceHandler.getOtherCategories())
                    updateTitle()
                }
            }
        }
    }

    private fun initCategoryAdapter(categories: List<CategoryTab>) {
        categoryPagerAdapter = CategoryPagerAdapter(this, supportFragmentManager)
        categoryPagerAdapter.categories = categories
        view_pager.adapter = categoryPagerAdapter
    }

    /**
     * 检查是否有更新
     */
    private fun checkUpdate(showResult: Boolean = false) {
        Fuel.get("https://api.github.com/repos/eprendre/tingshu/releases/latest")
            .responseJson { request, response, result ->
                runOnUiThread {
                    try {
                        val limit = response.header("X-RateLimit-Remaining").first().toInt()//有些设备会报错
                        if (limit == 0 && showResult) {
                            toast("请求更新太频繁了，请稍后再试")
                            return@runOnUiThread
                        }
                        when (result) {
                            is Result.Failure -> {
                                val ex = result.getException()
                                ex.printStackTrace()
                            }
                            is Result.Success -> {
                                val data = result.get().obj()
                                val tagName = data.getString("tag_name")
                                val versionName = "v${BuildConfig.VERSION_NAME}"
                                if (tagName != versionName) {
                                    val body = data.getString("body")
                                    val downloadUrl =
                                        data.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")
                                    AlertDialog.Builder(this)
                                        .setTitle("发现新版本: $tagName")
                                        .setMessage(body)
                                        .setPositiveButton("github 更新[国内较慢]") { _, _ ->
                                            val i = Intent(Intent.ACTION_VIEW)
                                            i.data = Uri.parse(downloadUrl)
                                            startActivity(i)
                                        }
                                        .setNegativeButton("蓝奏云") { _, _ ->
                                            val i = Intent(Intent.ACTION_VIEW)
                                            i.data = Uri.parse("https://www.lanzous.com/b873905")
                                            startActivity(i)
                                        }
                                        .setNeutralButton("取消", null)
                                        .show()
                                } else {
                                    if (showResult) {
                                        toast("没有更新")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (showResult) {
                            toast("检查更新出错")
                        }
                        e.printStackTrace()
                    }
                }
            }
    }

    private fun updateTitle() {
        val sources = resources.getStringArray(R.array.source_entries)
        val index = resources.getStringArray(R.array.source_values).indexOfFirst { it == Prefs.source }
        if (index != -1) {
            supportActionBar?.title = "${getString(R.string.app_name)} - ${sources[index]}"
        }
    }

    private val myConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as TingShuService.MyLocalBinder
            myService = binder.getService()
            isBound = true
            mediaController = MediaControllerCompat(this@MainActivity, myService.mediaSession.sessionToken)
            updatePlayerInfo()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> {
                startActivity<SearchActivity>()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(myConnection)
    }
}
