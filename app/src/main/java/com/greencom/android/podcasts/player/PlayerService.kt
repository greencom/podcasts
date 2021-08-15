package com.greencom.android.podcasts.player

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.*
import android.util.Log
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
import com.google.android.exoplayer2.PlaybackException
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

private const val PLAYER_SERVICE_TAG = "PLAYER_SERVICE_TAG"

private const val PLAYER_CHANNEL_ID = "PLAYER_CHANNEL_ID"
private const val PLAYER_NOTIFICATION_ID = 1

// TODO
@AndroidEntryPoint
class PlayerService : MediaSessionService() {

    @Inject lateinit var playerRepository: PlayerRepository

    private lateinit var mediaSession: MediaSession

    private lateinit var exoPlayer: SimpleExoPlayer

    private lateinit var sessionPlayerConnector: SessionPlayerConnector

    private lateinit var mediaControlReceiver: MediaControlReceiver

    private var scope: CoroutineScope? = null

    private var notificationJob: Job? = null

    private var sleepTimerJob: Job? = null

    private val _sleepTimerRemainingTime = MutableStateFlow(Long.MIN_VALUE)

    private val _exoPlayerState = MutableStateFlow(Player.STATE_IDLE)

    private val _isPlaying = MutableStateFlow(false)

    private var isServiceStarted = false

    private var isServiceBound = false

    private var isServiceForeground = false

    private var playWhenReady = false

    private var startPosition = 0L

