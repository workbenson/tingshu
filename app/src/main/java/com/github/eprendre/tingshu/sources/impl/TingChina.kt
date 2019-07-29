package com.github.eprendre.tingshu.sources.impl

import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.sources.*
import com.github.eprendre.tingshu.utils.*
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Completable
import io.reactivex.Single
import org.jsoup.Jsoup
import java.net.URLEncoder

object TingChina : TingShu {
    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
            val url = "http://www.tingchina.com/search1.asp?mainlei=0&lei=0&keyword=$encodedKeywords"
            val doc = Jsoup.connect(url).get()

            val totalPage = 1
            val list = ArrayList<Book>()
            val elementList = doc.select(".singerlist1 dd ul li a")
            elementList.forEach { element ->
                val bookUrl = element.absUrl("href")
                val title = element.text()
                val book = Book("", bookUrl, title, "", "")
                list.add(book)
            }
            return@fromCallable Pair(list, totalPage)
        }
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            TingShuSourceHandler.downloadCoverForNotification()

            //获取章节列表
            val doc = Jsoup.connect(bookUrl).get()
            val episodes = doc.select(".main03 .list .b2 a").map {
                Episode(it.text(), it.attr("abs:href"))
            }
            App.playList = episodes
            Prefs.currentIntro = doc.selectFirst(".main03 .book02").ownText()
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(exoPlayer: ExoPlayer, dataSourceFactory: DataSource.Factory): AudioUrlExtractor {
        TingChinaAudioUrlWebViewExtractor.setUp(exoPlayer, dataSourceFactory, true) { str ->
            val url2 = Regex("url\\[2]= \"(.*?)\";").find(str)?.groupValues?.get(1)
            val url3 = Regex("url\\[3]= \"(.*?)\";").find(str)?.groupValues?.get(1)

            if (url2 == null || url3 == null) {
                return@setUp null
            }

            return@setUp "$url2$url3"
        }
        return TingChinaAudioUrlWebViewExtractor
    }

    override fun getMainCategoryTabs(): List<CategoryTab> {
        return listOf(
            CategoryTab("玄幻奇幻", "http://www.tingchina.com/yousheng/leip_135_1.htm"),
            CategoryTab("网络热门", "http://www.tingchina.com/yousheng/leip_146_1.htm"),
            CategoryTab("科幻有声", "http://www.tingchina.com/yousheng/leip_128_1.htm"),
            CategoryTab("武侠小说", "http://www.tingchina.com/yousheng/leip_133_1.htm"),
            CategoryTab("都市言情", "http://www.tingchina.com/yousheng/leip_125_1.htm"),
            CategoryTab("鬼故事", "http://www.tingchina.com/yousheng/leip_129_1.htm")
        )
    }

    override fun getOtherCategoryTabs(): List<CategoryTab> {
        return listOf(
            CategoryTab("历史军事", "http://www.tingchina.com/yousheng/leip_130_1.htm"),
            CategoryTab("官场商战", "http://www.tingchina.com/yousheng/leip_126_1.htm"),
            CategoryTab("刑侦推理", "http://www.tingchina.com/yousheng/leip_134_1.htm"),
            CategoryTab("经典纪实", "http://www.tingchina.com/yousheng/leip_127_1.htm"),
            CategoryTab("通俗文学", "http://www.tingchina.com/yousheng/leip_132_1.htm"),
            CategoryTab("人物传记", "http://www.tingchina.com/yousheng/leip_131_1.htm")
        )
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).get()
            val pages = doc.selectFirst(".yema span").children()
            val currentPage = Regex(".+leip_\\d+_(\\d+)\\.htm").find(url)!!.groupValues[1].toInt()
            var totalPage = currentPage
            if (pages.last().absUrl("href") != url) {
                totalPage = currentPage + 1
            }
            var nextUrl = ""
            if (currentPage != totalPage) {
                val index = pages.indexOfFirst { it.text() == currentPage.toString() }
                nextUrl = pages[index + 1].absUrl("href")
            }
            println("$currentPage/$totalPage")
            println("nextUrl: $nextUrl")

            val list = ArrayList<Book>()
            val elementList = doc.select(".showlist dl")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst("dt a img").absUrl("src")
                val titleElement = element.selectFirst("dd .title a")
                val bookUrl = titleElement.absUrl("href")
                val (title, author, artist) = titleElement.text().split(" ").let {
                    Triple(
                        it[0].replace("《", "").replace("》", ""),
                        it[1],
                        it[2].split("　")[0]
                    )
                }
                val intro = element.selectFirst("dd .info").ownText()
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro })
            }
            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }

}