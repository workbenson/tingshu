package com.github.eprendre.tingshu.sources.impl

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Handler
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.webkit.*
import androidx.core.text.isDigitsOnly
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.extensions.*
import com.github.eprendre.tingshu.sources.AudioUrlExtractor
import com.github.eprendre.tingshu.sources.TingShu
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.utils.*
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object M520TingShu : TingShu {
    private lateinit var extractor: M520AudioUrlExtractor

    override fun getMainCategoryTabs(): List<CategoryTab> {
        return listOf(
            CategoryTab("玄幻奇幻", "http://m.520tingshu.com/list/?1.html"),
            CategoryTab("修真武侠", "http://m.520tingshu.com/list/?2.html"),
            CategoryTab("恐怖灵异", "http://m.520tingshu.com/list/?3.html"),
            CategoryTab("都市言情", "http://m.520tingshu.com/list/?4.html"),
            CategoryTab("穿越有声", "http://m.520tingshu.com/list/?43.html"),
            CategoryTab("网游小说", "http://m.520tingshu.com/list/?6.html")
        )
    }

    override fun getOtherCategoryTabs(): List<CategoryTab> {
        return listOf(
            CategoryTab("评书大全", "http://m.520tingshu.com/list/?8.html"),
            CategoryTab("粤语古仔", "http://m.520tingshu.com/list/?5.html"),
            CategoryTab("百家讲坛", "http://m.520tingshu.com/list/?9.html"),
            CategoryTab("历史纪实", "http://m.520tingshu.com/list/?11.html"),
            CategoryTab("军事", "http://m.520tingshu.com/list/?13.html"),
            CategoryTab("推理", "http://m.520tingshu.com/list/?46.html"),
            CategoryTab("儿童", "http://m.520tingshu.com/list/?29.html"),
            CategoryTab("广播剧", "http://m.520tingshu.com/list/?10.html"),
            CategoryTab("官场商战", "http://m.520tingshu.com/list/?47.html"),
            CategoryTab("相声小说", "http://m.520tingshu.com/list/?44.html"),
            CategoryTab("ebc5系列", "http://m.520tingshu.com/list/?48.html"),
            CategoryTab("通俗文学", "http://m.520tingshu.com/list/?12.html")
        )
    }

    override fun getAudioUrlExtractor(exoPlayer: ExoPlayer, dataSourceFactory: DataSource.Factory): AudioUrlExtractor {
        if (!M520TingShu::extractor.isInitialized) {
            extractor = M520AudioUrlExtractor(exoPlayer, dataSourceFactory)
        }
        return extractor
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            val doc = Jsoup.connect(bookUrl).get()
//            val cover = doc.selectFirst("#wrap .vod .vodbox img").attr("src")

            TingShuSourceHandler.downloadCoverForNotification()

//            val vodmain = doc.selectFirst("#wrap .vod .vodbox .vodmain")
//            Prefs.currentBookName = vodmain.selectFirst(".title").text()

//            vodmain.select(".actor").let {
//                Prefs.author = it[0].text()
//                Prefs.artist = it[1].ownText()
//            }

            val episodes = doc.select(".vodlist .sdn li a").map {
                Episode(it.text(), it.attr("abs:href"))
            }

            App.playList = episodes
            return@fromCallable null
        }
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val list = ArrayList<Book>()
            val doc = Jsoup.connect(url).get()
            val pages = doc.select(".main .page a")
            val totalPage = pages.last { it.text().isDigitsOnly() }.text().toInt()
            val currentPage = doc.selectFirst(".main .page span").text().toInt()
            val nextUrl = pages[pages.size - 2].attr("abs:href")

            val elementList = doc.select(".main .lb_zk li")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst("a .L1 img").attr("src")
                val bookUrl = element.selectFirst("a").attr("abs:href")
                val (title, author, artist) = element.select("a .R1 p").let { row ->
                    Triple(row[0].text(), row[1].text(), row[2].text())
                }
                list.add(Book(coverUrl, bookUrl, title, author, artist))
            }

            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }

    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val url =
                "http://m.520tingshu.com/search.asp?searchword=${URLEncoder.encode(keywords, "gb2312")}&page=$page"
            val list = ArrayList<Book>()
            val doc = Jsoup.connect(url).get()
            val totalPage = doc.select(".main .page a").last().attr("href").let {
                it.split("&")[0].split("=")[1].toInt()
            }

            val elementList = doc.select(".main .lb_zk li")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst("a .L1 img").attr("src")
                val bookUrl = element.selectFirst("a").attr("abs:href")
                val (title, author, artist) = element.select("a .R1 p").let { row ->
                    Triple(row[0].text(), row[1].text(), row[2].text())
                }
                list.add(Book(coverUrl, bookUrl, title, author, artist))
            }
            return@fromCallable Pair(list, totalPage)
        }
    }

    private class M520AudioUrlExtractor(
        private val exoPlayer: ExoPlayer,
        private val dataSourceFactory: DataSource.Factory
    ) : AudioUrlExtractor {
        private val compositeDisposable by lazy { CompositeDisposable() }
        private val webView by lazy { WebView(App.appContext) }
        private var isPageFinished = false
        private var isAudioGet = false
        private var isError = false

        init {
            //jsoup 只能解析静态页面，使用 webview 可以省不少力气
            webView.settings.javaScriptEnabled = true
            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    //有时候页面某些元素卡加载进度 onPageFinished 永远也不会调用便超时了
                    //但这个时候实际播放地址早就出来了，所以获取播放地址的方法放到这边
                    if (newProgress > 60 && !isPageFinished) {
                        isPageFinished = true
                        tryGetAudioSrc()
                    }
                }
            }
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
//                    if (currentUrl == url && !isPageFinished) {
//                        isPageFinished = true
//                        tryGetAudioSrc()
//                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    when (errorCode) {
                        ERROR_TIMEOUT, ERROR_HOST_LOOKUP -> {
                            compositeDisposable.clear()
                            postError()
                        }
                    }
                }

                @SuppressLint("NewApi")
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    onReceivedError(view, error.errorCode, error.description.toString(), request.url.toString())
                }
            }
        }

        private fun postError() {
            if (!isAudioGet) {
                isError = true
                RxBus.post(RxEvent.ParsingPlayUrlErrorEvent())
                webView.loadUrl("about:blank")
            }
        }

        override fun extract(url: String) {
            compositeDisposable.clear()
            isAudioGet = false
            isPageFinished = false
            isError = false
            webView.loadUrl(url)
            Completable.timer(12, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe {
                    postError()
                }
                .addTo(compositeDisposable)
        }

        /**
         * 页面加载结束的时候 jp_audio_0 里面的音频地址不会马上被塞进去，需要等页面的ajax回调结束才有。
         * 这里进行反复读取直到拿到音频的播放地址
         */
        @Synchronized
        private fun tryGetAudioSrc() {
            if (isAudioGet || isError) {
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
                compositeDisposable.clear()
                isAudioGet = true
                val bookname = Prefs.currentEpisodeName + " - " + Prefs.currentBookName

                var art = App.coverBitmap
                if (art == null) {
                    art = BitmapFactory.decodeResource(App.appContext.resources, R.drawable.ic_notification)
                }

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
                webView.loadUrl("about:blank")
            }
        }
    }

}
