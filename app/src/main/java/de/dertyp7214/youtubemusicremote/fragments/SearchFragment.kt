package de.dertyp7214.youtubemusicremote.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.appcompat.view.ContextThemeWrapper
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
import de.dertyp7214.youtubemusicremote.screens.MainActivity
import de.dertyp7214.youtubemusicremote.types.*
import de.dertyp7214.youtubemusicremote.viewmodels.SearchViewModel

class SearchFragment : Fragment() {

    private lateinit var layoutView: View

    private val gson = Gson().newBuilder().enableComplexMapKeySerialization().create()

    private val items = ArrayList<SearchItem>()

    private val mutableMusicData = MutableLiveData<MusicData>()
    private val mutableSpanCount = MutableLiveData<Int>()

    private val webSocket by lazy { CustomWebSocket.webSocketInstance }
    private val searchViewModel by lazy { ViewModelProvider(requireActivity())[SearchViewModel::class.java] }
    private val searchBar by lazy { layoutView.findViewById<SearchBar>(R.id.searchBar) }
    private val recyclerView by lazy { layoutView.findViewById<RecyclerView>(R.id.recyclerView) }
    private val progressBar by lazy { layoutView.findViewById<ProgressBar>(R.id.progressBar) }
    private val shuffleButton by lazy { layoutView.findViewById<MaterialButton>(R.id.shuffle) }
    private val currentCover by lazy { layoutView.findViewById<ImageView>(R.id.currentCover) }
    private val currentTitle by lazy { layoutView.findViewById<TextView>(R.id.currentTitle) }
    private val currentArtist by lazy { layoutView.findViewById<TextView>(R.id.currentArtist) }
    private val currentPlayPause by lazy { layoutView.findViewById<ImageButton>(R.id.currentPlayPause) }
    private val currentProgress by lazy { layoutView.findViewById<ProgressBar>(R.id.currentProgress) }
    private val adapter by lazy {
        SearchAdapter(requireContext(), items, contextMenu = { view, shelfPlayData ->
            showMenu(view, shelfPlayData, R.menu.queue_menu)
        }) {
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

    private var showAll = false

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
            if (!open) searchBar.clearText().also { showAll = false }
            else fetchPlaylists()
        }

        searchViewModel.observeQuery(this) { searchBar.text = it ?: "" }

        mutableSpanCount.observe(requireActivity()) {
            recyclerView.layoutManager.apply {
                if (this is GridLayoutManager) spanCount = it
            }
        }

        currentPlayPause.setOnClickListener {
            webSocket?.playPause()
        }

        mutableMusicData.observe(requireActivity()) { musicData ->
            val stateList = ColorStateList(
                arrayOf(
                    intArrayOf(-android.R.attr.state_enabled),
                    intArrayOf()
                ),
                intArrayOf(
                    Color.GRAY,
                    musicData.color
                )
            )

            adapter.mutableStateList.postValue(stateList)

            progressBar.indeterminateTintList = stateList
            shuffleButton.strokeColor = stateList
            shuffleButton.rippleColor = stateList
            shuffleButton.setTextColor(stateList)

            currentCover.setImageDrawable(musicData.cover)
            currentTitle.text = musicData.title
            currentArtist.text = musicData.artist

            currentProgress.progressTintList = stateList

            currentPlayPause.setImageResource(if (musicData.playing) R.drawable.ic_pause else R.drawable.ic_play)
        }

        MainActivity.currentSongInfo.observe(requireActivity()) {
            currentProgress.max = it.songDuration.toInt()
            currentProgress.progress = it.elapsedSeconds
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

    fun setSongInfo(songInfo: SongInfo) {
        songInfo.coverData?.let { coverData ->
            val color = getFallBackColor(
                coverData.lightVibrant,
                coverData.lightMuted,
                coverData.vibrant,
                coverData.dominant
            )
            if (mutableMusicData.value?.let {
                    it.title != songInfo.title ||
                            it.artist != songInfo.artist ||
                            it.color != color ||
                            it.cover != coverData.cover ||
                            it.playing != songInfo.playPaused
                } != false) mutableMusicData.postValue(
                MusicData(
                    color,
                    coverData.cover,
                    songInfo.title,
                    songInfo.artist,
                    songInfo.isPaused == false
                )
            )
        }
    }

    fun handleBack(): Boolean {
        return if (searchViewModel.getSearchOpen() == true) {
            if (showAll) searchBar.search().also { showAll = false }
            else if (searchBar.focus) searchBar.close()
            else if (items.firstOrNull()?.type == Type.SONGS) fetchPlaylists()
            else goBack()
            true
        } else false
    }

    private fun showMenu(
        v: View,
        shelfPlayData: ShelfPlayData,
        menuLayout: Int = R.menu.main_menu
    ) {
        val activity = requireActivity()
        val playlistMenu =
            shelfPlayData.type == "playlists" || shelfPlayData.type == "playlist songs"
        PopupMenu(activity, v, Gravity.CENTER, 0, R.style.Theme_YouTubeMusicRemote).apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_share -> {
                        MainActivity.currentSongInfo.value?.let { songInfo ->
                            activity.share(ShareInfo.fromSongInfo(songInfo))
                        }
                        true
                    }
                    R.id.menu_start_radio -> {
                        if (playlistMenu) webSocket?.playlistContextMenu(
                            ContextAction.RADIO,
                            shelfPlayData.index,
                            shelfPlayData.type == "playlist songs"
                        )
                        else webSocket?.searchContextMenu(
                            ContextAction.RADIO,
                            shelfPlayData.index,
                            shelfPlayData.shelf
                        )
                        true
                    }
                    R.id.menu_play_next -> {
                        if (playlistMenu) webSocket?.playlistContextMenu(
                            ContextAction.NEXT,
                            shelfPlayData.index,
                            shelfPlayData.type == "playlist songs"
                        )
                        else webSocket?.searchContextMenu(
                            ContextAction.NEXT,
                            shelfPlayData.index,
                            shelfPlayData.shelf
                        )
                        true
                    }
                    R.id.menu_add_to_queue -> {
                        if (playlistMenu) webSocket?.playlistContextMenu(
                            ContextAction.QUEUE,
                            shelfPlayData.index,
                            shelfPlayData.type == "playlist songs"
                        )
                        else webSocket?.searchContextMenu(
                            ContextAction.QUEUE,
                            shelfPlayData.index,
                            shelfPlayData.shelf
                        )
                        true
                    }
                    else -> false
                }
            }
            inflate(menuLayout)
            menu.findItem(R.id.menu_share)?.isVisible = false
            menu.findItem(R.id.menu_remove_from_queue)?.isVisible = false
            menu.findItem(R.id.menu_play_next)?.isVisible = false
            menu.findItem(R.id.menu_add_to_queue)?.isVisible = false

            when (shelfPlayData.type) {
                "songs", "videos", "albums", "community playlists", "playlists", "playlist songs" -> {
                    menu.findItem(R.id.menu_play_next)?.isVisible = true
                    menu.findItem(R.id.menu_add_to_queue)?.isVisible = true
                }
            }
        }.show()
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
        private val contextMenu: (View, ShelfPlayData) -> Unit,
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

