package com.github.eprendre.tingshu.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.widget.GlideApp
import kotlinx.android.synthetic.main.item_search.view.*

class SearchAdapter(private val itemClickListener: (Book) -> Unit) :
    ListAdapter<Book, SearchViewHolder>(Book.diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search, parent, false)
        return SearchViewHolder(view, itemClickListener)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position))
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