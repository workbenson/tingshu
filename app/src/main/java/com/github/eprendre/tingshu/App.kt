package com.github.eprendre.tingshu

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import com.github.eprendre.tingshu.utils.Episode
import com.github.eprendre.tingshu.utils.Prefs

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        Prefs.init()

    }

    companion object {
        lateinit var appContext: Context
        var playList: List<Episode> = emptyList()
        var coverBitmap: Bitmap? = null
        fun currentEpisodeIndex() = playList.indexOfFirst { it.url == Prefs.currentEpisodeUrl }
        fun currentEpisode() = playList.first { it.url == Prefs.currentEpisodeUrl }
    }
}