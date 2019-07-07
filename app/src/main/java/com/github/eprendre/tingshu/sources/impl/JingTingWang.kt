package com.github.eprendre.tingshu.sources.impl

import android.graphics.BitmapFactory
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
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
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.jsoup.Jsoup
import java.net.URLEncoder

object JingTingWang : TingShu {
    private lateinit var extractor: JingTingWangAudioUrlExtractor

    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val encodedKeywords = URLEncoder.encode(keywords, "utf-8")
            val url = "http://m.audio699.com/search?keyword=$encodedKeywords"
            val doc = Jsoup.connect(url).get()
            val totalPage = 1
            val list = ArrayList<Book>()
            val elementList = doc.select(".category .clist a")
            elementList.forEach { element ->
                val bookUrl = element.attr("href")
                val coverUrl = element.selectFirst("img").attr("src")
                val title = element.selectFirst("h3").text()
                val (author, artist) = element.select("p").map { it.text() }.let {
                    Pair(it[0], it[1])
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

            val episodes = doc.select(".plist a").map {
                Episode(it.text(), it.attr("abs:href"))
            }
            App.playList = episodes
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(exoPlayer: ExoPlayer, dataSourceFactory: DataSource.Factory): AudioUrlExtractor {
        if (!JingTingWang::extractor.isInitialized) {
            extractor = JingTingWangAudioUrlExtractor(exoPlayer, dataSourceFactory)
        }
        return extractor
    }

    override fun getMainCategoryTabs(): List<CategoryTab> {
        return listOf(
            CategoryTab("科幻玄幻", "http://m.audio699.com/list/2_1.html"),
            CategoryTab("灵异推理", "http://m.audio699.com/list/1_1.html")
        )
    }

    override fun getOtherCategoryTabs(): List<CategoryTab> {
        return listOf(
            CategoryTab("都市言情", "http://m.audio699.com/list/3_1.html"),
            CategoryTab("穿越历史", "http://m.audio699.com/list/4_1.html"),
            CategoryTab("其他类型", "http://m.audio699.com/list/5_1.html")
        )
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).get()
            val currentPage = extractPage(url)

            val pageButtons = doc.select(".cpage > a")
            val totalPage = extractPage(pageButtons.last().attr("href"))
            println("$currentPage/$totalPage")
            val nextUrl = pageButtons[2].attr("href")
            println("nextUrl: $nextUrl")

            val list = ArrayList<Book>()
            val elementList = doc.select(".clist > a")
            elementList.forEach { element ->
                val bookUrl = element.attr("href")
                val coverUrl = element.selectFirst("img").attr("src")
                val title = element.selectFirst("h3").text()
                val (author, artist) = element.select("p").map { it.text() }.let {
                    Pair(it[0], it[1])
                }
                list.add(Book(coverUrl, bookUrl, title, author, artist))
            }

            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }

    private class JingTingWangAudioUrlExtractor(
        private val exoPlayer: ExoPlayer,
        private val dataSourceFactory: DataSource.Factory
    ) : AudioUrlExtractor {
        val compositeDisposable = CompositeDisposable()

        override fun extract(url: String) {
            compositeDisposable.clear()
            Single.fromCallable {
                val doc = Jsoup.connect(url).get()
                val audioUrl = doc.getElementsByTag("source")
                    .first()
                    .attr("src")
                return@fromCallable audioUrl
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = { audioUrl ->
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
                }, onError = {
                    RxBus.post(RxEvent.ParsingPlayUrlErrorEvent())
                }).addTo(compositeDisposable)
        }
    }

    private fun extractPage(url: String): Int {
        return "http://m.audio699.com/list/\\d+_(\\d+).html".toRegex().find(url)!!.groupValues[1].toInt()
    }
}