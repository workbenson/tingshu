package com.github.eprendre.tingshu.sources.impl

import android.view.View
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.*
import com.github.eprendre.tingshu.utils.*
import io.reactivex.Completable
import io.reactivex.Single
import org.jsoup.Jsoup
import java.lang.Exception
import java.net.URLEncoder

object WoTingPingShu : TingShu {
    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
            val url = "https://m.5tps.com/so.asp?keyword=$encodedKeywords&page=$page"
            val doc = Jsoup.connect(url).get()

            var totalPage = 1
            val list = ArrayList<Book>()
            try {
                totalPage = doc.selectFirst(".booksite > .bookbutton").text().split("/")[1].toInt()
                val elementList = doc.select(".top_list > a")
                elementList.forEach { element ->
                    val bookUrl = element.absUrl("href")
                    val coverUrl = ""
                    val title = element.ownText()
                    val author = ""
                    val (artist, status) = element.selectFirst(".peo").text().split("／").let {
                        Pair("播音: ${it[0]}", it[1])
                    }
                    list.add((Book(coverUrl, bookUrl, title, author, artist).apply {
                        this.status = status
                    }))
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

            val episodes = doc.select("#playlist > ul > li > a").map {
                Episode(it.text(), it.attr("abs:href"))
            }

            Prefs.playList = episodes
            Prefs.currentIntro = doc.selectFirst(".book_intro").text()
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(): AudioUrlExtractor {
        AudioUrlCommonExtractor.setUp { doc ->
            val iframeSrc = doc.getElementById("play").absUrl("src")
            val iframeStr = Jsoup.connect(iframeSrc).get().toString()

            return@setUp Regex("mp3:'(.*?)'").find(iframeStr)?.groupValues?.get(1) ?: ""
        }
        return AudioUrlCommonExtractor
    }

    override fun getCategoryMenus(): List<CategoryMenu> {
        val menu1 =
            CategoryMenu("评书分类", R.drawable.ic_radio_black_24dp, View.generateViewId(), listOf(
                CategoryTab("单田芳", "https://m.5tps.com/m_l_hot/1_1.html"),
                CategoryTab("刘兰芳", "https://m.5tps.com/m_l_hot/2_1.html"),
                CategoryTab("田连元", "https://m.5tps.com/m_l_hot/3_1.html"),
                CategoryTab("袁阔成", "https://m.5tps.com/m_l_hot/4_1.html"),
                CategoryTab("连丽如", "https://m.5tps.com/m_l_hot/5_1.html"),
                CategoryTab("张少佐", "https://m.5tps.com/m_l_hot/6_1.html"),
                CategoryTab("田战义", "https://m.5tps.com/m_l_hot/7_1.html"),
                CategoryTab("孙一", "https://m.5tps.com/m_l_hot/8_1.html"),
                CategoryTab("石连君", "https://m.5tps.com/m_l_hot/29_1.html"),
                CategoryTab("马长辉", "https://m.5tps.com/m_l_hot/25_1.html"),
                CategoryTab("王军", "https://m.5tps.com/m_l_hot/27_1.html"),
                CategoryTab("王玥波", "https://m.5tps.com/m_l_hot/28_1.html"),
                CategoryTab("王封臣", "https://m.5tps.com/m_l_hot/30_1.html"),
                CategoryTab("关永超", "https://m.5tps.com/m_l_hot/35_1.html"),
                CategoryTab("昊儒", "https://m.5tps.com/m_l_hot/26_1.html"),
                CategoryTab("粤语评书", "https://m.5tps.com/m_l_hot/12_1.html"),
                CategoryTab("其他评书", "https://m.5tps.com/m_l/13_1.html"))
            )

        val menu2 = CategoryMenu("有声小说", R.drawable.ic_library_books, View.generateViewId(), listOf(
            CategoryTab("玄幻奇幻", "https://m.5tps.com/m_l/46_1.html"),
            CategoryTab("恐怖惊悚", "https://m.5tps.com/m_l/14_1.html"),
            CategoryTab("言情通俗", "https://m.5tps.com/m_l/19_1.html"),
            CategoryTab("武侠小说", "https://m.5tps.com/m_l/11_1.html"),
            CategoryTab("历史军事", "https://m.5tps.com/m_l/15_1.html"),
            CategoryTab("刑侦反腐", "https://m.5tps.com/m_l/16_1.html"),
            CategoryTab("官场商战", "https://m.5tps.com/m_l/17_1.html"),
            CategoryTab("人物纪实", "https://m.5tps.com/m_l/18_1.html"),
            CategoryTab("有声文学", "https://m.5tps.com/m_l/10_1.html"),
            CategoryTab("童话寓言", "https://m.5tps.com/m_l/20_1.html"),
            CategoryTab("广播剧", "https://m.5tps.com/m_l/36_1.html"),
            CategoryTab("英文读物", "https://m.5tps.com/m_l/22_1.html"))
        )

        val menu3 = CategoryMenu("综艺节目", R.drawable.ic_more_horiz, View.generateViewId(), listOf(
            CategoryTab("今日头条", "https://m.5tps.com/m_l/40_1.html"),
            CategoryTab("商业财经", "https://m.5tps.com/m_l/42_1.html"),
            CategoryTab("脱口秀", "https://m.5tps.com/m_l/41_1.html"),
            CategoryTab("亲子教育", "https://m.5tps.com/m_l/43_1.html"),
            CategoryTab("教育培训", "https://m.5tps.com/m_l/44_1.html"),
            CategoryTab("百家讲坛", "https://m.5tps.com/m_l/9_1.html"),
            CategoryTab("综艺娱乐", "https://m.5tps.com/m_l/34_1.html"),
            CategoryTab("相声小品", "https://m.5tps.com/m_l/21_1.html"),
            CategoryTab("汽车音乐", "https://m.5tps.com/m_l/23_1.html"),
            CategoryTab("时尚生活", "https://m.5tps.com/m_l/45_1.html"),
            CategoryTab("戏曲", "https://m.5tps.com/m_l/38_1.html"),
            CategoryTab("二人转", "https://m.5tps.com/m_l/31_1.html"))
        )
        return listOf(menu1, menu2, menu3)
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).get()
            val nextUrl = doc.select(".page > a").firstOrNull { it.text().contains("下页") }?.attr("abs:href") ?: ""
            val (currentPage, totalPage) = doc.selectFirst(".booksite > .bookbutton > a").text().let {
                val pages = it.split(" ")[1].split("/")
                Pair(pages[0].toInt(), pages[1].toInt())
            }

            val list = ArrayList<Book>()
            val elementList = doc.select(".top_list > a")
            elementList.forEach { element ->
                val bookUrl = element.absUrl("href")
                val coverUrl = ""
                val title = element.ownText()
                val author = ""
                val (artist, status) = element.selectFirst(".peo").text().split("／").let {
                    Pair("播音: ${it[0]}", it[1])
                }
                list.add((Book(coverUrl, bookUrl, title, author, artist).apply {
                    this.status = status
                }))
            }

            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }

}