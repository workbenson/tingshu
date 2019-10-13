package com.github.eprendre.tingshu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.TingShu
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.ui.adapters.SearchAdapter
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.EndlessRecyclerViewScrollListener
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_search.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class SearchFragment : Fragment(), AnkoLogger {
    private val compositeDisposable = CompositeDisposable()
    private var currentPage = 1
    private var totalPage = 1
    private var keywords = ""
    private lateinit var oldList: ArrayList<Book>
    private lateinit var scrollListener: EndlessRecyclerViewScrollListener
    private lateinit var tingShu: TingShu

    private val listAdapter by lazy {
        SearchAdapter { book ->
            if (book.coverUrl.isBlank() && book.bookUrl.startsWith(TingShuSourceHandler.SOURCE_URL_TINGCHINA)) {
                activity?.toast("本条目加载中，请稍后...")
                return@SearchAdapter
            }
            Prefs.currentBook?.apply { RxBus.post(RxEvent.StorePositionEvent(this))}
            if (Prefs.currentBook == null || Prefs.currentBook!!.bookUrl != book.bookUrl) {
                App.findBookInHistoryOrFav(book) {
                    Prefs.currentBook = it
                    Prefs.addToHistory(it)
                    activity?.startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to it.bookUrl)
                }
            } else {
                activity?.startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to book.bookUrl)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keywords = arguments?.getString(ARG_KEYWORDS) ?: ""
        val index = arguments?.getInt(ARG_INDEX) ?: 0
        tingShu = TingShuSourceHandler.sourceList[index].second
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_search, container, false)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        state_layout.setErrorText("搜索出错啦")
        state_layout.setEmptyText("暂无搜索结果")

        val linearLayoutManager = LinearLayoutManager(activity)
        recycler_view.layoutManager = linearLayoutManager
        recycler_view.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
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
        search()
    }

    private fun search(page: Int = 1) {
        if (keywords.isBlank()) {
            state_layout.showEmpty()
            return
        }
        tingShu
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

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }

    companion object {
        private const val ARG_KEYWORDS = "keywords"
        private const val ARG_INDEX = "index"

        @JvmStatic
        fun newInstance(keywords: String, index: Int): SearchFragment {
            return SearchFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_KEYWORDS, keywords)
                    putInt(ARG_INDEX, index)
                }
            }
        }
    }
}