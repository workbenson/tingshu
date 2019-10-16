package com.github.eprendre.tingshu.sources

import android.annotation.SuppressLint
import android.os.Handler
import android.util.Log
import android.webkit.*
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import org.apache.commons.text.StringEscapeUtils
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * 某些网站的音频地址是异步加载的, Jsoup 搞不定，需要使用此类。
 */
object AudioUrlWebViewExtractor : AudioUrlExtractor {
    private val compositeDisposable by lazy { CompositeDisposable() }
    private val webView by lazy { WebView(App.appContext) }
    private var isPageFinished = false
    private var isAudioGet = false
    private var isAutoPlay = true
    private var isError = false
    private var userAgent = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0"
    private var script = ""
    private lateinit var parse: (String) -> String?
    private var isCache = false
    private var episodeUrl = ""

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
                    ERROR_TIMEOUT -> {
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

    fun setUp(isDeskTop: Boolean = false,
              script: String = "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
              parse: (String) -> String?) {
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
            RxBus.post(RxEvent.ParsingPlayUrlEvent(2))
            webView.loadUrl("about:blank")
        }
    }

    override fun extract(url: String, autoPlay: Boolean, isCache: Boolean) {
        compositeDisposable.clear()
        isAudioGet = false
        isPageFinished = false
        isError = false
        isAutoPlay = autoPlay
        episodeUrl = url
        this.isCache = isCache
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
            val url = parse(unescapedHtml)
            if (url.isNullOrBlank()) {
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
            val audioUrl = if (url.contains("%")) {
                url
            } else {
                val url1 = URL(url)
                val uri = URI(url1.protocol, url1.userInfo, url1.host, url1.port, url1.path, url1.query, url1.ref)
                uri.toASCIIString()//若音频地址含中文会导致某些设备播放失败
            }

            if (isCache) {
                RxBus.post(RxEvent.CacheEvent(episodeUrl, audioUrl, 0))
            } else {
                Prefs.currentAudioUrl = audioUrl
                if (isAutoPlay) {
                    RxBus.post(RxEvent.ParsingPlayUrlEvent(3))
                } else {
                    RxBus.post(RxEvent.ParsingPlayUrlEvent(1))
                }
            }

            webView.loadUrl("about:blank")
        }
    }

}