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
 * 声波FM测试
 */
class ShengBoUnitTest {

    /**
     * 测试获取播放地址
     */
    @Test
    fun audioUrl() {
        val doc = Jsoup.connect("http://fm.shengbo.org/Program/283349").get()
        val url = doc.selectFirst(".program-player > audio").absUrl("src")
        println(url)
    }

    @Test
    fun search() {
        val totalPage = 1
        val list = ArrayList<Book>()
    }

    @Test
    fun bookDetail() {
        val url = "http://fm.shengbo.org/Program/283197"
        val doc = Jsoup.connect(url).get()

        val episodes = ArrayList<Episode>()

        val title = doc.selectFirst(".card-header").text()
        episodes.add(Episode(title, url))
        val intro = doc.select(".card-body > .card-text").joinToString("\n") { it.text() }
        println(intro)

    }

    @Test
    fun category() {
        val url = "http://fm.shengbo.org/Category/34"
        val doc = Jsoup.connect(url).get()
        val pagination = doc.selectFirst(".pages > .pagination")
        var currentPage = 1
        var totalPage = 1
        var nextUrl = ""
        if (pagination.children().size > 0) {
            currentPage = pagination.selectFirst(".current").text().toInt()
            totalPage = pagination.selectFirst(".end")?.text()?.removePrefix("...")?.toInt() ?: currentPage
            nextUrl = pagination.selectFirst(".next")?.absUrl("href") ?: ""
        }
        println(currentPage)
        println(totalPage)
        println(nextUrl)

        val list = ArrayList<Book>()
        val elementList = doc.select(".row > .col-sm-8 > .card > .card-body > ul > li")
        elementList.forEach { element ->
            val children = element.children()
            val bookUrl = children[0].absUrl("href")
            val coverUrl = ""
            val title = children[0].text()
            val author = ""
            val artist = children[1].text()
            val status = children[3].text()
            val intro = children[2].text()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply {
                this.status = status
                this.intro = intro
            })
        }
        list.forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }
}