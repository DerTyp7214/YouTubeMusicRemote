package de.dertyp7214.youtubemusicremote.components

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.types.CoverData
import de.dertyp7214.youtubemusicremote.types.QueueItem

class QueueBottomSheet(
    private val onCoverDataChanged: (CoverData) -> Unit = {},
    private val onLongPress: (View, QueueItem) -> Unit = { _, _ -> },
    private val onClick: (BottomSheetDialogFragment, QueueItem) -> Unit
) :
    BaseBottomSheet() {
    private val queueItemsLiveData: MutableLiveData<List<QueueItem>> = MutableLiveData()
    private val coverDataLiveData: MutableLiveData<CoverData> = MutableLiveData()

    var queueItems: List<QueueItem>
        get() = queueItemsLiveData.value ?: listOf()
        set(value) = queueItemsLiveData.postValue(value)

    var coverData: CoverData
        get() = coverDataLiveData.value ?: CoverData()
        set(value) {
            if (coverData != value) coverDataLiveData.postValue(value)
        }

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
        val adapter = QueueItemAdapter(this, items, onLongPress = onLongPress) {
            onClick(this, it)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        fun setColors(coverData: CoverData) {
            val background =
                getFallBackColor(coverData.darkVibrant, coverData.muted, coverData.vibrant)
            root.backgroundTintList = ColorStateList.valueOf(background)

            val textColor = if (isDark(background)) Color.WHITE else Color.BLACK

            adapter.textColor.postValue(textColor)
            adapter.strokeColor.postValue(ColorUtils.setAlphaComponent(textColor, 128))
        }

        queueItemsLiveData.observe(this) { queueItemList ->
            items.clear()
            items.addAll(queueItemList)
            adapter.notifyDataSetChanged()
            setColors(coverData)
        }

        coverDataLiveData.observe(this) {
            if (isShowing) {
                onCoverDataChanged(it)
                blurFunction(false)
            }
            setColors(it)
        }

        setColors(coverData)

        return v
    }

    class QueueItemAdapter(
        private val dialogFragment: BottomSheetDialogFragment,
        private val items: List<QueueItem>,
        private val onLongPress: (View, QueueItem) -> Unit,
        private val onClick: (QueueItem) -> Unit
    ) :
        RecyclerView.Adapter<QueueItemAdapter.ViewHolder>() {

        init {
            setHasStableIds(true)
        }

        private val glide = Glide.with(dialogFragment)

        val textColor = MutableLiveData(Color.WHITE)
        val strokeColor = MutableLiveData(Color.WHITE)

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val root: MaterialCardView = v.findViewById(R.id.root)
            val cover: ImageView = v.findViewById(R.id.cover)
            val title: TextView = v.findViewById(R.id.title)
            val artist: TextView = v.findViewById(R.id.artist)
            val duration: TextView = v.findViewById(R.id.duration)
        }

        override fun getItemId(position: Int): Long {
            return items[position].hashCode().toLong()
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

            holder.root.strokeColor = strokeColor.value ?: Color.WHITE

            textColor.observe(dialogFragment) {
                Handler(Looper.getMainLooper()).post {
                    changeColors(it)
                }
            }

            strokeColor.observe(dialogFragment) {
                Handler(Looper.getMainLooper()).post {
                    animateColors(holder.root.strokeColorStateList?.defaultColor ?: -1, it) {
                        holder.root.strokeColor = it
                    }
                }
            }

            holder.root.setOnClickListener { if (position != 0) onClick(item) }
            holder.root.setOnLongClickListener {
                onLongPress(it, item)
                true
            }

            if (position == 0) {
                val context = dialogFragment.requireContext()
                holder.root.strokeWidth = 2.dpToPx(context)
                holder.root.setMargin(8.dpToPx(context))
            } else {
                holder.root.strokeWidth = 0
                holder.root.setMargin(0)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}