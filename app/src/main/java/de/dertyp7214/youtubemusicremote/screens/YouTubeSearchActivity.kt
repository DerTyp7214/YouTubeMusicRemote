package de.dertyp7214.youtubemusicremote.screens

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.api.YouTubeSearchItem
import de.dertyp7214.youtubemusicremote.api.YoutubeSearchApi
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.components.YouTubeRecyclerViewAdapter
import de.dertyp7214.youtubemusicremote.types.Action
import de.dertyp7214.youtubemusicremote.types.SendAction
import de.dertyp7214.youtubemusicremote.types.VideoIdData

@SuppressLint("NotifyDataSetChanged")
class YouTubeSearchActivity : AppCompatActivity() {

    private val api = YoutubeSearchApi(apiKey = "AIzaSyAQXrCk9RHyL8NbYhCoGfj7U_ifBcVuSsE")

    private val webSocket = CustomWebSocket.webSocketInstance

    private val items: ArrayList<YouTubeSearchItem> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_you_tube_search)

        val searching = MutableLiveData(false)

        val searchInput = findViewById<EditText>(R.id.searchInput)
        val searchButton = findViewById<ImageButton>(R.id.searchButton)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        val loading = findViewById<ProgressBar>(R.id.loading)

        val youTubeRecyclerViewAdapter = YouTubeRecyclerViewAdapter(this, items) { videoId ->
            webSocket?.send(SendAction(Action.VIDEO_ID, VideoIdData(videoId)))
            finish()
        }

        recyclerView.adapter = youTubeRecyclerViewAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        searching.observe(this) {
            if (it) {
                recyclerView.visibility = INVISIBLE
                loading.visibility = VISIBLE
            } else {
                recyclerView.visibility = VISIBLE
                loading.visibility = GONE
            }
        }

        fun search() {
            val query = searchInput.text.toString()

            if (searching.value == true) return
            searching.value = true
            api.searchAsync(query) {
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
    }
}