package com.github.eprendre.tingshu.sources.impl

import android.view.View
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.AudioUrlExtractor
import com.github.eprendre.tingshu.sources.AudioUrlWebViewExtractor
import com.github.eprendre.tingshu.sources.TingShu
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.utils.*
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Completable
import io.reactivex.Single
import org.jsoup.Jsoup
import java.lang.Exception
import java.net.URLEncoder

object LianTingWang : TingShu {
    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val encodedKeywords = URLEncoder.encode(keywords, "utf-8")
            val url = "https://ting55.com/search/$encodedKeywords/page/$page"
            val doc = Jsoup.connect(url).get()

            val pages = doc.selectFirst(".c-page").children()
            var totalPage = 1
            if (pages.size > 0) {
                totalPage = when (val lastPage = pages.last().ownText()) {
                    "下一页" -> pages[pages.size - 2].ownText().toInt()
                    "末页" -> pages[pages.size - 3].ownText().toInt()
                    else -> lastPage.toInt()
                }
            }
            val list = ArrayList<Book>()
            try {
                val elementList = doc.select(".category-list > ul > li")
                elementList.forEach { element ->
                    val coverUrl = element.selectFirst(".img > a > img").absUrl("src")
                    val bookUrl = element.selectFirst(".info > h4 > a").absUrl("href")
                    val title = element.selectFirst(".info > h4 > a").text()
                    val infos = element.select(".info > p")
                    val author = infos[0].text()
                    val artist = infos[1].text()
                    val intro = infos[2].text()
                    val status = infos[3].text()
                    list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                        this.status = status
                        this.intro = intro
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@fromCallable Pair(list, totalPage)
        }
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            val doc = Jsoup.connect(bookUrl).get()
            TingShuSourceHandler.downloadCoverForNotification()

            val episodes = doc.select(".playlist > .plist > ul > li > a").map {
                Episode(it.text(), it.attr("abs:href")).apply {
                    this.isFree = it.hasClass("free")
                }
            }
            val intro = doc.selectFirst(".intro").text()

            Prefs.playList = episodes
            Prefs.currentIntro = intro
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(
        exoPlayer: ExoPlayer,
        dataSourceFactory: DataSource.Factory
    ): AudioUrlExtractor {
        AudioUrlWebViewExtractor.setUp(exoPlayer, dataSourceFactory, isDeskTop = true) { str ->
            val doc = Jsoup.parse(str)
            val audioElement = doc.getElementById("jp_audio_0")
            return@setUp audioElement?.attr("src")
        }
        return AudioUrlWebViewExtractor
    }

    override fun getCategoryMenus(): List<CategoryMenu> {
        val menu1 = CategoryMenu(
            "有声小说", R.drawable.ic_library_books, View.generateViewId(), listOf(
                CategoryTab("推荐", "https://ting55.com/tuijian"),
                CategoryTab("玄幻", "https://ting55.com/category/1"),
                CategoryTab("武侠", "https://ting55.com/category/2"),
                CategoryTab("都市", "https://ting55.com/category/3"),
                CategoryTab("言情", "https://ting55.com/category/4"),
                CategoryTab("穿越", "https://ting55.com/category/5"),
                CategoryTab("科幻", "https://ting55.com/category/6"),
                CategoryTab("推理", "https://ting55.com/category/7"),
                CategoryTab("恐怖", "https://ting55.com/category/8"),
                CategoryTab("惊悚", "https://ting55.com/category/9")
            )
        )
        val menu2 = CategoryMenu(
            "其它", R.drawable.ic_more_horiz, View.generateViewId(), listOf(
                CategoryTab("历史", "https://ting55.com/category/10"),
                CategoryTab("经典", "https://ting55.com/category/11"),
                CategoryTab("相声", "https://ting55.com/category/12"),
                CategoryTab("评书", "https://ting55.com/category/14"),
                CategoryTab("百家讲坛", "https://ting55.com/category/13")
            )
        )
        return listOf(menu1, menu2)
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).get()
            val cPage = doc.selectFirst(".c-page")
            val currentPage = cPage.selectFirst("span").text().toInt()

            val pages = cPage.children()
            val nextUrl = pages.firstOrNull { it.ownText() == "下一页" }?.absUrl("href") ?: ""
            val totalPage = when (val lastPage = pages.last().ownText()) {
                "下一页" -> pages[pages.size - 2].ownText().toInt()
                "末页" -> pages[pages.size - 3].ownText().toInt()
                else -> lastPage.toInt()
            }

            val list = ArrayList<Book>()
            val elementList = doc.select(".category-list > ul > li")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst(".img > a > img").absUrl("src")
                val bookUrl = element.selectFirst(".info > h4 > a").absUrl("href")
                val title = element.selectFirst(".info > h4 > a").text()
                val infos = element.select(".info > p")
                val author = infos[1].text()
                val artist = infos[2].text()
                val intro = infos[3].text()
                val status = infos[4].text()
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                    this.status = status
                    this.intro = intro
                })
            }
            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }

}