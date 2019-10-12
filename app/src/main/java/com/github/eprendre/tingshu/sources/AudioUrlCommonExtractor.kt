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

/**
 * Jsoup 就可搞定的音频提取用此类
 */
object AudioUrlCommonExtractor : AudioUrlExtractor {
    private val compositeDisposable by lazy { CompositeDisposable() }
    private lateinit var parse: (Document) -> String

    fun setUp(parse: (Document) -> String) {
        this.parse = parse
    }

    override fun extract(url: String, autoPlay: Boolean) {
        compositeDisposable.clear()
        Single.fromCallable {
            return@fromCallable parse(Jsoup.connect(url).get())
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { audioUrl ->
                Prefs.currentAudioUrl = audioUrl
                if (autoPlay) {
                    RxBus.post(RxEvent.ParsingPlayUrlEvent(3))
                } else {
                    RxBus.post(RxEvent.ParsingPlayUrlEvent(1))
                }

            }, onError = {
                RxBus.post(RxEvent.ParsingPlayUrlEvent(2))
            }).addTo(compositeDisposable)
    }
}