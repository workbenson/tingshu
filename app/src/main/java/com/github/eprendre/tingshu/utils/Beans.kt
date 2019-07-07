package com.github.eprendre.tingshu.utils

import androidx.annotation.Keep
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Keep
data class Episode(val title: String, val url: String) {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Episode>() {
            override fun areItemsTheSame(oldItem: Episode, newItem: Episode): Boolean {
                return oldItem.url == newItem.url
            }

            override fun areContentsTheSame(oldItem: Episode, newItem: Episode): Boolean {
                return oldItem.url == newItem.url && oldItem.title == newItem.title
            }

        }
    }
}

@Keep
@Entity(tableName = "my_books", indices = [Index(value = ["book_url"], unique = true)])
data class Book(
    @ColumnInfo(name = "cover_url")
    val coverUrl: String,
    @ColumnInfo(name = "book_url")
    val bookUrl: String,
    val title: String,
    val author: String,
    val artist: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
    var intro: String = ""
    var currentEpisodeUrl: String? = null
    var currentEpisodeName: String? = null
    var currentEpisodePosition: Long = 0

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.bookUrl == newItem.bookUrl &&
                        oldItem.currentEpisodeUrl == newItem.currentEpisodeUrl &&
                        oldItem.currentEpisodePosition == newItem.currentEpisodePosition
            }

        }
    }
}

@Keep
data class CategoryTab(
    val title: String,
    val url: String
)

@Keep
data class Category(
    val list: List<Book>,
    val currentPage: Int,
    val totalPage: Int,
    val currentUrl: String,
    val nextUrl: String
)