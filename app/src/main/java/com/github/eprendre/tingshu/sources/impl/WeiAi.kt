package com.github.eprendre.tingshu.sources.impl

import android.view.View
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.*
import com.github.eprendre.tingshu.utils.*
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Completable
import io.reactivex.Single
import org.jsoup.Jsoup
import java.lang.Exception
import java.net.URLEncoder

object WeiAi : TingShu {
    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val encodedKeywords = URLEncoder.encode(keywords, "utf-8")
            val url = "http://www.ting199.com/search?keyword=$encodedKeywords"
            val doc = Jsoup.connect(url).get()
            val totalPage = 1
            val list = ArrayList<Book>()
            val elementList = doc.select(".top_list > a")
            elementList.forEach { element ->
                val bookUrl = element.absUrl("href")
                val coverUrl = ""
                val title = element.ownText()
                val author = ""
                val artist = element.selectFirst("span").text()
                list.add(Book(coverUrl, bookUrl, title, author, artist))
            }
            return@fromCallable Pair(list, totalPage)
        }
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            val doc = Jsoup.connect(bookUrl).get()
            TingShuSourceHandler.downloadCoverForNotification()

            val episodes = doc.select("#playlist > ul > li > a").map {
                Episode(it.text(), it.absUrl("href"))
            }

            val intro = doc.selectFirst(".book_intro").text()

            Prefs.playList = episodes
            Prefs.currentIntro = intro
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(): AudioUrlExtractor {
        AudioUrlCommonExtractor.setUp { doc ->
            return@setUp doc.selectFirst("audio").attr("src")
        }
        return AudioUrlCommonExtractor
    }

    override fun getCategoryMenus(): List<CategoryMenu> {
        val menu1 = CategoryMenu(
            "首页", R.drawable.ic_home_black_24dp, View.generateViewId(), listOf(
                CategoryTab("首页", "http://www.ting199.com")
            )
        )
        return listOf(menu1)
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).get()
            val currentPage = 1
            val totalPage = 1

            val list = ArrayList<Book>()
            val elementList = doc.select(".top_list > a")
            elementList.forEach { element ->
                val bookUrl = element.absUrl("href")
                val coverUrl = ""
                val title = element.ownText()
                val author = ""
                val artist = element.selectFirst("span").text()
                list.add(Book(coverUrl, bookUrl, title, author, artist))
            }
            return@fromCallable Category(list, currentPage, totalPage, url, "")
        }
    }

    fun fetchBookInfo(book: Book): Completable {
        return Completable.fromCallable {
            val doc = Jsoup.connect(book.bookUrl).get()
            val coverUrl = doc.selectFirst(".booksite > .bookimg > img").absUrl("src")
            val infos = doc.selectFirst(".booksite > .bookinfo > .info").children()
            val artist = infos[0].text()
            val status = infos[1].text()
            val intro = doc.selectFirst(".book_intro").text()
            book.coverUrl = coverUrl
            book.artist = artist
            book.status = status
            book.intro = intro
            return@fromCallable null
        }
    }
}