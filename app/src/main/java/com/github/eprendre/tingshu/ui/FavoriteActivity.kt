package com.github.eprendre.tingshu.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.db.AppDatabase
import com.github.eprendre.tingshu.ui.adapters.FavoriteAdapter
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import com.github.eprendre.tingshu.widget.RxBus
import com.github.eprendre.tingshu.widget.RxEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_favorite.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.startActivity
import java.text.Collator
import java.util.*
import kotlin.Comparator

class FavoriteActivity : AppCompatActivity(), AnkoLogger {
    private val compositeDisposable = CompositeDisposable()
    private val onItemClickListener: (Book) -> Unit = { book ->
        Prefs.currentBook?.apply { RxBus.post(RxEvent.StorePositionEvent(this)) }
        Prefs.currentBook = book
        Prefs.addToHistory(book)
        startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to book.bookUrl)
    }

    private val onItemLongClickListener: (Book) -> Unit = {
        AlertDialog.Builder(this)
            .setTitle("是否取消收藏?")
            .setPositiveButton("是") { dialog, which ->
                //取消收藏
                AppDatabase.getInstance(this).bookDao()
                    .deleteBooks(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onSuccess = {
                    }, onError = {
                        it.printStackTrace()
                    })
                    .addTo(compositeDisposable)
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

        state_layout.setEmptyText("暂无收藏")
        state_layout.setErrorText("加载出错啦")
        val linearLayoutManager = LinearLayoutManager(this)
        recycler_view.layoutManager = linearLayoutManager
        recycler_view.addItemDecoration(
            DividerItemDecoration(this,
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadData() {
        compositeDisposable.clear()
        AppDatabase.getInstance(this)
            .bookDao()
            .loadAllBooks(Prefs.sortType)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                state_layout.showLoading()
            }
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = {
                if (it.isEmpty()) {
                    state_layout.showEmpty()
                } else {
                    state_layout.showContent()
                    when (Prefs.sortType){
                        0, 1 -> {
                            listAdapter.submitList(it)
                        }
                        2 -> {
                            val list = it.sortedWith(SortChinese())
                            listAdapter.submitList(list)
                        }
                        3 -> {
                            val list = it.sortedWith(SortChinese()).reversed()
                            listAdapter.submitList(list)
                        }
                    }
                }
            }, onError = {
                it.printStackTrace()
                state_layout.showError()
            })
            .addTo(compositeDisposable)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.favorite_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> {
                AlertDialog.Builder(this)
                    .setSingleChoiceItems(SORT_TYPES, Prefs.sortType) { dialog, which ->
                        Prefs.sortType = which
                        loadData()
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    companion object {
        val SORT_TYPES = arrayOf(
            "按添加时间排序（正序）",
            "按添加时间排序（倒序）",
            "按标题排序（正序）",
            "按标题排序（倒序）"
        )
    }

    class SortChinese : Comparator<Book> {
        override fun compare(o1: Book, o2: Book): Int {
            val collator: Collator = Collator.getInstance(Locale.CHINA)
            return when {
                collator.compare(o1.title, o2.title) > 0 -> 1
                collator.compare(o1.title, o2.title) < 0 -> -1
                else -> 0
            }
        }
    }
}