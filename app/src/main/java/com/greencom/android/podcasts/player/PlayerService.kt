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
import com.greencom.android.podcasts.repository.PlayerRepository
import com.greencom.android.podcasts.ui.MainActivity
import com.greencom.android.podcasts.utils.PLAYER_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import androidx.media.app.NotificationCompat as MediaNotificationCompat

private const val PLAYER_CHANNEL_ID = "PLAYER_CHANNEL_ID"
private const val PLAYER_NOTIFICATION_ID = 1

// TODO
@AndroidEntryPoint
class PlayerService : MediaSessionService() {

    @Inject lateinit var playerRepository: PlayerRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)

    private var notificationJob: Job? = null

    private lateinit var mediaSession: MediaSession
    private lateinit var player: MediaPlayer

    private var currentEpisodeStartPosition = 0L

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

    @ExperimentalTime
    private val sessionCallback: MediaSession.SessionCallback by lazy {
        object : MediaSession.SessionCallback() {
            override fun onSetMediaUri(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                uri: Uri,
                extras: Bundle?
            ): Int {
                Log.d(PLAYER_TAG,"sessionCallback: onSetMediaUri()")

                updateEpisodeState()

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
                }

                val startPosition = extras?.getLong(EPISODE_START_POSITION) ?: 0L
                currentEpisodeStartPosition = startPosition

                var result = player.setMediaItem(mediaItemBuilder.build()).get()
                if (result.resultCode != SessionPlayer.PlayerResult.RESULT_SUCCESS) {
                    Log.d(PLAYER_TAG, "player.setMediaItem() ERROR ${result.resultCode}")
                }

                result = player.prepare().get()
                if (result.resultCode != SessionPlayer.PlayerResult.RESULT_SUCCESS) {
                    Log.d(PLAYER_TAG, "player.prepare() ERROR ${result.resultCode}")
                }

                result = player.seekTo(startPosition).get()
                if (result.resultCode != SessionPlayer.PlayerResult.RESULT_SUCCESS) {
                    Log.d(PLAYER_TAG, "player.seekTo() ERROR ${result.resultCode}")
                }

                result = player.play().get()
                return result.resultCode
            }
        }
    }

    @ExperimentalTime
    private val playerCallback: MediaPlayer.PlayerCallback by lazy {
        object : MediaPlayer.PlayerCallback() {
            override fun onCurrentMediaItemChanged(player: SessionPlayer, item: MediaItem) {
                updateNotification()
            }

            override fun onPlayerStateChanged(player: SessionPlayer, playerState: Int) {
                Log.d(PLAYER_TAG, "playerCallback: onPlayerStateChanged(), state $playerState")
                updateNotification()

                if ((playerState.isPlayerPaused() || playerState.isPlayerError()) &&
                    player.currentPosition != currentEpisodeStartPosition
                ) {
                    updateEpisodeState()
                }
            }

            override fun onError(mp: MediaPlayer, item: MediaItem, what: Int, extra: Int) {
                Log.d(PLAYER_TAG,"playerCallback: onError(), what $what, extra $extra")
                super.onError(mp, item, what, extra)
            }
        }
    }

    @ExperimentalTime
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

    @ExperimentalTime
    override fun onDestroy() {
        super.onDestroy()
        Log.d(PLAYER_TAG,"PlayerService.onDestroy()")

        updateEpisodeState()

        mediaSession.close()
        player.unregisterPlayerCallback(playerCallback)
        player.close()
        unregisterReceiver(mediaControlReceiver)
        scope.cancel()
        removeNotification()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        Log.d(PLAYER_TAG,"PlayerService.onGetSession()")
        return mediaSession
    }

    private fun safePlay() {
        when {
            player.currentPosition in 0 until player.duration -> {
                player.play()
            }
            player.currentPosition >= player.duration -> {
                player.seekTo(0L)
                player.play()
            }
        }
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

    @ExperimentalTime
    private fun updateEpisodeState() {
        Log.d(PLAYER_TAG, "updateEpisodeState()")
        val episode = CurrentEpisode.from(player.currentMediaItem)
        if (episode.isNotEmpty()) {
            val position = player.currentPosition
            val duration = player.duration
            scope.launch {
                playerRepository.updateEpisodeState(episode.id, position, duration)
            }
        }
    }

    @ExperimentalTime
    private fun updateNotification() {
        val mediaItem = player.currentMediaItem
        if (mediaItem == null) {
            removeNotification()
            return
        }

        notificationJob?.cancel()
        notificationJob = scope.launch {
            val notificationBuilder = NotificationCompat.Builder(this@PlayerService, PLAYER_CHANNEL_ID)
                .setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.media_session_service_notification_ic_music_note)

            val playPauseAction: PendingIntent
            val playPauseIcon: Int
            val playPauseTitle: String
            when (player.playerState) {
                MediaPlayer.PLAYER_STATE_PLAYING -> {
                    playPauseAction = PendingIntent.getBroadcast(
                        this@PlayerService,
                        0,
                        Intent(ACTION_PAUSE),
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                    playPauseIcon = R.drawable.ic_pause_32
                    playPauseTitle = getString(R.string.notification_pause)
                }
                MediaPlayer.PLAYER_STATE_PAUSED -> {
                    playPauseAction = PendingIntent.getBroadcast(
                        this@PlayerService,
                        0,
                        Intent(ACTION_PLAY),
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                    playPauseIcon = R.drawable.ic_play_32
                    playPauseTitle = getString(R.string.notification_play)
                }
                else -> {
                    removeNotification()
                    return@launch
                }
            }

            val episode = CurrentEpisode.from(mediaItem)
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
                PendingIntent.FLAG_CANCEL_CURRENT
            )
            val backwardSkipIcon = R.drawable.ic_backward_10_32
            val backwardSkipTitle = getString(R.string.notification_backward)

            val forwardSkipAction = PendingIntent.getBroadcast(
                this@PlayerService,
                0,
                Intent(ACTION_SKIP_FORWARD),
                PendingIntent.FLAG_CANCEL_CURRENT
            )
            val forwardSkipIcon = R.drawable.ic_forward_30_32
            val forwardSkipTitle = getString(R.string.notification_forward)

            val activityIntent = Intent(this@PlayerService, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val contentIntent = PendingIntent.getActivity(
                this@PlayerService,
                0,
                activityIntent,
                0
            )

            notificationBuilder
                .setContentIntent(contentIntent)
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
            notificationManger.notify(PLAYER_NOTIFICATION_ID, notification)
        } else {
            startForeground(PLAYER_NOTIFICATION_ID, notification)
            isForeground = true
        }
    }

    private fun removeNotification() {
        notificationJob?.cancel()
        notificationManger.cancel(PLAYER_NOTIFICATION_ID)
        stopForeground(true)
        isForeground = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_player_channel_name)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(PLAYER_CHANNEL_ID, name, importance).apply {
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
                ACTION_PLAY -> safePlay()
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

        private const val ACTION_PLAY = "com.greencom.android.podcasts.ACTION_PLAY"
        private const val ACTION_PAUSE = "com.greencom.android.podcasts.ACTION_PAUSE"
        private const val ACTION_SKIP_BACKWARD = "com.greencom.android.podcasts.ACTION_SKIP_BACKWARD"
        private const val ACTION_SKIP_FORWARD = "com.greencom.android.podcasts.ACTION_SKIP_FORWARD"
    }
}