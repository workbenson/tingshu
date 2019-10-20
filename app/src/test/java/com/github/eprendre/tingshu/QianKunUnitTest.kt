package com.github.eprendre.tingshu

import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.github.eprendre.tingshu.extensions.testConfig
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import org.jetbrains.anko.collections.forEachWithIndex
import org.jsoup.Jsoup
import org.junit.Test
import java.net.URLEncoder

/**
 * 乾坤听书网测试
 */
class QianKunUnitTest {
    @Test
    fun search() {
        val keywords = "仙"
        val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
        val url = "http://m.qktsw.com/search.asp?page=16&searchword=$encodedKeywords"
        val doc = Jsoup.connect(url).testConfig().get()

        val totalPage = doc.selectFirst(".page").ownText().split("/")[1]

        val list = ArrayList<Book>()
        val elementList = doc.select("#cateList_wap .bookbox")
        elementList.forEach { element ->
            val bookUrl = element.absUrl("bookid")
            val coverUrl = element.selectFirst(".bookimg img").attr("orgsrc")
            val bookinfo = element.selectFirst(".bookinfo")
            val title = bookinfo.selectFirst(".bookname").text()
            val (author, artist) = bookinfo.selectFirst(".author").text().split(" ").let {
                Pair(it[0], it[1])
            }
            val intro = bookinfo.selectFirst(".intro_line").text()
            val status = bookinfo.selectFirst(".update").text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                this.intro = intro
                this.status = status
            })
        }
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    /**
     * 书籍详细
     */
    @Test
    fun bookDetail() {
        val doc = Jsoup.connect("http://m.qktsw.com/tingshu/56179.html").testConfig().get()

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
        val doc = Jsoup.connect("http://m.qktsw.com/tingbook/1.html").testConfig().get()
        val nextUrl = doc.select(".page a").firstOrNull { it.text().contains("下页") }?.attr("abs:href") ?: ""
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
            val bookUrl = element.absUrl("bookid")
            val coverUrl = element.selectFirst(".bookimg img").attr("orgsrc")
            val bookInfo = element.selectFirst(".bookinfo")
            val title = bookInfo.selectFirst(".bookname").text()
            val (author, artist) = bookInfo.selectFirst(".author").text().split(" ").let {
                Pair(it[0], it[1])
            }
            val intro = bookInfo.selectFirst(".intro_line").text()
            val status = bookInfo.selectFirst(".update").text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                this.intro = intro
                this.status = status
            })
        }
        list.take(5).forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    @Test
    fun fetchCategory() {
        val doc = Jsoup.connect("http://m.qktsw.com/category.html").testConfig().get()
        val catbox = doc.select(".cat_box")
        catbox.forEachWithIndex { i, element ->
            val sb = StringBuilder()
            sb.append("val menu${i+1} = CategoryMenu(")
            val title = element.selectFirst(".cat_tit").text()
            sb.append("\"$title\", R.drawable.ic_library_books, View.generateViewId(), listOf(\n")

            val list = element.select(".cat_list li a").map { a ->
                val href = a.absUrl("href")
                val text = a.text()
                return@map "CategoryTab(\"$text\", \"$href\")"
            }.joinToString(",\n")

            sb.append(list)
            sb.append("\n)\n)")
            println(sb.toString())
        }
    }
}