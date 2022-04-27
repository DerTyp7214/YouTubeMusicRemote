package de.dertyp7214.youtubemusicremote.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.components.CustomWebSocketListener
import de.dertyp7214.youtubemusicremote.core.checkWebSocket
import de.dertyp7214.youtubemusicremote.core.parseImageColorsAsync
import de.dertyp7214.youtubemusicremote.screens.MainActivity
import de.dertyp7214.youtubemusicremote.types.Action
import de.dertyp7214.youtubemusicremote.types.RepeatMode
import de.dertyp7214.youtubemusicremote.types.SocketResponse
import de.dertyp7214.youtubemusicremote.types.SongInfo
import android.os.Process as OsProcess
import androidx.media.app.NotificationCompat as NotificationCompatMedia

class MediaPlayer : Service() {

    companion object {
        const val ACTION_DISLIKE = "action_dislike"
        const val ACTION_PREVIOUS = "action_previous"
        const val ACTION_PLAY_PAUSE = "action_playPause"
        const val ACTION_NEXT = "action_next"
        const val ACTION_LIKE = "action_like"
        const val ACTION_REPEAT = "action_repeat"
        const val ACTION_SHUFFLE = "action_shuffle"
        const val ACTION_STOP = "action_stop"
        const val ACTION_REFETCH = "action_refetch"

        const val CHANNEL_ID = "MediaNotification"

        private var currentSongInfo = SongInfo()

        var isRunning = false
            private set
    }

    val webSocket: CustomWebSocket?
        get() {
            return CustomWebSocket.webSocketInstance
        }

    private var initialized = false

    private val gson = Gson().newBuilder().enableComplexMapKeySerialization().create()

    private val mediaStatus = MutableLiveData(MediaStatus())
    private val metadata = MutableLiveData(MetaData())

    private val mediaSession by lazy { MediaSessionCompat(applicationContext, "mediaSession") }

    private val notificationManager by lazy { NotificationManagerCompat.from(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun init() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW
            )
        )

