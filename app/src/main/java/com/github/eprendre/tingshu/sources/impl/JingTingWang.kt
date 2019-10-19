package com.github.eprendre.tingshu.sources.impl

import android.view.View
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.AudioUrlCommonExtractor
import com.github.eprendre.tingshu.sources.AudioUrlExtractor
import com.github.eprendre.tingshu.sources.TingShu
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.utils.*
import io.reactivex.Completable
import io.reactivex.Single
import org.jsoup.Jsoup
import java.net.URLEncoder

object JingTingWang : TingShu {

    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val encodedKeywords = URLEncoder.encode(keywords, "utf-8")
            val url = "http://m.audio699.com/search?keyword=$encodedKeywords"
            val doc = Jsoup.connect(url).userAgent(App.userAgent).get()
            val totalPage = 1
            val list = ArrayList<Book>()
            val elementList = doc.select(".category .clist a")
            elementList.forEach { element ->
                val bookUrl = element.attr("href")
                val coverUrl = element.selectFirst("img").attr("src")
                val title = element.selectFirst("h3").text()
                val (author, artist, status) = element.select("p").map { it.text() }.let {
                    Triple(it[0], it[1], it[2])
                }
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.status = status })
            }

            return@fromCallable Pair(list, totalPage)
        }
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            val doc = Jsoup.connect(bookUrl).userAgent(App.userAgent).get()
            TingShuSourceHandler.downloadCoverForNotification()

            val episodes = doc.select(".plist a").map {
                Episode(it.text(), it.attr("abs:href"))
            }
            Prefs.playList = episodes
            Prefs.currentIntro = doc.selectFirst(".intro").text()
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(): AudioUrlExtractor {
       AudioUrlCommonExtractor.setUp { doc ->
           return@setUp doc.getElementsByTag("source")
               ?.first()
               ?.attr("src") ?: ""
       }
        return AudioUrlCommonExtractor
    }

    override fun getCategoryMenus(): List<CategoryMenu> {
        val menu1 = CategoryMenu(
            "小说", R.drawable.ic_library_books, View.generateViewId(), listOf(
                CategoryTab("科幻玄幻", "http://m.audio699.com/list/2_1.html"),
                CategoryTab("灵异推理", "http://m.audio699.com/list/1_1.html")
            )
        )
        val menu2 = CategoryMenu(
            "其它", R.drawable.ic_more_horiz, View.generateViewId(), listOf(
                CategoryTab("都市言情", "http://m.audio699.com/list/3_1.html"),
                CategoryTab("穿越历史", "http://m.audio699.com/list/4_1.html"),
                CategoryTab("其他类型", "http://m.audio699.com/list/5_1.html")
            )
        )
        return listOf(menu1, menu2)
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).userAgent(App.userAgent).get()
            val currentPage = extractPage(url)

            val pageButtons = doc.select(".cpage > a")
            val totalPage = extractPage(pageButtons.last().attr("href"))
            val nextUrl = pageButtons[2].attr("href")

            val list = ArrayList<Book>()
            val elementList = doc.select(".clist > a")
            elementList.forEach { element ->
                val bookUrl = element.attr("href")
                val coverUrl = element.selectFirst("img").attr("src")
                val title = element.selectFirst("h3").text()
                val (author, artist, status) = element.select("p").map { it.text() }.let {
                    Triple(it[0], it[1], it[2])
                }
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.status = status })
            }

            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }

    private fun extractPage(url: String): Int {
        return "http://m.audio699.com/list/\\d+_(\\d+).html".toRegex().find(url)!!.groupValues[1].toInt()
    }
}