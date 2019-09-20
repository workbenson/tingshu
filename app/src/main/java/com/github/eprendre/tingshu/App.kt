package com.github.eprendre.tingshu

import android.content.Context
import android.graphics.Bitmap
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.github.eprendre.tingshu.utils.Prefs
import com.tencent.bugly.crashreport.CrashReport
import io.reactivex.plugins.RxJavaPlugins

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        Prefs.init()
        RxJavaPlugins.setErrorHandler { }
        CrashReport.initCrashReport(applicationContext, "1103deea28", true)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object {
        lateinit var appContext: Context
        var coverBitmap: Bitmap? = null
        var isRetry = true
        fun currentEpisodeIndex() = Prefs.playList.indexOfFirst { it.url == Prefs.currentEpisodeUrl }
        fun currentEpisode() = Prefs.playList.first { it.url == Prefs.currentEpisodeUrl }
    }
}