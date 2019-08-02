package com.github.eprendre.tingshu.ui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.db.AppDatabase
import com.github.eprendre.tingshu.ui.adapters.FavoriteAdapter
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Prefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_favorite.*
import org.jetbrains.anko.startActivity

class FavoriteActivity : AppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()
    private val onItemClickListener: (Book) -> Unit = {
        Prefs.currentCover = it.coverUrl
        Prefs.currentBookName = it.title
        Prefs.artist = it.artist
        Prefs.author = it.author
        Prefs.currentEpisodeName = it.currentEpisodeName
        Prefs.currentEpisodePosition = it.currentEpisodePosition
        Prefs.currentEpisodeUrl = it.currentEpisodeUrl
        startActivity<PlayerActivity>(PlayerActivity.ARG_BOOKURL to it.bookUrl)
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
        setContentView(R.layout.activity_favorite)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        initViews()
        AppDatabase.getInstance(this)
            .bookDao()
            .loadAllBooks()
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
                    listAdapter.submitList(it)
                }
            }, onError = {
                it.printStackTrace()
                state_layout.showError()
            })
            .addTo(compositeDisposable)
    }

    private fun initViews() {
        state_layout.setEmptyText("暂无收藏")
        state_layout.setErrorText("加载出错啦")
        val linearLayoutManager = LinearLayoutManager(this)
        recycler_view.layoutManager = linearLayoutManager
        recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler_view.adapter = listAdapter
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }
}
