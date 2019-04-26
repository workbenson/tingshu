package com.github.eprendre.tingshu.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.utils.Episode
import com.github.eprendre.tingshu.utils.Prefs
import kotlinx.android.synthetic.main.item_episode.view.*

class EpisodeAdapter(private val itemClickedListener: (Episode) -> Unit) :
    ListAdapter<Episode, EpisodeViewHolder>(Episode.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false)
        return EpisodeViewHolder(view, itemClickedListener)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

}

class EpisodeViewHolder(view: View, itemClickedListener: (Episode) -> Unit) : RecyclerView.ViewHolder(view) {
    private val titleView: TextView = view.title
    var item: Episode? = null

    init {
        view.setOnClickListener {
            item?.let(itemClickedListener)
        }
    }

    fun bind(episode: Episode) {
        item = episode
        titleView.text = episode.title
        if (episode.url == Prefs.currentEpisodeUrl) {
            titleView.setTextColor(ContextCompat.getColor(itemView.context,
                R.color.colorAccent
            ))
        } else {
            titleView.setTextColor(ContextCompat.getColor(itemView.context,
                R.color.primary_text
            ))
        }
    }
}