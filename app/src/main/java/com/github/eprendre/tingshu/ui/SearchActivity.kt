package com.github.eprendre.tingshu.ui

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.ui.adapters.CategoryAdapter
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.EndlessRecyclerViewScrollListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_search.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.startActivity

class SearchActivity : AppCompatActivity(), AnkoLogger {
    private val compositeDisposable = CompositeDisposable()
    private var currentPage = 1
    private var totalPage = 1
    private var keywords = ""
    private lateinit var oldList: ArrayList<Book>
    private lateinit var scrollListener: EndlessRecyclerViewScrollListener

    private val listAdapter by lazy {
        CategoryAdapter {
            //        Prefs.currentBookUrl = it.bookUrl//这个不能在这里赋值，在PlayerActivity检测后再赋值避免不必要的bug
            Prefs.currentCover = it.coverUrl
            Prefs.currentBookName = it.title
            Prefs.artist = it.artist
            Prefs.author = it.author
            startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to it.bookUrl)
        }
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
        onBackPressed()
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
        TingShuSourceHandler
            .search(keywords, page)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                if (page == 1) {
                    state_layout.showLoading()
                }
            }
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = {
                currentPage = page
                totalPage = it.second
                val newList = ArrayList<Book>()
                if (page == 1) {
                    scrollListener.resetState()
                } else {
                    newList.addAll(oldList)
                }
                newList.addAll(it.first)
                listAdapter.submitList(newList)//diff 需要 new 一个 List 进去才会比较
                oldList = newList
                if (newList.isEmpty()) {
                    state_layout.showEmpty()
                } else {
                    state_layout.showContent()
                }
            }, onError = {
                it.printStackTrace()
                state_layout.showError()
            })
            .addTo(compositeDisposable)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}
