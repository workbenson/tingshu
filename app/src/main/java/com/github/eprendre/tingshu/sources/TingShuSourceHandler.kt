package com.github.eprendre.tingshu.sources

import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Completable
import io.reactivex.Single

/**
 * 里面有两种方法
 * 一种直接根据保存好的配置请求相应的站点
 * 另一种需要动态的根据传入url去判断用对应的站点解析
 */
object TingShuSourceHandler {
    const val SOURCE_URL_56 = "http://m.ting56.com"
    const val SOURCE_URL_520 = "http://m.520tingshu.com"
    private lateinit var tingShu: TingShu

    init {
        setupConfig()
    }

    fun setupConfig() {
        when (Prefs.source) {
            Prefs.SOURCE_56TINGSHU -> tingShu = M56TingShu
            Prefs.SOURCE_520TINGSHU -> tingShu = M520TingShu
        }
    }
    //以下直接从已设置好的站点去获取数据
    fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return tingShu.search(keywords, page)
    }


    //以下的方法需要根据传入的url判断用哪个站点解析
    fun getAudioUrlExtractor(
        url: String,
        exoPlayer: ExoPlayer,
        dataSourceFactory: DataSource.Factory
    ): AudioUrlExtractor? {
        return when {
            url.startsWith(SOURCE_URL_56) -> {
                M56TingShu.getAudioUrlExtractor(exoPlayer, dataSourceFactory)
            }
            url.startsWith(SOURCE_URL_520) -> {
                M520TingShu.getAudioUrlExtractor(exoPlayer, dataSourceFactory)
            }
            else -> null
        }
    }

    fun playFromBookUrl(bookUrl: String): Completable {
        return when {
            bookUrl.startsWith(SOURCE_URL_56) -> {
                M56TingShu.playFromBookUrl(bookUrl)
            }
            bookUrl.startsWith(SOURCE_URL_520) -> {
                M520TingShu.playFromBookUrl(bookUrl)
            }
            else -> M56TingShu.playFromBookUrl(bookUrl)
        }
    }
}