package de.dertyp7214.youtubemusicremote.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.android.material.card.MaterialCardView
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.api.YouTubeSearchItem
import de.dertyp7214.youtubemusicremote.core.animateTextColor
import de.dertyp7214.youtubemusicremote.core.getThumbnail

class YouTubeRecyclerViewAdapter(
    private val context: Context,
    private val items: List<YouTubeSearchItem>,
    private val glide: RequestManager = Glide.with(context),
    private val callback: (videoId: String) -> Unit
) : RecyclerView.Adapter<YouTubeRecyclerViewAdapter.ViewHolder>() {

    var textColor: Int = -1
        set(value) {
            field = value
            internalTextColor.value = value
        }
    private val internalTextColor = MutableLiveData(textColor)

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

        holder.title.setTextColor(textColor)
        holder.channelName.setTextColor(textColor)

        holder.layout.strokeColor = ColorUtils.setAlphaComponent(textColor, 80)

        item.snippet.getThumbnail()?.let { youTubeThumbnail ->
            glide.asDrawable().load(youTubeThumbnail.url).into(holder.thumbnail)
        }

        holder.layout.setOnClickListener {
            callback(item.id.videoId)
        }

        ViewTreeLifecycleOwner.get(holder.layout)?.let { lifecycleOwner ->
            internalTextColor.observe(lifecycleOwner) { newColor ->
                holder.title.animateTextColor(newColor) {
                    holder.channelName.setTextColor(it)
                    holder.layout.strokeColor = ColorUtils.setAlphaComponent(it, 80)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}