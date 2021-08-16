package com.greencom.android.podcasts.player

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.UriMediaItem
import androidx.media2.session.*
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.android.exoplayer2.C.CONTENT_TYPE_SPEECH
import com.google.android.exoplayer2.C.USAGE_MEDIA
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.media2.DefaultMediaItemConverter
import com.google.android.exoplayer2.ext.media2.SessionCallbackBuilder
import com.google.android.exoplayer2.ext.media2.SessionPlayerConnector
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.player.PlayerService.MediaControlReceiver
import com.greencom.android.podcasts.player.PlayerService.PlayerServiceBinder
import com.greencom.android.podcasts.repository.PlayerRepository
import com.greencom.android.podcasts.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.time.ExperimentalTime

/** Notification channel ID used for the player. */
private const val PLAYER_CHANNEL_ID = "PLAYER_CHANNEL_ID"

/** Notification ID used for the player. */
private const val PLAYER_NOTIFICATION_ID = 1

// Seeking increments in ms used by exoPlayer.
private const val SEEK_BACK_INCREMENT = 10_000L
private const val SEEK_FORWARD_INCREMENT = 30_000L

/**
 * The service used for media playback using Media2 and ExoPlayer, use [PlayerServiceConnection]
 * to communicate with the MediaSession. Also responsible for displaying a media notification
 * while playing. The service is foreground only if the `ExoPlayer.isPlaying` returns `true`.
 * Otherwise the media notification can be dismissed, resulting in a call to `stopSelf()`.
 * Media notification controls are handled by the [MediaControlReceiver].
 *
 * PlayerService is bound and started, use [PlayerServiceBinder] for the binding.
 */
@AndroidEntryPoint
class PlayerService : MediaSessionService() {

    /** [PlayerRepository] provides access to the player-related Room and DataStore methods. */
    @Inject lateinit var playerRepository: PlayerRepository

    /** The instance of Media2 MediaSession. Uses [sessionCallback]. */
    private lateinit var mediaSession: MediaSession

    /**
     * The instance of ExoPlayer. Uses [exoPlayerListener] and [audioAttributes]. Use
     * [exoPlayerHandler] to get access to the player from another threads.
     */
    private lateinit var exoPlayer: SimpleExoPlayer

    /**
     * ExoPlayer wrapper for the [exoPlayer] that allows to use it within the
     * [mediaSession]'s [sessionCallback].
     */
    private lateinit var sessionPlayerConnector: SessionPlayerConnector

    /** The instance of the [MediaControlReceiver] that handles the player notification controls. */
    private lateinit var mediaControlReceiver: MediaControlReceiver

    /**
     * PlayerService [CoroutineScope]. Uses [SupervisorJob] and [Dispatchers.Main].
     * Cancels on [onDestroy].
     */
    private var scope: CoroutineScope? = null

    /**
     * The [Job] that manages the updating of the player media notification, see
     * [updatePlayerNotification].
     */
    private var playerNotificationJob: Job? = null

    /**
     * The [Job] that runs the sleep timer. See [_sleepTimerRemainingTime], [setSleepTimer]
     * and [removeSleepTimer].
     */
    private var sleepTimerJob: Job? = null

    /**
     * [MutableStateFlow] that contains the remaining time of the sleep timer. See [sleepTimerJob].
     */
    private val _sleepTimerRemainingTime = MutableStateFlow(Long.MIN_VALUE)

    /**
     * [MutableStateFlow] that contains the current state of the [exoPlayer],
     * see [updateExoPlayerState].
     */
    private val _exoPlayerState = MutableStateFlow(Player.STATE_IDLE)

    /**
     * [MutableStateFlow] that contains the current `isPlaying` state of the [exoPlayer],
     * see [updateIsPlaying].
     */
    private val _isPlaying = MutableStateFlow(false)

    /**
     * Whether the service is started. [stopSelf] will be called if the user dismisses
     * the player media notification.
     */
    private var isServiceStarted = false

