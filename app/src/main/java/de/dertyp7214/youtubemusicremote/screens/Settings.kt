package de.dertyp7214.youtubemusicremote.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.LightingColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import de.dertyp7214.youtubemusicremote.BuildConfig.VERSION_CODE
import de.dertyp7214.youtubemusicremote.BuildConfig.VERSION_NAME
import de.dertyp7214.youtubemusicremote.Config.PLAY_URL
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.fragments.CoverFragment
import de.dertyp7214.youtubemusicremote.services.MediaPlayer
import de.dertyp7214.youtubemusicremote.types.CoverData
import kotlin.math.roundToInt

class Settings : AppCompatActivity() {

    private val code = (Math.random() * 100).roundToInt()

    private val coverLiveData = MutableLiveData<CoverData>()

    private lateinit var adapter: SettingsAdapter

    private val settings by lazy {
        listOf(SettingsElement(
            "appVersion",
            getString(R.string.settings_app_version),
            "$VERSION_NAME ($VERSION_CODE)"
        ), SettingsElement(
            "fromPlayStore",
            R.string.settings_from_playstore,
            if (verifyInstallerId()) R.string.yes else R.string.no,
            this
        ) { _, _ ->
            openUrl(PLAY_URL(packageName))
        }, SettingsElement(
            "currentUrl", getString(R.string.settings_current_url), MediaPlayer.URL
        ), SettingsElement(
            "spacer1", type = SettingsType.SPACER
        ), SettingsElement(
            "visualizeAudio",
            R.string.settings_visualize_audio_title,
            R.string.settings_visualize_audio_subtext,
            this,
            SettingsType.SWITCH
        ) { id, value ->
            if (value is Boolean) preferences.edit {
                putBoolean(id, value)
                CoverFragment.instance?.visualizeAudioPreferenceChanged(value)
            }
        }, SettingsElement(
            "visualizeAudioSize",
            R.string.settings_visualize_audio_size_title,
            R.string.settings_visualize_audio_size_subtext,
            this,
            SettingsType.RANGE(1, 8, visualizeSize),
            { visualizeAudio }
        ) { id, value ->
            if (value is Int) preferences.edit {
                putInt(id, value)
                CoverFragment.instance?.visualizeSize(value)
            }
        }, SettingsElement(
            "useCustomLockScreen",
            R.string.settings_use_custom_lock_screen_title,
            R.string.settings_use_custom_lock_screen_subtext,
            this,
            SettingsType.SWITCH
        ) { id, value ->
            if (value is Boolean) preferences.edit {
                putBoolean(id, value)
                startForegroundService(
                    Intent(
                        applicationContext, MediaPlayer::class.java
                    ).setAction(MediaPlayer.ACTION_LOCK_SCREEN)
                )
            }
        }, SettingsElement("customLockscreenOnlyWhilePlaying",
            R.string.settings_custom_lock_screen_only_while_playing_title,
            R.string.settings_custom_lock_screen_only_while_playing_subtext,
            this,
            SettingsType.SWITCH,
            { useCustomLockScreen }) { id, value ->
            if (value is Boolean) preferences.edit {
                putBoolean(id, value)
            }
        }, SettingsElement("customLockscreenVisualizeAudio",
            R.string.settings_custom_lock_screen_visualize_audio_title,
            R.string.settings_custom_lock_screen_visualize_audio_subtext,
            this,
            SettingsType.SWITCH,
            {
                useCustomLockScreen && visualizeAudio
            }) { id, value ->
            if (value is Boolean) preferences.edit {
                putBoolean(id, value)
            }
        }, SettingsElement(
            "customLockscreenVisualizeAudioSize",
            R.string.settings_visualize_audio_size_title,
            R.string.settings_visualize_audio_size_subtext,
            this,
            SettingsType.RANGE(1, 8, customLockscreenVisualizeAudioSize),
            { useCustomLockScreen && visualizeAudio && customLockscreenVisualizeAudio }
        ) { id, value ->
            if (value is Int) preferences.edit {
                putInt(id, value)
            }
        }, SettingsElement(
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
                        applicationContext, MediaPlayer::class.java
                    ).setAction(MediaPlayer.ACTION_REFETCH)
                )
            }
        })
    }

    private fun setColors(color: Int? = null) {
        fun applyColors(color: Int) {
            adapter.setColor(color)
        }

        if (color == null) {
            coverLiveData.observe(this) { coverData ->
                window.decorView.background = coverData.background?.fitToScreen(this)?.apply {
                    colorFilter = LightingColorFilter(0xFF7B7B7B.toInt(), 0x00000000)
                }

                val coverColor = getFallBackColor(coverData.vibrant, coverData.muted)

                applyColors(coverColor)
            }

            if (coverLiveData.value != null) coverLiveData.postValue(coverLiveData.value)
        } else applyColors(color)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
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
            if (it.coverData != null && coverLiveData.value != it.coverData) coverLiveData.postValue(
                it.coverData!!
            )
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

    override fun onPause() {
        super.onPause()
        overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class SettingsAdapter(
        settings: List<SettingsElement>, private val activity: FragmentActivity
    ) : RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {
        private val mutableColor = MutableLiveData<Int>()

        private val settings: List<SettingsElement>
            get() = field.filter { it.visible(it.id) }

        init {
            setHasStableIds(true)
            this.settings = settings
        }

        override fun getItemId(position: Int): Long {
            return settings[position].hashCode().toLong()
        }

        open class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val root: ViewGroup = v.findViewById(R.id.root)
            val title: TextView = v.findViewById(R.id.title)
            val subText: TextView = v.findViewById(R.id.subText)
        }

        class ViewHolderSwitch(v: View) : ViewHolder(v) {
            val switch: SwitchMaterial = v.findViewById(R.id.switchThumb)
        }

        class ViewHolderRange(v: View) : ViewHolder(v) {
            val seekBar: SeekBar = v.findViewById(R.id.seekBar)
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
                3 -> ViewHolderRange(
                    LayoutInflater.from(activity).inflate(R.layout.settings_range, parent, false)
                )
                else -> ViewHolder(
                    LayoutInflater.from(activity).inflate(R.layout.settings_default, parent, false)
                )
            }
        }

        override fun getItemViewType(position: Int) = when (settings[position].type) {
            SettingsType.SWITCH -> 1
            SettingsType.SPACER -> 2
            SettingsType.RANGE -> 3
            else -> 0
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val settingsElement = settings[position]

            holder.title.text = settingsElement.title
            holder.subText.text = settingsElement.subText.replace(
                "{{value}}",
                settingsElement.getValue<Any>(activity).toString()
            )

            holder.root.setOnClickListener {
                settingsElement.onClick(
                    settingsElement.id, null
                )
            }

            when (holder) {
                is ViewHolderSwitch -> {
                    holder.switch.isChecked = settingsElement.getValue(activity)
                    holder.switch.setOnCheckedChangeListener { _, b ->
                        settingsElement.onClick(settingsElement.id, b)
                        delayed(250) { this@SettingsAdapter.notifyDataSetChanged() }
                    }
                    holder.root.setOnClickListener {
                        holder.switch.isChecked = !holder.switch.isChecked
                    }
                }
                is ViewHolderRange -> {
                    holder.seekBar.min = settingsElement.type.from
                    holder.seekBar.max = settingsElement.type.to
                    holder.seekBar.progress = settingsElement.getValue(activity)
                    holder.seekBar.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onStartTrackingTouch(p0: SeekBar?) {}
                        override fun onStopTrackingTouch(p0: SeekBar?) {}
                        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                            if (p2) {
                                settingsElement.onClick(settingsElement.id, p1)
                                holder.subText.text = settingsElement.subText.replace(
                                    "{{value}}",
                                    settingsElement.getValue<Any>(activity).toString()
                                )
                            }
                        }
                    })
                }
            }

            mutableColor.observe(activity) {
                when (holder) {
                    is ViewHolderSwitch -> holder.switch.setColor(it)
                    is ViewHolderRange -> {
                        holder.seekBar.progressTintList = ColorStateList.valueOf(it)
                        holder.seekBar.thumbTintList = ColorStateList.valueOf(it)
                    }
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
    val visible: (String) -> Boolean = { true },
    val onClick: (String, Any?) -> Unit = { _, _ -> }
) {
    constructor(
        id: String,
        @StringRes title: Int,
        @StringRes subText: Int,
        context: Context,
        type: SettingsType = SettingsType.DEFAULT,
        visible: (String) -> Boolean = { true },
        onClick: (String, Any?) -> Unit = { _, _ -> }
    ) : this(id, context.getString(title), context.getString(subText), type, visible, onClick)

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(context: Context): T = when (type) {
        SettingsType.SWITCH -> context.preferences.getBoolean(id, false) as T
        SettingsType.RANGE -> context.preferences.getInt(id, type.default) as T
        else -> context.preferences.getString(id, "") as T
    }
}

enum class SettingsType {
    DEFAULT, SPACER, SWITCH, RANGE;

    var from: Int = 0
        private set
    var to: Int = 0
        private set
    var default: Int = 0
        private set

    operator fun invoke(
        from: Int = this.from,
        to: Int = this.to,
        default: Int = this.default
    ): SettingsType {
        this.from = from
        this.to = to
        this.default = default
        return this
    }
}