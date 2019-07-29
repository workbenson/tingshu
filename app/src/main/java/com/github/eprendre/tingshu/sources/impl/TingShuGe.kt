package com.github.eprendre.tingshu.sources.impl

import com.github.eprendre.tingshu.App
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

object TingShuGe : TingShu {

    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val list = ArrayList<Book>()
            val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
            val url = "http://www.tingshuge.com/search.asp?page=$page&searchword=$encodedKeywords&searchtype=-1"
            val doc = Jsoup.connect(url).get()
            val pages = doc.selectFirst("#channelright .list_mod .pagesnums").select("li")
            val totalPage = pages[pages.size - 3].text().toInt()
            val elementList = doc.select("#channelright .list_mod .clist li")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst("a img").attr("abs:src")
                val bookUrl = element.selectFirst("a").attr("abs:href")
                val (title, author, artist) = element.select("p").let { row ->
                    Triple(row[0].text(), row[2].text(), row[3].text())
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

            val episodes = doc.selectFirst(".numlist").select("li a").map {
                Episode(it.text(), it.attr("abs:href"))
            }

            App.playList = episodes
            Prefs.currentIntro = null
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(exoPlayer: ExoPlayer, dataSourceFactory: DataSource.Factory): AudioUrlExtractor {
        AudioUrlWebViewExtractor.setUp(exoPlayer, dataSourceFactory) { doc ->
            val audioElement = doc.getElementById("jp_audio_0")
            return@setUp audioElement?.attr("src")
        }
        return AudioUrlWebViewExtractor
    }

    override fun getMainCategoryTabs(): List<CategoryTab> {
        return listOf(
            CategoryTab("玄幻", "http://www.tingshuge.com/List/4.html"),
            CategoryTab("武侠", "http://www.tingshuge.com/List/5.html"),
            CategoryTab("仙侠", "http://www.tingshuge.com/List/73.html"),
            CategoryTab("网游", "http://www.tingshuge.com/List/16.html"),
            CategoryTab("科幻", "http://www.tingshuge.com/List/6.html"),
            CategoryTab("推理", "http://www.tingshuge.com/List/7.html"),
            CategoryTab("悬疑", "http://www.tingshuge.com/List/71.html"),
            CategoryTab("恐怖", "http://www.tingshuge.com/List/8.html"),
            CategoryTab("灵异", "http://www.tingshuge.com/List/9.html"),
            CategoryTab("都市", "http://www.tingshuge.com/List/10.html"),
            CategoryTab("穿越", "http://www.tingshuge.com/List/15.html"),
            CategoryTab("言情", "http://www.tingshuge.com/List/11.html"),
            CategoryTab("校园", "http://www.tingshuge.com/List/12.html")
        )
    }

    override fun getOtherCategoryTabs(): List<CategoryTab> {
        return listOf(
            CategoryTab("历史", "http://www.tingshuge.com/List/13.html"),
            CategoryTab("军事", "http://www.tingshuge.com/List/14.html"),
            CategoryTab("官场", "http://www.tingshuge.com/List/17.html"),
            CategoryTab("商战", "http://www.tingshuge.com/List/18.html"),
            CategoryTab("儿童", "http://www.tingshuge.com/List/22.html"),
            CategoryTab("戏曲", "http://www.tingshuge.com/List/25.html"),
            CategoryTab("百家讲坛", "http://www.tingshuge.com/List/30.html"),
            CategoryTab("人文", "http://www.tingshuge.com/List/21.html"),
            CategoryTab("诗歌", "http://www.tingshuge.com/List/69.html"),
            CategoryTab("相声", "http://www.tingshuge.com/List/24.html"),
            CategoryTab("小品", "http://www.tingshuge.com/List/66.html"),
            CategoryTab("励志", "http://www.tingshuge.com/List/28.html"),
            CategoryTab("婚姻", "http://www.tingshuge.com/List/72.html"),
            CategoryTab("养生", "http://www.tingshuge.com/List/29.html"),
            CategoryTab("英语", "http://www.tingshuge.com/List/27.html"),
            CategoryTab("教育", "http://www.tingshuge.com/List/70.html"),
            CategoryTab("儿歌", "http://www.tingshuge.com/List/67.html"),
            CategoryTab("笑话", "http://www.tingshuge.com/List/23.html"),
            CategoryTab("佛学", "http://www.tingshuge.com/List/74.html"),
            CategoryTab("广播剧", "http://www.tingshuge.com/List/68.html"),
            CategoryTab("国学", "http://www.tingshuge.com/List/19.html"),
            CategoryTab("名著", "http://www.tingshuge.com/List/20.html"),
            CategoryTab("评书大全", "http://www.tingshuge.com/List/26.html")
        )
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val list = ArrayList<Book>()
            val doc = Jsoup.connect(url).get()
            val container = doc.getElementById("channelleft")
            val pages = container.selectFirst(".list_mod .pagesnums").select("li")
            val totalPage = pages[pages.size - 3].text().toInt()
            val currentPage = container.getElementById("pagenow").text().toInt()
            val nextUrl = pages[pages.size - 2].selectFirst("a")?.attr("abs:href") ?: ""

            val elementList = container.select(".list_mod .clist li")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst("a img").attr("abs:src")
                val bookUrl = element.selectFirst("a").attr("abs:href")
                val (title, author, artist) = element.select("p").let { row ->
                    Triple(row[0].text(), row[2].text(), row[3].text())
                }
                list.add(Book(coverUrl, bookUrl, title, author, artist))
            }
            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }
}