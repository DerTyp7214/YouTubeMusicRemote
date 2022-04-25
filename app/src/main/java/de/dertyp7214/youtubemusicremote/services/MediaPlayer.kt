package de.dertyp7214.youtubemusicremote.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.core.parseImageColorsAsync
import de.dertyp7214.youtubemusicremote.screens.MainActivity
import de.dertyp7214.youtubemusicremote.types.Action
import de.dertyp7214.youtubemusicremote.types.SocketResponse
import de.dertyp7214.youtubemusicremote.types.SongInfo
import androidx.media.app.NotificationCompat as NotificationCompatMedia

class MediaPlayer : Service() {

    companion object {
        const val ACTION_DISLIKE = "action_dislike"
        const val ACTION_PREVIOUS = "action_previous"
        const val ACTION_PLAY_PAUSE = "action_playPause"
        const val ACTION_NEXT = "action_next"
        const val ACTION_LIKE = "action_like"
        const val ACTION_STOP = "action_stop"
        const val ACTION_REFETCH = "action_refetch"

        const val CHANNEL_ID = "MediaNotification"
        const val NOTIFICATION_ID = 187

        private var currentSongInfo = SongInfo()
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
        })

        webSocket?.webSocketListener?.onMessage { _, text ->
            try {
                val socketResponse = gson.fromJson(text, SocketResponse::class.java)

                when (socketResponse.action) {
                    Action.SONG_INFO -> {
                        gson.fromJson(
                            socketResponse.data, SongInfo::class.java
                        ).parseImageColorsAsync(applicationContext, currentSongInfo) {
                            currentSongInfo = it
                            MainActivity.currentSongInfo.postValue(it)
                            startService(
                                Intent(
                                    this,
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

        initialized = true
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null || intent.action == null) return

        when (intent.action) {
            ACTION_DISLIKE -> webSocket?.dislike()
            ACTION_PREVIOUS -> webSocket?.previous()
            ACTION_PLAY_PAUSE -> webSocket?.playPause()
            ACTION_NEXT -> webSocket?.next()
            ACTION_LIKE -> webSocket?.like()
            ACTION_REFETCH -> {
                val songInfo = currentSongInfo
                MediaStatus(
                    songInfo.isPaused == false,
                    songInfo.songDuration.toLong() * 1000,
                    songInfo.elapsedSeconds.toLong() * 1000
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
            PlaybackStateCompat.Builder().setState(
                if (!mediaStatus.playing) PlaybackStateCompat.STATE_PAUSED else PlaybackStateCompat.STATE_PLAYING,
                mediaStatus.progress,
                1f
            ).setActions(PlaybackStateCompat.ACTION_SEEK_TO).build()
        )
    }

    private fun buildNotification(mediaStatus: MediaStatus) {
        val mediaStyle =
            NotificationCompatMedia.MediaStyle().setMediaSession(mediaSession.sessionToken)

        val metaData = this.metadata.value ?: return

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

            if (metaData.disliked)
                addAction(generateAction(R.drawable.ic_disliked, "Dislike", ACTION_DISLIKE))
            else addAction(generateAction(R.drawable.ic_dislike, "Dislike", ACTION_DISLIKE))
            addAction(generateAction(R.drawable.ic_previous, "Previews", ACTION_PREVIOUS))
            if (mediaStatus.playing)
                addAction(generateAction(R.drawable.ic_pause, "Pause", ACTION_PLAY_PAUSE))
            else addAction(generateAction(R.drawable.ic_play, "Play", ACTION_PLAY_PAUSE))
            addAction(generateAction(R.drawable.ic_next, "Next", ACTION_NEXT))
            if (metaData.liked)
                addAction(generateAction(R.drawable.ic_liked, "Like", ACTION_LIKE))
            else addAction(generateAction(R.drawable.ic_like, "Like", ACTION_LIKE))
        }
        mediaStyle.setShowActionsInCompactView(1, 2, 3)
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!initialized) init()

        handleIntent(intent)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    data class MediaStatus(
        val playing: Boolean = false,
        val duration: Long = -1,
        val progress: Long = -1
    )

    data class MetaData(
        val title: String = "",
        val artist: String = "",
        val album: String = "",
        val liked: Boolean = false,
        val disliked: Boolean = false,
        val cover: Bitmap? = null,
    )
}