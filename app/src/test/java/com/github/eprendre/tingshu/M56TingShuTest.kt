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

class M56TingShuTest {

    @Test
    fun fetchCategory() {
        val doc = Jsoup.connect("http://m.ting56.com/mulu.html").testConfig().get()
        val box = doc.select(".chan_box")
        box.forEachWithIndex { i, element ->
            val sb = StringBuilder()
            sb.append("val menu${i+1} = CategoryMenu(")
            val title = element.selectFirst("h2").text()
            sb.append("\"$title\", R.drawable.ic_library_books, View.generateViewId(), listOf(\n")

            val list = element.select(".pd_class > li > a").map { a ->
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