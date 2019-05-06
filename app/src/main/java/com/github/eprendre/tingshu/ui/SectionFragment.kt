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
import com.github.eprendre.tingshu.ui.adapters.SearchAdapter
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.EndlessRecyclerViewScrollListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_section.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class SectionFragment : Fragment() {
    var sectionUrl = ""
    private val compositeDisposable = CompositeDisposable()
    private var currentPage = 1
    private var totalPage = 1
    private var nextUrl = ""
    private lateinit var oldList: ArrayList<Book>
    private lateinit var scrollListener: EndlessRecyclerViewScrollListener

    private val listAdapter by lazy {
        SearchAdapter {
            //这里数据都一样的，可以共用SearchAdapter
            Prefs.currentCover = it.coverUrl
            Prefs.currentBookName = it.title
            Prefs.artist = it.artist
            Prefs.author = it.author
            activity?.startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to it.bookUrl)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sectionUrl = arguments?.getString(ARG_SECTION_URL) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_section, container, false)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        state_layout.setErrorText("加载出错了，点击重试")
        state_layout.setErrorListener {
            fetch(sectionUrl)
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
        swiperefresh_layout.setOnRefreshListener {
            fetch(sectionUrl)
        }
        fetch(sectionUrl)
    }

    private fun fetch(url: String) {
        TingShuSourceHandler.getSectionDetail(url)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                if (sectionUrl == url && !swiperefresh_layout.isRefreshing) {
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
                if (it.currentUrl != sectionUrl) {//如果不是刷新则先添加老数据
                    newList.addAll(oldList)
                }
                newList.addAll(it.list)
                listAdapter.submitList(newList)
                oldList = newList
            }, onError = {
                it.printStackTrace()
                swiperefresh_layout.isRefreshing = false
                if (sectionUrl == url) {
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
        private const val ARG_SECTION_URL = "section_url"

        @JvmStatic
        fun newInstance(sectionUrl: String): SectionFragment {
            return SectionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SECTION_URL, sectionUrl)
                }
            }
        }
    }
}