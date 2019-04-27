package com.github.eprendre.tingshu.ui

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.EndlessRecyclerViewScrollListener
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
    private var currentPage = 1
    private var totalPage = 1
    private var keywords = ""
    private lateinit var oldList: ArrayList<Book>
    private lateinit var scrollListener: EndlessRecyclerViewScrollListener

    private val listAdapter = SearchAdapter {
        Prefs.currentBookUrl = it.bookUrl
        Prefs.currentCover = it.coverUrl
        Prefs.currentBookName = it.title
        Prefs.artist = it.artist
        Prefs.author = it.author
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

        val linearLayoutManager = LinearLayoutManager(this)
        recycler_view.layoutManager = linearLayoutManager
        recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler_view.adapter = listAdapter
        scrollListener = object : EndlessRecyclerViewScrollListener(linearLayoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                if (currentPage == totalPage) {
                    return
                }
                search(page)
            }
        }
        recycler_view.addOnScrollListener(scrollListener)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)

        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.isIconified = false
        searchView.queryHint = "搜索"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query.isNotBlank()) {
                    keywords = query
                    supportActionBar?.title = keywords
                    search()
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

    private fun search(page: Int = 1) {
        Single.fromCallable {
            val url = "http://m.ting56.com/search.asp?searchword=${URLEncoder.encode(keywords, "gb2312")}&page=$page"
            val list = ArrayList<Book>()
            val doc = Jsoup.connect(url).get()
            val container = doc.selectFirst(".xsdz")
            container.getElementById("page_num1").text().split("/").let {
                currentPage = it[0].toInt()
                totalPage = it[1].toInt()
            }
            val elementList = container.getElementsByClass("list-ov-tw")
            elementList.forEach { item ->
                val coverUrl = item.selectFirst(".list-ov-t a img").attr("original")
                val ov = item.selectFirst(".list-ov-w")
                val bookUrl = "http://m.ting56.com${ov.selectFirst(".bt a").attr("href")}"
                val title = ov.selectFirst(".bt a").text()
                val (author, artist) = ov.select(".zz").let { element ->
                    Pair(element[0].text(), element[1].text())
                }
                val intro = ov.selectFirst(".nr").text()
                list.add(Book(coverUrl, bookUrl, title, author, artist, intro))
            }
            return@fromCallable list
        }
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                if (page == 1) {
                    state_layout.showLoading()
                }
            }
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .retry(3)
            .subscribeBy(onSuccess = {
                val newList = ArrayList<Book>()
                if (page == 1) {
                    scrollListener.resetState()
                } else {
                    newList.addAll(oldList)
                }
                newList.addAll(it)
                oldList = newList
                listAdapter.submitList(newList)//diff 需要 new 一个 List 进去才会比较
                if (newList.isEmpty()) {
                    state_layout.showEmpty()
                } else {
                    state_layout.showContent()
                }
            }, onError = {
                state_layout.showError()
            })
            .addTo(compositeDisposable)

    }
}
