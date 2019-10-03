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
import java.net.URLEncoder

object TianTianPingShu : TingShu {
    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
            val url = "https://www.pingshu365.com/search/1.asp?page=$page&keyword=$encodedKeywords&stype="
            val doc = Jsoup.connect(url).get()
            val pages = doc.selectFirst(".fy").ownText().let { text ->
                Regex(".+页次:(\\d+)/(\\d+).+").find(text)!!.groupValues
            }
            val currentPage = pages[1].toInt()
            val totalPage = pages[2].toInt()

            val list = ArrayList<Book>()
            val elementList = doc.select("#ss .ssl .book")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst(".pic img").absUrl("src")
                val titleElement = element.selectFirst(".text p span a")
                val bookUrl = titleElement.absUrl("href")
                val title = titleElement.text()
                val lis = element.select(".text ul li")
                val author = lis[1].text()
                val artist = lis[0].text()
                val intro = lis.last().text()
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro })
            }
            return@fromCallable Pair(list, totalPage)
        }
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            TingShuSourceHandler.downloadCoverForNotification()
            val doc = Jsoup.connect(bookUrl).get()
            val episodes = doc.select("#playlist ul li").map {
                val a = it.selectFirst("a")
                Episode(a.text(), a.absUrl("href"))
            }
            Prefs.playList = episodes
            Prefs.currentIntro = doc.selectFirst("#ss .listb").text()
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(exoPlayer: ExoPlayer, dataSourceFactory: DataSource.Factory): AudioUrlExtractor {
        AudioUrlWebViewExtractor.setUp(exoPlayer, dataSourceFactory, true) { doc ->
            val audioElement = doc.getElementById("jp_audio_0")
            return@setUp audioElement?.attr("src")
        }
        return AudioUrlWebViewExtractor
    }

    override fun getCategoryMenus(): List<CategoryMenu> {
        val menu1 = CategoryMenu(
            "评书", R.drawable.ic_radio_black_24dp, View.generateViewId(), listOf(
                CategoryTab("单田芳", "https://www.pingshu365.com/list/293_1.html"),
                CategoryTab("袁阔成", "https://www.pingshu365.com/list/294_1.html"),
                CategoryTab("田连元", "https://www.pingshu365.com/list/295_1.html"),
                CategoryTab("刘兰芳", "https://www.pingshu365.com/list/297_1.html"),
                CategoryTab("王玥波", "https://www.pingshu365.com/list/296_1.html"),
                CategoryTab("连丽如", "https://www.pingshu365.com/list/298_1.html"),
                CategoryTab("张少佐", "https://www.pingshu365.com/list/299_1.html"),
                CategoryTab("其他", "https://www.pingshu365.com/list/302_1.html")
            )
        )
        val menu2 = CategoryMenu(
            "其它", R.drawable.ic_more_horiz, View.generateViewId(), listOf(
                CategoryTab("最新更新", "https://www.pingshu365.com/new/1.html"),
                CategoryTab("相声", "https://www.pingshu365.com/html/xs1.html"),
                CategoryTab("戏曲", "https://www.pingshu365.com/html/xq1.html"),
                CategoryTab("百家讲坛", "https://www.pingshu365.com/html/bj1.html")
            )
        )
        val menu3 = CategoryMenu(
            "排行榜", R.drawable.ic_looks_one_black_24dp, View.generateViewId(), listOf(
                CategoryTab("评书排行榜", "https://www.pingshu365.com/top/ps1.html"),
                CategoryTab("有声小说排行榜", "https://www.pingshu365.com/top/ys1.html"),
                CategoryTab("相声排行榜", "https://www.pingshu365.com/top/xs1.html"),
                CategoryTab("戏曲排行榜", "https://www.pingshu365.com/top/xq1.html"),
                CategoryTab("百家讲坛排行榜", "https://www.pingshu365.com/top/bj1.html")
            )
        )
        val menu4 = CategoryMenu(
            "小说", R.drawable.ic_library_books, View.generateViewId(), listOf(
                CategoryTab("仙侠玄幻", "https://www.pingshu365.com/list/306_1.html"),
                CategoryTab("恐怖悬疑", "https://www.pingshu365.com/list/304_1.html"),
                CategoryTab("都市言情", "https://www.pingshu365.com/list/308_1.html"),
                CategoryTab("盗墓探险", "https://www.pingshu365.com/list/305_1.html"),
                CategoryTab("传统武侠", "https://www.pingshu365.com/list/310_1.html"),
                CategoryTab("官场刑侦", "https://www.pingshu365.com/list/307_1.html"),
                CategoryTab("职场商战", "https://www.pingshu365.com/list/311_1.html"),
                CategoryTab("历史军事", "https://www.pingshu365.com/list/309_1.html"),
                CategoryTab("人物传记", "https://www.pingshu365.com/list/313_1.html"),
                CategoryTab("科学教育", "https://www.pingshu365.com/list/315_1.html"),
                CategoryTab("纪实文学", "https://www.pingshu365.com/list/316_1.html"),
                CategoryTab("经典文学", "https://www.pingshu365.com/list/314_1.html"),
                CategoryTab("儿童读物", "https://www.pingshu365.com/list/312_1.html"),
                CategoryTab("其他类别", "https://www.pingshu365.com/list/300_1.html")
            )
        )

        return listOf(menu1, menu2, menu3, menu4)
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).get()

            val nextUrl = doc.select("#ss .ssl .fy a").firstOrNull { it.ownText() == "下一页" }?.absUrl("href") ?: ""
            val pages = doc.selectFirst("#ss .ssl .fy").ownText().let { text ->
                Regex(".+(\\d+)/(\\d+).+").find(text)!!.groupValues
            }
            val currentPage = pages[1].toInt()
            val totalPage = pages[2].toInt()

            println("$currentPage/$totalPage")
            println("nextUrl: $nextUrl")

            val list = ArrayList<Book>()
            val elementList = doc.select("#ss .ssl .book")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst(".pic img").absUrl("src")
                val titleElement = element.selectFirst(".text p span a")
                val bookUrl = titleElement.absUrl("href")
                val title = titleElement.text()
                val lis = element.select(".text ul li")
                val author = lis[1].text()
                val artist = lis[0].text()
                val intro = lis.last().text()
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro })
            }

            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }

}