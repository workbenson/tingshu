package com.github.eprendre.tingshu

import android.content.Context
import android.graphics.Bitmap
import androidx.multidex.MultiDexApplication
import com.github.eprendre.tingshu.utils.Episode
import com.github.eprendre.tingshu.utils.Prefs
import io.reactivex.plugins.RxJavaPlugins

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        Prefs.init()
        RxJavaPlugins.setErrorHandler { }
    }

    companion object {
        lateinit var appContext: Context
        var playList: List<Episode> = emptyList()
        var coverBitmap: Bitmap? = null
        var isRetry = true
        fun currentEpisodeIndex() = playList.indexOfFirst { it.url == Prefs.currentEpisodeUrl }
        fun currentEpisode() = playList.first { it.url == Prefs.currentEpisodeUrl }
    }
}