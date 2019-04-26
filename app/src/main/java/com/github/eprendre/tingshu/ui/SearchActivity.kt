package com.github.eprendre.tingshu.ui

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.utils.Book
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_search.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.startActivity
import org.jsoup.Jsoup
import java.net.URLEncoder

class SearchActivity : AppCompatActivity(), AnkoLogger {
    private val compositeDisposable = CompositeDisposable()

    private val listAdapter = SearchAdapter {
        startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to it.bookUrl)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initViews()
    }

    private fun initViews() {
        state_layout.setErrorText("搜索出错啦")
        state_layout.setEmptyText("暂无搜索结果")

        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler_view.adapter = listAdapter

    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)

        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.isIconified = false
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query.isNotBlank()) {
                    search(query)
                    searchView.setQuery("", false)
                    searchView.clearFocus()
                    searchView.isIconified = true
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
        return true
    }

    private fun search(keywords: String) {
        supportActionBar?.title = keywords
        Single.fromCallable {
            val url = "http://m.ting56.com/search.asp?searchword=${URLEncoder.encode(keywords, "gb2312")}"
            val bookList = ArrayList<Book>()
            val doc = Jsoup.connect(url).get()
            val elementList = doc.selectFirst(".xsdz").getElementsByClass("list-ov-tw")
            elementList.forEach { item ->
                val coverUrl = item.selectFirst(".list-ov-t a img").attr("original")
                val ov = item.selectFirst(".list-ov-w")
                val bookUrl = "http://m.ting56.com${ov.selectFirst(".bt a").attr("href")}"
                val title = ov.selectFirst(".bt a").text()
                val (author, artist) = ov.select(".zz").let { element ->
                    Pair(element[0].text(), element[1].text())
                }
                val intro = ov.selectFirst(".nr").text()
                bookList.add(Book(coverUrl, bookUrl, title, author, artist, intro))
            }
            return@fromCallable bookList
        }
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { state_layout.showLoading() }
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .retry(3)
            .subscribeBy(onSuccess = {
                listAdapter.submitList(it)
                state_layout.showContent()
            }, onError = {
                state_layout.showError()
            })
            .addTo(compositeDisposable)

    }
}
