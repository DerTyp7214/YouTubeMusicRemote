package de.dertyp7214.youtubemusicremote.components

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.animateTextColor
import de.dertyp7214.youtubemusicremote.core.getFallBackColor
import de.dertyp7214.youtubemusicremote.core.isDark
import de.dertyp7214.youtubemusicremote.core.liveBlur
import de.dertyp7214.youtubemusicremote.types.QueueItem
import de.dertyp7214.youtubemusicremote.types.SongInfo

class QueueBottomSheet(private val onClick: (BottomSheetDialogFragment, QueueItem) -> Unit) :
    BottomSheetDialogFragment() {
    private val isShowing
        get() = dialog?.isShowing ?: false

    private val queueItemsLiveData: MutableLiveData<List<QueueItem>> = MutableLiveData()
    private val songInfoLiveData: MutableLiveData<SongInfo> = MutableLiveData()

    var queueItems: List<QueueItem>
        get() = queueItemsLiveData.value ?: listOf()
        set(value) = queueItemsLiveData.postValue(value)

    var songInfo: SongInfo
        get() = songInfoLiveData.value ?: SongInfo()
        set(value) = songInfoLiveData.postValue(value)

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.queue_bottom_sheet, container, false)

        val items = ArrayList(queueItems)

        val root: ViewGroup = v.findViewById(R.id.queueRoot)
        val recyclerView: RecyclerView = v.findViewById(R.id.queueRecyclerView)
        val adapter = QueueItemAdapter(this, items) {
            onClick(this, it)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        queueItemsLiveData.observe(this) {
            items.clear()
            items.addAll(it)
            adapter.notifyDataSetChanged()
        }

        fun setColors(songInfo: SongInfo) {
            val coverData = songInfo.coverData ?: return

            val background =
                getFallBackColor(coverData.darkVibrant, coverData.muted, coverData.vibrant)
            root.backgroundTintList = ColorStateList.valueOf(background)

            if (isDark(background)) adapter.textColor.postValue(Color.WHITE)
            else adapter.textColor.postValue(Color.BLACK)
        }

        songInfoLiveData.observe(this, ::setColors)

        setColors(songInfo)

        return v
    }

    fun showWithBlur(appCompatActivity: AppCompatActivity, blurContent: View, blurView: View) {
        if (!isShowing) {
            liveBlur(appCompatActivity, blurContent, 2, { isShowing }) {
                blurView.foreground = it
            }

            val oldFragment = appCompatActivity.supportFragmentManager.findFragmentByTag(TAG)

            if (!isAdded && oldFragment == null) show(
                appCompatActivity.supportFragmentManager,
                TAG
            )
        }
    }

    class QueueItemAdapter(
        private val dialogFragment: BottomSheetDialogFragment,
        private val items: List<QueueItem>,
        private val onClick: (QueueItem) -> Unit
    ) :
        RecyclerView.Adapter<QueueItemAdapter.ViewHolder>() {

        private val glide = Glide.with(dialogFragment)

        val textColor = MutableLiveData(Color.WHITE)

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val root: ViewGroup = v.findViewById(R.id.root)
            val cover: ImageView = v.findViewById(R.id.cover)
            val title: TextView = v.findViewById(R.id.title)
            val artist: TextView = v.findViewById(R.id.artist)
            val duration: TextView = v.findViewById(R.id.duration)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(dialogFragment.requireContext())
                    .inflate(R.layout.queue_item, parent, false)
            )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            glide
                .asDrawable()
                .load(item.image)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(16)))
                .into(holder.cover)

            holder.title.text = item.title
            holder.artist.text = item.artist
            holder.duration.text = item.duration

            fun changeColors(color: Int) {
                holder.title.animateTextColor(color) {
                    holder.artist.setTextColor(it)
                    holder.duration.setTextColor(it)
                }
            }

            changeColors(textColor.value ?: Color.WHITE)

            textColor.observe(dialogFragment) {
                changeColors(it)
            }

            holder.root.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        const val TAG = "QueueBottomSheet"
    }
}