    /** Whether the service is bound. */
    private var isServiceBound = false

    /**
     * Whether the service is foreground. The service should be foreground when
     * `exoPlayer.isPlaying` returns true to prevent the media notification from being
     * dismissed.
     */
    private var isServiceForeground = false

    /**
     * Whether the new media item should start playing after installation or not. This value
     * will be used in the [Player.Listener.onMediaMetadataChanged]. E.g., an episode
     * explicitly set by the user should start playing while the restored last played
     * episode should not start playing on the app start.
     */
    private var playWhenReady = false

    /**
     * The position in ms from which the player should start playing the episode.
     * Most of times is the position where the episode was stopped last time. Also used
     * for playing the episode from the specified timecode, see [setEpisodeAndPlayFromTimecode].
     */
    private var startPosition = 0L

    /** The instance of [Handler] that uses [exoPlayer]'s application [Looper]. */
    private val exoPlayerHandler: Handler by lazy {
        Handler(exoPlayer.applicationLooper)
    }

    /** The [Intent] used for starting [PlayerService]. */
    private val playerServiceIntent: Intent by lazy {
        Intent(this, PlayerService::class.java).apply {
            action = SERVICE_INTERFACE
        }
    }

    /**
     * The [NotificationManagerCompat] that handles all the notification-related work.
     * See [createPlayerNotificationChannel], [setPlayerNotification],
     * [removePlayerNotification].
     */
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(this)
    }

    /**
     * The [DefaultMediaItemConverter] that converts Media2 media items to ExoPlayer media
     * items and vice versa.
     */
    private val mediaItemConverter: DefaultMediaItemConverter by lazy {
        DefaultMediaItemConverter()
    }

    /**
     * The [NotificationCompat.Builder] used to create and update all media notifications.
     * Default settings that will not change in the future are pre-installed. See
     * [updatePlayerNotification].
     */
    @ExperimentalTime
    private val playerNotificationBuilder: NotificationCompat.Builder by lazy {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        // Action to be performed when the user clicks the notification.
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action to be performed when the user dismisses the notification.
        val deleteIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(PLAYER_ACTION_CLOSE),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationCompat.Builder(this, PLAYER_CHANNEL_ID)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionCompatToken)
                    .setShowActionsInCompactView(1) // Show play/pause button.
            )
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_podcasts_24)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
    }

    /**
     * The instance of [PendingIntent] used for 'seek backward' action in the player
     * media notification. See [updatePlayerNotification].
     */
    private val seekBackwardAction: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            this@PlayerService,
            0,
            Intent(PLAYER_ACTION_SEEK_BACKWARD),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * The instance of [PendingIntent] used for 'seek forward' action in the player
     * media notification. See [updatePlayerNotification].
     */
    private val seekForwardAction: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            this@PlayerService,
            0,
            Intent(PLAYER_ACTION_SEEK_FORWARD),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** The [AudioAttributes] used by the [exoPlayer] to handle audio focus. */
    private val audioAttributes: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setUsage(USAGE_MEDIA)
            .setContentType(CONTENT_TYPE_SPEECH)
            .build()
    }

    /**
     * The instance of [MediaSession.SessionCallback]. The callback is created with the
     * helper [SessionCallbackBuilder] provided by the ExoPlayer.
     */
    @ExperimentalTime
    private val sessionCallback: MediaSession.SessionCallback by lazy {
        SessionCallbackBuilder(this, sessionPlayerConnector)
            .setMediaItemProvider { _, _, mediaId ->
                val episode = runBlocking {
                    playerRepository.getEpisode(mediaId)
                } ?: return@setMediaItemProvider null

                // Play an episode from the position where the episode was stopped last time.
                playWhenReady = true
                startPosition = episode.position
                createMedia2MediaItem(episode)
            }
            .setAllowedCommandProvider(allowedCommandProvider)
            .setCustomCommandProvider(customCommandProvider)
            .build()
    }

    /**
     * The instance of [SessionCallbackBuilder.AllowedCommandProvider]. Used to handle
     * default controller commands.
     */
    @ExperimentalTime
    private val allowedCommandProvider: SessionCallbackBuilder.AllowedCommandProvider by lazy {
        object : SessionCallbackBuilder.AllowedCommandProvider {
            override fun acceptConnection(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo
            ): Boolean {
                return controllerInfo.packageName == this@PlayerService.packageName
            }

            override fun getAllowedCommands(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                baseAllowedSessionCommands: SessionCommandGroup
            ): SessionCommandGroup {
                return baseAllowedSessionCommands
            }

            override fun onCommandRequest(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                command: SessionCommand
            ): Int {
                when (command.commandCode) {
                    SessionCommand.COMMAND_CODE_PLAYER_PLAY -> {
                        // Handle PLAY commands by yourself.
                        exoPlayerHandler.post {
                            safePlay()
                        }
                        return SessionResult.RESULT_ERROR_UNKNOWN
                    }
                    SessionCommand.COMMAND_CODE_PLAYER_SET_MEDIA_ITEM -> {
                        exoPlayerHandler.post {
                            // Update the state of the previous episode before setting a new one.
                            updateEpisodeState()

                            // ExoPlayer does not set a new item after an error while
                            // in IDLE state for some reason. prepare() solves this.
                            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                                exoPlayer.prepare()
                            }
                        }
                    }
                }
                return SessionResult.RESULT_SUCCESS
            }
        }
    }

    /**
     * The instance of [SessionCallbackBuilder.CustomCommandProvider]. Used to handle
     * custom commands. Custom commands are defined in the [CustomCommand] object and
     * keys to retrieve the appropriate data along with custom commands are defined
     * in the [CustomCommandKey] object.
     */
    @ExperimentalTime
    private val customCommandProvider: SessionCallbackBuilder.CustomCommandProvider by lazy {
        object : SessionCallbackBuilder.CustomCommandProvider {
            override fun onCustomCommand(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle?
            ): SessionResult {
                return when (customCommand.customAction) {
                    CustomCommand.SET_EPISODE_AND_PLAY_FROM_TIMECODE -> {
                        // Update the previous episode state first.
                        exoPlayerHandler.post {
                            updateEpisodeState()
                        }
                        val episodeId = args?.getString(
                            CustomCommandKey.SET_EPISODE_AND_PLAY_FROM_TIMECODE_EPISODE_ID_KEY
                        ) ?: ""
                        val timecode = args?.getLong(
                            CustomCommandKey.SET_EPISODE_AND_PLAY_FROM_TIMECODE_TIMECODE_KEY
                        ) ?: 0L
                        setEpisodeAndPlayFromTimecode(episodeId, timecode)
                        SessionResult(SessionResult.RESULT_SUCCESS, null)
                    }
                    CustomCommand.SET_SLEEP_TIMER -> {
                        val duration = args?.getLong(CustomCommandKey.SET_SLEEP_TIMER_DURATION_KEY)
                            ?: Long.MIN_VALUE
                        setSleepTimer(duration)
                        SessionResult(SessionResult.RESULT_SUCCESS, null)
                    }
                    CustomCommand.REMOVE_SLEEP_TIMER -> {
                        removeSleepTimer()
                        SessionResult(SessionResult.RESULT_SUCCESS, null)
                    }
                    CustomCommand.MARK_CURRENT_EPISODE_COMPLETED -> {
                        // Remove the current media item and player notification.
                        exoPlayerHandler.post {
                            if (exoPlayer.currentMediaItem != null) {
                                exoPlayer.removeMediaItem(0)
                            }
                        }
                        removePlayerNotification()
                        SessionResult(SessionResult.RESULT_SUCCESS, null)
                    }
                    else -> SessionResult(SessionResult.RESULT_ERROR_UNKNOWN, null)
                }
            }

            // Specify allowed custom commands.
            override fun getCustomCommands(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo
            ): SessionCommandGroup {
                return SessionCommandGroup.Builder()
                    .addCommand(SessionCommand(CustomCommand.SET_EPISODE_AND_PLAY_FROM_TIMECODE, null))
                    .addCommand(SessionCommand(CustomCommand.SET_SLEEP_TIMER, null))
                    .addCommand(SessionCommand(CustomCommand.REMOVE_SLEEP_TIMER, null))
                    .addCommand(SessionCommand(CustomCommand.MARK_CURRENT_EPISODE_COMPLETED, null))
                    .build()
            }
        }
    }

    /** The instance of [Player.Listener] used by [exoPlayer]. */
    @ExperimentalTime
    private val exoPlayerListener: Player.Listener by lazy {
        object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: com.google.android.exoplayer2.MediaMetadata) {
                exoPlayer.prepare()
                seekToStartPosition()

                if (playWhenReady) {
                    safePlay()
                }

                updatePlayerNotification()
                updateLastPlayedEpisodeId()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateExoPlayerState(playbackState)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateIsPlaying(isPlaying)
                updatePlayerNotification()

                // Update the episode state when the player is paused.
                if (!isPlaying) {
                    updateEpisodeState()
                }
            }
        }
    }

    @ExperimentalTime
    override fun onCreate() {
        super.onCreate()

        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Allow cross-protocol redirects.
        val httpDataSourceFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSourceFactory(this, httpDataSourceFactory)

        exoPlayer = SimpleExoPlayer.Builder(this)
            // Allow cross-protocol redirects.
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(SEEK_BACK_INCREMENT)
            .setSeekForwardIncrementMs(SEEK_FORWARD_INCREMENT)
            .build()
            .apply {
                addListener(exoPlayerListener)
                // Restore playback speed.
                scope?.launch {
                    setPlaybackSpeed(playerRepository.getPlaybackSpeed().first() ?: 1.0F)
                }
            }

        sessionPlayerConnector = SessionPlayerConnector(exoPlayer)

        mediaSession = MediaSession.Builder(this, sessionPlayerConnector)
            .setSessionCallback(Executors.newSingleThreadExecutor(), sessionCallback)
            .build()

        createPlayerNotificationChannel()
        createMediaControlReceiver()

        restoreLastPlayedEpisode()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isServiceStarted = true
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        isServiceBound = true
        return PlayerServiceBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceBound = false
        return true
    }

    override fun onRebind(intent: Intent?) {
        isServiceBound = true
    }

    @ExperimentalTime
    override fun onDestroy() {
        super.onDestroy()
        mediaSession.close()
        sessionPlayerConnector.close()
        exoPlayer.removeListener(exoPlayerListener)
        exoPlayer.release()
        unregisterReceiver(mediaControlReceiver)
        scope?.cancel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    /**
     * Custom `play()` command that behaves differently depending on the [exoPlayer] state.
     * - If the [exoPlayer] is in the [Player.STATE_IDLE], calls [SimpleExoPlayer.prepare]
     * before `play()`.
     * - If the [exoPlayer] is in the [Player.STATE_ENDED], calls [SimpleExoPlayer.seekToPrevious]
     * before `play()`.
     *
     * Otherwise just calls `play()`.
     */
    private fun safePlay() {
        when (exoPlayer.playbackState) {
            Player.STATE_IDLE -> {
                exoPlayer.prepare()
                exoPlayer.play()
            }
            Player.STATE_ENDED -> {
                exoPlayer.seekToPrevious()
                exoPlayer.play()
            }
            else -> exoPlayer.play()
        }
    }

    /** Seeks to the [startPosition] and resets it. */
    private fun seekToStartPosition() {
        if (startPosition > 0) {
            exoPlayer.seekTo(startPosition)
            startPosition = 0
        }
    }

    /**
     * Creates an ExoPlayer media item, sets it to the player and sets the [startPosition]
     * value according to a given [timecode].
     */
    private fun setEpisodeAndPlayFromTimecode(episodeId: String, timecode: Long) {
        scope?.launch(Dispatchers.IO) {
            val episode = playerRepository.getEpisode(episodeId) ?: return@launch
            val media2MediaItem = createMedia2MediaItem(episode)
            val exoPlayerMediaItem = mediaItemConverter.convertToExoPlayerMediaItem(media2MediaItem)
            playWhenReady = true
            startPosition = timecode
            exoPlayerHandler.post {
                exoPlayer.setMediaItem(exoPlayerMediaItem)
            }
        }
    }

    /**
     * Sets a sleep timer for a given [duration], updates [_sleepTimerRemainingTime] value
     * every second until the end and then pauses [exoPlayer]. This method removes the previous
     * timer so only one sleep timer can exist at the same time. See [removeSleepTimer],
     * [sleepTimerJob].
     */
    private fun setSleepTimer(duration: Long) {
        removeSleepTimer()
        if (duration <= 0) return

        sleepTimerJob = scope?.launch(Dispatchers.Default) {
            var remainingTime = duration
            _sleepTimerRemainingTime.value = remainingTime
            while (remainingTime > 0) {
                ensureActive()
                delay(1000)
                remainingTime -= 1000
                _sleepTimerRemainingTime.value = remainingTime
            }

            exoPlayerHandler.post {
                exoPlayer.pause()
            }
            removeSleepTimer()
        }
    }

    /**
     * Removes the current sleep timer if it is set and sets [_sleepTimerRemainingTime] to
     * `Long.MIN_VALUE`. See [setSleepTimer], [sleepTimerJob].
     */
    private fun removeSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingTime.value = Long.MIN_VALUE
    }

    /** Updates [_exoPlayerState] value. */
    private fun updateExoPlayerState(playbackState: Int) {
        _exoPlayerState.value = playbackState
    }

    /** Updates [_isPlaying] value. */
    private fun updateIsPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    /** Updates episode state. See [PlayerRepository.updateEpisodeState]. */
    @ExperimentalTime
    private fun updateEpisodeState() {
        val episode = MediaItemEpisode.from(sessionPlayerConnector.currentMediaItem)
        if (episode.isNotEmpty()) {
            val position = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            scope?.launch(Dispatchers.IO) {
                playerRepository.updateEpisodeState(episode.id, position, duration)
            }
        }
    }

    /**
     * Updates the ID of the last played episode. Allows player to restore the episode
     * on the app start. See [restoreLastPlayedEpisode].
     */
    @ExperimentalTime
    private fun updateLastPlayedEpisodeId() {
        val episode = MediaItemEpisode.from(sessionPlayerConnector.currentMediaItem)
        if (episode.isNotEmpty()) {
            scope?.launch(Dispatchers.IO) {
                playerRepository.setLastPlayedEpisodeId(episode.id)
            }
        }
    }

    /**
     * Restores the last played episode and sets it to the player. DO NOT start playing
     * automatically. See [updateLastPlayedEpisodeId].
     */
    private fun restoreLastPlayedEpisode() {
        scope?.launch(Dispatchers.IO) {
            val episodeId = playerRepository.getLastPlayedEpisodeId().first() ?: return@launch
            val episode = playerRepository.getEpisode(episodeId) ?: return@launch
            if (!episode.isCompleted) {
                val media2MediaItem = createMedia2MediaItem(episode)
                val exoPlayerMediaItem = mediaItemConverter.convertToExoPlayerMediaItem(media2MediaItem)
                playWhenReady = false
                startPosition = episode.position
                exoPlayerHandler.post {
                    exoPlayer.setMediaItem(exoPlayerMediaItem)
                }
            } else {
                playerRepository.setLastPlayedEpisodeId("")
            }
        }
    }

    /** Creates a Media2 media item from a given [episode] and returns it. */
    private fun createMedia2MediaItem(episode: Episode): MediaItem {
        return UriMediaItem.Builder(Uri.parse(episode.audio))
            .setMetadata(
                MediaMetadata.Builder()
                    .putString(EpisodeMetadata.ID, episode.id)
                    .putString(EpisodeMetadata.TITLE, episode.title)
                    .putString(EpisodeMetadata.IMAGE, episode.image)
                    .putString(EpisodeMetadata.PODCAST_ID, episode.podcastId)
                    .putString(EpisodeMetadata.PODCAST_TITLE, episode.podcastTitle)
                    .putLong(EpisodeMetadata.DURATION, episode.audioLength * 1000L)
                    .build()
            )
            .build()
    }

    /**
     * Updates the media notification. If the current [exoPlayer] media item is null,
     * removes the notification using [removePlayerNotification]. See [playerNotificationBuilder],
     * [playerNotificationJob].
     */
    @ExperimentalTime
    private fun updatePlayerNotification() {
        val currentEpisode = MediaItemEpisode.from(sessionPlayerConnector.currentMediaItem)
        if (currentEpisode.isEmpty()) {
            removePlayerNotification()
            return
        }

        playerNotificationJob?.cancel()
        playerNotificationJob = scope?.launch(Dispatchers.Default) {
            // Set up play/pause button.
            val playPauseAction: PendingIntent
            val playPauseIcon: Int
            val playPauseTitle: String
            if (_isPlaying.value) {
                playPauseAction = PendingIntent.getBroadcast(
                    this@PlayerService,
                    0,
                    Intent(PLAYER_ACTION_PAUSE),
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                playPauseIcon = R.drawable.ic_pause_32
                playPauseTitle = getString(R.string.notification_pause)
            } else {
                playPauseAction = PendingIntent.getBroadcast(
                    this@PlayerService,
                    0,
                    Intent(PLAYER_ACTION_PLAY),
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                playPauseIcon = R.drawable.ic_play_32
                playPauseTitle = getString(R.string.notification_play)
            }

            // Load episode image.
            val loader = this@PlayerService.imageLoader
            val request = ImageRequest.Builder(this@PlayerService)
                .data(currentEpisode.image)
                .build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val largeIcon = (result as? BitmapDrawable)?.bitmap

            playerNotificationBuilder
                .setContentTitle(currentEpisode.title)
                .setContentText(currentEpisode.podcastTitle)
                .setLargeIcon(largeIcon)
                .clearActions()
                .addAction(
                    R.drawable.ic_backward_10_32,
                    getString(R.string.notification_backward),
                    seekBackwardAction
                )
                .addAction(playPauseIcon, playPauseTitle, playPauseAction)
                .addAction(
                    R.drawable.ic_forward_30_32,
                    getString(R.string.notification_forward),
                    seekForwardAction
                )
            setPlayerNotification(playerNotificationBuilder.build())
        }
    }

    /**
     * Sets the media notification and starts the service, if it is not started.
     * The following actions will be performed depending on the [SimpleExoPlayer.isPlaying]
     * state:
     * - If `isPlaying` returns `true`, the service should be started in the foreground, so
     * the user can not dismiss the notification.
     * - If `isPlaying` returns `false`, the service should be removed from the foreground,
     * so the user can dismiss the notification (resulting in a [stopSelf] call). See
     * [MediaControlReceiver].
     */
    private fun setPlayerNotification(notification: Notification) {
        notificationManager.notify(PLAYER_NOTIFICATION_ID, notification)
        if (!isServiceStarted) {
            startService(playerServiceIntent)
            isServiceStarted = true
        }

        if (_isPlaying.value) {
            if (!isServiceForeground) {
                startForeground(PLAYER_NOTIFICATION_ID, notification)
                isServiceForeground = true
            }
        } else {
            stopForeground(false)
            isServiceForeground = false
        }
    }

    /**
     * Removes the media notification, removes the service from the foreground and calls
     * [stopSelf].
     */
    private fun removePlayerNotification() {
        playerNotificationJob?.cancel()
        notificationManager.cancel(PLAYER_NOTIFICATION_ID)
        stopForeground(false)
        isServiceForeground = false
        stopSelf()
        isServiceStarted = false
    }

    /** Creates a notification channel for the player media notifications. */
    private fun createPlayerNotificationChannel() {
        val importance = NotificationManagerCompat.IMPORTANCE_LOW
        val channel = NotificationChannelCompat.Builder(PLAYER_CHANNEL_ID, importance)
            .setName(getString(R.string.notification_player_channel_name))
            .setLightsEnabled(false)
            .setVibrationEnabled(false)
            .build()
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create and register an instance of the [MediaControlReceiver] that handles
     * media notification controls.
     */
    private fun createMediaControlReceiver() {
        mediaControlReceiver = MediaControlReceiver()
        val filter = IntentFilter().apply {
            addAction(PLAYER_ACTION_PLAY)
            addAction(PLAYER_ACTION_PAUSE)
            addAction(PLAYER_ACTION_SEEK_BACKWARD)
            addAction(PLAYER_ACTION_SEEK_FORWARD)
            addAction(PLAYER_ACTION_CLOSE)
        }
        registerReceiver(mediaControlReceiver, filter)
    }

    /**
     * Custom [BroadcastReceiver] that handles media notification controls. Use
     * [createMediaControlReceiver] to create and register an instance.
     *
     * Note: [PLAYER_ACTION_CLOSE] resulting in a call to [stopSelf].
     */
    inner class MediaControlReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                PLAYER_ACTION_PLAY -> safePlay()
                PLAYER_ACTION_PAUSE -> exoPlayer.pause()
                PLAYER_ACTION_SEEK_BACKWARD -> exoPlayer.seekBack()
                PLAYER_ACTION_SEEK_FORWARD -> exoPlayer.seekForward()
                PLAYER_ACTION_CLOSE -> {
                    stopSelf()
                    isServiceStarted = false
                }
            }
        }
    }

    /**
     * Custom [Binder] that exposes service properties through the service binding
     * mechanism.
     */
    inner class PlayerServiceBinder : Binder() {
        /** MediaSession token. */
        val sessionToken: SessionToken
            get() = mediaSession.token

        /** StateFlow with the ExoPlayer state. */
        val exoPlayerState: StateFlow<Int>
            get() = _exoPlayerState.asStateFlow()

        /** StateFlow with the ExoPlayer `isPlaying` state. */
        val isPlaying: StateFlow<Boolean>
            get() = _isPlaying.asStateFlow()

        /**
         * StateFlow with the remaining time of the sleep timer in milliseconds.
         * If the timer is not set, the value is `Long.MIN_VALUE`.
         */
        val sleepTimerRemainingTime: StateFlow<Long>
            get() = _sleepTimerRemainingTime.asStateFlow()
    }

    companion object {
        /** Play action for [MediaControlReceiver]. */
        private const val PLAYER_ACTION_PLAY = "com.greencom.android.podcasts.PLAYER_ACTION_PLAY"

        /** Pause action for [MediaControlReceiver]. */
        private const val PLAYER_ACTION_PAUSE = "com.greencom.android.podcasts.PLAYER_ACTION_PAUSE"

        /** Seek backward action for [MediaControlReceiver]. */
        private const val PLAYER_ACTION_SEEK_BACKWARD =
            "com.greencom.android.podcasts.PLAYER_ACTION_SEEK_BACKWARD"

        /** Seek forward action for [MediaControlReceiver]. */
        private const val PLAYER_ACTION_SEEK_FORWARD =
            "com.greencom.android.podcasts.PLAYER_ACTION_SEEK_FORWARD"

        /** Close player action for [MediaControlReceiver]. */
        private const val PLAYER_ACTION_CLOSE = "com.greencom.android.podcasts.PLAYER_ACTION_CLOSE"
    }
}