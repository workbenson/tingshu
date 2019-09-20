package com.github.eprendre.tingshu.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.ui.adapters.SearchPagerAdapter
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.MySuggestionProvider
import kotlinx.android.synthetic.main.activity_search.*
import org.jetbrains.anko.AnkoLogger

class SearchActivity : AppCompatActivity(), AnkoLogger {
    private var keywords = ""
    private var lastTabIndex = 0
    private lateinit var searchPagerAdapter: SearchPagerAdapter
    private val sources by lazy {
        resources.getStringArray(R.array.source_entries).toList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        lastTabIndex = resources.getStringArray(R.array.source_values).indexOfFirst { it == Prefs.source }
        if (lastTabIndex == -1) {
            lastTabIndex = 0
        }
    }

    override fun onStart() {
        super.onStart()
        tabs.setupWithViewPager(view_pager)
        initPagerAdapter()
    }

    private fun initPagerAdapter() {
        searchPagerAdapter = SearchPagerAdapter(this, supportFragmentManager)
        searchPagerAdapter.sources = sources
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
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setIconifiedByDefault(false) // Do not iconify the widget; expand it by default
        searchView.isSubmitButtonEnabled = true
        searchView.isQueryRefinementEnabled = true
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
