package com.github.eprendre.tingshu.sources.impl

import android.view.View
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.AudioUrlCommonExtractor
import com.github.eprendre.tingshu.sources.AudioUrlExtractor
import com.github.eprendre.tingshu.sources.TingShu
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.utils.*
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Completable
import io.reactivex.Single
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder

object HuanTingWang : TingShu {

    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
            val url = "http://m.ting89.com/search.asp?searchword=$encodedKeywords&page=$page"
            val doc = Jsoup.connect(url).get()

            val totalPage = doc.selectFirst(".page").ownText().split("/")[1].toInt()

            val list = ArrayList<Book>()
            val elementList = doc.select("#cateList_wap .bookbox")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst(".bookimg img").attr("orgsrc")
                val bookUrl = "${TingShuSourceHandler.SOURCE_URL_HUANTINGWANG}/book/?${element.attr("bookid")}.html"
                val bookInfo = element.selectFirst(".bookinfo")
                val title = bookInfo.selectFirst(".bookname").text()
                val (author, artist) = bookInfo.selectFirst(".author").text().split(" ").let {
                    Pair(it[0], it[1])
                }
                val intro = bookInfo.selectFirst(".intro_line").text()
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
            Prefs.currentIntro = doc.selectFirst(".book_intro").text()
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(exoPlayer: ExoPlayer, dataSourceFactory: DataSource.Factory): AudioUrlExtractor {
        AudioUrlCommonExtractor.setUp(exoPlayer, dataSourceFactory) { doc ->
            val result = doc.getElementsByTag("script")
                .first { !it.hasAttr("src") && !it.hasAttr("type") }
                .html()
                .let {
                    Regex("datas=\\(\"(.*)\"\\.split")
                        .find(it)?.groupValues?.get(1)
                }
            if (result == null) {
                throw Exception("幻听网提取播放地址失败")
            } else {
                val list = URLDecoder.decode(result, "gb2312").split("&")
                return@setUp list[0]
            }
        }
        return AudioUrlCommonExtractor
    }

    override fun getCategoryMenus(): List<CategoryMenu> {
        val menu1 = CategoryMenu(
            "小说", R.drawable.ic_library_books, View.generateViewId(), listOf(
                CategoryTab("玄幻玄幻", "http://m.ting89.com/booklist/?1.html"),
                CategoryTab("武侠仙侠", "http://m.ting89.com/booklist/?2.html"),
                CategoryTab("科幻世界", "http://m.ting89.com/booklist/?5.html"),
                CategoryTab("网络游戏", "http://m.ting89.com/booklist/?11.html"),
                CategoryTab("现代都市", "http://m.ting89.com/booklist/?3.html"),
                CategoryTab("女生言情", "http://m.ting89.com/booklist/?4.html"),
                CategoryTab("女生穿越", "http://m.ting89.com/booklist/?38.html"),
                CategoryTab("推理悬念", "http://m.ting89.com/booklist/?6.html"),
                CategoryTab("恐怖故事", "http://m.ting89.com/booklist/?7.html"),
                CategoryTab("悬疑惊悚", "http://m.ting89.com/booklist/?8.html")
            )
        )

        val menu2 = CategoryMenu(
            "其它", R.drawable.ic_more_horiz, View.generateViewId(), listOf(
                CategoryTab("历史传记", "http://m.ting89.com/booklist/?9.html"),
                CategoryTab("铁血军魂", "http://m.ting89.com/booklist/?10.html"),
                CategoryTab("经典传记", "http://m.ting89.com/booklist/?35.html"),
                CategoryTab("百家讲坛", "http://m.ting89.com/booklist/?36.html"),
                CategoryTab("粤语", "http://m.ting89.com/booklist/?40.html"),
                CategoryTab("儿童故事", "http://m.ting89.com/booklist/?16.html"),
                CategoryTab("相声", "http://m.ting89.com/booklist/?34.html"),
                CategoryTab("评书", "http://m.ting89.com/booklist/?13.html")
            )
        )
        return listOf(menu1, menu2)
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).get()
            val nextUrl = doc.select(".page a").firstOrNull { it.text().contains("下页") }?.attr("abs:href") ?: ""
            val pages = doc.selectFirst(".page").ownText().let { text ->
                Regex("(\\d+)/(\\d+)").find(text)!!.groupValues
            }
            val currentPage = pages[1].toInt()
            val totalPage = pages[2].toInt()

            val list = ArrayList<Book>()
            val elementList = doc.select("#cateList_wap .bookbox")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst(".bookimg img").attr("orgsrc")
                val bookUrl = "${TingShuSourceHandler.SOURCE_URL_HUANTINGWANG}/book/?${element.attr("bookid")}.html"
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