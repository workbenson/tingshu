package com.github.eprendre.tingshu

import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.github.eprendre.tingshu.extensions.testConfig
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import org.jsoup.Jsoup
import org.junit.Test
import java.net.URLEncoder

/**
 * 有声小说吧测试
 */
class YouShengUnitTest {

    fun audioUrl() {
        "document.getElementById(\"xplayer\").contentDocument.getElementById(\"viframe\").contentDocument.documentElement.innerHTML"
    }

    /**
     * 测试搜索
     */
    @Test
    fun search() {
        val keywords = "凡人"

        val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
        val url = "http://m.ysxs8.com/search.asp?searchword=$encodedKeywords&page=1"
        val doc = Jsoup.connect(url).testConfig().get()

        val totalPage = doc.selectFirst(".page").ownText().split("/")[1].toInt()
        println(totalPage)

        val list = ArrayList<Book>()
        val elementList = doc.select("#cateList_wap > .bookbox")
        elementList.forEach { element ->
            val coverUrl = element.selectFirst(".bookimg > img").attr("orgsrc")
            val title = element.selectFirst(".bookname").text()
            val bookId = element.attr("id")
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
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    /**
     * 书籍详情页，用来获取章节列表
     */
    @Test
    fun bookDetail() {
        val url = "http://m.ysxs8.com/downlist/11359.html"

        val doc = Jsoup.connect(url).testConfig().get()

        val episodes = doc.select("#playlist > ul > li > a").map {
            Episode(it.text(), it.attr("abs:href"))
        }

        val intro = doc.selectFirst(".book_intro")
        println(intro)

        episodes.take(10).forEach { println(it) }
        assertThat(episodes.size).isGreaterThan(0)
    }

    /**
     * 分类
     */
    @Test
    fun category() {
        val url = "http://m.ysxs8.com/downlist/r52_1.html"
        val doc = Jsoup.connect(url).testConfig().get()

        val pageElement = doc.selectFirst(".page")
        val nextUrl = pageElement.selectFirst("a").absUrl("href")

        val (currentPage, totalPage) = pageElement.ownText().replace("页次 ", "").split("/").let {
            Pair(it[0].toInt(), it[1].toInt())
        }

        println(nextUrl)
        println("$currentPage / $totalPage")

        val list = ArrayList<Book>()
        val elementList = doc.select("#cateList_wap > .bookbox")
        elementList.forEach { element ->
            val coverUrl = element.selectFirst(".bookimg > img").attr("orgsrc")
            val title = element.selectFirst(".bookname").text()
            val bookId = element.attr("id")
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
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    @Test
    fun fetchCategory() {
        val doc = Jsoup.connect("http://m.ysxs8.com/category.html").testConfig().get()
        val navs = doc.select(".cat_tit > a")
        val sb = StringBuilder()

        val list = navs.map { a ->
            val href = a.absUrl("href")
            val text = a.text()
            return@map "CategoryTab(\"$text\", \"$href\")"
        }.joinToString(",\n")

        sb.append(list)
        println(sb.toString())
    }
}