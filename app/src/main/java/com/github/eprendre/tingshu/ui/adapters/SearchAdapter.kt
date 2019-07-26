package com.github.eprendre.tingshu.ui.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.widget.GlideApp
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.item_search.view.*
import org.jetbrains.anko.toast
import org.jsoup.Jsoup


class SearchAdapter(private val itemClickListener: (Book) -> Unit) :
    ListAdapter<Book, SearchViewHolder>(Book.diffCallback) {
    val compositeDisposable = CompositeDisposable()

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
                val author = lis.get(5).text()
                val artist = lis.get(4).text()

                val bookInfo = doc.selectFirst(".book02").ownText()
                book.coverUrl = coverUrl
                book.author = author
                book.artist = artist
                book.intro = bookInfo
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
    var item: Book? = null

    init {
        view.setOnClickListener {
            item?.let(itemClickListener)
        }
        view.setOnLongClickListener {
            item?.let {
                val clipboard = view.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(it.title, it.bookUrl)
                clipboard.primaryClip = clip
                view.context.toast("${it.bookUrl} 已复制到剪切板")
            }
            return@setOnLongClickListener true
        }
    }

    fun bind(book: Book) {
        item = book
        titleView.text = book.title
        authorView.text = book.author
        artistView.text = book.artist
        introView.text = book.intro
        GlideApp.with(itemView).load(book.coverUrl).into(coverView)
    }

}