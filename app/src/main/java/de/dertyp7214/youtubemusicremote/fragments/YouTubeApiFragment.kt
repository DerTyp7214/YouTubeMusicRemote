package de.dertyp7214.youtubemusicremote.fragments

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.api.YouTubeSearchItem
import de.dertyp7214.youtubemusicremote.api.YoutubeSearchApi
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.components.YouTubeRecyclerViewAdapter
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.types.Action
import de.dertyp7214.youtubemusicremote.types.SendAction
import de.dertyp7214.youtubemusicremote.types.SongInfo
import de.dertyp7214.youtubemusicremote.types.VideoIdData
import de.dertyp7214.youtubemusicremote.viewmodels.YouTubeViewModel
import kotlin.math.roundToInt

@SuppressLint("NotifyDataSetChanged")
class YouTubeApiFragment : Fragment() {

    private var currentSongInfo = MutableLiveData(SongInfo())

    private val api =
        YoutubeSearchApi(apiKey = "AIzaSyAQXrCk9RHyL8NbYhCoGfj7U_ifBcVuSsE").also { it.saveInstance() }

    private val items: ArrayList<YouTubeSearchItem> = arrayListOf()

    private val youtubeViewModel by lazy { ViewModelProvider(requireActivity())[YouTubeViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_youtube_api, container, false)

        val searching = MutableLiveData(false)

        val searchInput = v.findViewById<TextInputEditText>(R.id.searchInput)
        val searchButton = v.findViewById<ImageButton>(R.id.searchButton)

        val recyclerView = v.findViewById<RecyclerView>(R.id.recyclerView)

        val loading = v.findViewById<ProgressBar>(R.id.loading)

        val youTubeRecyclerViewAdapter =
            YouTubeRecyclerViewAdapter(requireContext(), items) { videoId ->
                CustomWebSocket.webSocketInstance?.send(
                    SendAction(
                        Action.VIDEO_ID,
                        VideoIdData(videoId)
                    )
                )
                youtubeViewModel.setChannelId(null)
                youtubeViewModel.setSearchOpen(false)
            }

        recyclerView.adapter = youTubeRecyclerViewAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        searching.observe(requireActivity()) {
            if (it) {
                recyclerView.visibility = View.INVISIBLE
                loading.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                loading.visibility = View.GONE
            }
        }

        fun search() {
            val query = searchInput.text.toString()

            if (searching.value == true) return
            searching.value = true
            api.searchAsync(query, channelId = youtubeViewModel.getChannelId()) {
                items.clear()
                items.addAll(it)
                youTubeRecyclerViewAdapter.notifyDataSetChanged()
                searching.value = false
            }
        }

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search()
                true
            } else false
        }

        searchButton.setOnClickListener { search() }

        val color = Color.WHITE
        searchInput.setHintTextColor(ColorUtils.setAlphaComponent(color, 120))
        searchInput.backgroundTintList = ColorStateList.valueOf(color)
        loading.indeterminateTintList = ColorStateList.valueOf(color)
        searchButton.imageTintList = ColorStateList.valueOf(color)

        youtubeViewModel.observeChannelId(this) {
            if (it != null) search()
        }

        youtubeViewModel.observerSearchOpen(this) {
            items.clear()
            youTubeRecyclerViewAdapter.notifyDataSetChanged()

            if (!it) {
                searchInput.clearFocus()
                searchInput.text?.clear()
            }
        }

        currentSongInfo.observe(requireActivity()) { songInfo ->
            val coverData = songInfo?.coverData ?: return@observe

            val mainColor = if (isDark(coverData.parsedDominant)) Color.WHITE else Color.BLACK
            val topColor = if (isDark(
                    coverData.background?.let {
                        if (searchInput != null) {
                            val activity = requireActivity()
                            val bitmap = it.toBitmap()
                            val screenHeight = activity.window.decorView.height.toFloat()
                            val groupHeight =
                                searchInput.height.toFloat() + activity.getStatusBarHeight() + 16.dpToPx(
                                    activity
                                )
                            val ratio = bitmap.height / screenHeight
                            bitmap.resize(
                                0,
                                0,
                                bitmap.width,
                                (groupHeight * ratio).roundToInt()
                            ).toDrawable(activity)
                        } else it
                    }?.getDominantColor(coverData.parsedDominant) ?: coverData.parsedDominant
                )
            ) Color.WHITE else Color.BLACK

            searchInput.animateTextColor(topColor) { color ->
                searchInput.setHintTextColor(ColorUtils.setAlphaComponent(color, 120))
                searchInput.backgroundTintList = ColorStateList.valueOf(color)
                searchButton.imageTintList = ColorStateList.valueOf(color)
            }
            animateColors(loading.indeterminateTintList?.defaultColor ?: -1, mainColor) { color ->
                loading.indeterminateTintList = ColorStateList.valueOf(color)
            }
        }

        return v
    }

    fun setSongInfo(songInfo: SongInfo) {
        if (!songInfo.srcChanged(currentSongInfo.value)) return

        currentSongInfo.value = songInfo
    }
}