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

object M520TingShu : TingShu {
    private lateinit var extractor: M520AudioUrlExtractor

    override fun getAudioUrlExtractor(exoPlayer: ExoPlayer, dataSourceFactory: DataSource.Factory): AudioUrlExtractor {
        if (!::extractor.isInitialized) {
            extractor = M520AudioUrlExtractor(exoPlayer, dataSourceFactory)
        }
        return extractor
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            val doc = Jsoup.connect(bookUrl).get()
//            val cover = doc.selectFirst("#wrap .vod .vodbox img").attr("src")

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
                val intro = ""//520tingshu 没有简介
                list.add(Book(coverUrl, bookUrl, title, author, artist, intro))
            }
            return@fromCallable Pair(list, totalPage)
        }
    }

    private class M520AudioUrlExtractor(
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
            }
        }
    }

}