        mediaStatus.observeForever {
            setMediaSessionPlaybackState(it)
            buildNotification(it)
        }
        metadata.observeForever {
            setMediaSessionMetaData(it)
            mediaStatus.value?.let { status -> buildNotification(status) }
        }

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                webSocket?.playPause()
            }

            override fun onPause() {
                super.onPause()
                webSocket?.playPause()
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                webSocket?.previous()
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                webSocket?.next()
            }

            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                webSocket?.seek((pos / 1000).toInt())
            }

            override fun onCustomAction(action: String?, extras: Bundle?) {
                super.onCustomAction(action, extras)
                handleCustomAction(action)
            }

            override fun onStop() {
                super.onStop()
                startService(
                    Intent(applicationContext, MediaPlayer::class.java).setAction(ACTION_STOP)
                )
            }
        })

        if (webSocket == null) {
            val urls = PreferenceManager.getDefaultSharedPreferences(
                applicationContext
            ).getStringSet("url", setOf())
            var url: String? = null
            fun checkUrls(urls: List<String>, index: Int) {
                checkWebSocket(urls[index], gson) { connected, _ ->
                    if (connected) url = urls[index]
                    else if (index < urls.lastIndex) checkUrls(urls, index + 1)
                }
            }
            checkUrls(urls?.toList() ?: listOf(), 0)
            url?.let { CustomWebSocket(it, CustomWebSocketListener()).setInstance() }
        }
        webSocket?.webSocketListener?.apply {
            onMessage { _, text ->
                try {
                    val socketResponse = gson.fromJson(text, SocketResponse::class.java)

                    when (socketResponse.action) {
                        Action.SONG_INFO -> {
                            gson.fromJson(
                                socketResponse.data, SongInfo::class.java
                            ).parseImageColorsAsync(applicationContext, currentSongInfo) {
                                currentSongInfo = it
                                MainActivity.currentSongInfo.postValue(it)
                                startForegroundService(
                                    Intent(
                                        applicationContext,
                                        MediaPlayer::class.java
                                    ).setAction(ACTION_REFETCH)
                                )
                            }
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        initialized = true
    }

    private fun handleCustomAction(action: String?) {
        when (action) {
            ACTION_DISLIKE -> webSocket?.dislike()
            ACTION_PREVIOUS -> webSocket?.previous()
            ACTION_PLAY_PAUSE -> webSocket?.playPause()
            ACTION_NEXT -> webSocket?.next()
            ACTION_LIKE -> webSocket?.like()
            ACTION_REPEAT -> webSocket?.repeat()
            ACTION_SHUFFLE -> webSocket?.shuffle()
            ACTION_STOP -> {
                webSocket?.close()
                stopForeground(true)
                OsProcess.killProcess(OsProcess.myPid())
            }
            ACTION_REFETCH -> {
                val songInfo = currentSongInfo
                MediaStatus(
                    songInfo.isPaused == false,
                    songInfo.songDuration.toLong() * 1000,
                    songInfo.elapsedSeconds.toLong() * 1000,
                    songInfo.repeatMode
                ).apply { if (mediaStatus.value != this) mediaStatus.value = this }
                MetaData(
                    songInfo.title,
                    songInfo.artist,
                    songInfo.album,
                    songInfo.liked,
                    songInfo.disliked,
                    songInfo.coverData?.cover?.toBitmap()
                ).apply { if (metadata.value != this) metadata.value = this }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null || intent.action == null) return

        handleCustomAction(intent.action)
    }

    private fun generatePendingIntent(action: String): PendingIntent {
        return PendingIntent.getService(
            applicationContext,
            1,
            Intent(applicationContext, MediaPlayer::class.java).setAction(action),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun generateAction(
        icon: Int, title: String, action: String
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            icon, title, generatePendingIntent(action)
        ).build()
    }

    private fun setMediaSessionMetaData(metaData: MetaData) {
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder().putBitmap(
                MediaMetadata.METADATA_KEY_ALBUM_ART, metaData.cover
            ).putText(MediaMetadata.METADATA_KEY_TITLE, metaData.title)
                .putText(MediaMetadata.METADATA_KEY_ARTIST, metaData.artist)
                .putText(MediaMetadata.METADATA_KEY_ALBUM, metaData.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, mediaStatus.value?.duration ?: -1)
                .putRating(
                    MediaMetadata.METADATA_KEY_RATING,
                    RatingCompat.newThumbRating(metaData.liked)
                ).build()
        )
    }

    private fun setMediaSessionPlaybackState(mediaStatus: MediaStatus) {
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder().apply {
                setState(
                    if (!mediaStatus.playing) PlaybackStateCompat.STATE_PAUSED else PlaybackStateCompat.STATE_PLAYING,
                    mediaStatus.progress,
                    1f
                )
                setActions(
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            or PlaybackStateCompat.ACTION_PAUSE
                            or PlaybackStateCompat.ACTION_PLAY
                            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                            or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                            or PlaybackStateCompat.ACTION_SEEK_TO
                )
                when (mediaStatus.repeatMode) {
                    RepeatMode.ONE -> addCustomAction(
                        ACTION_REPEAT,
                        "Repeat",
                        R.drawable.ic_repeat_once
                    )
                    RepeatMode.ALL -> addCustomAction(ACTION_REPEAT, "Repeat", R.drawable.ic_repeat)
                    RepeatMode.NONE -> addCustomAction(
                        ACTION_REPEAT,
                        "Repeat",
                        R.drawable.ic_repeat_off
                    )
                }
                addCustomAction(ACTION_SHUFFLE, "Shuffle", R.drawable.ic_shuffle)
            }.build()
        )
    }

    private fun buildNotification(mediaStatus: MediaStatus) {
        val mediaStyle =
            NotificationCompatMedia.MediaStyle().setMediaSession(mediaSession.sessionToken)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_launcher_small)
            setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    1,
                    Intent(applicationContext, MainActivity::class.java),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE
                    else PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            setDeleteIntent(generatePendingIntent(ACTION_STOP))
            setStyle(mediaStyle)

            when (mediaStatus.repeatMode) {
                RepeatMode.NONE -> addAction(
                    generateAction(
                        R.drawable.ic_repeat_off,
                        "Repeat",
                        ACTION_REPEAT
                    )
                )
                RepeatMode.ONE -> addAction(
                    generateAction(
                        R.drawable.ic_repeat_once,
                        "Repeat",
                        ACTION_REPEAT
                    )
                )
                RepeatMode.ALL -> addAction(
                    generateAction(
                        R.drawable.ic_repeat,
                        "Repeat",
                        ACTION_REPEAT
                    )
                )
            }
            addAction(generateAction(R.drawable.ic_previous, "Previews", ACTION_PREVIOUS))
            if (mediaStatus.playing)
                addAction(generateAction(R.drawable.ic_pause, "Pause", ACTION_PLAY_PAUSE))
            else addAction(generateAction(R.drawable.ic_play, "Play", ACTION_PLAY_PAUSE))
            addAction(generateAction(R.drawable.ic_next, "Next", ACTION_NEXT))
            addAction(generateAction(R.drawable.ic_close, "Stop", ACTION_STOP))
        }
        mediaStyle.setShowCancelButton(true)
        mediaStyle.setCancelButtonIntent(generatePendingIntent(ACTION_STOP))
        mediaStyle.setShowActionsInCompactView(1, 2, 3)
        startForeground(1, notification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!initialized) init()

        handleIntent(intent)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        isRunning = true
        super.onCreate()
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    data class MediaStatus(
        val playing: Boolean = false,
        val duration: Long = -1,
        val progress: Long = -1,
        val repeatMode: RepeatMode = RepeatMode.NONE
    )

    data class MetaData(
        val title: String = "",
        val artist: String = "",
        val album: String = "",
        val liked: Boolean = false,
        val disliked: Boolean = false,
        val cover: Bitmap? = null,
    )

    class Restarter : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context?.startForegroundService(Intent(context, MediaPlayer::class.java))
        }
    }
}