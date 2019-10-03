package com.github.eprendre.tingshu.sources.impl

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

object QianKun : TingShu {
    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
            val url = "http://m.qktsw.com/search.asp?page=$page&searchword=$encodedKeywords"
            val doc = Jsoup.connect(url).get()

            val totalPage = doc.selectFirst(".page").ownText().split("/")[1].toInt()

            val list = ArrayList<Book>()
            val elementList = doc.select("#cateList_wap .bookbox")
            elementList.forEach { element ->
                val bookUrl = element.absUrl("bookid")
                val coverUrl = element.selectFirst(".bookimg img").attr("orgsrc")
                val bookinfo = element.selectFirst(".bookinfo")
                val title = bookinfo.selectFirst(".bookname").text()
                val (author, artist) = bookinfo.selectFirst(".author").text().split(" ").let {
                    Pair(it[0], it[1])
                }
                val intro = bookinfo.selectFirst(".intro_line").text()
                val status = bookinfo.selectFirst(".update").text()
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

            val episodes = doc.select("#playlist li a").map {
                Episode(it.text(), it.attr("abs:href"))
            }

            Prefs.playList = episodes
            Prefs.currentIntro = doc.selectFirst(".book_intro").text()
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(
        exoPlayer: ExoPlayer,
        dataSourceFactory: DataSource.Factory
    ): AudioUrlExtractor {
        AudioUrlWebViewExtractor.setUp(exoPlayer, dataSourceFactory,
            script = "(function() { return ('<html>'+document.getElementById(\"xplayer\").contentDocument.documentElement.innerHTML+'</html>'); })();") { str ->
            val doc = Jsoup.parse(str)
            val audioElement = doc.selectFirst("audio")
            return@setUp audioElement?.attr("src")
        }
        return AudioUrlWebViewExtractor
    }

    override fun getCategoryMenus(): List<CategoryMenu> {
        val menu1 = CategoryMenu(
            "有声小说", R.drawable.ic_library_books, View.generateViewId(), listOf(
                CategoryTab("玄幻", "http://m.qktsw.com/book/7.html"),
                CategoryTab("恐怖", "http://m.qktsw.com/book/1.html"),
                CategoryTab("惊悚", "http://m.qktsw.com/book/2.html"),
                CategoryTab("武侠", "http://m.qktsw.com/book/3.html"),
                CategoryTab("推理", "http://m.qktsw.com/book/4.html"),
                CategoryTab("历史", "http://m.qktsw.com/book/5.html"),
                CategoryTab("军事", "http://m.qktsw.com/book/6.html"),
                CategoryTab("言情", "http://m.qktsw.com/book/8.html"),
                CategoryTab("都市", "http://m.qktsw.com/book/9.html"),
                CategoryTab("科幻", "http://m.qktsw.com/book/10.html"),
                CategoryTab("穿越", "http://m.qktsw.com/book/11.html"),
                CategoryTab("网游", "http://m.qktsw.com/book/12.html"),
                CategoryTab("经典", "http://m.qktsw.com/book/13.html"),
                CategoryTab("财经", "http://m.qktsw.com/book/14.html"),
                CategoryTab("粤语", "http://m.qktsw.com/book/75.html"),
                CategoryTab("管理", "http://m.qktsw.com/book/350.html"),
                CategoryTab("人文", "http://m.qktsw.com/book/351.html"),
                CategoryTab("史学", "http://m.qktsw.com/book/352.html"),
                CategoryTab("励志", "http://m.qktsw.com/book/353.html"),
                CategoryTab("健康养生", "http://m.qktsw.com/book/354.html"),
                CategoryTab("百家讲坛", "http://m.qktsw.com/book/355.html"),
                CategoryTab("广播剧", "http://m.qktsw.com/book/79.html"),
                CategoryTab("文学名著", "http://m.qktsw.com/book/16.html"),
                CategoryTab("诗歌散文", "http://m.qktsw.com/book/15.html")
            )
        )
        val menu2 = CategoryMenu(
            "评书", R.drawable.ic_radio_black_24dp, View.generateViewId(), listOf(
                CategoryTab("刘兰芳", "http://m.qktsw.com/book/543.html"),
                CategoryTab("单田芳", "http://m.qktsw.com/book/591.html"),
                CategoryTab("袁阔成", "http://m.qktsw.com/book/650.html"),
                CategoryTab("孙一", "http://m.qktsw.com/book/600.html"),
                CategoryTab("田战义", "http://m.qktsw.com/book/603.html"),
                CategoryTab("田连元", "http://m.qktsw.com/book/604.html"),
                CategoryTab("王军", "http://m.qktsw.com/book/611.html"),
                CategoryTab("王传林", "http://m.qktsw.com/book/621.html"),
                CategoryTab("王玥波", "http://m.qktsw.com/book/623.html"),
                CategoryTab("张悦楷", "http://m.qktsw.com/book/655.html"),
                CategoryTab("张庆升", "http://m.qktsw.com/book/672.html"),
                CategoryTab("仲维维", "http://m.qktsw.com/book/681.html"),
                CategoryTab("梁锦辉", "http://m.qktsw.com/book/550.html"),
                CategoryTab("张少佐", "http://m.qktsw.com/book/685.html")
            )
        )
        val menu3 = CategoryMenu(
            "相声小品", R.drawable.ic_people_black_24dp, View.generateViewId(), listOf(
                CategoryTab("郭德纲", "http://m.qktsw.com/book/496.html"),
                CategoryTab("侯耀文", "http://m.qktsw.com/book/515.html"),
                CategoryTab("姜昆", "http://m.qktsw.com/book/526.html"),
                CategoryTab("洛桑", "http://m.qktsw.com/book/560.html"),
                CategoryTab("刘宝瑞", "http://m.qktsw.com/book/562.html"),
                CategoryTab("马季", "http://m.qktsw.com/book/580.html"),
                CategoryTab("牛群", "http://m.qktsw.com/book/583.html"),
                CategoryTab("冰心", "http://m.qktsw.com/book/816.html"),
                CategoryTab("曹云金", "http://m.qktsw.com/book/485.html"),
                CategoryTab("大兵", "http://m.qktsw.com/book/488.html"),
                CategoryTab("李伯清", "http://m.qktsw.com/book/554.html"),
                CategoryTab("赵本山", "http://m.qktsw.com/book/701.html"),
                CategoryTab("小沈阳", "http://m.qktsw.com/book/702.html"),
                CategoryTab("鬼故事", "http://m.qktsw.com/book/728.html"),
                CategoryTab("宋小宝", "http://m.qktsw.com/book/729.html"),
                CategoryTab("王小利", "http://m.qktsw.com/book/730.html"),
                CategoryTab("于谦", "http://m.qktsw.com/book/817.html")
            )
        )
        val menu4 = CategoryMenu(
            "儿童", R.drawable.ic_face_black_24dp, View.generateViewId(), listOf(
                CategoryTab("童话故事", "http://m.qktsw.com/book/321.html"),
                CategoryTab("寓言故事", "http://m.qktsw.com/book/322.html"),
                CategoryTab("儿童歌曲", "http://m.qktsw.com/book/323.html"),
                CategoryTab("儿童启蒙", "http://m.qktsw.com/book/324.html"),
                CategoryTab("粤语儿童故事", "http://m.qktsw.com/book/325.html")
            )
        )
        val menu5 = CategoryMenu(
            "戏曲", R.drawable.ic_toys_black_24dp, View.generateViewId(), listOf(
                CategoryTab("京剧", "http://m.qktsw.com/book/17.html"),
                CategoryTab("评剧", "http://m.qktsw.com/book/18.html"),
                CategoryTab("越剧", "http://m.qktsw.com/book/19.html"),
                CategoryTab("黄梅戏", "http://m.qktsw.com/book/20.html"),
                CategoryTab("豫剧", "http://m.qktsw.com/book/21.html"),
                CategoryTab("晋剧", "http://m.qktsw.com/book/22.html"),
                CategoryTab("昆曲", "http://m.qktsw.com/book/23.html"),
                CategoryTab("秦腔", "http://m.qktsw.com/book/24.html"),
                CategoryTab("沪剧", "http://m.qktsw.com/book/25.html"),
                CategoryTab("川剧", "http://m.qktsw.com/book/26.html"),
                CategoryTab("潮剧", "http://m.qktsw.com/book/27.html"),
                CategoryTab("河北梆子", "http://m.qktsw.com/book/28.html"),
                CategoryTab("曲剧", "http://m.qktsw.com/book/29.html"),
                CategoryTab("婺剧", "http://m.qktsw.com/book/30.html"),
                CategoryTab("河南坠子", "http://m.qktsw.com/book/31.html"),
                CategoryTab("蒲剧", "http://m.qktsw.com/book/32.html"),
                CategoryTab("粤剧", "http://m.qktsw.com/book/35.html"),
                CategoryTab("二人转", "http://m.qktsw.com/book/43.html"),
                CategoryTab("其它戏曲", "http://m.qktsw.com/book/44.html")
            )
        )
        val menu6 = CategoryMenu(
            "其他", R.drawable.ic_more_horiz, View.generateViewId(), listOf(
                CategoryTab("笑话", "http://m.qktsw.com/tingbook/6.html"),
                CategoryTab("佛学", "http://m.qktsw.com/book/715.html")
            )
        )
        return listOf(menu1, menu2, menu3, menu4, menu5, menu6)
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).get()
            val nextUrl =
                doc.select(".page a").firstOrNull { it.text().contains("下页") }?.attr("abs:href")
                    ?: ""
            val pages = doc.selectFirst(".page").ownText().let { text ->
                Regex("(\\d+)/(\\d+)").find(text)!!.groupValues
            }
            val currentPage = pages[1].toInt()
            val totalPage = pages[2].toInt()

            println("$currentPage/$totalPage")
            println("nextUrl: $nextUrl")

            val list = ArrayList<Book>()
            val elementList = doc.select("#cateList_wap .bookbox")
            elementList.forEach { element ->
                val bookUrl = element.absUrl("bookid")
                val coverUrl = element.selectFirst(".bookimg img").attr("orgsrc")
                val bookInfo = element.selectFirst(".bookinfo")
                val title = bookInfo.selectFirst(".bookname").text()
                val (author, artist) = bookInfo.selectFirst(".author").text().split(" ").let {
                    Pair(it[0], it[1])
                }
                val intro = bookInfo.selectFirst(".intro_line").text()
                val status = bookInfo.selectFirst(".update").text()
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                    this.intro = intro
                    this.status = status
                })
            }

            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }

}