package com.github.eprendre.tingshu

import androidx.core.text.isDigitsOnly
import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.github.eprendre.tingshu.extensions.testConfig
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import org.jetbrains.anko.collections.forEachWithIndex
import org.jsoup.Jsoup
import org.junit.Test
import java.net.URLEncoder

/**
 * 恋听网爬虫测试
 */
class LianTingUnitTest {

    /**
     * 测试搜索
     */
    @Test
    fun search() {
        val keywords = "全职"
        val encodedKeywords = URLEncoder.encode(keywords, "utf-8")
        val page = 1
        val url = "https://ting55.com/search/$encodedKeywords/page/$page"
        val doc = Jsoup.connect(url).testConfig().get()

        val pages = doc.selectFirst(".c-page").children()
        var totalPage = 1
        if (pages.size > 0) {
            totalPage = when (val lastPage = pages.last().ownText()) {
                "下一页" -> pages[pages.size - 2].ownText().toInt()
                "末页" -> pages[pages.size - 3].ownText().toInt()
                else -> lastPage.toInt()
            }
        }

        println(totalPage)

        val list = ArrayList<Book>()
        val elementList = doc.select(".category-list > ul > li")
        elementList.forEach { element ->
            val coverUrl = element.selectFirst(".img > a > img").absUrl("src")
            val bookUrl = element.selectFirst(".info > h4 > a").absUrl("href")
            val title = element.selectFirst(".info > h4 > a").text()
            val infos = element.select(".info > p")
            val author = infos[0].text()
            val artist = infos[1].text()
            val status = infos[3].text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.status = status })
        }
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    /**
     * 书籍详细
     */
    @Test
    fun bookDetail() {
        val doc = Jsoup.connect("https://ting55.com/book/14020").testConfig().get()

        val episodes = doc.select(".playlist > .plist > ul > li > a").map {
            Episode(it.text(), it.attr("abs:href")).apply {
                this.isFree = it.hasClass("free")
            }
        }
        val intro = doc.selectFirst(".intro").text()
        println(intro)

        episodes.take(20).forEach { println(it) }
        assertThat(episodes.size).isGreaterThan(0)
    }

    /**
     * 分类
     */
    @Test
    fun category() {
        val doc = Jsoup.connect("https://ting55.com/category/1/page/67").testConfig().get()

        val cPage = doc.selectFirst(".c-page")
        val currentPage = cPage.selectFirst("span").text().toInt()

        val pages = cPage.children()
        val nextUrl = pages.firstOrNull { it.ownText() == "下一页" }?.absUrl("href")
        val totalPage = when (val lastPage = pages.last().ownText()) {
            "下一页" -> pages[pages.size - 2].ownText().toInt()
            "末页" -> pages[pages.size - 3].ownText().toInt()
            else -> lastPage.toInt()
        }
        println("$currentPage/$totalPage")
        println("nextUrl: $nextUrl")

        val list = ArrayList<Book>()
        val elementList = doc.select(".category-list > ul > li")
        elementList.forEach { element ->
            val coverUrl = element.selectFirst(".img > a > img").absUrl("src")
            val bookUrl = element.selectFirst(".info > h4 > a").absUrl("href")
            val title = element.selectFirst(".info > h4 > a").text()
            val infos = element.select(".info > p")
            val author = infos[0].text()
            val artist = infos[1].text()
            val status = infos[3].text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.status = status })
        }
        list.take(5).forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    @Test
    fun fetchCategory() {
        val doc = Jsoup.connect("https://ting55.com/").testConfig().get()
        val navs = doc.select(".nav > a")
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
