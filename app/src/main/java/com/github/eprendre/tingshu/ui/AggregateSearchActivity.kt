package com.github.eprendre.tingshu.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.ui.adapters.SearchAdapter
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.MySuggestionProvider
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_aggregate_search.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class AggregateSearchActivity : AppCompatActivity(), AnkoLogger {
    private val compositeDisposable = CompositeDisposable()
    private var searchView: SearchView? = null
    private val listAdapter by lazy {
        SearchAdapter { book ->
            if (book.coverUrl.isBlank() && book.bookUrl.startsWith(TingShuSourceHandler.SOURCE_URL_TINGCHINA)) {
                toast("本条目加载中，请稍后...")
                return@SearchAdapter
            }
            Prefs.currentBook?.apply { RxBus.post(RxEvent.StorePositionEvent(this)) }
            if (Prefs.currentBook == null || Prefs.currentBook!!.bookUrl != book.bookUrl) {
                App.findBookInHistoryOrFav(book) {
                    Prefs.currentBook = it
                    Prefs.addToHistory(it)
                    startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to it.bookUrl)
                }
            } else {
                startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to book.bookUrl)
            }
        }
    }
    private val bookList: ArrayList<Book> by lazy { ArrayList<Book>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (Prefs.currentTheme) {
            0 -> setTheme(R.style.AppTheme)
            1 -> setTheme(R.style.DarkTheme)
            2 -> setTheme(R.style.BlueTheme)
        }
        setContentView(R.layout.activity_aggregate_search)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        state_layout.setErrorText("搜索出错啦")
        state_layout.setEmptyText("暂无搜索结果")
        state_layout.showEmpty()

        val linearLayoutManager = LinearLayoutManager(this)
        recycler_view.layoutManager = linearLayoutManager
        recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler_view.adapter = listAdapter
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) {
            return
        }
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                SearchRecentSuggestions(
                    this,
                    MySuggestionProvider.AUTHORITY,
                    MySuggestionProvider.MODE
                )
                    .saveRecentQuery(query, null)
                search(query)
                supportActionBar?.title = query
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView?.let {
            it.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            it.setIconifiedByDefault(false) // Do not iconify the widget; expand it by default
            it.isSubmitButtonEnabled = true
            it.isQueryRefinementEnabled = true
        }
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        searchView?.clearFocus()
    }

    private fun search(keywords: String) {
        compositeDisposable.clear()
        bookList.clear()
        listAdapter.submitList(ArrayList<Book>())
        state_layout.showLoading()
        val selectedSources = Prefs.selectedSources
        var sourceList = TingShuSourceHandler.sourceList
        if (selectedSources != null) {
            sourceList = selectedSources.map {  value ->
                sourceList.first { it.first == value }
            }
        }

        sourceList.forEach { pair ->
            pair.second
                .search(keywords, 1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = { pair ->
                    if (Prefs.isAccurateSearch) {
                        bookList.addAll(pair.first.filter { it.title.contains(keywords, true) })
                    } else {
                        bookList.addAll(pair.first)
                    }
                    listAdapter.submitList(ArrayList<Book>().apply {
                        addAll(bookList)
                    })
                    if (bookList.isEmpty()) {
                        state_layout.showEmpty()
                    } else {
                        state_layout.showContent()
                    }
                }, onError = {
                    it.printStackTrace()
                })
                .addTo(compositeDisposable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        listAdapter.clear()
    }
}