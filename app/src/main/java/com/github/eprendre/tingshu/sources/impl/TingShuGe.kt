package com.github.eprendre.tingshu.sources.impl

import android.graphics.BitmapFactory
import android.os.Handler
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.extensions.*
import com.github.eprendre.tingshu.sources.AudioUrlExtractor
import com.github.eprendre.tingshu.sources.TingShu
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.utils.*
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Completable
import io.reactivex.Single
import org.apache.commons.lang3.StringEscapeUtils
import org.jsoup.Jsoup
import java.net.URLEncoder

object TingShuGe : TingShu {
    private lateinit var extractor: TingShuGeAudioUrlExtractor

    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val list = ArrayList<Book>()
            val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
            val url = "http://www.tingshuge.com/search.asp?page=$page&searchword=$encodedKeywords&searchtype=-1"
            val doc = Jsoup.connect(url).get()
            val pages = doc.selectFirst("#channelright .list_mod .pagesnums").select("li")
            val totalPage = pages[pages.size - 3].text().toInt()
            val elementList = doc.select("#channelright .list_mod .clist li")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst("a img").attr("abs:src")
                val bookUrl = element.selectFirst("a").attr("abs:href")
                val (title, author, artist) = element.select("p").let { row ->
                    Triple(row[0].text(), row[2].text(), row[3].text())
                }
                list.add(Book(coverUrl, bookUrl, title, author, artist))
            }
            return@fromCallable Pair(list, totalPage)
        }
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            val doc = Jsoup.connect(bookUrl).get()
            TingShuSourceHandler.downloadCoverForNotification()

            val episodes = doc.selectFirst(".numlist").select("li a").map {
                Episode(it.text(), it.attr("abs:href"))
            }

            App.playList = episodes
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(exoPlayer: ExoPlayer, dataSourceFactory: DataSource.Factory): AudioUrlExtractor {
        if (!TingShuGe::extractor.isInitialized) {
            extractor =
                TingShuGeAudioUrlExtractor(
                    exoPlayer,
                    dataSourceFactory
                )
        }
        return extractor
    }

    override fun getMainSectionTabs(): List<SectionTab> {
        return listOf(
            SectionTab("玄幻", "http://www.tingshuge.com/List/4.html"),
            SectionTab("武侠", "http://www.tingshuge.com/List/5.html"),
            SectionTab("仙侠", "http://www.tingshuge.com/List/73.html"),
            SectionTab("网游", "http://www.tingshuge.com/List/16.html"),
            SectionTab("科幻", "http://www.tingshuge.com/List/6.html"),
            SectionTab("推理", "http://www.tingshuge.com/List/7.html"),
            SectionTab("悬疑", "http://www.tingshuge.com/List/71.html"),
            SectionTab("恐怖", "http://www.tingshuge.com/List/8.html"),
            SectionTab("灵异", "http://www.tingshuge.com/List/9.html"),
            SectionTab("都市", "http://www.tingshuge.com/List/10.html"),
            SectionTab("穿越", "http://www.tingshuge.com/List/15.html"),
            SectionTab("言情", "http://www.tingshuge.com/List/11.html"),
            SectionTab("校园", "http://www.tingshuge.com/List/12.html")
        )
    }

    override fun getOtherSectionTabs(): List<SectionTab> {
        return listOf(
            SectionTab("历史", "http://www.tingshuge.com/List/13.html"),
            SectionTab("军事", "http://www.tingshuge.com/List/14.html"),
            SectionTab("官场", "http://www.tingshuge.com/List/17.html"),
            SectionTab("商战", "http://www.tingshuge.com/List/18.html"),
            SectionTab("儿童", "http://www.tingshuge.com/List/22.html"),
            SectionTab("戏曲", "http://www.tingshuge.com/List/25.html"),
            SectionTab("百家讲坛", "http://www.tingshuge.com/List/30.html"),
            SectionTab("人文", "http://www.tingshuge.com/List/21.html"),
            SectionTab("诗歌", "http://www.tingshuge.com/List/69.html"),
            SectionTab("相声", "http://www.tingshuge.com/List/24.html"),
            SectionTab("小品", "http://www.tingshuge.com/List/66.html"),
            SectionTab("励志", "http://www.tingshuge.com/List/28.html"),
            SectionTab("婚姻", "http://www.tingshuge.com/List/72.html"),
            SectionTab("养生", "http://www.tingshuge.com/List/29.html"),
            SectionTab("英语", "http://www.tingshuge.com/List/27.html"),
            SectionTab("教育", "http://www.tingshuge.com/List/70.html"),
            SectionTab("儿歌", "http://www.tingshuge.com/List/67.html"),
            SectionTab("笑话", "http://www.tingshuge.com/List/23.html"),
            SectionTab("佛学", "http://www.tingshuge.com/List/74.html"),
            SectionTab("广播剧", "http://www.tingshuge.com/List/68.html"),
            SectionTab("国学", "http://www.tingshuge.com/List/19.html"),
            SectionTab("名著", "http://www.tingshuge.com/List/20.html"),
            SectionTab("评书大全", "http://www.tingshuge.com/List/26.html")
        )
    }

    override fun getSectionDetail(url: String): Single<Section> {
        return Single.fromCallable {
            val list = ArrayList<Book>()
            val doc = Jsoup.connect(url).get()
            val container = doc.getElementById("channelleft")
            val pages = container.selectFirst(".list_mod .pagesnums").select("li")
            val totalPage = pages[pages.size - 3].text().toInt()
            val currentPage = container.getElementById("pagenow").text().toInt()
            val nextUrl = pages[pages.size - 2].selectFirst("a")?.attr("abs:href") ?: ""

            val elementList = container.select(".list_mod .clist li")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst("a img").attr("abs:src")
                val bookUrl = element.selectFirst("a").attr("abs:href")
                val (title, author, artist) = element.select("p").let { row ->
                    Triple(row[0].text(), row[2].text(), row[3].text())
                }
                list.add(Book(coverUrl, bookUrl, title, author, artist))
            }
            return@fromCallable Section(list, currentPage, totalPage, url, nextUrl)
        }
    }

    private class TingShuGeAudioUrlExtractor(
        private val exoPlayer: ExoPlayer,
        private val dataSourceFactory: DataSource.Factory
    ) : AudioUrlExtractor {
        private val webView by lazy { WebView(App.appContext) }
        private var isPageFinished = false
        private var isAudioGet = false

        init {
            //jsoup 只能解析静态页面，使用 webview 可以省不少力气
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!isPageFinished) {
                        isPageFinished = true
                        tryGetAudioSrc()
                    }
                }
            }
        }

        override fun extract(url: String) {
            isAudioGet = false
            isPageFinished = false
            webView.loadUrl(url)
        }

        @Synchronized
        private fun tryGetAudioSrc() {
            if (isAudioGet) {
                return
            }
            //提取webview的html
            webView.evaluateJavascript(
                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"
            ) { html ->
                val unescapedHtml = StringEscapeUtils.unescapeJava(html)//提取出来的html需要unescape

                //使用Jsoup解析html文本
                val doc = Jsoup.parse(unescapedHtml)

                val audioElement = doc.getElementById("jp_audio_0")
                val audioUrl = audioElement?.attr("src")
                if (audioUrl.isNullOrBlank()) {
                    Handler().postDelayed({
                        tryGetAudioSrc()
                    }, 500)
                    return@evaluateJavascript
                }
                if (isAudioGet) {
                    return@evaluateJavascript
                }
                isAudioGet = true

                var art = App.coverBitmap
                if (art == null) {
                    art = BitmapFactory.decodeResource(App.appContext.resources, R.drawable.ic_notification)
                }

                val bookname = Prefs.currentEpisodeName + " - " + Prefs.currentBookName

                val metadata = MediaMetadataCompat.Builder()
                    .apply {
                        title = bookname
                        artist = Prefs.artist
                        mediaUri = audioUrl

                        displayTitle = bookname
                        displaySubtitle = Prefs.artist
                        downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
                        albumArt = art
                    }
                    .build()

                val source = metadata.toMediaSource(dataSourceFactory)
                exoPlayer.prepare(source)
                if (Prefs.currentEpisodePosition > 0) {
                    exoPlayer.seekTo(Prefs.currentEpisodePosition)
                }
            }
        }
    }
}