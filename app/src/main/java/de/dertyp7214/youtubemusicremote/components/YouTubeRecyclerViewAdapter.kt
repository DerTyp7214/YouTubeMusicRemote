package de.dertyp7214.youtubemusicremote.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.android.material.card.MaterialCardView
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.api.YouTubeSearchItem
import de.dertyp7214.youtubemusicremote.core.getThumbnail

class YouTubeRecyclerViewAdapter(
    private val context: Context,
    private val items: List<YouTubeSearchItem>,
    private val glide: RequestManager = Glide.with(context),
    private val callback: (videoId: String) -> Unit
) : RecyclerView.Adapter<YouTubeRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val thumbnail: ImageView = v.findViewById(R.id.thumbnail)
        val channelName: TextView = v.findViewById(R.id.channel)
        val title: TextView = v.findViewById(R.id.title)
        val layout: MaterialCardView = v.findViewById(R.id.card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.youtube_search_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.snippet.title
        holder.channelName.text = item.snippet.channelTitle

        item.snippet.getThumbnail()?.let { youTubeThumbnail ->
            glide.asDrawable().load(youTubeThumbnail.url).into(holder.thumbnail)
        }

        holder.layout.setOnClickListener {
            callback(item.id.videoId)
        }
    }

    override fun getItemCount(): Int = items.size
}