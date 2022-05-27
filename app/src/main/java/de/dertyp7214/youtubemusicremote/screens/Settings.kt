package de.dertyp7214.youtubemusicremote.screens

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import de.dertyp7214.youtubemusicremote.BuildConfig.VERSION_CODE
import de.dertyp7214.youtubemusicremote.BuildConfig.VERSION_NAME
import de.dertyp7214.youtubemusicremote.Config.PLAY_URL
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.services.MediaPlayer
import de.dertyp7214.youtubemusicremote.types.CoverData
import kotlin.math.roundToInt

class Settings : AppCompatActivity() {

    private val code = (Math.random() * 100).roundToInt()

    private val coverLiveData = MutableLiveData<CoverData>()

    private lateinit var adapter: SettingsAdapter

    private val settings by lazy {
        listOf(
            SettingsElement(
                "appVersion",
                getString(R.string.settings_app_version),
                "$VERSION_NAME ($VERSION_CODE)"
            ),
            SettingsElement(
                "fromPlayStore",
                R.string.settings_from_playstore,
                if (verifyInstallerId()) R.string.yes else R.string.no,
                this
            ) { _, _ ->
                openUrl(PLAY_URL(packageName))
            },
            SettingsElement(
                "currentUrl",
                getString(R.string.settings_current_url),
                MediaPlayer.URL
            ),
            SettingsElement(
                "spacer1",
                type = SettingsType.SPACER
            ),
            SettingsElement(
                "useRatingInNotification",
                R.string.settings_use_rating_title,
                R.string.settings_use_rating_subtext,
                this,
                SettingsType.SWITCH
            ) { id, value ->
                if (value is Boolean) preferences.edit {
                    putBoolean(id, value)
                    startForegroundService(
                        Intent(
                            applicationContext,
                            MediaPlayer::class.java
                        ).setAction(MediaPlayer.ACTION_REFETCH)
                    )
                }
            }
        )
    }

    private fun setColors(color: Int? = null) {
        fun applyColors(color: Int) {
            adapter.setColor(color)
        }

        if (color == null) {
            fun fetchCoverData() {
                if (coverLiveData.value == null) WallpaperManager.getInstance(this).drawable?.apply {
                    val vibrant = Palette.Builder(toBitmap()).maximumColorCount(32).generate().let {
                        it.getVibrantColor(it.getMutedColor(it.getDominantColor(Color.CYAN)))
                    }
                    blur(this@Settings) {
                        coverLiveData.postValue(
                            CoverData(
                                it,
                                vibrant = vibrant
                            )
                        )
                    }
                } else coverLiveData.postValue(coverLiveData.value)
            }
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                code
            ) else fetchCoverData()

            coverLiveData.observe(this) { coverData ->
                window.decorView.background = coverData.background?.fitToScreen(this)?.apply {
                    colorFilter = LightingColorFilter(0xFF7B7B7B.toInt(), 0x00000000)
                }

                val coverColor = getFallBackColor(coverData.vibrant, coverData.muted)

                applyColors(coverColor)
            }
        } else applyColors(color)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val view = findViewById<ViewGroup>(R.id.view)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.settings)

        adapter = SettingsAdapter(settings, this)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        var initialized = false

        MainActivity.currentSongInfo.observe(this) {
            if (it.coverData != null && coverLiveData.value != it.coverData)
                coverLiveData.postValue(it.coverData!!)
        }

        view.setHeight(getStatusBarHeight())

        window.decorView.rootView.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialized) {
                setColors()
                MainActivity.currentSongInfo.value?.coverData?.let { coverLiveData.postValue(it) }
                initialized = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class SettingsAdapter(
        private val settings: List<SettingsElement>,
        private val activity: FragmentActivity
    ) : RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {
        private val mutableColor = MutableLiveData<Int>()

        open class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val root: ViewGroup = v.findViewById(R.id.root)
            val title: TextView = v.findViewById(R.id.title)
            val subText: TextView = v.findViewById(R.id.subText)
        }

        class ViewHolderSwitch(v: View) : ViewHolder(v) {
            val switch: SwitchMaterial = v.findViewById(R.id.switchThumb)
        }

        fun setColor(color: Int) = mutableColor.postValue(color)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return when (viewType) {
                1 -> ViewHolderSwitch(
                    LayoutInflater.from(activity).inflate(R.layout.settings_switch, parent, false)
                )
                2 -> ViewHolder(
                    LayoutInflater.from(activity).inflate(R.layout.settings_spacer, parent, false)
                )
                else -> ViewHolder(
                    LayoutInflater.from(activity).inflate(R.layout.settings_default, parent, false)
                )
            }
        }

        override fun getItemViewType(position: Int) = when (settings[position].type) {
            SettingsType.SWITCH -> 1
            SettingsType.SPACER -> 2
            else -> 0
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val settingsElement = settings[position]

            holder.title.text = settingsElement.title
            holder.subText.text = settingsElement.subText

            when (holder) {
                is ViewHolderSwitch -> {
                    holder.switch.isChecked = settingsElement.getValue(activity)
                    holder.switch.setOnCheckedChangeListener { _, b ->
                        settingsElement.onClick(settingsElement.id, b)
                    }
                    holder.root.setOnClickListener {
                        holder.switch.isChecked = !holder.switch.isChecked
                    }
                }
                else -> holder.root.setOnClickListener { settingsElement.onClick(settingsElement.id, null) }
            }

            mutableColor.observe(activity) {
                when (holder) {
                    is ViewHolderSwitch -> holder.switch.setColor(it)
                }
            }
        }

        override fun getItemCount(): Int = settings.size
    }
}

data class SettingsElement(
    val id: String,
    val title: String = "",
    val subText: String = "",
    val type: SettingsType = SettingsType.DEFAULT,
    val onClick: (String, Any?) -> Unit = { _, _ -> }
) {
    constructor(
        id: String,
        @StringRes title: Int,
        @StringRes subText: Int,
        context: Context,
        type: SettingsType = SettingsType.DEFAULT,
        onClick: (String, Any?) -> Unit = { _, _ -> }
    ) : this(id, context.getString(title), context.getString(subText), type, onClick)

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(context: Context): T = when (type) {
        SettingsType.SWITCH -> context.preferences.getBoolean(id, false) as T
        else -> context.preferences.getString(id, "") as T
    }
}

enum class SettingsType {
    DEFAULT,
    SPACER,
    SWITCH
}