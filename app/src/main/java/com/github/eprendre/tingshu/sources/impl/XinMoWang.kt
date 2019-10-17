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
import java.net.URLEncoder

object XinMoWang : TingShu {

    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val url = "http://m.ixinmo.com/search.html"
            val doc = Jsoup.connect(url).data("searchword", keywords).post()
            val totalPage = 1
            val list = ArrayList<Book>()
            val elementList = doc.select(".xxzx > .list-ov-tw")
            elementList.forEach { element ->
                val bookUrl = element.selectFirst(".list-ov-t > a").absUrl("href")
                val coverUrl = element.selectFirst(".list-ov-t > a > img").absUrl("src")
                val a = element.selectFirst(".list-ov-w > a")
                val title = a.selectFirst(".bt").text()
                val (author, artist) = a.select(".zz").let {
                    Pair(it[0].text(), it[1].text())
                }
                val intro = a.selectFirst(".nr").text()
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                    this.intro = intro
                })
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
           return@setUp doc.selectFirst("audio > source").attr("src")
       }
        return AudioUrlCommonExtractor
    }

    override fun getCategoryMenus(): List<CategoryMenu> {
        val menu1 = CategoryMenu(
            "首页", R.drawable.ic_home_black_24dp, View.generateViewId(), listOf(
                CategoryTab("恐怖", "http://m.ixinmo.com/mulu/1_1.html"),
                CategoryTab("科幻", "http://m.ixinmo.com/mulu/2_1.html"),
                CategoryTab("仙侠", "http://m.ixinmo.com/mulu/3_1.html"),
                CategoryTab("都市", "http://m.ixinmo.com/mulu/4_1.html"),
                CategoryTab("言情", "http://m.ixinmo.com/mulu/5_1.html"),
                CategoryTab("穿越", "http://m.ixinmo.com/mulu/6_1.html"),
                CategoryTab("古言", "http://m.ixinmo.com/mulu/7_1.html"),
                CategoryTab("官场", "http://m.ixinmo.com/mulu/8_1.html"),
                CategoryTab("其它", "http://m.ixinmo.com/mulu/9_1.html")
            )
        )
        return listOf(menu1)
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).get()
            val (currentPage, totalPage) = doc.selectFirst("#page_num1").text().split("/").let {
                Pair(it[0].toInt(), it[1].toInt())
            }
            val nextUrl = doc.selectFirst("#page_next1").absUrl("href")

            val list = ArrayList<Book>()
            val elementList = doc.select(".xsdz > .list-ov-tw")
            elementList.forEach { element ->
                val bookUrl = element.selectFirst(".list-ov-t > a").absUrl("href")
                val coverUrl = element.selectFirst(".list-ov-t > a > img").absUrl("src")
                val a = element.selectFirst(".list-ov-w > a")
                val title = a.selectFirst(".bt").text()
                val (author, artist) = a.select(".zz").let {
                    Pair(it[0].text(), it[1].text())
                }
                val intro = a.selectFirst(".nr").text()
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                    this.intro = intro
                })
            }

            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }
}