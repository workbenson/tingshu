package com.github.eprendre.tingshu.utils

import androidx.recyclerview.widget.DiffUtil

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

data class Book(
    val coverUrl: String,
    val bookUrl: String,
    val title: String,
    val author: String,
    val artist: String,
    val intro: String
) {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.bookUrl == newItem.bookUrl
            }

        }
    }
}

data class SectionTab(
    val title: String,
    val url: String
)

data class Section(
    val list: List<Book>,
    val currentPage: Int,
    val totalPage: Int,
    val currentUrl: String,
    val nextUrl: String
)