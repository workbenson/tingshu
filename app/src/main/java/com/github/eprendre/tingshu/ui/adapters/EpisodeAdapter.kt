package com.github.eprendre.tingshu.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.extensions.getColorAccent
import com.github.eprendre.tingshu.extensions.getTextColorPrimary
import com.github.eprendre.tingshu.extensions.md5
import com.github.eprendre.tingshu.utils.Episode
import com.github.eprendre.tingshu.utils.Prefs
import kotlinx.android.synthetic.main.item_episode.view.*
import java.io.File

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
    private val chargeView: TextView = view.charge
    private val cachedView: TextView = view.cached
    var item: Episode? = null

    init {
        view.setOnClickListener {
            item?.let(itemClickedListener)
        }
    }

    fun bind(episode: Episode) {
        item = episode
        titleView.text = episode.title
        if (episode.url == Prefs.currentBook!!.currentEpisodeUrl) {
            titleView.setTextColor(itemView.context.getColorAccent())
        } else {
            titleView.setTextColor(itemView.context.getTextColorPrimary())
        }
        if (episode.isFree) {
            chargeView.visibility = View.GONE
        } else {
            chargeView.visibility = View.VISIBLE
        }
        val file = File(App.appContext.externalCacheDir, episode.url.md5())
        if (file.exists()) {
            cachedView.visibility = View.VISIBLE
        } else {
            cachedView.visibility = View.GONE
        }
    }
}