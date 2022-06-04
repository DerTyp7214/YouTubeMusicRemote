package de.dertyp7214.youtubemusicremote.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.components.SearchBar
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.types.*
import de.dertyp7214.youtubemusicremote.viewmodels.SearchViewModel

class SearchFragment : Fragment() {

    private lateinit var layoutView: View

    private val gson = Gson().newBuilder().enableComplexMapKeySerialization().create()

    private val items = ArrayList<SearchItem>()

    private val mutableSongInfo = MutableLiveData<SongInfo>()
    private val mutableSpanCount = MutableLiveData<Int>()

    private val webSocket by lazy { CustomWebSocket.webSocketInstance }
    private val searchViewModel by lazy { ViewModelProvider(requireActivity())[SearchViewModel::class.java] }
    private val searchBar by lazy { layoutView.findViewById<SearchBar>(R.id.searchBar) }
    private val recyclerView by lazy { layoutView.findViewById<RecyclerView>(R.id.recyclerView) }
    private val progressBar by lazy { layoutView.findViewById<ProgressBar>(R.id.progressBar) }
    private val shuffleButton by lazy { layoutView.findViewById<MaterialButton>(R.id.shuffle) }
    private val adapter by lazy {
        SearchAdapter(requireContext(), items) {
            if (it.type == Type.PLAYLISTS && it.playlists != null) fetchPlaylistContent(it.playlists.index)
            else if (it.type == Type.SONGS && it.playlistContent != null) webSocket?.playPlaylist(
                false,
                it.playlistContent.index
            )?.run {
                goBack()
            } else if (it.type == Type.SHELF_PLAY_DATA && it.shelfPlayData != null) webSocket?.playSearchSong(
                it.shelfPlayData.index,
                it.shelfPlayData.shelf
            ) else if (it.type == Type.SHELF_SONGS && it.playlistContent != null) webSocket?.playSearchSong(
                it.playlistContent.index
            ) else if (it.type == Type.FUNCTION) it.run(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        layoutView = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        recyclerView.setHasFixedSize(true)

        progressBar.isIndeterminate = true

        searchBar.setMargins(0, getStatusBarHeight(), 0, 0)

        searchBar.setOnSearchListener {
            progressBar.visibility = VISIBLE
            webSocket?.search(it)
            updateData(listOf())
        }

        searchBar.setOnCloseListener {
            fetchPlaylists()
        }

        searchViewModel.observerSearchOpen(this) { open ->
            if (!open) searchBar.clearText()
            else fetchPlaylists()
        }

        searchViewModel.observeQuery(this) { searchBar.text = it ?: "" }

        mutableSpanCount.observe(requireActivity()) {
            recyclerView.layoutManager.apply {
                if (this is GridLayoutManager) spanCount = it
            }
        }

        mutableSongInfo.observe(requireActivity()) {
            it.coverData?.let { coverData ->
                val color = getFallBackColor(
                    coverData.lightVibrant,
                    coverData.lightMuted,
                    coverData.vibrant,
                    coverData.dominant
                )

                val stateList = ColorStateList(
                    arrayOf(
                        intArrayOf(-android.R.attr.state_enabled),
                        intArrayOf()
                    ),
                    intArrayOf(
                        Color.GRAY,
                        color
                    )
                )

                adapter.mutableStateList.postValue(stateList)

                progressBar.indeterminateTintList = stateList
                shuffleButton.strokeColor = stateList
                shuffleButton.rippleColor = stateList
                shuffleButton.setTextColor(stateList)
            }
        }

        shuffleButton.setOnClickListener {
            if (items.firstOrNull()?.type == Type.SONGS) {
                webSocket?.playPlaylist(true, 0)
                goBack()
            }
        }

        webSocket?.webSocketListener?.onMessage { _, text ->
            try {
                val socketResponse = gson.fromJson(text, SocketResponse::class.java)

                when (socketResponse.action) {
                    Action.PLAYLISTS -> {
                        progressBar.visibility = INVISIBLE
                        val playlists: List<Playlists> = gson.fromJson(
                            socketResponse.data,
                            object : TypeToken<List<Playlists>>() {}.type
                        )

                        updateData(playlists.map { SearchItem(Type.PLAYLISTS, playlists = it) })
                        mutableSpanCount.postValue(3)
                    }
                    Action.PLAYLIST -> {
                        progressBar.visibility = INVISIBLE
                        val playlistContent: List<PlaylistContent> = gson.fromJson(
                            socketResponse.data,
                            object : TypeToken<List<PlaylistContent>>() {}.type
                        )

                        updateData(playlistContent.map {
                            SearchItem(
                                Type.SONGS,
                                playlistContent = it
                            )
                        }) {
                            shuffleButton.isEnabled = true
                        }
                        mutableSpanCount.postValue(1)
                    }
                    Action.SEARCH_MAIN_RESULT -> {
                        progressBar.visibility = INVISIBLE
                        val searchResults: List<SearchMainResultData> = gson.fromJson(
                            socketResponse.data,
                            object : TypeToken<List<SearchMainResultData>>() {}.type
                        )

                        updateData(searchResults.map {
                            SearchItem(
                                Type.SEARCH,
                                searchMainResultData = it
                            )
                        })
                        mutableSpanCount.postValue(1)
                    }
                    Action.SHOW_SHELF_RESULTS -> {
                        progressBar.visibility = INVISIBLE
                        val shelfResults: List<ShowShelfResultData> = gson.fromJson(
                            socketResponse.data,
                            object : TypeToken<List<ShowShelfResultData>>() {}.type
                        )

                        updateData(shelfResults.map {
                            SearchItem(
                                Type.SHELF_SONGS,
                                playlistContent = it.toPlaylistContent()
                            )
                        })
                        mutableSpanCount.postValue(1)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        mutableSpanCount.postValue(3)

        return layoutView
    }

    fun setSongInfo(songInfo: SongInfo) = mutableSongInfo.postValue(songInfo)

    fun handleBack(): Boolean {
        return if (searchViewModel.getSearchOpen() == true) {
            if (searchBar.focus) searchBar.clearText()
            else if (items.firstOrNull()?.type == Type.SONGS) fetchPlaylists()
            else goBack()
            true
        } else false
    }

    private fun goBack() {
        searchBar.clearText()
        searchViewModel.setSearchOpen(false)
        searchViewModel.setQuery(null)
        updateData(listOf())
        webSocket?.openPlayer()
    }

    private fun fetchPlaylists() {
        shuffleButton.visibility = GONE
        progressBar.visibility = VISIBLE
        webSocket?.requestPlaylists()
        updateData(listOf())
    }

    private fun fetchPlaylistContent(index: Int) {
        shuffleButton.isEnabled = false
        shuffleButton.visibility = VISIBLE
        progressBar.visibility = VISIBLE
        webSocket?.requestPlaylist(index)
        updateData(listOf())
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateData(newData: List<SearchItem>, also: () -> Unit = {}) {
        Handler(requireActivity().mainLooper).post {
            synchronized(items) {
                items.clear()
                items.addAll(newData)
                adapter.notifyDataSetChanged()
                also()
            }
        }
    }

    private class SearchAdapter(
        private val context: Context,
        private val list: List<SearchItem>,
        private val onClick: (SearchItem) -> Unit
    ) :
        RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

        private val glide = Glide.with(context)

        val mutableStateList = MutableLiveData<ColorStateList>()

        init {
            setHasStableIds(true)
        }

        open class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

        class SongViewHolder(v: View) : ViewHolder(v) {
            val root: View = v.findViewById(R.id.root)
            val cover: ImageView = v.findViewById(R.id.cover)
            val title: TextView = v.findViewById(R.id.title)
            val artist: TextView = v.findViewById(R.id.artist)
        }

        class PlayListViewHolder(v: View) : ViewHolder(v) {
            val root: View = v.findViewById(R.id.root)
            val thumbnail: ImageView = v.findViewById(R.id.thumbnail)
            val title: TextView = v.findViewById(R.id.title)
            val subTitle: TextView = v.findViewById(R.id.artist)
        }

        class SearchViewHolder(v: View) : ViewHolder(v) {
            val root: View = v.findViewById(R.id.root)
            val title: TextView = v.findViewById(R.id.title)
            val showAllButton: MaterialButton = v.findViewById(R.id.showAll)
            val recyclerView: RecyclerView = v.findViewById(R.id.searchRecyclerView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
            0 -> SongViewHolder(
                LayoutInflater.from(context).inflate(R.layout.song_item, parent, false)
            )
            1 -> PlayListViewHolder(
                LayoutInflater.from(context).inflate(R.layout.playlist_item, parent, false)
            )
            2 -> SearchViewHolder(
                LayoutInflater.from(context).inflate(R.layout.search_item, parent, false)
            )
            else -> ViewHolder(View(context))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            when (holder) {
                is SongViewHolder -> {
                    list[position].playlistContent?.let { item ->
                        holder.root.setOnClickListener {
                            onClick(list[position])
                        }

                        holder.title.text = item.title
                        holder.artist.text = item.artist

                        glide.asDrawable()
                            .load(item.thumbnails.reduce { t1, t2 -> if (t1.width > t2.width) t1 else t2 }.url)
                            .override(SIZE_ORIGINAL)
                            .into(holder.cover)
                    }
                }
                is PlayListViewHolder -> {
                    list[position].playlists?.let { item ->
                        holder.root.setOnClickListener {
                            onClick(list[position])
                        }

                        holder.title.text = item.title
                        holder.subTitle.text = item.subtitle

                        glide.asDrawable()
                            .load(item.thumbnails.reduce { t1, t2 -> if (t1.width > t2.width) t1 else t2 }.url)
                            .override(SIZE_ORIGINAL)
                            .into(holder.thumbnail)
                    }
                }
                is SearchViewHolder -> {
                    list[position].searchMainResultData?.let { item ->
                        holder.root.setOnClickListener { }

                        holder.showAllButton.setOnClickListener {
                            onClick(SearchItem(Type.FUNCTION) {
                                progressBar.visibility = VISIBLE
                                webSocket?.showShelf(item.index)
                                updateData(listOf())
                            })
                        }

                        holder.showAllButton.visibility = if (item.showAll) VISIBLE else GONE

                        holder.title.text = item.title
                        holder.recyclerView.adapter =
                            object : RecyclerView.Adapter<SongViewHolder>() {
                                override fun onCreateViewHolder(
                                    parent: ViewGroup,
                                    viewType: Int
                                ) = SongViewHolder(
                                    LayoutInflater.from(context)
                                        .inflate(R.layout.song_item, parent, false)
                                )

                                override fun onBindViewHolder(
                                    holder: SongViewHolder,
                                    position: Int
                                ) {
                                    val entry = item.entries[position]

                                    holder.root.setOnClickListener {
                                        when (item.type) {
                                            "songs", "videos", "albums", "community playlists" -> onClick(
                                                SearchItem(
                                                    Type.SHELF_PLAY_DATA,
                                                    shelfPlayData = ShelfPlayData(
                                                        entry.index,
                                                        item.index
                                                    )
                                                )
                                            )
                                        }
                                    }

                                    holder.title.text = entry.title
                                    holder.artist.text = entry.subTitle.joinToString(" • ")

                                    glide.asDrawable()
                                        .load(entry.thumbnails.reduce { t1, t2 -> if (t1.width > t2.width) t1 else t2 }.url)
                                        .override(SIZE_ORIGINAL)
                                        .into(holder.cover)
                                }

                                override fun getItemCount() = item.entries.size
                            }
                        holder.recyclerView.layoutManager = LinearLayoutManager(context)
                        holder.recyclerView.setHasFixedSize(true)

                        context.getActivity()?.let {
                            if (it is FragmentActivity) mutableStateList.observe(it) { stateList ->
                                holder.showAllButton.rippleColor = stateList
                                holder.showAllButton.strokeColor = stateList
                                holder.showAllButton.setTextColor(stateList)
                            }
                        }
                    }
                }
            }
        }

        override fun getItemViewType(position: Int) = when (list[position].type) {
            Type.SONGS, Type.SHELF_SONGS -> 0
            Type.PLAYLISTS -> 1
            Type.SEARCH -> 2
            else -> -1
        }

        override fun getItemId(position: Int) = list[position].hashCode().toLong()
        override fun getItemCount(): Int = list.size
    }

    private data class SearchItem(
        val type: Type,
        val playlistContent: PlaylistContent? = null,
        val playlists: Playlists? = null,
        val searchMainResultData: SearchMainResultData? = null,
        val shelfPlayData: ShelfPlayData? = null,
        val run: SearchFragment.() -> Unit = {}
    )

    private data class ShelfPlayData(
        val index: Int,
        val shelf: Int?
    )

    private enum class Type {
        SONGS,
        SHELF_SONGS,
        PLAYLISTS,
        SEARCH,
        SHELF_PLAY_DATA,
        FUNCTION
    }
}