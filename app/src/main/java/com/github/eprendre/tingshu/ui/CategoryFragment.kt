package com.github.eprendre.tingshu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
import kotlinx.android.synthetic.main.fragment_category.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class CategoryFragment : Fragment(), AnkoLogger {
    var categoryUrl = ""
    private val compositeDisposable = CompositeDisposable()
    private var currentPage = 1
    private var totalPage = 1
    private var nextUrl = ""
    private lateinit var oldList: ArrayList<Book>
    private lateinit var scrollListener: EndlessRecyclerViewScrollListener

    private val listAdapter by lazy {
        CategoryAdapter {
            //这里数据都一样的，可以共用SearchAdapter
            Prefs.currentCover = it.coverUrl
            Prefs.currentBookName = it.title
            Prefs.artist = it.artist
            Prefs.author = it.author
            Prefs.addToHistory(it)
            activity?.startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to it.bookUrl)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryUrl = arguments?.getString(ARG_CATEGORY_URL) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_category, container, false)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        state_layout.setErrorText("加载出错了，点击重试")
        state_layout.setErrorListener {
            fetch(categoryUrl)
        }
        val linearLayoutManager = LinearLayoutManager(context)
        recycler_view.layoutManager = linearLayoutManager
        recycler_view.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recycler_view.adapter = listAdapter
        scrollListener = object : EndlessRecyclerViewScrollListener(linearLayoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                if (currentPage == totalPage) {
                    return
                }
                fetch(nextUrl)
            }
        }
        recycler_view.addOnScrollListener(scrollListener)
        swiperefresh_layout.setColorSchemeResources(R.color.colorAccent)
        swiperefresh_layout.setOnRefreshListener {
            fetch(categoryUrl)
        }
        fetch(categoryUrl)
    }

    private fun fetch(url: String) {
        TingShuSourceHandler.getCategoryDetail(url)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                if (categoryUrl == url && !swiperefresh_layout.isRefreshing) {
                    state_layout.showLoading()
                }
            }
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = {
                state_layout.showContent()
                swiperefresh_layout.isRefreshing = false
                currentPage = it.currentPage
                totalPage = it.totalPage
                nextUrl = it.nextUrl
                val newList = ArrayList<Book>()
                if (it.currentUrl != categoryUrl) {//如果不是刷新则先添加老数据
                    newList.addAll(oldList)
                }
                newList.addAll(it.list)
                listAdapter.submitList(newList)
                oldList = newList
            }, onError = {
                it.printStackTrace()
                swiperefresh_layout.isRefreshing = false
                if (categoryUrl == url) {
                    state_layout.showError()
                } else {
                    context?.toast("加载下一页出错啦")
                }
            })
            .addTo(compositeDisposable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }

    companion object {
        private const val ARG_CATEGORY_URL = "category_url"

        @JvmStatic
        fun newInstance(categoryUrl: String): CategoryFragment {
            return CategoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_URL, categoryUrl)
                }
            }
        }
    }
}