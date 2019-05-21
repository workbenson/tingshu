package com.github.eprendre.tingshu

import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.startsWith
import assertk.fail
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import org.jsoup.Jsoup
import org.junit.Test
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * 幻听网爬虫测试
 */
class HuanTingUnitTest {

    /**
     * 测试获取播放地址
     */
    @Test
    fun audioUrl() {
        val doc = Jsoup.connect("http://m.ting89.com/playbook/?15235-0-0.html").get()
        val result = doc.getElementsByTag("script")
            .first { !it.hasAttr("src") && !it.hasAttr("type") }
            .html()
            .let {
                Regex("datas=\\(\"(.*)\"\\.split")
                    .find(it)?.groupValues?.get(1)
            }
        if (result == null) {
            println("null")
            fail("获取出错")
        } else {
            val url = URLDecoder.decode(result, "gb2312").split("&")[0]
            println(url)
            assertThat(url).startsWith("http")
        }
    }

    /**
     * 测试搜索
     */
    @Test
    fun search() {
        val keywords = "仙"
        val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
        val url = "http://m.ting89.com/search.asp?searchword=$encodedKeywords&page=1"
        val doc = Jsoup.connect(url).get()

        val totalPage = doc.selectFirst(".page").ownText().split("/")[1]
        println("总页数: $totalPage")

        val list = ArrayList<Book>()
        val elementList = doc.select("#cateList_wap .bookbox")
        elementList.forEach { element ->
            val coverUrl = element.selectFirst(".bookimg img").attr("orgsrc")
            val bookUrl = "${TingShuSourceHandler.SOURCE_URL_HUANTINGWANG}/book/?${element.attr("bookid")}.html"
            val bookInfo = element.selectFirst(".bookinfo")
            val title = bookInfo.selectFirst(".bookname").text()
            val (author, artist) = bookInfo.selectFirst(".author").text().split(" ").let {
                Pair(it[0], it[1])
            }
            val intro = bookInfo.selectFirst(".intro_line").text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro})
        }
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    /**
     * 书籍详细
     */
    @Test
    fun bookDetail() {
        val doc = Jsoup.connect("http://m.ting89.com/book/?14790.html").get()

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
        val doc = Jsoup.connect("http://m.ting89.com/booklist/?2-2.html").get()
        val nextUrl = doc.select(".page a").first { it.text().contains("下页") }.attr("abs:href")
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
            val coverUrl = element.selectFirst(".bookimg img").attr("orgsrc")
            val bookUrl = "${TingShuSourceHandler.SOURCE_URL_HUANTINGWANG}/book/?${element.attr("bookid")}.html"
            val bookInfo = element.selectFirst(".bookinfo")
            val title = bookInfo.selectFirst(".bookname").text()
            val (author, artist) = bookInfo.selectFirst(".author").text().split(" ").let {
                Pair(it[0], it[1])
            }
            val intro = bookInfo.selectFirst(".intro_line").text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro})
        }
        list.take(5).forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }
}
