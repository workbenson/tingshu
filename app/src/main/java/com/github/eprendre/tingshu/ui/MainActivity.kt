package com.github.eprendre.tingshu.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.TingShuService
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.GlideApp
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import org.jetbrains.anko.startActivity

class MainActivity : AppCompatActivity() {
    var isBound = false
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var myService: TingShuService
    private val headerView by lazy { nav_view.getHeaderView(0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setCheckedItem(R.id.nav_home)
        nav_view.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {

                }
                R.id.nav_settings -> {
                    startActivity<SettingsActivity>()
                }
            }
            drawer_layout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }

        volumeControlStream = AudioManager.STREAM_MUSIC

        val intent = Intent(this, TingShuService::class.java)
        startService(intent)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
    }

    private fun initViews() {
        if (Prefs.currentBookUrl.isNullOrBlank()) {
            headerView.container.setOnClickListener(null)
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
        }
    }

    override fun onResume() {
        super.onResume()
        if (isBound) {
            initViews()
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
            invalidateOptionsMenu()
            initViews()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.player_menu, menu)
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
