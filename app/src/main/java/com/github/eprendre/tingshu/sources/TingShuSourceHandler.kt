package com.github.eprendre.tingshu.sources

import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.utils.Section
import com.github.eprendre.tingshu.utils.SectionTab
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
    private val sourceList by lazy {
        val array = App.appContext.resources.getStringArray(R.array.source_values)
        listOf(
            Pair(array[0], M56TingShu),
            Pair(array[1], M520TingShu)
        )
    }

    init {
        setupConfig()
    }

    fun setupConfig() {
        tingShu = findSource(Prefs.source)
    }

    //以下直接从已设置好的站点去获取数据
    fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return tingShu.search(keywords, page)
    }

    fun getMainSections(): List<SectionTab> {
        return tingShu.getMainSectionTabs()
    }

    fun getOtherSections(): List<SectionTab> {
        return tingShu.getOtherSectionTabs()
    }

    //以下的方法需要根据传入的url判断用哪个站点解析
    fun getSectionDetail(url: String): Single<Section> {
        return findSource(url).getSectionDetail(url)
    }

    fun getAudioUrlExtractor(
        url: String,
        exoPlayer: ExoPlayer,
        dataSourceFactory: DataSource.Factory
    ): AudioUrlExtractor {
        return findSource(url).getAudioUrlExtractor(exoPlayer, dataSourceFactory)
    }

    fun playFromBookUrl(bookUrl: String): Completable {
        return findSource(bookUrl).playFromBookUrl(bookUrl)
    }

    private fun findSource(url: String): TingShu {
        return sourceList
            .first { url.startsWith(it.first) }
            .second
    }
}