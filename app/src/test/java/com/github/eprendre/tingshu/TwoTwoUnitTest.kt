package com.github.eprendre.tingshu

import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import org.jsoup.Jsoup
import org.junit.Test
import java.net.URLEncoder

/**
 * 22听书网爬虫测试
 */
class TwoTwoUnitTest {

    /**
     * 测试搜索
     */
    @Test
    fun search() {
        val keywords = "仙"
        val encodedKeywords = URLEncoder.encode(keywords, "utf-8")
        val page = 6
        val url = "https://m.ting22.com/search.php?q=$encodedKeywords&page=$page"
        val doc = Jsoup.connect(url).get()

        val nextPageUrl = doc.selectFirst("#more-wrapper a").attr("href")
        val totalPage = if (nextPageUrl.contains("javascript")) page else page + 1
        println(totalPage)

        val list = ArrayList<Book>()
        val elementList = doc.select(".card")
        elementList.forEach { element ->
            val coverUrl = element.selectFirst(".pic img").absUrl("src")
            val bookUrl = element.selectFirst(".link").absUrl("href")
            val title = element.selectFirst(".title").text()
            val infos = element.select(".con div")
            val author = infos[0].text()
            val artist = infos[2].text()
            val intro = infos[4].text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro })
        }
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    /**
     * 书籍详细
     */
    @Test
    fun bookDetail() {
        val doc = Jsoup.connect("https://m.ting22.com/book/213.html").get()

        val episodes = doc.select("#playlist li a").map {
            Episode(it.text(), it.attr("abs:href"))
        }
        val intro = doc.selectFirst(".bookintro").text()

        episodes.take(10).forEach { println(it) }
        assertThat(episodes.size).isGreaterThan(0)
    }

    /**
     * 分类
     */
    @Test
    fun category() {
        val doc = Jsoup.connect("https://m.ting22.com/yousheng/").get()
        val nextUrl = doc.select(".page")[1].select("a")[1].attr("abs:href")
        val pages = doc.select(".page")[1].ownText().let { text ->
            Regex("(\\d+)/(\\d+)").find(text)!!.groupValues
        }
        val currentPage = pages[1].toInt()
        val totalPage = pages[2].toInt()

        println("$currentPage/$totalPage")
        println("nextUrl: $nextUrl")

        val list = ArrayList<Book>()
        val elementList = doc.select(".bookbox")
        elementList.forEach { element ->
            val coverUrl = element.selectFirst(".bookimg img").absUrl("src")
            val bookUrl = element.absUrl("href")
            val bookInfo = element.selectFirst(".bookinfo")
            val title = bookInfo.selectFirst(".bookname").text()
            val (author, artist) = bookInfo.selectFirst(".author").text().split(" ").let {
                Pair(it[0], it[1])
            }
            val intro = bookInfo.selectFirst(".intro_line").text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro })
        }
        list.take(5).forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }
}
