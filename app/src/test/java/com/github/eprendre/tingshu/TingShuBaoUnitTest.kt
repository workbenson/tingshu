package com.github.eprendre.tingshu

import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import org.jsoup.Jsoup
import org.junit.Test
import java.net.URLEncoder

/**
 * 听书宝测试
 * 音频地址需要用到 webview 这里不方便测试
 */
class TingShuBaoUnitTest {

    /**
     * 测试搜索
     */
    @Test
    fun search() {
        val keywords = "仙"

        val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
        val url = "https://www.tingshubao.com/search.asp?page=11&searchword=$encodedKeywords&searchtype=-1"
        val doc = Jsoup.connect(url).get()

        val pages = doc.selectFirst(".fanye").children().map { it.ownText() }.filter { it.matches("\\d+".toRegex()) }
        val totalPage = pages.last().toInt()

        val list = ArrayList<Book>()
        val elementList = doc.select(".list-works li")
        elementList.forEach { element ->
            val coverUrl = element.selectFirst(".list-imgbox img").absUrl("data-original")
            val titleElement = element.selectFirst(".list-book-dt a")
            val bookUrl = titleElement.absUrl("href")
            val title = titleElement.ownText()
            val (author, artist) = element.select(".list-book-cs .book-author").let {
                Pair(it[0].ownText(), it[1].ownText())
            }
            val intro = element.selectFirst(".list-book-des").text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro })
        }
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    /**
     * 书籍详情页，用来获取章节列表
     */
    @Test
    fun bookDetail() {
        val url = "https://www.tingshubao.com/book/24.html"

        val doc = Jsoup.connect(url).get()

        val episodes = doc.select("#playlist li a").map {
            Episode(it.text(), it.attr("abs:href"))
        }

        episodes.take(10).forEach { println(it) }
        assertThat(episodes.size).isGreaterThan(0)
    }

    /**
     * 分类
     */
    @Test
    fun category() {
        val url = "https://www.tingshubao.com/list/27.html"
        val doc = Jsoup.connect(url).get()

        val nextUrl = doc.select(".fanye a").firstOrNull { it.text().contains("下一页") }?.attr("abs:href") ?: ""
        val currentPage = doc.selectFirst(".fanye strong").ownText().toInt()
        val totalPage = doc.selectFirst(".fanye").children().map { it.ownText() }.filter { it.matches("\\d+".toRegex()) }.last().toInt()

        val list = ArrayList<Book>()
        val elementList = doc.select(".list-works li")
        elementList.forEach { element ->
            val coverUrl = element.selectFirst(".list-imgbox img").absUrl("data-original")
            val titleElement = element.selectFirst(".list-book-dt a")
            val bookUrl = titleElement.absUrl("href")
            val title = titleElement.ownText()
            val (author, artist) = element.select(".list-book-cs .book-author").let {
                Pair(it[0].ownText(), it[1].ownText())
            }
            val intro = element.selectFirst(".list-book-des").ownText()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro })
        }

        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }
}