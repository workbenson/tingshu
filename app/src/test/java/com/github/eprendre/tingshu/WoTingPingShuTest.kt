package com.github.eprendre.tingshu

import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.github.eprendre.tingshu.extensions.testConfig
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import org.jetbrains.anko.collections.forEachWithIndex
import org.jsoup.Jsoup
import org.junit.Test
import java.lang.Exception
import java.net.URLEncoder

class WoTingPingShuTest {

    @Test
    fun audioUrl() {
        val url = "https://m.5tps.com/play_m/27593_50_1_1.html"
        val src = Jsoup.connect(url).testConfig().get().getElementById("play").absUrl("src")
        val doc = Jsoup.connect(src).testConfig().get().toString()
        val audioUrl = Regex("mp3:'(.*?)'").find(doc)?.groupValues?.get(1)
        println(audioUrl)
    }

    @Test
    fun search() {
        val keywords = "修仙"
        val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
        val url = "https://m.5tps.com/so.asp?keyword=$encodedKeywords&page=1"
        val doc = Jsoup.connect(url).testConfig().get()

        try {
            val totalPage = doc.selectFirst(".booksite > .bookbutton").text().split("/")[1]
            println(totalPage)
            val list = ArrayList<Book>()
            val elementList = doc.select(".top_list > a")
            elementList.forEach { element ->
                val bookUrl = element.absUrl("href")
                val coverUrl = ""
                val title = element.ownText()
                val author = ""
                val (artist, status) = element.selectFirst(".peo").text().split("／").let {
                    Pair("播音: ${it[0]}", it[1])
                }
                list.add((Book(coverUrl, bookUrl, title, author, artist).apply {
                    this.status = status
                }))
            }
            list.forEach { println(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun bookDetail() {
        val doc = Jsoup.connect("https://m.5tps.com/m_h/27054.html").testConfig().get()

        val episodes = doc.select("#playlist > ul > li > a").map {
            Episode(it.text(), it.attr("abs:href"))
        }
        episodes.take(10).forEach { println(it) }
        assertThat(episodes.size).isGreaterThan(0)
    }

    @Test
    fun category() {
        val doc = Jsoup.connect("https://m.5tps.com/m_l/46_1.html").testConfig().get()
        val nextUrl = doc.select(".page > a").firstOrNull { it.text().contains("下页") }?.attr("abs:href") ?: ""
        val (currentPage, totalPage) = doc.selectFirst(".booksite > .bookbutton > a").text().let {
            val pages = it.split(" ")[1].split("/")
            Pair(pages[0], pages[1])
        }
        println("$currentPage/$totalPage")
        println("nextUrl: $nextUrl")

        val list = ArrayList<Book>()
        val elementList = doc.select(".top_list > a")
        elementList.forEach { element ->
            val bookUrl = element.absUrl("href")
            val coverUrl = ""
            val title = element.ownText()
            val author = ""
            val (artist, status) = element.selectFirst(".peo").text().split("／").let {
                Pair("播音: ${it[0]}", it[1])
            }
            list.add((Book(coverUrl, bookUrl, title, author, artist).apply {
                this.status = status
            }))
        }
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }

    @Test
    fun fetchCategory() {
        val doc = Jsoup.connect("https://m.5tps.com/list.html").testConfig().get()
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