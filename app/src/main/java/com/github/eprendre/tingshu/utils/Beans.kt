package com.github.eprendre.tingshu.utils

import androidx.recyclerview.widget.DiffUtil

data class Episode(val title: String, val url: String) {
    companion object {
        val difffCallback = object : DiffUtil.ItemCallback<Episode>() {
            override fun areItemsTheSame(oldItem: Episode, newItem: Episode): Boolean {
                return oldItem.url == newItem.url
            }

            override fun areContentsTheSame(oldItem: Episode, newItem: Episode): Boolean {
                return oldItem.url == newItem.url && oldItem.title == newItem.title
            }

        }
    }
}