        class TabsViewHolder(v: View) : ViewHolder(v) {
            val root: View = v.findViewById(R.id.root)
            val tabsList: LinearLayout = v.findViewById(R.id.tabsLayout)
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
            3 -> TabsViewHolder(
                LayoutInflater.from(context).inflate(R.layout.tabs_item, parent, false)
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

                        holder.root.setOnLongClickListener {
                            contextMenu(it, ShelfPlayData(item.index, type = "playlist songs"))
                            true
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

                        holder.root.setOnLongClickListener {
                            contextMenu(it, ShelfPlayData(item.index, type = "playlists"))
                            true
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

                        holder.root.setOnLongClickListener {
                            contextMenu(it, ShelfPlayData(item.index, type = item.type))
                            true
                        }

                        holder.showAllButton.setOnClickListener {
                            onClick(SearchItem(Type.FUNCTION) {
                                showAll = true
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

                                    holder.root.setOnLongClickListener {
                                        contextMenu(
                                            it,
                                            ShelfPlayData(entry.index, item.index, item.type)
                                        )
                                        true
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
                is TabsViewHolder -> {
                    list[position].searchMainResultData?.let { item ->
                        holder.root.setOnClickListener { }
                        holder.tabsList.apply {
                            val themeWrapper = ContextThemeWrapper(
                                context,
                                com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton
                            )
                            removeAllViews()
                            item.entries.forEach { tab ->
                                addView(
                                    MaterialButton(themeWrapper).apply {
                                        text = tab.title
                                        isEnabled = tab.type == "unselected"
                                        layoutParams = LinearLayout.LayoutParams(
                                            WRAP_CONTENT, WRAP_CONTENT, 1f
                                        )
                                        setMargin(8.dpToPx(context))
                                        setOnClickListener {
                                            onClick(SearchItem(Type.FUNCTION) {
                                                progressBar.visibility = VISIBLE
                                                updateData(listOf())
                                                webSocket?.selectSearchTab(tab.index)
                                            })
                                        }

                                        context.getActivity()?.let {
                                            if (it is FragmentActivity) mutableStateList.observe(it) { stateList ->
                                                backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                                                strokeColor = stateList
                                                rippleColor = stateList
                                                setTextColor(stateList)
                                            }
                                        }
                                    })
                            }
                        }
                    }
                }
            }
        }

        override fun getItemViewType(position: Int) = when (list[position].type) {
            Type.SONGS, Type.SHELF_SONGS -> 0
            Type.PLAYLISTS -> 1
            Type.SEARCH -> if (list[position].searchMainResultData?.type == "tabs") 3 else 2
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
        val shelf: Int? = null,
        val type: String? = null
    )

    private data class MusicData(
        val color: Int,
        val cover: Drawable?,
        val title: String,
        val artist: String,
        val playing: Boolean
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