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
class WeiAiUnitTest {

    /**
     * 测试获取播放地址
     */
    @Test
    fun audioUrl() {
        val doc = Jsoup.connect("http://www.ting199.com/bofang/?l=41&a=1").testConfig().get()
        val url = doc.selectFirst("audio").attr("src")

        println(url)
    }

    @Test
    fun search() {
        val keywords = "仙"
        val encodedKeywords = URLEncoder.encode(keywords, "utf-8")
        val url = "http://www.ting199.com/search?keyword=$encodedKeywords"
        val doc = Jsoup.connect(url).testConfig().get()
        val totalPage = 1
        val list = ArrayList<Book>()
        val elementList = doc.select(".top_list > a")
        elementList.forEach { element ->
            val bookUrl = element.absUrl("href")
            val coverUrl = ""
            val title = element.ownText()
            val author = ""
            val artist = element.selectFirst("span").text()
            list.add(Book(coverUrl, bookUrl, title, author, artist))
        }
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    @Test
    fun fetchBookInfo() {
//        return Completable.fromCallable {
            val doc = Jsoup.connect("http://www.ting199.com/list/?l=1188").testConfig().get()
            val coverUrl = doc.selectFirst(".booksite > .bookimg > img").absUrl("src")
            val infos = doc.selectFirst(".booksite > .bookinfo > .info").children()
            val artist = infos[0].text()
            val status = infos[1].text()
            val intro = doc.selectFirst(".book_intro").text()
//            book.coverUrl = coverUrl
//            book.artist = artist
//            book.status = status
//            book.intro = intro
//            return@fromCallable null
//        }
    }

    @Test
    fun bookDetail() {
        val doc = Jsoup.connect("http://www.ting199.com/list/?l=1188").testConfig().get()

        val episodes = doc.select("#playlist > ul > li > a").map {
            Episode(it.text(), it.absUrl("href"))
        }

        val intro = doc.selectFirst(".book_intro").text()
        episodes.take(10).forEach { println(it) }
        assertThat(episodes.size).isGreaterThan(0)
    }

    @Test
    fun category() {
        val url = "http://www.ting199.com/"
        val doc = Jsoup.connect(url).testConfig().get()
        val currentPage = 1
        val totalPage = 1

        val list = ArrayList<Book>()
        val elementList = doc.select(".top_list > a")
        elementList.forEach { element ->
            val bookUrl = element.absUrl("href")
            val coverUrl = ""
            val title = element.ownText()
            val author = ""
            val artist = element.selectFirst("span").text()
            list.add(Book(coverUrl, bookUrl, title, author, artist))
        }
        list.take(5).forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }
}