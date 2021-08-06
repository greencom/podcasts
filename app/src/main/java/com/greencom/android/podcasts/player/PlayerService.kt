package com.greencom.android.podcasts.player

import android.app.*
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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.AudioAttributesCompat
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.session.*
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.repository.PlayerRepository
import com.greencom.android.podcasts.ui.MainActivity
import com.greencom.android.podcasts.utils.PLAYER_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import androidx.media.app.NotificationCompat as MediaNotificationCompat

private const val CHANNEL_ID = "PLAYER_CHANNEL_ID"
private const val NOTIFICATION_ID = 1

// TODO
@AndroidEntryPoint
class PlayerService : MediaSessionService() {

    @Inject lateinit var repository: PlayerRepository

    private var scope: CoroutineScope? = null

    private var notificationJob: Job? = null

    private lateinit var mediaSession: MediaSession

    private lateinit var player: MediaPlayer

    private lateinit var mediaControlReceiver: BroadcastReceiver

    private val serviceIntent: Intent by lazy {
        Intent(this, PlayerService::class.java).apply {
            action = SERVICE_INTERFACE
        }
    }

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

    private val isNotPlaying: Boolean
        get() = !isPlaying

    private var isServiceStarted = false

    private var isServiceForeground = false

    private var isServiceBound = false

