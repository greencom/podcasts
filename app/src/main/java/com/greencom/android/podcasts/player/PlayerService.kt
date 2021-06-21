package com.greencom.android.podcasts.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.AudioAttributesCompat
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.session.MediaSession
import androidx.media2.session.MediaSessionService
import androidx.media2.session.SessionToken
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.utils.PLAYER_TAG
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import androidx.media.app.NotificationCompat as MediaNotificationCompat

// TODO
class PlayerService : MediaSessionService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)

    private var notificationJob: Job? = null

    private lateinit var mediaSession: MediaSession
    private lateinit var player: MediaPlayer

    private lateinit var mediaControlReceiver: BroadcastReceiver

    private var isForeground = false

    private val notificationManger: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(this)
    }

    private val audioAttrs: AudioAttributesCompat by lazy {
        AudioAttributesCompat.Builder()
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
            .build()
    }

    private val isPlaying: Boolean
        get() = player.playerState == MediaPlayer.PLAYER_STATE_PLAYING

    private val isPaused: Boolean
        get() = player.playerState == MediaPlayer.PLAYER_STATE_PAUSED

    private val sessionCallback: MediaSession.SessionCallback by lazy {
        object : MediaSession.SessionCallback() {
            override fun onSetMediaUri(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                uri: Uri,
                extras: Bundle?
            ): Int {
                Log.d(PLAYER_TAG,"sessionCallback: onSetMediaUri()")
                resetPlayer()

                val mediaItemBuilder = UriMediaItem.Builder(uri)
                if (extras != null) {
                    mediaItemBuilder
                        .setMetadata(MediaMetadata.Builder()
                                .putString(EPISODE_ID, extras.getString(EPISODE_ID))
                                .putString(EPISODE_TITLE, extras.getString(EPISODE_TITLE))
                                .putString(EPISODE_PUBLISHER, extras.getString(EPISODE_PUBLISHER))
                                .putString(EPISODE_IMAGE, extras.getString(EPISODE_IMAGE))
                                .putLong(EPISODE_DURATION, extras.getLong(EPISODE_DURATION))
                                .build())
                        .setStartPosition(extras.getLong(EPISODE_START_POSITION))
                }

                val result = player.setMediaItem(mediaItemBuilder.build()).get()
                if (result.resultCode != SessionPlayer.PlayerResult.RESULT_SUCCESS) {
                    Log.d(PLAYER_TAG, "player.setMediaItem() ERROR ${result.resultCode}")
                }
                return result.resultCode
            }
        }
    }

    private val playerCallback: MediaPlayer.PlayerCallback by lazy {
        object : MediaPlayer.PlayerCallback() {
            override fun onCurrentMediaItemChanged(player: SessionPlayer, item: MediaItem) {
                updateNotification(item, player.playerState)
            }

            override fun onPlayerStateChanged(player: SessionPlayer, playerState: Int) {
                updateNotification(player.currentMediaItem, playerState)
            }

            override fun onError(mp: MediaPlayer, item: MediaItem, what: Int, extra: Int) {
                Log.d(PLAYER_TAG,"playerCallback: onError(), what $what, extra $extra")
                super.onError(mp, item, what, extra)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(PLAYER_TAG,"PlayerService.onCreate()")

        player = MediaPlayer(this).apply {
            registerPlayerCallback(Executors.newSingleThreadExecutor(), playerCallback)
            setAudioAttributes(audioAttrs)
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionCallback(Executors.newSingleThreadExecutor(), sessionCallback)
            .build()

        createNotificationChannel()
        createMediaControlReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(PLAYER_TAG,"PlayerService.onStartCommand()")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(PLAYER_TAG,"PlayerService.onBind()")
        return PlayerServiceBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(PLAYER_TAG,"PlayerService.onUnbind()")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(PLAYER_TAG,"PlayerService.onDestroy()")
        mediaSession.close()
        player.unregisterPlayerCallback(playerCallback)
        player.close()
        unregisterReceiver(mediaControlReceiver)
        scope.cancel()
        removeNotification()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        Log.d(PLAYER_TAG,"PlayerService.onGetSession()")
        return mediaSession
    }

    private fun skipBackwardOrForward(skipValue: Int) {
        val value = player.currentPosition + skipValue
        val newPosition = when {
            value <= 0L -> 0L
            value >= player.duration -> player.duration
            else -> value
        }
        player.seekTo(newPosition)
    }

    private fun resetPlayer() {
        player.reset()
        player.setAudioAttributes(audioAttrs)
    }

    private fun updateNotification(mediaItem: MediaItem?, playerState: Int) {
        if (mediaItem == null) {
            removeNotification()
            return
        }

        notificationJob?.cancel()
        notificationJob = scope.launch {
            val notificationBuilder = NotificationCompat.Builder(this@PlayerService, CHANNEL_ID)
                .setOngoing(true)
                .setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.media_session_service_notification_ic_music_note)

            val playPauseAction: PendingIntent
            val playPauseIcon: Int
            val playPauseTitle: String
            when (playerState) {
                MediaPlayer.PLAYER_STATE_PLAYING -> {
                    playPauseAction = PendingIntent.getBroadcast(
                        this@PlayerService,
                        0,
                        Intent(ACTION_PAUSE),
                        0
                    )
                    playPauseIcon = R.drawable.ic_pause_24
                    playPauseTitle = getString(R.string.notification_pause)
                }
                MediaPlayer.PLAYER_STATE_PAUSED -> {
                    playPauseAction = PendingIntent.getBroadcast(
                        this@PlayerService,
                        0,
                        Intent(ACTION_PLAY),
                        0
                    )
                    playPauseIcon = R.drawable.ic_play_24
                    playPauseTitle = getString(R.string.notification_play)
                }
                else -> {
                    removeNotification()
                    return@launch
                }
            }

            val episode = PlayerServiceConnection.CurrentEpisode.from(mediaItem)
            val loader = baseContext.imageLoader
            val request = ImageRequest.Builder(this@PlayerService)
                .data(episode.image)
                .build()
            val result = (loader.execute(request) as SuccessResult).drawable
            val bitmap = (result as BitmapDrawable).bitmap

            val backwardSkipAction = PendingIntent.getBroadcast(
                this@PlayerService,
                0,
                Intent(ACTION_SKIP_BACKWARD),
                0
            )
            val backwardSkipIcon = R.drawable.ic_backward_10_24
            val backwardSkipTitle = getString(R.string.notification_backward)

            val forwardSkipAction = PendingIntent.getBroadcast(
                this@PlayerService,
                0,
                Intent(ACTION_SKIP_FORWARD),
                0
            )
            val forwardSkipIcon = R.drawable.ic_forward_30_24
            val forwardSkipTitle = getString(R.string.notification_forward)

            notificationBuilder
                .setContentTitle(episode.title)
                .setContentText(episode.publisher)
                .setLargeIcon(bitmap)
                .addAction(backwardSkipIcon, backwardSkipTitle, backwardSkipAction)
                .addAction(playPauseIcon, playPauseTitle, playPauseAction)
                .addAction(forwardSkipIcon, forwardSkipTitle, forwardSkipAction)
                .setStyle(MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionCompatToken)
                    .setShowActionsInCompactView(1))
            setNotification(notificationBuilder.build())
        }
    }

    private fun setNotification(notification: Notification) {
        if (isForeground) {
            notificationManger.notify(NOTIFICATION_ID, notification)
        } else {
            startForeground(NOTIFICATION_ID, notification)
            isForeground = true
        }
    }

    private fun removeNotification() {
        notificationJob?.cancel()
        notificationManger.cancel(NOTIFICATION_ID)
        stopForeground(true)
        isForeground = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_player_channel_name)
            val importance = NotificationManager.IMPORTANCE_NONE
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                enableVibration(false)
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createMediaControlReceiver() {
        mediaControlReceiver = MediaControlReceiver()
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_PLAY)
            addAction(ACTION_SKIP_BACKWARD)
            addAction(ACTION_SKIP_FORWARD)
        }
        registerReceiver(mediaControlReceiver, filter)
    }

    inner class PlayerServiceBinder : Binder() {
        val sessionToken: SessionToken
            get() = mediaSession.token
    }

    inner class MediaControlReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY -> player.play()
                ACTION_PAUSE -> player.pause()
                ACTION_SKIP_BACKWARD -> skipBackwardOrForward(SKIP_BACKWARD_VALUE)
                ACTION_SKIP_FORWARD -> skipBackwardOrForward(SKIP_FORWARD_VALUE)
            }
        }
    }

    companion object {
        const val EPISODE_ID = MediaMetadata.METADATA_KEY_MEDIA_ID
        const val EPISODE_TITLE = MediaMetadata.METADATA_KEY_TITLE
        const val EPISODE_PUBLISHER = MediaMetadata.METADATA_KEY_AUTHOR
        const val EPISODE_IMAGE = MediaMetadata.METADATA_KEY_ART_URI
        const val EPISODE_DURATION = MediaMetadata.METADATA_KEY_DURATION
        const val EPISODE_START_POSITION = "EPISODE_START_POSITION"

        const val SKIP_FORWARD_VALUE = 30_000
        const val SKIP_BACKWARD_VALUE = -10_000

        private const val CHANNEL_ID = "CHANNEL_PLAYER"
        private const val NOTIFICATION_ID = 1

        private const val ACTION_PLAY = "com.greencom.android.podcasts.ACTION_PLAY"
        private const val ACTION_PAUSE = "com.greencom.android.podcasts.ACTION_PAUSE"
        private const val ACTION_SKIP_BACKWARD = "com.greencom.android.podcasts.ACTION_SKIP_BACKWARD"
        private const val ACTION_SKIP_FORWARD = "com.greencom.android.podcasts.ACTION_SKIP_FORWARD"
    }
}