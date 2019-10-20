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
 * https://www.pingshu365.com
 * 天天评书网 测试
 */
class TianTianPingShuTest {

    @Test
    fun search() {
        val keywords = "("
        val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
        val url = "https://www.pingshu365.com/search/1.asp?page=16&keyword=$encodedKeywords&stype="
        val doc = Jsoup.connect(url).testConfig().get()
        val pages = doc.selectFirst(".fy").ownText().let { text ->
            Regex(".+页次:(\\d+)/(\\d+).+").find(text)!!.groupValues
        }
        val currentPage = pages[1].toInt()
        val totalPage = pages[2].toInt()

        val list = ArrayList<Book>()
        val elementList = doc.select("#ss .ssl .book")
        elementList.forEach { element ->
            val coverUrl = element.selectFirst(".pic img").absUrl("src")
            val titleElement = element.selectFirst(".text p span a")
            val bookUrl = titleElement.absUrl("href")
            val title = titleElement.text()
            val lis = element.select(".text ul li")
            val author = lis[1].text()
            val artist = lis[0].text()
            val intro = lis.last().text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro })
        }
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    @Test
    fun bookDetail() {
        val url = "https://www.pingshu365.com/ps/3000.html"
        val doc = Jsoup.connect(url).testConfig().get()
        val episodes = doc.select("#playlist ul li").map {
            val a = it.selectFirst("a")
            Episode(a.text(), a.absUrl("href"))
        }
        episodes.take(10).forEach { println(it) }
        assertThat(episodes.size).isGreaterThan(0)
    }

    @Test
    fun category() {
        val url = "https://www.pingshu365.com/list/293_1.html"
        val doc = Jsoup.connect(url).testConfig().get()

        val nextUrl = doc.select("#ss .ssl .fy a").firstOrNull { it.ownText() == "下一页" }?.absUrl("href") ?: ""
        val pages = doc.selectFirst("#ss .ssl .fy").ownText().let { text ->
            Regex(".+(\\d+)/(\\d+).+").find(text)!!.groupValues
        }
        val currentPage = pages[1].toInt()
        val totalPage = pages[2].toInt()

        println("$currentPage/$totalPage")
        println("nextUrl: $nextUrl")

        val list = ArrayList<Book>()
        val elementList = doc.select("#ss .ssl .book")
        elementList.forEach { element ->
            val coverUrl = element.selectFirst(".pic img").absUrl("src")
            val titleElement = element.selectFirst(".text p span a")
            val bookUrl = titleElement.absUrl("href")
            val title = titleElement.text()
            val lis = element.select(".text ul li")
            val author = lis[1].text()
            val artist = lis[0].text()
            val intro = lis.last().text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro })
        }

        list.take(5).forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }
}