    @ExperimentalTime
    private val sessionCallback: MediaSession.SessionCallback by lazy {
        object : MediaSession.SessionCallback() {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): SessionCommandGroup {
                Log.d(PLAYER_TAG, "sessionCallback: onConnect()")
                isServiceBound = true
                updateNotification(true)
                return SessionCommandGroup.Builder()
                    .addAllPredefinedCommands(SessionCommand.COMMAND_VERSION_2)
                    .addCommand(SessionCommand(CustomSessionCommand.RESET_PLAYER, null))
                    .build()
            }

            override fun onDisconnected(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ) {
                super.onDisconnected(session, controller)
                Log.d(PLAYER_TAG,"sessionCallback: onDisconnected()")
                isServiceBound = false
            }

            override fun onCreateMediaItem(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaId: String
            ): MediaItem? {
                Log.d(PLAYER_TAG,"sessionCallback: onCreateMediaItem()")
                updateEpisodeState()
                saveLastEpisode()

                resetPlayer()

                var mediaItem: MediaItem? = null
                val audio: String
                val duration: Long
                val title: String
                val podcastTitle: String
                val podcastId: String
                val image: String

                runBlocking {
                    val episode = repository.getEpisode(mediaId) ?: return@runBlocking
                    audio = episode.audio
                    duration = Duration.seconds(episode.audioLength).inWholeMilliseconds
                    title = episode.title
                    podcastTitle = episode.podcastTitle
                    podcastId = episode.podcastId
                    image = episode.image

                    mediaItem = UriMediaItem.Builder(Uri.parse(audio))
                        .setMetadata(MediaMetadata.Builder()
                            .putString(EpisodeMetadata.ID, mediaId)
                            .putString(EpisodeMetadata.TITLE, title)
                            .putString(EpisodeMetadata.PODCAST_TITLE, podcastTitle)
                            .putString(EpisodeMetadata.PODCAST_ID, podcastId)
                            .putString(EpisodeMetadata.IMAGE, image)
                            .putLong(EpisodeMetadata.DURATION, duration)
                            .build())
                        .build()
                }
                return mediaItem
            }

            override fun onCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                command: SessionCommand
            ): Int {
                return when (command.commandCode) {
                    SessionCommand.COMMAND_CODE_PLAYER_PLAY -> {
                        safePlay()
                        SessionResult.RESULT_ERROR_INVALID_STATE
                    }
                    SessionCommand.COMMAND_CODE_PLAYER_PAUSE -> {
                        updateEpisodeState()
                        SessionResult.RESULT_SUCCESS
                    }
                    else -> super.onCommandRequest(session, controller, command)
                }
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle?
            ): SessionResult {
                return when (customCommand.customAction) {
                    CustomSessionCommand.RESET_PLAYER -> {
                        resetPlayer()
                        removeNotification()
                        SessionResult(SessionResult.RESULT_SUCCESS, null)
                    }
                    else -> super.onCustomCommand(session, controller, customCommand, args)
                }
            }
        }
    }

    @ExperimentalTime
    private val playerCallback: MediaPlayer.PlayerCallback by lazy {
        object : MediaPlayer.PlayerCallback() {
            override fun onCurrentMediaItemChanged(player: SessionPlayer, item: MediaItem) {
                Log.d(PLAYER_TAG, "playerCallback: onCurrentMediaItemChanged()")
                updateNotification()
            }

            override fun onPlayerStateChanged(player: SessionPlayer, playerState: Int) {
                Log.d(PLAYER_TAG, "playerCallback: onPlayerStateChanged(), state $playerState")
                updateNotification()
                saveLastEpisode()

                if (playerState == MediaPlayer.PLAYER_STATE_PLAYING && !isServiceStarted) {
                    startService(serviceIntent)
                }

                if (playerState == MediaPlayer.PLAYER_STATE_ERROR) {
                    updateEpisodeState()
                    // Show a toast.
                    scope?.launch {
                        Toast.makeText(
                            this@PlayerService,
                            R.string.player_error,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    @ExperimentalTime
    override fun onCreate() {
        super.onCreate()
        Log.d(PLAYER_TAG,"PlayerService.onCreate()")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
        super.onStartCommand(intent, flags, startId)
        Log.d(PLAYER_TAG,"PlayerService.onStartCommand()")
        isServiceStarted = true
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(PLAYER_TAG,"PlayerService.onBind()")
        return PlayerServiceBinder()
    }

    @ExperimentalTime
    override fun onDestroy() {
        super.onDestroy()
        Log.d(PLAYER_TAG,"PlayerService.onDestroy()")
        mediaSession.close()
        player.unregisterPlayerCallback(playerCallback)
        player.close()
        unregisterReceiver(mediaControlReceiver)
        scope?.cancel()
        removeNotification()
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

    private fun skipBackwardOrForward(value: Long) {
        var position = player.currentPosition + value
        position = when {
            position <= 0L -> 0L
            position >= player.duration -> player.duration
            else -> position
        }
        player.seekTo(position)
    }

    private fun resetPlayer() {
        runBlocking {
            player.reset()
            player.setAudioAttributes(audioAttrs)
            val playbackSpeed = repository.getPlaybackSpeed().first() ?: 1.0F
            player.playbackSpeed = playbackSpeed
        }
    }

    @ExperimentalTime
    private fun updateEpisodeState() {
        val episode = CurrentEpisode.from(player.currentMediaItem)
        if (episode.isNotEmpty()) {
            val position = player.currentPosition
            val duration = player.duration
            scope?.launch {
                repository.updateEpisodeState(episode.id, position, duration)
            }
        }
    }

    @ExperimentalTime
    private fun saveLastEpisode() {
        Log.d(PLAYER_TAG, "saveLastEpisode()")
        val episode = CurrentEpisode.from(player.currentMediaItem)
        if (episode.isNotEmpty()) {
            scope?.launch {
                repository.setLastEpisodeId(episode.id)
            }
        }
    }

    @ExperimentalTime
    private fun updateNotification(forceForeground: Boolean = false) {
        val mediaItem = player.currentMediaItem
        if (mediaItem == null) {
            removeNotification()
            return
        }

        val playerState = player.playerState
        if (
            playerState == MediaPlayer.PLAYER_STATE_IDLE ||
            playerState == MediaPlayer.PLAYER_STATE_ERROR
        ) {
            removeNotification()
            return
        }

        notificationJob?.cancel()
        notificationJob = scope?.launch {
            val notificationBuilder = NotificationCompat.Builder(this@PlayerService, CHANNEL_ID)
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
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    playPauseIcon = R.drawable.ic_pause_32
                    playPauseTitle = getString(R.string.notification_pause)
                }
                MediaPlayer.PLAYER_STATE_PAUSED -> {
                    playPauseAction = PendingIntent.getBroadcast(
                        this@PlayerService,
                        0,
                        Intent(ACTION_PLAY),
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val backwardSkipIcon = R.drawable.ic_backward_10_32
            val backwardSkipTitle = getString(R.string.notification_backward)

            val forwardSkipAction = PendingIntent.getBroadcast(
                this@PlayerService,
                0,
                Intent(ACTION_SKIP_FORWARD),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val deleteIntent = PendingIntent.getBroadcast(
                this@PlayerService,
                0,
                Intent(ACTION_CLOSE),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)
                .setContentTitle(episode.title)
                .setContentText(episode.podcastTitle)
                .setLargeIcon(bitmap)
                .addAction(backwardSkipIcon, backwardSkipTitle, backwardSkipAction)
                .addAction(playPauseIcon, playPauseTitle, playPauseAction)
                .addAction(forwardSkipIcon, forwardSkipTitle, forwardSkipAction)
                .setStyle(MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionCompatToken)
                    .setShowActionsInCompactView(1))
            setNotification(notificationBuilder.build(), forceForeground)
        }
    }

    private fun setNotification(notification: Notification, forceForeground: Boolean) {
        notificationManger.notify(NOTIFICATION_ID, notification)
        if (forceForeground) {
            startForeground(NOTIFICATION_ID, notification)
            isServiceForeground = true
        }

        if (isPlaying && !isServiceForeground) {
            startForeground(NOTIFICATION_ID, notification)
            isServiceForeground = true
        }

        if (isNotPlaying && !isServiceBound) {
            stopForeground(false)
            isServiceForeground = false
        }
    }

    private fun removeNotification() {
        notificationJob?.cancel()
        notificationManger.cancel(NOTIFICATION_ID)
        stopForeground(true)
        isServiceForeground = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_player_channel_name)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                enableVibration(false)
            }
            notificationManger.createNotificationChannel(channel)
        }
    }

    private fun createMediaControlReceiver() {
        mediaControlReceiver = MediaControlReceiver()
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_PLAY)
            addAction(ACTION_SKIP_BACKWARD)
            addAction(ACTION_SKIP_FORWARD)
            addAction(ACTION_CLOSE)
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
                ACTION_SKIP_BACKWARD -> skipBackwardOrForward(PLAYER_SKIP_BACKWARD_VALUE)
                ACTION_SKIP_FORWARD -> skipBackwardOrForward(PLAYER_SKIP_FORWARD_VALUE)
                ACTION_CLOSE -> {
                    stopSelf()
                    isServiceStarted = false
                }
            }
        }
    }

    companion object {
        private const val ACTION_PLAY = "com.greencom.android.podcasts.ACTION_PLAY"
        private const val ACTION_PAUSE = "com.greencom.android.podcasts.ACTION_PAUSE"
        private const val ACTION_SKIP_BACKWARD = "com.greencom.android.podcasts.ACTION_SKIP_BACKWARD"
        private const val ACTION_SKIP_FORWARD = "com.greencom.android.podcasts.ACTION_SKIP_FORWARD"
        private const val ACTION_CLOSE = "com.greencom.android.podcasts.ACTION_CLOSE"
    }
}