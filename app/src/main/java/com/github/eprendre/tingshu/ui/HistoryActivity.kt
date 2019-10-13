package com.github.eprendre.tingshu.ui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.ui.adapters.FavoriteAdapter
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_favorite.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.startActivity

class HistoryActivity : AppCompatActivity(), AnkoLogger {
    private val compositeDisposable = CompositeDisposable()
    private val onItemClickListener: (Book) -> Unit = { book ->
        Prefs.currentBook?.apply { RxBus.post(RxEvent.StorePositionEvent(this)) }
        Prefs.currentBook = book
        Prefs.addToHistory(book)
        startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to book.bookUrl)
    }

    private val onItemLongClickListener: (Book) -> Unit = { book ->
        AlertDialog.Builder(this)
            .setTitle("是否删除本条记录?")
            .setPositiveButton("是") { dialog, which ->
                Prefs.historyList = Prefs.historyList.apply { remove(book) }
                loadData()
            }
            .setNegativeButton("否", null)
            .show()
    }

    private val listAdapter by lazy {
        FavoriteAdapter(onItemClickListener, onItemLongClickListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (Prefs.currentTheme) {
            0 -> setTheme(R.style.AppTheme)
            1 -> setTheme(R.style.DarkTheme)
            2 -> setTheme(R.style.BlueTheme)
        }

        setContentView(R.layout.activity_favorite)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        state_layout.setEmptyText("暂无浏览记录")
        state_layout.setErrorText("加载出错啦")
        val linearLayoutManager = LinearLayoutManager(this)
        recycler_view.layoutManager = linearLayoutManager
        recycler_view.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        recycler_view.adapter = listAdapter
        listAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                linearLayoutManager.scrollToPositionWithOffset(0, 0)
            }
        })
        loadData()
    }

    private fun loadData() {
        compositeDisposable.clear()
        Single.fromCallable {
            return@fromCallable Prefs.historyList
        }
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                state_layout.showLoading()
            }
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = {
                if (it.isEmpty()) {
                    state_layout.showEmpty()
                } else {
                    state_layout.showContent()
                    listAdapter.submitList(it)
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}