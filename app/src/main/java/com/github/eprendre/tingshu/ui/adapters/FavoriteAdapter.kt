package com.github.eprendre.tingshu.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.widget.GlideApp
import kotlinx.android.synthetic.main.item_favorite.view.*
import org.jetbrains.anko.toast

class FavoriteAdapter(private val itemClickListener: (Book) -> Unit) :
    ListAdapter<Book, FavoriteViewHolder>(Book.diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite, parent, false)
        return FavoriteViewHolder(view, itemClickListener)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}

class FavoriteViewHolder(view: View, itemClickListener: (Book) -> Unit) : RecyclerView.ViewHolder(view) {
    private val titleView = view.title_text
    private val authorView = view.author_text
    private val artistView = view.artist_text
    private val coverView = view.cover_image
    private val episodeView = view.episode_text
    var item: Book? = null

    init {
        view.setOnClickListener {
            item?.let(itemClickListener)
        }
        view.setOnLongClickListener {
            item?.let {
                view.context.toast(it.bookUrl)
            }
            return@setOnLongClickListener true
        }
    }

    fun bind(book: Book) {
        item = book
        titleView.text = book.title
        authorView.text = book.author
        artistView.text = book.artist
        book.currentEpisodeName?.let {
            episodeView.text = "上次播放：$it ${DateUtils.formatElapsedTime(book.currentEpisodePosition / 1000)}"
        }
        GlideApp.with(itemView).load(book.coverUrl).into(coverView)
    }
}