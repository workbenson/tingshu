package com.github.eprendre.tingshu.sources

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Handler
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.webkit.*
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.extensions.*
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

/**
 * 某些网站的音频地址是异步加载的, Jsoup 搞不定，需要使用此类。
 */
object AudioUrlWebViewExtractor : AudioUrlExtractor {
    private val compositeDisposable by lazy { CompositeDisposable() }
    private val webView by lazy { WebView(App.appContext) }
    private var isPageFinished = false
    private var isAudioGet = false
    private var isError = false
    private var userAgent = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0"
    private var script = ""
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var parse: (String) -> String?

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
                //56听书的页面没 redirect 但是 onPageFinished 会被多次调用
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

    fun setUp(exoPlayer: ExoPlayer,
              dataSourceFactory: DataSource.Factory,
              isDeskTop: Boolean = false,
              script: String = "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
              parse: (String) -> String?) {
        this.exoPlayer = exoPlayer
        this.dataSourceFactory = dataSourceFactory
        this.parse = parse
        if (isDeskTop) {
            webView.settings.userAgentString = userAgent
        } else {
            webView.settings.userAgentString = null
        }
        this.script = script
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
        webView.evaluateJavascript(script) { html ->
            val unescapedHtml = StringEscapeUtils.unescapeJava(html)//提取出来的html需要unescape
            val audioUrl = parse(unescapedHtml)
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

            val book = Prefs.currentBook!!
            val bookname = book.currentEpisodeName + " - " + book.title

            val metadata = MediaMetadataCompat.Builder()
                .apply {
                    title = bookname
                    artist = book.artist
                    mediaUri = audioUrl

                    displayTitle = bookname
                    displaySubtitle = book.artist
                    downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
                    if (Prefs.showAlbumInLockScreen) {
                        var art = App.coverBitmap
                        if (art == null) {
                            art = BitmapFactory.decodeResource(App.appContext.resources, R.drawable.ic_notification)
                        }
                        albumArt = art
                    }
                }
                .build()

            val source = metadata.toMediaSource(dataSourceFactory)
            exoPlayer.prepare(source)
            App.currentPosition {
                exoPlayer.seekTo(book.currentEpisodePosition)
            }
            webView.loadUrl("about:blank")
        }
    }

}