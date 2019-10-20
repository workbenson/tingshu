package com.github.eprendre.tingshu.sources.impl

import android.view.View
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.extensions.config
import com.github.eprendre.tingshu.sources.AudioUrlCommonExtractor
import com.github.eprendre.tingshu.sources.AudioUrlExtractor
import com.github.eprendre.tingshu.sources.TingShu
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.utils.*
import io.reactivex.Completable
import io.reactivex.Single
import org.jsoup.Jsoup

object ShengBoFM : TingShu {

    override fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return Single.fromCallable {
            val totalPage = 1
            val list = ArrayList<Book>()
            return@fromCallable Pair(list, totalPage)
        }
    }

    override fun playFromBookUrl(bookUrl: String): Completable {
        return Completable.fromCallable {
            TingShuSourceHandler.downloadCoverForNotification()
            val doc = Jsoup.connect(bookUrl).config().get()

            val episodes = ArrayList<Episode>()

            val title = doc.selectFirst(".card-header").text()
            episodes.add(Episode(title, bookUrl))
            val intro = doc.select(".card-body > .card-text").joinToString("\n") { it.text() }
            Prefs.playList = episodes
            Prefs.currentIntro = intro
            return@fromCallable null
        }
    }

    override fun getAudioUrlExtractor(): AudioUrlExtractor {
       AudioUrlCommonExtractor.setUp { doc ->
           return@setUp doc.selectFirst(".program-player > audio")?.absUrl("src") ?: ""
       }
        return AudioUrlCommonExtractor
    }

    override fun getCategoryMenus(): List<CategoryMenu> {
        val menu0 = CategoryMenu(
            "首页", R.drawable.ic_home_black_24dp, View.generateViewId(), listOf(
                CategoryTab("最新节目", "http://fm.shengbo.org"),
                CategoryTab("校园广播", "http://fm.shengbo.org/Category/52"),
                CategoryTab("活动录音", "http://fm.shengbo.org/Category/56"),
                CategoryTab("两岸无障碍", "http://fm.shengbo.org/Category/85")
            )
        )
        val menu1 = CategoryMenu(
            "声波课堂", R.drawable.ic_library_books, View.generateViewId(), listOf(
                CategoryTab("最新节目", "http://fm.shengbo.org/Category/33"),
                CategoryTab("电脑/网络", "http://fm.shengbo.org/Category/45"),
                CategoryTab("手机/数码", "http://fm.shengbo.org/Category/46"),
                CategoryTab("医疗健康", "http://fm.shengbo.org/Category/55"),
                CategoryTab("易学相关", "http://fm.shengbo.org/Category/66"),
                CategoryTab("综合/其他", "http://fm.shengbo.org/Category/71"),
                CategoryTab("2018年度金盲杖空间·视障者职业拓展计划", "http://fm.shengbo.org/Category/76")
            )
        )
        val menu2 = CategoryMenu(
            "个人电台", R.drawable.ic_radio_black_24dp, View.generateViewId(), listOf(
                CategoryTab("最新节目", "http://fm.shengbo.org/Category/34"),
                CategoryTab("综合台", "http://fm.shengbo.org/Category/47"),
                CategoryTab("音乐台", "http://fm.shengbo.org/Category/48"),
                CategoryTab("文学台", "http://fm.shengbo.org/Category/59"),
                CategoryTab("曲艺台", "http://fm.shengbo.org/Category/60")
            )
        )
        val menu3 = CategoryMenu(
            "视觉讲述", R.drawable.ic_hearing_black_24dp, View.generateViewId(), listOf(
                CategoryTab("最新节目", "http://fm.shengbo.org/Category/36"),
                CategoryTab("口述影像", "http://fm.shengbo.org/Category/50"),
                CategoryTab("赛事讲解", "http://fm.shengbo.org/Category/51"),
                CategoryTab("耳朵阅读", "http://fm.shengbo.org/Category/63"),
                CategoryTab("耳朵旅行", "http://fm.shengbo.org/Category/67"),
                CategoryTab("无锡新吴区阳光志愿者协会为盲人讲电影项目组", "http://fm.shengbo.org/Category/82")
            )
        )
        val menu4 = CategoryMenu(
            "多才多艺", R.drawable.ic_music_note_black_24dp, View.generateViewId(), listOf(
                CategoryTab("最新节目", "http://fm.shengbo.org/Category/41"),
                CategoryTab("原创", "http://fm.shengbo.org/Category/49"),
                CategoryTab("翻唱", "http://fm.shengbo.org/Category/61"),
                CategoryTab("器乐", "http://fm.shengbo.org/Category/62"),
                CategoryTab("朗诵", "http://fm.shengbo.org/Category/65"),
                CategoryTab("曲艺、戏曲", "http://fm.shengbo.org/Category/72"),
                CategoryTab("中国视障好声音2017", "http://fm.shengbo.org/Category/74"),
                CategoryTab("2018狗年新春才艺大展示", "http://fm.shengbo.org/Category/75"),
                CategoryTab("剑河杯歌唱比赛录音", "http://fm.shengbo.org/Category/78")
            )
        )
        return listOf(menu0, menu1, menu2, menu3, menu4)
    }

    override fun getCategoryDetail(url: String): Single<Category> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url).config().get()
            val pagination = doc.selectFirst(".pages > .pagination")
            var currentPage = 1
            var totalPage = 1
            var nextUrl = ""
            if (pagination.children().size > 0) {
                currentPage = pagination.selectFirst(".current").text().toInt()
                totalPage = pagination.selectFirst(".end")?.text()?.removePrefix("...")?.toInt() ?: currentPage
                nextUrl = pagination.selectFirst(".next")?.absUrl("href") ?: ""
            }
            println(currentPage)
            println(totalPage)
            println(nextUrl)

            val list = ArrayList<Book>()
            val elementList = doc.select(".row > .col-sm-8 > .card > .card-body > ul > li")
            elementList.forEach { element ->
                val children = element.children()
                val bookUrl = children[0].absUrl("href")
                val coverUrl = ""
                val title = children[0].text()
                val author = ""
                val artist = children[1].text()
                val status = children[3].text()
                val intro = children[2].text()
                list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                    this.status = status
                    this.intro = intro
                })
            }
            return@fromCallable Category(list, currentPage, totalPage, url, nextUrl)
        }
    }
}