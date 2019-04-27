package com.github.eprendre.tingshu.sources

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.extensions.*
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DataSource
import org.apache.commons.lang3.StringEscapeUtils
import org.jsoup.Jsoup

/**
 * 解析56听书 http://m.ting56.com/
 */
class WLPlaybackPreparer(
    private val exoPlayer: ExoPlayer,
    private val dataSourceFactory: DataSource.Factory
) : MediaSessionConnector.PlaybackPreparer {
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
            val bookname = doc.getElementsByClass("bookname").first().text().replace("在线收听", "")

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

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
    }

    override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) {
    }

    override fun getSupportedPrepareActions(): Long {
        return PlaybackStateCompat.ACTION_PLAY_FROM_URI
    }

    override fun getCommands(): Array<String>? {
        return null
    }

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
        if (uri == null) {
            return
        }
        isAudioGet = false
        isPageFinished = false
        Prefs.currentEpisodeUrl = uri.toString()
        Prefs.currentEpisodeName = App.currentEpisode().title
        RxBus.post(RxEvent.ParsingPlayUrlEvent())
        webView.loadUrl(uri.toString())
    }

    override fun onPrepare() {
    }
}