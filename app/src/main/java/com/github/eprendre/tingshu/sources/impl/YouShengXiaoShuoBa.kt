package com.github.eprendre.tingshu.sources.impl

import android.util.Log
import android.view.View
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

object YouShengXiaoShuoBa : TingShu {
    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
            val url = "http://m.ysxs8.com/search.asp?searchword=$encodedKeywords&page=$page"
            val doc = Jsoup.connect(url).get()

            val totalPage = doc.selectFirst(".page").ownText().split("/")[1].toInt()

            val list = ArrayList<Book>()
            val elementList = doc.select("#cateList_wap > .bookbox")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst(".bookimg > img").attr("orgsrc")
                val title = element.selectFirst(".bookname").text()
                val bookId = element.attr("bookid")
                val bookUrl = "http://m.ysxs8.com/downlist/$bookId.html"
                val (author, artist) = element.selectFirst(".author").let {
                    val array = it.text().split(" ")
                    Pair(array[0], array[1])
                }
                val status = element.selectFirst(".update").text()
                val intro = element.selectFirst(".intro_line").text()
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                    this.intro = intro
                    this.status = status
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
                Episode(it.text(), it.attr("abs:href"))
            }

            Prefs.playList = episodes
            Prefs.currentIntro = doc.selectFirst(".book_intro").text()
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(): AudioUrlExtractor {
        AudioUrlWebViewExtractor.setUp(
            script = "(function() { return ('<html>'+document.getElementById(\"xplayer\").contentDocument.getElementById(\"viframe\").contentDocument.documentElement.innerHTML+'</html>'); })();") { str ->
            Log.i("parseeee", str)
            val doc = Jsoup.parse(str)
            val audioElement = doc.selectFirst("audio")
            return@setUp audioElement?.attr("src")
        }
        return AudioUrlWebViewExtractor
    }

    override fun getCategoryMenus(): List<CategoryMenu> {
        val menu1 = CategoryMenu(
            "有声小说", R.drawable.ic_library_books, View.generateViewId(), listOf(
                CategoryTab("网络玄幻", "http://m.ysxs8.com/downlist/r52_1.html"),
                CategoryTab("探险盗墓", "http://m.ysxs8.com/downlist/r45_1.html"),
                CategoryTab("恐怖悬疑", "http://m.ysxs8.com/downlist/r17_1.html"),
                CategoryTab("评书下载", "http://m.ysxs8.com/downlist/r3_1.html"),
                CategoryTab("历史军事", "http://m.ysxs8.com/downlist/r15_1.html"),
                CategoryTab("传统武侠", "http://m.ysxs8.com/downlist/r12_1.html"),
                CategoryTab("都市言情", "http://m.ysxs8.com/downlist/r13_1.html"),
                CategoryTab("官场刑侦", "http://m.ysxs8.com/downlist/r14_1.html"),
                CategoryTab("人物传记", "http://m.ysxs8.com/downlist/r16_1.html")
            )
        )
        val menu2 = CategoryMenu(
            "其它", R.drawable.ic_more_horiz, View.generateViewId(), listOf(
                CategoryTab("相声戏曲", "http://m.ysxs8.com/downlist/r7_1.html"),
                CategoryTab("管理营销", "http://m.ysxs8.com/downlist/r6_1.html"),
                CategoryTab("广播剧", "http://m.ysxs8.com/downlist/r18_1.html"),
                CategoryTab("百家讲坛", "http://m.ysxs8.com/downlist/r32_1.html"),
                CategoryTab("外语读物", "http://m.ysxs8.com/downlist/r35_1.html"),
                CategoryTab("儿童读物", "http://m.ysxs8.com/downlist/r4_1.html"),
                CategoryTab("明朝那些事儿", "http://m.ysxs8.com/downlist/r36_1.html"),
                CategoryTab("有声文学", "http://m.ysxs8.com/downlist/r41_1.html"),
                CategoryTab("职场商战", "http://m.ysxs8.com/downlist/r81_1.html")
            )
        )
        return listOf(menu1, menu2)
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).get()
            val pageElement = doc.selectFirst(".page")
            val nextUrl = pageElement.selectFirst("a").absUrl("href")

            val (currentPage, totalPage) = pageElement.ownText().replace("页次 ", "").split("/").let {
                Pair(it[0].toInt(), it[1].toInt())
            }

            val list = ArrayList<Book>()
            val elementList = doc.select("#cateList_wap > .bookbox")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst(".bookimg > img").attr("orgsrc")
                val title = element.selectFirst(".bookname").text()
                val bookId = element.attr("bookid")
                val bookUrl = "http://m.ysxs8.com/downlist/$bookId.html"
                val (author, artist) = element.selectFirst(".author").let {
                    val array = it.text().split(" ")
                    Pair(array[0], array[1])
                }
                val status = element.selectFirst(".update").text()
                val intro = element.selectFirst(".intro_line").text()
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                    this.intro = intro
                    this.status = status
                })
            }

            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }

}