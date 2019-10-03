package com.github.eprendre.tingshu

import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.startsWith
import assertk.fail
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import org.jsoup.Jsoup
import org.junit.Test
import java.net.URLEncoder

/**
 * 静听网测试
 */
class JingTingUnitTest {

    /**
     * 测试获取播放地址
     */
    @Test
    fun audioUrl() {
        val doc = Jsoup.connect("http://m.audio699.com/book/1555/1.html").get()
        val url = doc.getElementsByTag("source")
            .first()
            .attr("src")

        if (url == null) {
            println("null")
            fail("获取出错")
        } else {
            println(url)
            assertThat(url).startsWith("http")
        }
    }

    @Test
    fun search() {
        val keywords = "仙"
        val encodedKeywords = URLEncoder.encode(keywords, "utf-8")
        val url = "http://m.audio699.com/search?keyword=$encodedKeywords"
        val doc = Jsoup.connect(url).get()
        val totalPage = 1
        val list = ArrayList<Book>()
        val elementList = doc.select(".category .clist a")
        elementList.forEach { element ->
            val bookUrl = element.attr("href")
            val coverUrl = element.selectFirst("img").attr("src")
            val title = element.selectFirst("h3").text()
            val (author, artist, status) = element.select("p").map { it.text() }.let {
                Triple(it[0], it[1], it[2])
            }
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.status = status })
        }
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    @Test
    fun bookDetail() {
        val doc = Jsoup.connect("http://m.audio699.com/book/1555.html").get()

        val episodes = doc.select(".plist a").map {
            Episode(it.text(), it.attr("abs:href"))
        }

        episodes.take(10).forEach { println(it) }
        assertThat(episodes.size).isGreaterThan(0)
    }

    @Test
    fun category() {
        val url = "http://m.audio699.com/list/2_1.html"
        val doc = Jsoup.connect(url).get()
        val currentPage = extractPage(url)

        val pageButtons = doc.select(".cpage > a")
        val totalPage = extractPage(pageButtons.last().attr("href"))
        println("$currentPage/$totalPage")
        val nextUrl = pageButtons[2].attr("href")
        println("nextUrl: $nextUrl")

        val list = ArrayList<Book>()
        val elementList = doc.select(".clist > a")
        elementList.forEach { element ->
            val bookUrl = element.attr("href")
            val coverUrl = element.selectFirst("img").attr("src")
            val title = element.selectFirst("h3").text()
            val (author, artist, status) = element.select("p").map { it.text() }.let {
                Triple(it[0], it[1], it[2])
            }
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.status = status })
        }
        list.take(5).forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    private fun extractPage(url: String): String {
        return "http://m.audio699.com/list/\\d+_(\\d+).html".toRegex().find(url)!!.groupValues[1]
    }
}