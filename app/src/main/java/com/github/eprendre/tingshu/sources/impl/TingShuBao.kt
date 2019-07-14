package com.github.eprendre.tingshu.sources.impl

import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.sources.AudioUrlExtractor
import com.github.eprendre.tingshu.sources.AudioUrlWebViewExtractor
import com.github.eprendre.tingshu.sources.TingShu
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Category
import com.github.eprendre.tingshu.utils.CategoryTab
import com.github.eprendre.tingshu.utils.Episode
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Completable
import io.reactivex.Single
import org.jsoup.Jsoup
import java.net.URLEncoder

object TingShuBao : TingShu {
    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
            val url = "https://www.tingshubao.com/search.asp?page=$page&searchword=$encodedKeywords&searchtype=-1"
            val doc = Jsoup.connect(url).get()

            val pages = doc.selectFirst(".fanye")
                .children()
                .map { it.ownText() }
                .filter { it.matches("\\d+".toRegex()) }
            val totalPage = pages.last().toInt()

            val list = ArrayList<Book>()
            val elementList = doc.select(".list-works li")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst(".list-imgbox img").absUrl("data-original")
                val titleElement = element.selectFirst(".list-book-dt a")
                val bookUrl = titleElement.absUrl("href")
                val title = titleElement.ownText()
                val (author, artist) = element.select(".list-book-cs .book-author").let {
                    Pair(it[0].ownText(), it[1].ownText())
                }
                val intro = element.selectFirst(".list-book-des").ownText()
                list.add(Book(coverUrl, bookUrl, title, "作者: $author", "播音: $artist").apply { this.intro = intro })
            }

            return@fromCallable Pair(list, totalPage)
        }
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            TingShuSourceHandler.downloadCoverForNotification()
            val doc = Jsoup.connect(bookUrl).get()

            val episodes = doc.select("#playlist li a").map {
                Episode(it.text(), it.attr("abs:href"))
            }
            App.playList = episodes
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
            CategoryTab("玄幻武侠", "https://www.tingshubao.com/list/27.html"),
            CategoryTab("都市小说", "https://www.tingshubao.com/list/28.html"),
            CategoryTab("网游科幻", "https://www.tingshubao.com/list/33.html"),
            CategoryTab("惊悚悬疑", "https://www.tingshubao.com/list/31.html")
        )
    }

    override fun getOtherCategoryTabs(): List<CategoryTab> {
        return listOf(
            CategoryTab("言情小说", "https://www.tingshubao.com/list/29.html"),
            CategoryTab("历史军事", "https://www.tingshubao.com/list/32.html"),
            CategoryTab("评书", "https://www.tingshubao.com/list/2.html")
        )
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).get()

            val nextUrl = doc.select(".fanye a").firstOrNull { it.text().contains("下一页") }?.attr("abs:href") ?: ""
            val currentPage = doc.selectFirst(".fanye strong").ownText().toInt()
            val totalPage = doc.selectFirst(".fanye").children().map { it.ownText() }.filter { it.matches("\\d+".toRegex()) }.last().toInt()

            val list = ArrayList<Book>()
            val elementList = doc.select(".list-works li")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst(".list-imgbox img").absUrl("data-original")
                val titleElement = element.selectFirst(".list-book-dt a")
                val bookUrl = titleElement.absUrl("href")
                val title = titleElement.ownText()
                val (author, artist) = element.select(".list-book-cs .book-author").let {
                    Pair(it[0].ownText(), it[1].ownText())
                }
                val intro = element.selectFirst(".list-book-des").ownText()
                list.add(Book(coverUrl, bookUrl, title, "作者: $author", "播音: $artist").apply { this.intro = intro })
            }

            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }

}