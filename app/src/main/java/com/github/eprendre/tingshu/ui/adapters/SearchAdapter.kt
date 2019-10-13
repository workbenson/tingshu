package com.github.eprendre.tingshu.ui.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.room.EmptyResultSetException
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.db.AppDatabase
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.widget.GlideApp
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.item_search.view.*
import org.jetbrains.anko.toast
import org.jsoup.Jsoup


class SearchAdapter(private val itemClickListener: (Book) -> Unit) :
    ListAdapter<Book, SearchViewHolder>(Book.diffCallback) {
    private val compositeDisposable = CompositeDisposable()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search, parent, false)
        return SearchViewHolder(view, itemClickListener)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val book = getItem(position)
        holder.bind(book)

        //TingChina 的搜索页面比较特殊，需要另外异步读取一下。
        if (book.coverUrl.isBlank() && book.bookUrl.startsWith(TingShuSourceHandler.SOURCE_URL_TINGCHINA)) {
            Completable.fromCallable {
                val doc = Jsoup.connect(book.bookUrl).get()
                val book01 = doc.getElementsByClass("book01").first()
                val coverUrl = book01.selectFirst("img").absUrl("src")
                val lis = book01.select("ul li")
                val author = lis[5].text()
                val artist = lis[4].text()
                val status = lis[6].text()

                val bookInfo = doc.selectFirst(".book02").ownText()
                book.coverUrl = coverUrl
                book.author = author
                book.artist = artist
                book.intro = bookInfo
                book.status = status
                return@fromCallable null
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    notifyDataSetChanged()
                }, {
                    it.printStackTrace()
                })
                .addTo(compositeDisposable)
        }
    }

    fun clear() {
        compositeDisposable.clear()
    }
}

class SearchViewHolder(view: View, itemClickListener: (Book) -> Unit) : RecyclerView.ViewHolder(view) {
    private val titleView = view.title_text
    private val authorView = view.author_text
    private val artistView = view.artist_text
    private val introView = view.intro_text
    private val coverView = view.cover_image
    private val statusView = view.status_text
    private val sourceView = view.source_text
    var item: Book? = null

    init {
        view.setOnClickListener {
            item?.let(itemClickListener)
        }
        view.setOnLongClickListener { view ->
            if (item == null) {
                return@setOnLongClickListener true
            }
            AppDatabase.getInstance(view.context)
                .bookDao()
                .findByBookUrl(item!!.bookUrl)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = {
                }, onError = {
                    if (it is EmptyResultSetException) {//这个代表数据库里没有
                        AlertDialog.Builder(view.context)
                            .setMessage("是否添加收藏？")
                            .setPositiveButton("是") { dialog, which ->
                                AppDatabase.getInstance(view.context).bookDao()
                                    .insertBooks(item!!)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeBy(onComplete = {
                                        view.context.toast("添加成功")
                                    }, onError = {
                                        view.context.toast("添加失败")
                                        it.printStackTrace()
                                    })
                            }
                            .setNegativeButton("否", null)
                            .show()
                    }
                })
            return@setOnLongClickListener true
        }
    }

    fun bind(book: Book) {
        item = book
        titleView.text = book.title
        authorView.text = book.author
        artistView.text = book.artist
        introView.text = book.intro
        statusView.text = book.status
        sourceView.text = App.getSourceTitle(book.bookUrl)
        GlideApp.with(itemView).load(book.coverUrl).into(coverView)
    }

}