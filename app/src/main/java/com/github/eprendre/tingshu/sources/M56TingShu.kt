package com.github.eprendre.tingshu.sources

import android.graphics.BitmapFactory
import android.os.Handler
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.extensions.*
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.GlideApp
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Completable
import io.reactivex.Single
import org.apache.commons.lang3.StringEscapeUtils
import org.jsoup.Jsoup
import java.net.URLEncoder
import kotlin.contracts.Returns

object M56TingShu : TingShu {
    private lateinit var extractor: M56AudioUrlExtractor

    override fun getAudioUrlExtractor(exoPlayer: ExoPlayer, dataSourceFactory: DataSource.Factory): AudioUrlExtractor {
        if (!::extractor.isInitialized) {
            extractor = M56AudioUrlExtractor(exoPlayer, dataSourceFactory)
        }

        return extractor
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            val doc = Jsoup.connect(bookUrl).get()
//            val book = doc.getElementsByClass("list-ov-tw").first()
//            val cover = book.getElementsByTag("img").first().attr("src")
            //下载封面
            val glideOptions = RequestOptions()
                .fallback(R.drawable.default_art)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            App.coverBitmap = GlideApp.with(App.appContext)
                .applyDefaultRequestOptions(glideOptions)
                .asBitmap()
                .load(Prefs.currentCover)
                .submit(144, 144)
                .get()

            //获取书本信息
//            val bookInfos = book.getElementsByTag("span").map { it.text() }
//            Prefs.currentBookName = bookInfos[0]
//            Prefs.author = bookInfos[2]
//            Prefs.artist = bookInfos[3]

            //获取章节列表
            val episodes = doc.getElementById("playlist")
                .getElementsByTag("a")
                .map {
                    Episode(it.text(), it.attr("abs:href"))
                }
            App.playList = episodes
            return@fromCallable null
        }
    }

    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            //            var currentPage: Int
            var totalPage: Int
            val url = "http://m.ting56.com/search.asp?searchword=${URLEncoder.encode(keywords, "gb2312")}&page=$page"
            val list = ArrayList<Book>()
            val doc = Jsoup.connect(url).get()
            val container = doc.selectFirst(".xsdz")
            container.getElementById("page_num1").text().split("/").let {
                //                currentPage = it[0].toInt()
                totalPage = it[1].toInt()
            }
            val elementList = container.getElementsByClass("list-ov-tw")
            elementList.forEach { item ->
                val coverUrl = item.selectFirst(".list-ov-t a img").attr("original")
                val ov = item.selectFirst(".list-ov-w")
                val bookUrl = ov.selectFirst(".bt a").attr("abs:href")
                val title = ov.selectFirst(".bt a").text()
                val (author, artist) = ov.select(".zz").let { element ->
                    Pair(element[0].text(), element[1].text())
                }
                val intro = ov.selectFirst(".nr").text()
                list.add(Book(coverUrl, bookUrl, title, author, artist, intro))
            }
            return@fromCallable Pair(list, totalPage)
        }
    }

    private class M56AudioUrlExtractor(
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
                    //56听书的页面没 redirect 但是 onPageFinished 会被多次调用
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

        /**
         * 页面加载结束的时候 jp_audio_0 里面的音频地址不会马上被塞进去，需要等页面的ajax回调结束才有。
         * 这里进行反复读取直到拿到音频的播放地址
         */
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
