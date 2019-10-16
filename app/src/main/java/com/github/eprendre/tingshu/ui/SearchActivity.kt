package com.github.eprendre.tingshu.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.ui.adapters.SearchPagerAdapter
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.MySuggestionProvider
import kotlinx.android.synthetic.main.activity_search.*
import org.jetbrains.anko.AnkoLogger

class SearchActivity : AppCompatActivity(), AnkoLogger {
    private var keywords = ""
    private var lastTabIndex = 0
    private lateinit var searchPagerAdapter: SearchPagerAdapter
   val sources by lazy {
        val selectedSources = Prefs.selectedSources
        var sourceList = TingShuSourceHandler.sourceList
        if (selectedSources != null) {
            sourceList = selectedSources.map {  value ->
                sourceList.first { it.first == value }
            }
        }
        return@lazy sourceList
    }
    private val titles by lazy {
        sources.map { App.getSourceTitle(it.first) }
    }
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (Prefs.currentTheme) {
            0 -> setTheme(R.style.AppTheme)
            1 -> setTheme(R.style.DarkTheme)
            2 -> setTheme(R.style.BlueTheme)
        }
        setContentView(R.layout.activity_search)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        lastTabIndex = App.sourceValues.indexOfFirst { it == Prefs.source }
        if (lastTabIndex == -1) {
            lastTabIndex = 0
        }
        tabs.setupWithViewPager(view_pager)
        initPagerAdapter()
    }

    override fun onResume() {
        super.onResume()
        searchView?.clearFocus()
    }

    private fun initPagerAdapter() {
        searchPagerAdapter = SearchPagerAdapter(this, supportFragmentManager)
        searchPagerAdapter.titles = titles
        searchPagerAdapter.keywords = keywords
        view_pager.adapter = searchPagerAdapter
        view_pager.currentItem = lastTabIndex
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) {
            return
        }
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                SearchRecentSuggestions(this, MySuggestionProvider.AUTHORITY, MySuggestionProvider.MODE)
                    .saveRecentQuery(query, null)
                keywords = query
                supportActionBar?.title = keywords
                lastTabIndex = tabs.selectedTabPosition
                initPagerAdapter()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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
//        searchView.isIconified = false
//        searchView.queryHint = "搜索"
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                if (query != null && query.isNotBlank()) {
//                    keywords = query
//                    supportActionBar?.title = keywords
//                    search()
//                    searchView.setQuery("", false)
//                    searchView.clearFocus()
//                    searchView.isIconified = true
//                }
//                return true
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                return true
//            }
//        })
        return true
    }
}