    private val playerServiceIntent: Intent by lazy {
        Intent(this, PlayerService::class.java).apply {
            action = SERVICE_INTERFACE
        }
    }

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(this)
    }

    private val mediaItemConverter: DefaultMediaItemConverter by lazy {
        DefaultMediaItemConverter()
    }

    @ExperimentalTime
    private val playerNotificationBuilder: NotificationCompat.Builder by lazy {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
                    .setShowActionsInCompactView(1)
            )
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_podcasts_24)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
    }

    private val audioAttributes: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setUsage(USAGE_MEDIA)
            .setContentType(CONTENT_TYPE_SPEECH)
            .build()
    }

    @ExperimentalTime
    private val sessionCallback: MediaSession.SessionCallback by lazy {
        SessionCallbackBuilder(this, sessionPlayerConnector)
            .setMediaItemProvider { _, _, mediaId ->
                Log.d(PLAYER_SERVICE_TAG, "sessionCallback: mediaItemProvider()")
                val episode = runBlocking {
                    playerRepository.getEpisode(mediaId)
                } ?: return@setMediaItemProvider null

                playWhenReady = true
                startPosition = episode.position

                createMedia2MediaItem(episode)
            }
            .setPostConnectCallback { _, _ ->
                Handler(exoPlayer.applicationLooper).post {
                    updateExoPlayerState()
                    updateIsPlaying()
                }
            }
            .setAllowedCommandProvider(allowedCommandProvider)
            .setCustomCommandProvider(customCommandProvider)
            .build()
    }

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
                Log.d(PLAYER_SERVICE_TAG, "onCommandRequest() with command ${command.commandCode}")
                when (command.commandCode) {
                    SessionCommand.COMMAND_CODE_PLAYER_PLAY -> {
                        Handler(exoPlayer.applicationLooper).post {
                            safePlay()
                        }
                        return SessionResult.RESULT_ERROR_UNKNOWN
                    }
                    SessionCommand.COMMAND_CODE_PLAYER_SET_MEDIA_ITEM -> {
                        Handler(exoPlayer.applicationLooper).post {
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

    private val customCommandProvider: SessionCallbackBuilder.CustomCommandProvider by lazy {
        object : SessionCallbackBuilder.CustomCommandProvider {
            override fun onCustomCommand(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle?
            ): SessionResult {
                return when (customCommand.customAction) {
                    CustomSessionCommand.SET_SLEEP_TIMER -> {
                        val duration = args?.getLong(CustomSessionCommand.SET_SLEEP_TIMER_DURATION_KEY)
                            ?: Long.MIN_VALUE
                        setSleepTimer(duration)
                        SessionResult(SessionResult.RESULT_SUCCESS, null)
                    }
                    CustomSessionCommand.REMOVE_SLEEP_TIMER -> {
                        removeSleepTimer()
                        SessionResult(SessionResult.RESULT_SUCCESS, null)
                    }
                    else -> SessionResult(SessionResult.RESULT_ERROR_UNKNOWN, null)
                }
            }

            override fun getCustomCommands(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo
            ): SessionCommandGroup {
                return SessionCommandGroup.Builder()
                    .addCommand(SessionCommand(CustomSessionCommand.SET_SLEEP_TIMER, null))
                    .addCommand(SessionCommand(CustomSessionCommand.REMOVE_SLEEP_TIMER, null))
                    .build()
            }
        }
    }

    @ExperimentalTime
    private val exoPlayerListener: Player.Listener by lazy {
        object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: com.google.android.exoplayer2.MediaMetadata) {
                Log.d(PLAYER_SERVICE_TAG, "exoPlayerListener: onMediaMetadataChanged()")
                exoPlayer.prepare()

                seekToStartPosition()

                if (playWhenReady) {
                    safePlay()
                }

                updateNotification()

                updateLastPlayedEpisodeId()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(PLAYER_SERVICE_TAG, "exoPlayerListener: onPlaybackStateChanged() with state $playbackState")
                updateExoPlayerState(playbackState)
                updateNotification()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(PLAYER_SERVICE_TAG, "exoPlayerListener: onIsPlayingChanged() with isPlaying $isPlaying")
                updateIsPlaying(isPlaying)
                updateNotification()

                if (!isPlaying) {
                    updateEpisodeState()
                }
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                Log.d(PLAYER_SERVICE_TAG, "exoPlayerListener: onIsLoadingChanged() with isLoading $isLoading")
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.d(PLAYER_SERVICE_TAG, "exoPlayerListener: onPlayerError() with error $error")
            }
        }
    }

    @ExperimentalTime
    override fun onCreate() {
        Log.d(PLAYER_SERVICE_TAG, "onCreate()")
        super.onCreate()

        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Allow cross-protocol redirects.
        val httpDataSourceFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSourceFactory(this, httpDataSourceFactory)

        exoPlayer = SimpleExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(30_000L)
            .build()
            .apply { addListener(exoPlayerListener) }

        scope?.launch {
            exoPlayer.setPlaybackSpeed(playerRepository.getPlaybackSpeed().first() ?: 1.0F)
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
        Log.d(PLAYER_SERVICE_TAG, "onStartCommand()")
        super.onStartCommand(intent, flags, startId)
        isServiceStarted = true
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(PLAYER_SERVICE_TAG, "onBind()")
        super.onBind(intent)
        isServiceBound = true
        return PlayerServiceBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(PLAYER_SERVICE_TAG, "onUnbind()")
        isServiceBound = false
        return true
    }

    override fun onRebind(intent: Intent?) {
        Log.d(PLAYER_SERVICE_TAG, "onRebind()")
        isServiceBound = true
    }

    @ExperimentalTime
    override fun onDestroy() {
        Log.d(PLAYER_SERVICE_TAG, "onDestroy()")
        super.onDestroy()
        mediaSession.close()
        exoPlayer.release()
        exoPlayer.removeListener(exoPlayerListener)
        sessionPlayerConnector.close()
        unregisterReceiver(mediaControlReceiver)
        scope?.cancel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    private fun safePlay() {
        when (exoPlayer.playbackState) {
            Player.STATE_ENDED -> {
                exoPlayer.seekToPrevious()
                exoPlayer.play()
            }
            Player.STATE_IDLE -> {
                exoPlayer.prepare()
                exoPlayer.play()
            }
            else -> exoPlayer.play()
        }
    }

    private fun seekToStartPosition() {
        if (startPosition > 0) {
            exoPlayer.seekTo(startPosition)
            startPosition = 0
        }
    }

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

            Handler(exoPlayer.applicationLooper).post {
                exoPlayer.pause()
            }
            removeSleepTimer()
        }
    }

    private fun removeSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingTime.value = Long.MIN_VALUE
    }

    private fun updateExoPlayerState(playbackState: Int? = null) {
        val mPlaybackState = playbackState ?: exoPlayer.playbackState
        if (isServiceBound) {
            _exoPlayerState.value = mPlaybackState
        }
    }

    private fun updateIsPlaying(isPlaying: Boolean? = null) {
        val mIsPlaying = isPlaying ?: exoPlayer.isPlaying
        if (isServiceBound) {
            _isPlaying.value = mIsPlaying
        }
    }

    @ExperimentalTime
    private fun updateEpisodeState() {
        val exoPlayerMediaItem = exoPlayer.currentMediaItem ?: return
        val episode = CurrentEpisode.from(mediaItemConverter.convertToMedia2MediaItem(exoPlayerMediaItem))
        if (episode.isNotEmpty()) {
            val position = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            scope?.launch {
                playerRepository.updateEpisodeState(episode.id, position, duration)
            }
        }
    }

    @ExperimentalTime
    private fun updateLastPlayedEpisodeId() {
        val exoPlayerMediaItem = exoPlayer.currentMediaItem ?: return
        val episode = CurrentEpisode.from(mediaItemConverter.convertToMedia2MediaItem(exoPlayerMediaItem))
        if (episode.isNotEmpty()) {
            scope?.launch {
                playerRepository.setLastPlayedEpisodeId(episode.id)
            }
        }
    }

    private fun restoreLastPlayedEpisode() {
        scope?.launch {
            val episodeId = playerRepository.getLastPlayedEpisodeId().first() ?: return@launch
            val episode = playerRepository.getEpisode(episodeId) ?: return@launch
            if (!episode.isCompleted) {
                val media2MediaItem = createMedia2MediaItem(episode)
                val exoPlayerMediaItem = mediaItemConverter.convertToExoPlayerMediaItem(media2MediaItem)
                playWhenReady = false
                startPosition = episode.position
                exoPlayer.setMediaItem(exoPlayerMediaItem)
            } else {
                playerRepository.setLastPlayedEpisodeId("")
            }
        }
    }

    private fun createMedia2MediaItem(episode: Episode): MediaItem {
        return UriMediaItem.Builder(Uri.parse(episode.audio))
            .setMetadata(
                MediaMetadata.Builder()
                    .putString(EpisodeMetadata.ID, episode.id)
                    .putString(EpisodeMetadata.TITLE, episode.title)
                    .putString(EpisodeMetadata.IMAGE, episode.image)
                    .putString(EpisodeMetadata.PODCAST_ID, episode.podcastId)
                    .putString(EpisodeMetadata.PODCAST_TITLE, episode.podcastTitle)
                    .build()
            )
            .build()
    }

    @ExperimentalTime
    private fun updateNotification() {
        Log.d(PLAYER_SERVICE_TAG, "updateNotification()")
        val currentEpisode = CurrentEpisode.from(sessionPlayerConnector.currentMediaItem)
        if (currentEpisode.isEmpty()) {
            removePlayerNotification()
            return
        }

        notificationJob?.cancel()
        notificationJob = scope?.launch {
            val playPauseAction: PendingIntent
            val playPauseIcon: Int
            val playPauseTitle: String
            if (exoPlayer.isPlaying) {
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
                    PendingIntent.getBroadcast(
                        this@PlayerService,
                        0,
                        Intent(PLAYER_ACTION_SEEK_BACKWARD),
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .addAction(playPauseIcon, playPauseTitle, playPauseAction)
                .addAction(
                    R.drawable.ic_forward_30_32,
                    getString(R.string.notification_forward),
                    PendingIntent.getBroadcast(
                        this@PlayerService,
                        0,
                        Intent(PLAYER_ACTION_SEEK_FORWARD),
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            setPlayerNotification(playerNotificationBuilder.build())
        }
    }

    private fun setPlayerNotification(notification: Notification) {
        notificationManager.notify(PLAYER_NOTIFICATION_ID, notification)

        if (!isServiceStarted) {
            startService(playerServiceIntent)
            isServiceStarted = true
        }

        if (exoPlayer.isPlaying) {
            if (!isServiceForeground) {
                startForeground(PLAYER_NOTIFICATION_ID, notification)
                isServiceForeground = true
            }
        } else {
            stopForeground(false)
            isServiceForeground = false
        }
    }

    private fun removePlayerNotification() {
        notificationJob?.cancel()
        notificationManager.cancel(PLAYER_NOTIFICATION_ID)
        stopForeground(false)
        isServiceForeground = false
        stopSelf()
        isServiceStarted = false
    }

    private fun createPlayerNotificationChannel() {
        val importance = NotificationManagerCompat.IMPORTANCE_LOW
        val channel = NotificationChannelCompat.Builder(PLAYER_CHANNEL_ID, importance)
            .setName(getString(R.string.notification_player_channel_name))
            .setLightsEnabled(false)
            .setVibrationEnabled(false)
            .build()
        notificationManager.createNotificationChannel(channel)
    }

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

    inner class PlayerServiceBinder : Binder() {
        val sessionToken: SessionToken
            get() = mediaSession.token

        val exoPlayerState: StateFlow<Int>
            get() = _exoPlayerState.asStateFlow()

        val isPlaying: StateFlow<Boolean>
            get() = _isPlaying.asStateFlow()

        val sleepTimerRemainingTime: StateFlow<Long>
            get() = _sleepTimerRemainingTime
    }

    companion object {
        private const val PLAYER_ACTION_PLAY = "com.greencom.android.podcasts.PLAYER_ACTION_PLAY"
        private const val PLAYER_ACTION_PAUSE = "com.greencom.android.podcasts.PLAYER_ACTION_PAUSE"
        private const val PLAYER_ACTION_SEEK_BACKWARD = "com.greencom.android.podcasts.PLAYER_ACTION_SEEK_BACKWARD"
        private const val PLAYER_ACTION_SEEK_FORWARD = "com.greencom.android.podcasts.PLAYER_ACTION_SEEK_FORWARD"
        private const val PLAYER_ACTION_CLOSE = "com.greencom.android.podcasts.PLAYER_ACTION_CLOSE"
    }

//    @Inject lateinit var repository: PlayerRepository
//
//    private var scope: CoroutineScope? = null
//
//    private var notificationJob: Job? = null
//
//    private lateinit var mediaSession: MediaSession
//
//    private lateinit var player: MediaPlayer
//
//    private lateinit var mediaControlReceiver: BroadcastReceiver
//
//    private var sleepTimer: CountDownTimer? = null
//
//    private val _sleepTimer = MutableStateFlow(Long.MIN_VALUE)
//
//    private val serviceIntent: Intent by lazy {
//        Intent(this, PlayerService::class.java).apply {
//            action = SERVICE_INTERFACE
//        }
//    }
//
//    private val notificationManger: NotificationManagerCompat by lazy {
//        NotificationManagerCompat.from(this)
//    }
//
//    private val audioAttrs: AudioAttributesCompat by lazy {
//        AudioAttributesCompat.Builder()
//            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
//            .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
//            .build()
//    }
//
//    private val isPlaying: Boolean
//        get() = player.playerState == MediaPlayer.PLAYER_STATE_PLAYING
//
//    private val isNotPlaying: Boolean
//        get() = !isPlaying
//
//    private var isServiceStarted = false
//
//    private var isServiceForeground = false
//
//    private var isServiceBound = false
//
//    @ExperimentalTime
//    private val sessionCallback: MediaSession.SessionCallback by lazy {
//        object : MediaSession.SessionCallback() {
//            override fun onConnect(
//                session: MediaSession,
//                controller: MediaSession.ControllerInfo
//            ): SessionCommandGroup {
//                Log.d(PLAYER_TAG, "sessionCallback: onConnect()")
//                isServiceBound = true
//                updateNotification(true)
//                return SessionCommandGroup.Builder()
//                    .addAllPredefinedCommands(SessionCommand.COMMAND_VERSION_2)
//                    .addCommand(SessionCommand(CustomSessionCommand.RESET_PLAYER, null))
//                    .addCommand(SessionCommand(CustomSessionCommand.SET_SLEEP_TIMER, null))
//                    .addCommand(SessionCommand(CustomSessionCommand.REMOVE_SLEEP_TIMER, null))
//                    .build()
//            }
//
//            override fun onDisconnected(
//                session: MediaSession,
//                controller: MediaSession.ControllerInfo
//            ) {
//                super.onDisconnected(session, controller)
//                Log.d(PLAYER_TAG,"sessionCallback: onDisconnected()")
//                isServiceBound = false
//            }
//
//            override fun onCreateMediaItem(
//                session: MediaSession,
//                controller: MediaSession.ControllerInfo,
//                mediaId: String
//            ): MediaItem? {
//                Log.d(PLAYER_TAG,"sessionCallback: onCreateMediaItem()")
//                updateEpisodeState()
//                saveLastEpisode()
//
//                if (player.playerState == MediaPlayer.PLAYER_STATE_ERROR) {
//                    runBlocking {
//                        resetPlayer()
//                    }
//                }
//
//                var mediaItem: MediaItem? = null
//                val audio: String
//                val duration: Long
//                val title: String
//                val podcastTitle: String
//                val podcastId: String
//                val image: String
//
//                runBlocking {
//                    val episode = repository.getEpisode(mediaId) ?: return@runBlocking
//                    audio = episode.audio
//                    duration = Duration.seconds(episode.audioLength).inWholeMilliseconds
//                    title = episode.title
//                    podcastTitle = episode.podcastTitle
//                    podcastId = episode.podcastId
//                    image = episode.image
//
//                    mediaItem = UriMediaItem.Builder(Uri.parse(audio))
//                        .setMetadata(MediaMetadata.Builder()
//                            .putString(EpisodeMetadata.ID, mediaId)
//                            .putString(EpisodeMetadata.TITLE, title)
//                            .putString(EpisodeMetadata.PODCAST_TITLE, podcastTitle)
//                            .putString(EpisodeMetadata.PODCAST_ID, podcastId)
//                            .putString(EpisodeMetadata.IMAGE, image)
//                            .putLong(EpisodeMetadata.DURATION, duration)
//                            .build())
//                        .build()
//                }
//                return mediaItem
//            }
//
//            override fun onCommandRequest(
//                session: MediaSession,
//                controller: MediaSession.ControllerInfo,
//                command: SessionCommand
//            ): Int {
//                return when (command.commandCode) {
//                    SessionCommand.COMMAND_CODE_PLAYER_PLAY -> {
//                        safePlay()
//                        SessionResult.RESULT_ERROR_INVALID_STATE
//                    }
//                    else -> super.onCommandRequest(session, controller, command)
//                }
//            }
//
//            override fun onCustomCommand(
//                session: MediaSession,
//                controller: MediaSession.ControllerInfo,
//                customCommand: SessionCommand,
//                args: Bundle?
//            ): SessionResult {
//                return when (customCommand.customAction) {
//                    CustomSessionCommand.RESET_PLAYER -> {
//                        runBlocking {
//                            resetPlayer()
//                        }
//                        removeNotification()
//                        SessionResult(SessionResult.RESULT_SUCCESS, null)
//                    }
//                    CustomSessionCommand.SET_SLEEP_TIMER -> {
//                        val duration = args?.getLong(PLAYER_SET_SLEEP_TIMER) ?: Long.MIN_VALUE
//                        setSleepTimer(duration)
//                        SessionResult(SessionResult.RESULT_SUCCESS, null)
//                    }
//                    CustomSessionCommand.REMOVE_SLEEP_TIMER -> {
//                        removeSleepTimer()
//                        SessionResult(SessionResult.RESULT_SUCCESS, null)
//                    }
//                    else -> super.onCustomCommand(session, controller, customCommand, args)
//                }
//            }
//        }
//    }
//
//    @ExperimentalTime
//    private val playerCallback: MediaPlayer.PlayerCallback by lazy {
//        object : MediaPlayer.PlayerCallback() {
//            override fun onCurrentMediaItemChanged(player: SessionPlayer, item: MediaItem) {
//                Log.d(PLAYER_TAG, "playerCallback: onCurrentMediaItemChanged()")
//                updateNotification()
//            }
//
//            override fun onPlayerStateChanged(player: SessionPlayer, playerState: Int) {
//                Log.d(PLAYER_TAG, "playerCallback: onPlayerStateChanged(), state $playerState")
//                updateNotification()
//                saveLastEpisode()
//
//                if (playerState == MediaPlayer.PLAYER_STATE_PLAYING && !isServiceStarted) {
//                    startService(serviceIntent)
//                }
//
//                if (playerState == MediaPlayer.PLAYER_STATE_PAUSED) {
//                    updateEpisodeState()
//                }
//
//                if (playerState == MediaPlayer.PLAYER_STATE_ERROR) {
//                    updateEpisodeState()
//                    // Show a toast.
//                    scope?.launch {
//                        Toast.makeText(
//                            this@PlayerService,
//                            R.string.player_error,
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                }
//            }
//
//            override fun onPlaybackCompleted(player: SessionPlayer) {
//                Log.d(PLAYER_TAG, "playerCallback: onPlaybackCompleted()")
//                updateEpisodeState()
//            }
//        }
//    }
//
//    @ExperimentalTime
//    override fun onCreate() {
//        super.onCreate()
//        Log.d(PLAYER_TAG,"PlayerService.onCreate()")
//        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
//
//        player = MediaPlayer(this).apply {
//            registerPlayerCallback(Executors.newSingleThreadExecutor(), playerCallback)
//            setAudioAttributes(audioAttrs)
//        }
//
//        scope?.launch {
//            player.playbackSpeed = repository.getPlaybackSpeed().first() ?: 1.0F
//        }
//
//        mediaSession = MediaSession.Builder(this, player)
//            .setSessionCallback(Executors.newSingleThreadExecutor(), sessionCallback)
//            .build()
//
//        createNotificationChannel()
//        createMediaControlReceiver()
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        super.onStartCommand(intent, flags, startId)
//        Log.d(PLAYER_TAG,"PlayerService.onStartCommand()")
//        isServiceStarted = true
//        return START_NOT_STICKY
//    }
//
//    override fun onBind(intent: Intent): IBinder {
//        super.onBind(intent)
//        Log.d(PLAYER_TAG,"PlayerService.onBind()")
//        return PlayerServiceBinder()
//    }
//
//    @ExperimentalTime
//    override fun onDestroy() {
//        super.onDestroy()
//        Log.d(PLAYER_TAG,"PlayerService.onDestroy()")
//        sleepTimer?.cancel()
//        mediaSession.close()
//        player.unregisterPlayerCallback(playerCallback)
//        player.close()
//        unregisterReceiver(mediaControlReceiver)
//        scope?.cancel()
//        removeNotification()
//    }
//
//    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
//        Log.d(PLAYER_TAG,"PlayerService.onGetSession()")
//        return mediaSession
//    }
//
//    private fun safePlay() {
//        when {
//            player.currentPosition in 0 until player.duration -> {
//                player.play()
//            }
//            player.currentPosition >= player.duration -> {
//                player.seekTo(0L)
//                player.play()
//            }
//        }
//    }
//
//    private fun skipBackwardOrForward(value: Long) {
//        var position = player.currentPosition + value
//        position = when {
//            position <= 0L -> 0L
//            position >= player.duration -> player.duration
//            else -> position
//        }
//        player.seekTo(position)
//    }
//
//    private suspend fun resetPlayer() {
//        player.reset()
//        player.setAudioAttributes(audioAttrs)
//        player.playbackSpeed = repository.getPlaybackSpeed().first() ?: 1.0F
//    }
//
//    @ExperimentalTime
//    private fun updateEpisodeState() {
//        val episode = CurrentEpisode.from(player.currentMediaItem)
//        if (episode.isNotEmpty()) {
//            val position = player.currentPosition
//            val duration = player.duration
//            scope?.launch {
//                repository.updateEpisodeState(episode.id, position, duration)
//            }
//        }
//    }
//
//    @ExperimentalTime
//    private fun saveLastEpisode() {
//        Log.d(PLAYER_TAG, "saveLastEpisode()")
//        val episode = CurrentEpisode.from(player.currentMediaItem)
//        if (episode.isNotEmpty()) {
//            scope?.launch {
//                repository.setLastEpisodeId(episode.id)
//            }
//        }
//    }
//
//    private fun setSleepTimer(duration: Long) {
//        removeSleepTimer()
//        if (duration <= 0) return
//
//        scope?.launch {
//            sleepTimer = object : CountDownTimer(duration, 1000) {
//                override fun onTick(millisUntilFinished: Long) {
//                    _sleepTimer.value = millisUntilFinished
//                }
//
//                override fun onFinish() {
//                    player.pause()
//                    _sleepTimer.value = Long.MIN_VALUE
//                }
//            }.start()
//        }
//    }
//
//    private fun removeSleepTimer() {
//        sleepTimer?.cancel()
//        _sleepTimer.value = Long.MIN_VALUE
//    }
//
//    @ExperimentalTime
//    private fun updateNotification(forceForeground: Boolean = false) {
//        val mediaItem = player.currentMediaItem
//        if (mediaItem == null) {
//            removeNotification()
//            return
//        }
//
//        val playerState = player.playerState
//        if (
//            playerState == MediaPlayer.PLAYER_STATE_IDLE ||
//            playerState == MediaPlayer.PLAYER_STATE_ERROR
//        ) {
//            removeNotification()
//            return
//        }
//
//        notificationJob?.cancel()
//        notificationJob = scope?.launch {
//            val notificationBuilder = NotificationCompat.Builder(this@PlayerService, CHANNEL_ID)
//                .setSilent(true)
//                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//                .setSmallIcon(R.drawable.media_session_service_notification_ic_music_note)
//
//            val playPauseAction: PendingIntent
//            val playPauseIcon: Int
//            val playPauseTitle: String
//            when (playerState) {
//                MediaPlayer.PLAYER_STATE_PLAYING -> {
//                    playPauseAction = PendingIntent.getBroadcast(
//                        this@PlayerService,
//                        0,
//                        Intent(ACTION_PAUSE),
//                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
//                    )
//                    playPauseIcon = R.drawable.ic_pause_32
//                    playPauseTitle = getString(R.string.notification_pause)
//                }
//                MediaPlayer.PLAYER_STATE_PAUSED -> {
//                    playPauseAction = PendingIntent.getBroadcast(
//                        this@PlayerService,
//                        0,
//                        Intent(ACTION_PLAY),
//                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
//                    )
//                    playPauseIcon = R.drawable.ic_play_32
//                    playPauseTitle = getString(R.string.notification_play)
//                }
//                else -> {
//                    removeNotification()
//                    return@launch
//                }
//            }
//
//            val episode = CurrentEpisode.from(mediaItem)
//            val loader = baseContext.imageLoader
//            val request = ImageRequest.Builder(this@PlayerService)
//                .data(episode.image)
//                .build()
//            val result = (loader.execute(request) as SuccessResult).drawable
//            val bitmap = (result as BitmapDrawable).bitmap
//
//            val backwardSkipAction = PendingIntent.getBroadcast(
//                this@PlayerService,
//                0,
//                Intent(ACTION_SKIP_BACKWARD),
//                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//            val backwardSkipIcon = R.drawable.ic_backward_10_32
//            val backwardSkipTitle = getString(R.string.notification_backward)
//
//            val forwardSkipAction = PendingIntent.getBroadcast(
//                this@PlayerService,
//                0,
//                Intent(ACTION_SKIP_FORWARD),
//                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//            val forwardSkipIcon = R.drawable.ic_forward_30_32
//            val forwardSkipTitle = getString(R.string.notification_forward)
//
//            val activityIntent = Intent(this@PlayerService, MainActivity::class.java).apply {
//                action = Intent.ACTION_MAIN
//                addCategory(Intent.CATEGORY_LAUNCHER)
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            }
//            val contentIntent = PendingIntent.getActivity(
//                this@PlayerService,
//                0,
//                activityIntent,
//                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//
//            val deleteIntent = PendingIntent.getBroadcast(
//                this@PlayerService,
//                0,
//                Intent(ACTION_CLOSE),
//                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//
//            notificationBuilder
//                .setContentIntent(contentIntent)
//                .setDeleteIntent(deleteIntent)
//                .setContentTitle(episode.title)
//                .setContentText(episode.podcastTitle)
//                .setLargeIcon(bitmap)
//                .addAction(backwardSkipIcon, backwardSkipTitle, backwardSkipAction)
//                .addAction(playPauseIcon, playPauseTitle, playPauseAction)
//                .addAction(forwardSkipIcon, forwardSkipTitle, forwardSkipAction)
//                .setStyle(MediaNotificationCompat.MediaStyle()
//                    .setMediaSession(mediaSession.sessionCompatToken)
//                    .setShowActionsInCompactView(1))
//            setNotification(notificationBuilder.build(), forceForeground)
//        }
//    }
//
//    private fun setNotification(notification: Notification, forceForeground: Boolean) {
//        notificationManger.notify(NOTIFICATION_ID, notification)
//        if (forceForeground) {
//            startForeground(NOTIFICATION_ID, notification)
//            isServiceForeground = true
//        }
//
//        if (isPlaying && !isServiceForeground) {
//            startForeground(NOTIFICATION_ID, notification)
//            isServiceForeground = true
//        }
//
//        if (isNotPlaying && !isServiceBound) {
//            stopForeground(false)
//            isServiceForeground = false
//        }
//    }
//
//    private fun removeNotification() {
//        notificationJob?.cancel()
//        notificationManger.cancel(NOTIFICATION_ID)
//        stopForeground(true)
//        isServiceForeground = false
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = getString(R.string.notification_player_channel_name)
//            val importance = NotificationManager.IMPORTANCE_LOW
//            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
//                enableVibration(false)
//            }
//            notificationManger.createNotificationChannel(channel)
//        }
//    }
//
//    private fun createMediaControlReceiver() {
//        mediaControlReceiver = MediaControlReceiver()
//        val filter = IntentFilter().apply {
//            addAction(ACTION_PAUSE)
//            addAction(ACTION_PLAY)
//            addAction(ACTION_SKIP_BACKWARD)
//            addAction(ACTION_SKIP_FORWARD)
//            addAction(ACTION_CLOSE)
//        }
//        registerReceiver(mediaControlReceiver, filter)
//    }
//
//    inner class PlayerServiceBinder : Binder() {
//        val sessionToken: SessionToken
//            get() = mediaSession.token
//
//        val sleepTimer: StateFlow<Long>
//            get() = _sleepTimer.asStateFlow()
//    }
//
//    inner class MediaControlReceiver : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            when (intent?.action) {
//                ACTION_PLAY -> safePlay()
//                ACTION_PAUSE -> player.pause()
//                ACTION_SKIP_BACKWARD -> skipBackwardOrForward(PLAYER_SKIP_BACKWARD_VALUE)
//                ACTION_SKIP_FORWARD -> skipBackwardOrForward(PLAYER_SKIP_FORWARD_VALUE)
//                ACTION_CLOSE -> {
//                    stopSelf()
//                    isServiceStarted = false
//                }
//            }
//        }
//    }
//
//    companion object {
//        private const val ACTION_PLAY = "com.greencom.android.podcasts.ACTION_PLAY"
//        private const val ACTION_PAUSE = "com.greencom.android.podcasts.ACTION_PAUSE"
//        private const val ACTION_SKIP_BACKWARD = "com.greencom.android.podcasts.ACTION_SKIP_BACKWARD"
//        private const val ACTION_SKIP_FORWARD = "com.greencom.android.podcasts.ACTION_SKIP_FORWARD"
//        private const val ACTION_CLOSE = "com.greencom.android.podcasts.ACTION_CLOSE"
//    }
}