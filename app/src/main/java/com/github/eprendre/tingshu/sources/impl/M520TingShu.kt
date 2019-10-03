package com.github.eprendre.tingshu.sources.impl

import android.view.View
import androidx.core.text.isDigitsOnly
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

object M520TingShu : TingShu {
    override fun getCategoryMenus(): List<CategoryMenu> {
        val menu1 = CategoryMenu(
            "小说", R.drawable.ic_library_books, View.generateViewId(), listOf(
                CategoryTab("玄幻奇幻", "http://m.520tingshu.com/list/?1.html"),
                CategoryTab("修真武侠", "http://m.520tingshu.com/list/?2.html"),
                CategoryTab("恐怖灵异", "http://m.520tingshu.com/list/?3.html"),
                CategoryTab("都市言情", "http://m.520tingshu.com/list/?4.html"),
                CategoryTab("穿越有声", "http://m.520tingshu.com/list/?43.html"),
                CategoryTab("网游小说", "http://m.520tingshu.com/list/?6.html")
            )
        )

        val menu2 = CategoryMenu(
            "其它", R.drawable.ic_more_horiz, View.generateViewId(), listOf(
                CategoryTab("评书大全", "http://m.520tingshu.com/list/?8.html"),
                CategoryTab("粤语古仔", "http://m.520tingshu.com/list/?5.html"),
                CategoryTab("百家讲坛", "http://m.520tingshu.com/list/?9.html"),
                CategoryTab("历史纪实", "http://m.520tingshu.com/list/?11.html"),
                CategoryTab("军事", "http://m.520tingshu.com/list/?13.html"),
                CategoryTab("推理", "http://m.520tingshu.com/list/?46.html"),
                CategoryTab("儿童", "http://m.520tingshu.com/list/?29.html"),
                CategoryTab("广播剧", "http://m.520tingshu.com/list/?10.html"),
                CategoryTab("官场商战", "http://m.520tingshu.com/list/?47.html"),
                CategoryTab("相声小说", "http://m.520tingshu.com/list/?44.html"),
                CategoryTab("ebc5系列", "http://m.520tingshu.com/list/?48.html"),
                CategoryTab("通俗文学", "http://m.520tingshu.com/list/?12.html")
            )
        )
        return listOf(menu1, menu2)
    }

    override fun getAudioUrlExtractor(exoPlayer: ExoPlayer, dataSourceFactory: DataSource.Factory): AudioUrlExtractor {
        AudioUrlWebViewExtractor.setUp(exoPlayer, dataSourceFactory) { str ->
            val doc = Jsoup.parse(str)
            val audioElement = doc.getElementById("jp_audio_0")
            return@setUp audioElement?.attr("src")
        }
        return AudioUrlWebViewExtractor
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            val doc = Jsoup.connect(bookUrl).get()
//            val cover = doc.selectFirst("#wrap .vod .vodbox img").attr("src")

            TingShuSourceHandler.downloadCoverForNotification()

//            val vodmain = doc.selectFirst("#wrap .vod .vodbox .vodmain")
//            Prefs.currentBookName = vodmain.selectFirst(".title").text()

//            vodmain.select(".actor").let {
//                Prefs.author = it[0].text()
//                Prefs.artist = it[1].ownText()
//            }

            val episodes = doc.select(".vodlist .sdn li a").map {
                Episode(it.text(), it.attr("abs:href"))
            }

            Prefs.playList = episodes
            Prefs.currentIntro = doc.getElementById("voddetail").ownText()
            return@fromCallable null
        }
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val list = ArrayList<Book>()
            val doc = Jsoup.connect(url).get()
            val pages = doc.select(".main .page a")
            val totalPage = pages.last { it.text().isDigitsOnly() }.text().toInt()
            val currentPage = doc.selectFirst(".main .page span").text().toInt()
            val nextUrl = pages[pages.size - 2].attr("abs:href")

            val elementList = doc.select(".main .lb_zk li")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst("a .L1 img").attr("src")
                val bookUrl = element.selectFirst("a").attr("abs:href")
                val (title, author, artist) = element.select("a .R1 p").let { row ->
                    Triple(row[0].text(), row[1].text(), row[2].text())
                }
                list.add(Book(coverUrl, bookUrl, title, author, artist))
            }

            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }

    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val url =
                "http://m.520tingshu.com/search.asp?searchword=${URLEncoder.encode(keywords, "gb2312")}&page=$page"
            val list = ArrayList<Book>()
            val doc = Jsoup.connect(url).get()
            val totalPage = doc.select(".main .page a").last().attr("href").let {
                it.split("&")[0].split("=")[1].toInt()
            }

            val elementList = doc.select(".main .lb_zk li")
            elementList.forEach { element ->
                val coverUrl = element.selectFirst("a .L1 img").attr("src")
                val bookUrl = element.selectFirst("a").attr("abs:href")
                val (title, author, artist) = element.select("a .R1 p").let { row ->
                    Triple(row[0].text(), row[1].text(), row[2].text())
                }
                list.add(Book(coverUrl, bookUrl, title, author, artist))
            }
            return@fromCallable Pair(list, totalPage)
        }
    }

}
