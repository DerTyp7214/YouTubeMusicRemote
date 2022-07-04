package de.dertyp7214.youtubemusicremote.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.slider.Slider
import de.dertyp7214.composecomponents.ComposeSwitch
import de.dertyp7214.youtubemusicremote.BuildConfig.VERSION_CODE
import de.dertyp7214.youtubemusicremote.BuildConfig.VERSION_NAME
import de.dertyp7214.youtubemusicremote.Config.PLAY_URL
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.core.*
import de.dertyp7214.youtubemusicremote.screens.MainActivity
import de.dertyp7214.youtubemusicremote.services.MediaPlayer
import de.dertyp7214.youtubemusicremote.types.CoverData

class SettingsFragment : Fragment() {
    private val coverLiveData = MutableLiveData<CoverData>()

    private lateinit var adapter: SettingsAdapter
    private lateinit var layoutView: View

    private val window by lazy { requireActivity().window }
    private fun <T : View> findViewById(@IdRes id: Int) = layoutView.findViewById<T>(id)
    private fun verifyInstallerId() = requireActivity().verifyInstallerId()
    private fun openUrl(url: String) = requireActivity().openUrl(url)

    private val settings by lazy {
        listOf(SettingsElement(
            "appVersion",
            getString(R.string.settings_app_version),
            "$VERSION_NAME ($VERSION_CODE)"
        ), SettingsElement(
            "fromPlayStore",
            R.string.settings_from_playstore,
            if (verifyInstallerId()) R.string.yes else R.string.no,
            requireContext()
        ) { _, _ ->
            openUrl(PLAY_URL(requireActivity().packageName))
        }, SettingsElement(
            "currentUrl", getString(R.string.settings_current_url), MediaPlayer.URL
        ), SettingsElement(
            "spacer1", type = SettingsType.SPACER
        ), SettingsElement(
            "playlistColumns",
            R.string.settings_playlist_columns_title,
            R.string.settings_playlist_columns_subtext,
            requireContext(),
            SettingsType.RANGE(1, 20, playlistColumns)
        ) { id, value ->
            if (value is Int) preferences.edit {
                putInt(id, value)
            }
        }, SettingsElement(
            "visualizeAudio",
            R.string.settings_visualize_audio_title,
            R.string.settings_visualize_audio_subtext,
            requireContext(),
            SettingsType.SWITCH
        ) { id, value ->
            if (value is Boolean) preferences.edit {
                putBoolean(id, value)
                CoverFragment.instance?.visualizeAudioPreferenceChanged(value)
            }
        },SettingsElement(
            "mirrorBars",
            R.string.settings_visualize_audio_mirror_bars_title,
            R.string.settings_visualize_audio_mirror_bars_subtext,
            requireContext(),
            SettingsType.SWITCH,
            { visualizeAudio }
        ) { id, value ->
            if (value is Boolean) preferences.edit {
                putBoolean(id, value)
            }
        }, SettingsElement(
            "visualizeAudioSize",
            R.string.settings_visualize_audio_size_title,
            R.string.settings_visualize_audio_size_subtext,
            requireContext(),
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
            requireContext(),
            SettingsType.SWITCH
        ) { id, value ->
            if (value is Boolean) preferences.edit {
                putBoolean(id, value)
                requireActivity().startForegroundService(
                    Intent(
                        requireContext().applicationContext, MediaPlayer::class.java
                    ).setAction(MediaPlayer.ACTION_LOCK_SCREEN)
                )
            }
        }, SettingsElement("customLockscreenOnlyWhilePlaying",
            R.string.settings_custom_lock_screen_only_while_playing_title,
            R.string.settings_custom_lock_screen_only_while_playing_subtext,
            requireContext(),
            SettingsType.SWITCH,
            { useCustomLockScreen }) { id, value ->
            if (value is Boolean) preferences.edit {
                putBoolean(id, value)
            }
        }, SettingsElement("customLockscreenVisualizeAudio",
            R.string.settings_custom_lock_screen_visualize_audio_title,
            R.string.settings_custom_lock_screen_visualize_audio_subtext,
            requireContext(),
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
            requireContext(),
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
            requireContext(),
            SettingsType.SWITCH
        ) { id, value ->
            if (value is Boolean) preferences.edit {
                putBoolean(id, value)
                requireActivity().startForegroundService(
                    Intent(
                        requireContext().applicationContext, MediaPlayer::class.java
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
            coverLiveData.observe(requireActivity()) { coverData ->
                coverData.background?.fitToScreen(requireActivity())?.apply {
                    colorFilter = LightingColorFilter(0xFF7B7B7B.toInt(), 0x00000000)
                }

                val coverColor = getFallBackColor(coverData.vibrant, coverData.muted)

                applyColors(coverColor)
            }

            if (coverLiveData.value != null) coverLiveData.postValue(coverLiveData.value)
        } else applyColors(color)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        layoutView = inflater.inflate(R.layout.fragment_settings, container, false)

        val view = findViewById<ViewGroup>(R.id.view)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        toolbar.title = getString(R.string.settings)
        toolbar.setNavigationIconTint(Color.WHITE)
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        toolbar.setNavigationOnClickListener {
            MainActivity.run {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        adapter = SettingsAdapter(settings, requireActivity())

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        MainActivity.currentSongInfo.observe(requireActivity()) {
            if (it.coverData != null && coverLiveData.value != it.coverData) coverLiveData.postValue(
                it.coverData!!
            )
        }

        view.setHeight(getStatusBarHeight())

        setColors()

        return layoutView
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
            val composeSwitch: ComposeSwitch = v.findViewById(R.id.composeSwitch)
        }

        class ViewHolderRange(v: View) : ViewHolder(v) {
            val seekBar: Slider = v.findViewById(R.id.seekBar)
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
                    holder.composeSwitch.setChecked(settingsElement.getValue(activity), false)
                    holder.composeSwitch.setCheckedChangedListener {
                        settingsElement.onClick(settingsElement.id, it)
                        delayed(250) { this@SettingsAdapter.notifyDataSetChanged() }
                    }
                    holder.root.setOnClickListener {
                        holder.composeSwitch.setChecked(!holder.composeSwitch.getChecked())
                    }
                }
                is ViewHolderRange -> {
                    holder.seekBar.valueFrom = settingsElement.typeData.from.toFloat()
                    holder.seekBar.valueTo = settingsElement.typeData.to.toFloat()
                    holder.seekBar.value = settingsElement.getValue<Int>(activity).toFloat()
                    holder.seekBar.onProgressChanged { progress, userInput ->
                        if (userInput) {
                            @Suppress("RemoveRedundantCallsOfConversionMethods")
                            settingsElement.onClick(settingsElement.id, progress.toInt())
                            holder.subText.text = settingsElement.subText.replace(
                                "{{value}}",
                                settingsElement.getValue<Int>(activity).toString()
                            )
                        }
                    }
                }
            }

            mutableColor.observe(activity) {
                when (holder) {
                    is ViewHolderSwitch -> holder.composeSwitch.setColor(it)
                    is ViewHolderRange -> {
                        holder.seekBar.setColor(it)
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

    constructor(
        id: String,
        @StringRes title: Int,
        @StringRes subText: Int,
        context: Context,
        data: SettingsType.Data,
        visible: (String) -> Boolean = { true },
        onClick: (String, Any?) -> Unit = { _, _ -> }
    ) : this(id, context.getString(title), context.getString(subText), data(id), visible, onClick)

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(context: Context): T = when (type) {
        SettingsType.SWITCH -> context.defaultValue(id, false as T)
        SettingsType.RANGE -> context.preferences.getInt(
            id,
            type(id).default
        ) as T
        else -> context.preferences.getString(id, "") as T
    }

    val typeData = type(id)
}

enum class SettingsType {
    DEFAULT, SPACER, SWITCH, RANGE;

    private val map = HashMap<String, Data>()

    operator fun invoke(
        from: Int = 0,
        to: Int = 0,
        default: Int = 0
    ): Data {
        return Data(from, to, default, this)
    }

    operator fun invoke(key: String) = map[key] ?: Data(0, 0, 0, RANGE)

    data class Data(
        val from: Int = 0,
        val to: Int = 0,
        val default: Int = 0,
        private val type: SettingsType
    ) {
        operator fun invoke(key: String): SettingsType {
            type.map[key] = this
            return type
        }
    }
}