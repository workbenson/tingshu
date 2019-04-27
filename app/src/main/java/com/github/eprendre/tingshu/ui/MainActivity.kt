package com.github.eprendre.tingshu.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.TingShuService
import com.github.eprendre.tingshu.utils.Prefs
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity

class MainActivity : AppCompatActivity() {
    var isBound = false
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var myService: TingShuService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        volumeControlStream = AudioManager.STREAM_MUSIC
        last_play.visibility = View.GONE

        val intent = Intent(this, TingShuService::class.java)
        startService(intent)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
    }

    private fun initViews() {
        if (Prefs.currentBookUrl.isNullOrBlank()) {
            last_play.visibility = View.GONE
        } else {
            last_play.visibility = View.VISIBLE
            Glide.with(this).load(Prefs.currentCover).into(cover_image)
            book_name_text.text = Prefs.currentBookName
            author_text.text = Prefs.author
            artist_text.text = Prefs.artist
            episode_text.text = Prefs.currentEpisodeName
            last_play.setOnClickListener {
                if (mediaController.playbackState.state == PlaybackStateCompat.STATE_NONE) {
                    startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to Prefs.currentBookUrl)
                } else {
                    startActivity<PlayerActivity>()
                }
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.player).isVisible = !Prefs.currentBookUrl.isNullOrEmpty() && isBound
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.player_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.player -> {
                if (mediaController.playbackState.state == PlaybackStateCompat.STATE_NONE) {
                    startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to Prefs.currentBookUrl)
                } else {
                    startActivity<PlayerActivity>()
                }
                return true
            }
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
