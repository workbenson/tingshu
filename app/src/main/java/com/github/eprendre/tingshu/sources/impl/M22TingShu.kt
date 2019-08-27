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

object M22TingShu : TingShu {
    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val encodedKeywords = URLEncoder.encode(keywords, "utf-8")
            val url = "https://m.ting22.com/search.php?q=$encodedKeywords&page=$page"
            val doc = Jsoup.connect(url).get()

            val a = doc.selectFirst("#more-wrapper a")
            val nextPageUrl = a?.attr("href")
            val totalPage = if (nextPageUrl == null || nextPageUrl.contains("javascript")) page else page + 1

            val list = ArrayList<Book>()
            val elementList = doc.select(".card")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst(".pic img").absUrl("src")
                val bookUrl = element.selectFirst(".link").absUrl("href")
                val title = element.selectFirst(".title").text()
                val infos = element.select(".con div")
                val author = infos[0].text()
                val artist = infos[2].text()
                val intro = infos[4].text()
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro })
            }
            return@fromCallable Pair(list, totalPage)
        }
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            val doc = Jsoup.connect(bookUrl).get()
            TingShuSourceHandler.downloadCoverForNotification()

            val episodes = doc.select("#playlist li a").map {
                Episode(it.text(), it.attr("abs:href"))
            }
            App.playList = episodes
            Prefs.currentIntro = doc.selectFirst(".bookintro").text()
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(
        exoPlayer: ExoPlayer,
        dataSourceFactory: DataSource.Factory
    ): AudioUrlExtractor {
        AudioUrlWebViewExtractor.setUp(exoPlayer, dataSourceFactory) { doc ->
            val audioElement = doc.getElementById("jp_audio_0")
            return@setUp audioElement?.attr("src")
        }
        return AudioUrlWebViewExtractor
    }

    override fun getCategoryMenus(): List<CategoryMenu> {
        val menu1 = CategoryMenu(
            "有声小说", R.drawable.ic_library_books, View.generateViewId(), listOf(
                CategoryTab("玄幻武侠", "https://m.ting22.com/paihang/xuanhuan/"),
                CategoryTab("网游竞技", "https://m.ting22.com/paihang/wangyou/"),
                CategoryTab("言情", "https://m.ting22.com/paihang/dushi/"),
                CategoryTab("鬼故事", "https://m.ting22.com/paihang/kongbu/"),
                CategoryTab("军事历史", "https://m.ting22.com/paihang/junshi/"),
                CategoryTab("刑侦推理", "https://m.ting22.com/paihang/xingzhen/"),
                CategoryTab("职场商战", "https://m.ting22.com/paihang/shangzhan/")
            )
        )
        val menu2 = CategoryMenu(
            "排行榜", R.drawable.ic_looks_one_black_24dp, View.generateViewId(), listOf(
                CategoryTab("周排行榜", "https://m.ting22.com/yousheng/"),
                CategoryTab("月排行榜", "https://m.ting22.com/paihang/yousheng/")
            )
        )
        return listOf(menu1, menu2)
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).get()
            val nextUrl = doc.select(".page")[1].select("a")[1].attr("abs:href")
            val pages = doc.select(".page")[1].ownText().let { text ->
                Regex("(\\d+)/(\\d+)").find(text)!!.groupValues
            }
            val currentPage = pages[1].toInt()
            val totalPage = pages[2].toInt()

            val list = ArrayList<Book>()
            val elementList = doc.select(".bookbox")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst(".bookimg img").absUrl("src")
                val bookUrl = element.absUrl("href")
                val bookInfo = element.selectFirst(".bookinfo")
                val title = bookInfo.selectFirst(".bookname").text()
                val (author, artist) = bookInfo.selectFirst(".author").text().split(" ").let {
                    Pair(it[0], it[1])
                }
                val intro = bookInfo.selectFirst(".intro_line").text()
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro })
            }
            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }

}