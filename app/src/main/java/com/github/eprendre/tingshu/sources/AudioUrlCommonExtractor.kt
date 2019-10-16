package com.github.eprendre.tingshu.sources

import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.net.URL

/**
 * Jsoup 就可搞定的音频提取用此类
 */
object AudioUrlCommonExtractor : AudioUrlExtractor {
    private val compositeDisposable by lazy { CompositeDisposable() }
    private lateinit var parse: (Document) -> String

    fun setUp(parse: (Document) -> String) {
        this.parse = parse
    }

    override fun extract(url: String, autoPlay: Boolean, isCache: Boolean) {
        compositeDisposable.clear()
        Single.fromCallable {
            return@fromCallable parse(Jsoup.connect(url).get())
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { u ->
                val audioUrl = if (u.contains("%")) {
                    u
                } else {
                    val url1 = URL(u)
                    val uri = URI(url1.protocol, url1.userInfo, url1.host, url1.port, url1.path, url1.query, url1.ref)
                    uri.toASCIIString()//若音频地址含中文会导致某些设备播放失败
                }
                if (isCache) {
                    RxBus.post(RxEvent.CacheEvent(url, audioUrl, 0))
                } else {
                    Prefs.currentAudioUrl = audioUrl
                    if (autoPlay) {
                        RxBus.post(RxEvent.ParsingPlayUrlEvent(3))
                    } else {
                        RxBus.post(RxEvent.ParsingPlayUrlEvent(1))
                    }
                }
            }, onError = {
                RxBus.post(RxEvent.ParsingPlayUrlEvent(2))
            }).addTo(compositeDisposable)
    }
}