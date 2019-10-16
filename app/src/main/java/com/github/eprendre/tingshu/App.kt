package com.github.eprendre.tingshu

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import androidx.room.EmptyResultSetException
import com.github.eprendre.tingshu.db.AppDatabase
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import com.tencent.bugly.crashreport.CrashReport
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.text.Collator
import java.util.*
import kotlin.Comparator

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
        fun currentEpisodeIndex(): Int {
            var index = Prefs.playList.indexOfFirst { it.url == Prefs.currentBook!!.currentEpisodeUrl }
            if (index < 0) {
                index = 0
            }
            return index
        }
        val sourceValues: Array<String> by lazy { appContext.resources.getStringArray(R.array.source_values) }
        val sourceEntries: Array<String> by lazy { appContext.resources.getStringArray(R.array.source_entries) }

        fun getSortedSources(): List<Pair<String, String>> {
            var values = Prefs.selectedSources?.toList()
            if (values == null) {
                values = sourceValues.toList()
            }
            val entries = values.map { getSourceTitle(it) }.toTypedArray()
            val pairs = entries.zip(values).sortedWith(Comparator { o1: Pair<String, String>, o2: Pair<String, String> ->
                return@Comparator Collator.getInstance(Locale.CHINA).compare(o1.first, o2.first)
            })
            return pairs
        }

        @SuppressLint("CheckResult")
        fun findBookInHistoryOrFav(book: Book, f: (book:Book) -> Unit) {
            //从历史找书
            val historyBook = Prefs.historyList.firstOrNull { it.bookUrl == book.bookUrl }
            if (historyBook != null) {
                f(historyBook)
                return
            }

            //从收藏找书
            AppDatabase.getInstance(appContext)
                .bookDao()
                .findByBookUrl(book.bookUrl)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = {
                    f(it)
                }, onError = {
                    if (it is EmptyResultSetException) {
                        f(book)
                    } else {
                        it.printStackTrace()
                    }
                })
        }

        fun getSourceTitle(url: String): String {
            val index = sourceValues.indexOfFirst { url.startsWith(it) }
            return if (index > -1) {
                sourceEntries[index]
            } else {
                ""
            }
        }
    }
}