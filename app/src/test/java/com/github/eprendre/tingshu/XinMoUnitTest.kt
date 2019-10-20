package com.github.eprendre.tingshu

import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.startsWith
import assertk.fail
import com.github.eprendre.tingshu.extensions.testConfig
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import io.reactivex.Completable
import org.jsoup.Jsoup
import org.junit.Test
import java.net.URLEncoder

/**
 * 静听网测试
 */
class XinMoUnitTest {

    /**
     * 测试获取播放地址
     */
    @Test
    fun audioUrl() {
        val doc = Jsoup.connect("http://m.ixinmo.com/shu/1545/1.html").testConfig().get()
        val url = doc.selectFirst("audio > source").attr("src")
        println(url)
    }

    @Test
    fun search() {
        val keywords = "仙"
        val url = "http://m.ixinmo.com/search.html"
        val doc = Jsoup.connect(url).data("searchword", keywords).post()
        val totalPage = 1
        val list = ArrayList<Book>()
        val elementList = doc.select(".xxzx > .list-ov-tw")
        elementList.forEach { element ->
            val bookUrl = element.selectFirst(".list-ov-t > a").absUrl("href")
            val coverUrl = element.selectFirst(".list-ov-t > a > img").absUrl("src")
            val a = element.selectFirst(".list-ov-w > a")
            val title = a.selectFirst(".bt").text()
            val (author, artist) = a.select(".zz").let {
                Pair(it[0].text(), it[1].text())
            }
            val intro = a.selectFirst(".nr").text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                this.intro = intro
            })
        }
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    @Test
    fun bookDetail() {
        val doc = Jsoup.connect("http://m.ixinmo.com/shu/1549.html").testConfig().get()

        val episodes = doc.select("#playlist > ul > li > a").map {
            Episode(it.text(), it.absUrl("href"))
        }

        val intro = doc.selectFirst(".book_intro").text()
        episodes.take(10).forEach { println(it) }
        assertThat(episodes.size).isGreaterThan(0)
    }

    @Test
    fun category() {
        val url = "http://m.ixinmo.com/mulu/2_1.html"
        val doc = Jsoup.connect(url).testConfig().get()
        val (currentPage, totalPage) = doc.selectFirst("#page_num1").text().split("/").let {
            Pair(it[0].toInt(), it[1].toInt())
        }
        val nextUrl = doc.selectFirst("#page_next1").absUrl("href")
        println("$currentPage / $totalPage")
        println(nextUrl)

        val list = ArrayList<Book>()
        val elementList = doc.select(".xsdz > .list-ov-tw")
        elementList.forEach { element ->
            val bookUrl = element.selectFirst(".list-ov-t > a").absUrl("href")
            val coverUrl = element.selectFirst(".list-ov-t > a > img").absUrl("src")
            val a = element.selectFirst(".list-ov-w > a")
            val title = a.selectFirst(".bt").text()
            val (author, artist) = a.select(".zz").let {
                Pair(it[0].text(), it[1].text())
            }
            val intro = a.selectFirst(".nr").text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                this.intro = intro
            })
        }
        list.take(5).forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }
}