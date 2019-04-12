package com.github.eprendre.tingshu

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
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.github.eprendre.tingshu.utils.Prefs
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {
    var isBound = false
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var myService: TingShuService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        volumeControlStream = AudioManager.STREAM_MUSIC

        val intent = Intent(this, TingShuService::class.java)
        startService(intent)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)

        if (!Prefs.currentBookUrl.isNullOrEmpty()) {
            edittext.setText(Prefs.currentBookUrl)
        }

        fetch_button.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://m.ting56.com/"))
            startActivity(browserIntent)
        }
        gotobutton.setOnClickListener {
            if (edittext.text.isBlank()) {
                toast("请输入正确的网址")
                return@setOnClickListener
            }
            if (!edittext.text.startsWith("http://m.ting56.com/mp3")) {
                toast("请输入正确的网址")
                return@setOnClickListener
            }
            val bookurl = edittext.text.toString()
            Prefs.currentBookUrl = bookurl
            startActivity<PlayerActivity>("bookurl" to bookurl)
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
                    startActivity<PlayerActivity>("bookurl" to Prefs.currentBookUrl)
                } else {
                    startActivity<PlayerActivity>()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(myConnection)
    }
}
