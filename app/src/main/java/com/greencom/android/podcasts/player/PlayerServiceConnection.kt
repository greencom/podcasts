package com.greencom.android.podcasts.player

import android.content.Context
import android.util.Log
import androidx.media2.common.MediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.session.MediaController
import androidx.media2.session.SessionCommand
import androidx.media2.session.SessionCommandGroup
import androidx.media2.session.SessionToken
import com.greencom.android.podcasts.di.DispatcherModule.DefaultDispatcher
import com.greencom.android.podcasts.repository.PlayerRepository
import com.greencom.android.podcasts.utils.PLAYER_TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime

// TODO
@Singleton
class PlayerServiceConnection @Inject constructor(
    private val repository: PlayerRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    private lateinit var scope: CoroutineScope

    private var currentPositionJob: Job? = null

    private lateinit var controller: MediaController

    private val _currentEpisode = MutableStateFlow(CurrentEpisode.empty())
    val currentEpisode = _currentEpisode.asStateFlow()

    private val _duration = MutableStateFlow(Long.MAX_VALUE)
    val duration = _duration.asStateFlow()

    private val _playerState = MutableStateFlow(MediaPlayer.PLAYER_STATE_IDLE)
    val playerState = _playerState.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering = _isBuffering.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0F)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    val isPlaying: Boolean
        get() = if (::controller.isInitialized) {
            controller.playerState == MediaPlayer.PLAYER_STATE_PLAYING
        } else false

    val isPaused: Boolean
        get() = if (::controller.isInitialized) {
            controller.playerState == MediaPlayer.PLAYER_STATE_PAUSED
        } else false

    private var startPlaying = true

    private var startFromTimecode = false

    private var timecode = 0L

    @ExperimentalTime
    private val controllerCallback: MediaController.ControllerCallback by lazy {
        object : MediaController.ControllerCallback() {
            override fun onConnected(
                controller: MediaController,
                allowedCommands: SessionCommandGroup
            ) {
                Log.d(PLAYER_TAG, "controllerCallback: onConnected()")
                val currentEpisode = CurrentEpisode.from(controller.currentMediaItem)
                if (currentEpisode.isNotEmpty()) {
                    _currentEpisode.value = currentEpisode
                    _duration.value = controller.duration
                    _playerState.value = controller.playerState
                    postCurrentPosition()
                } else {
                    restoreEpisode()
                }
            }

            override fun onCurrentMediaItemChanged(controller: MediaController, item: MediaItem?) {
                Log.d(PLAYER_TAG, "controllerCallback: onCurrentMediaItemChanged()")
                val episode = CurrentEpisode.from(item)
                _isBuffering.value = true
                _currentEpisode.value = episode
                _duration.value = controller.duration

                val startPosition: Long
                if (startFromTimecode) {
                    startPosition = timecode
                    startFromTimecode = false
                    timecode = 0L
                } else {
                    runBlocking {
                        startPosition = repository.getEpisodePosition(episode.id) ?: 0L
                    }
                }

                controller.prepare().get()
                controller.seekTo(startPosition).get()
                if (startPlaying) controller.play()
            }

            override fun onPlayerStateChanged(controller: MediaController, state: Int) {
                Log.d(PLAYER_TAG, "controllerCallback: onPlayerStateChanged(), state $state")
                _isBuffering.value = false
                _playerState.value = state
                _duration.value = controller.duration
                postCurrentPosition()

                when (state) {
                    MediaPlayer.PLAYER_STATE_ERROR -> {
                        _currentEpisode.value = CurrentEpisode.empty()
                    }
                }
            }

            override fun onSeekCompleted(controller: MediaController, position: Long) {
                Log.d(PLAYER_TAG, "controllerCallback: onSeekCompleted()")
                _currentPosition.value = position
                if (startFromTimecode) {
                    controller.play()
                    startFromTimecode = false
                }
            }

            override fun onPlaybackSpeedChanged(controller: MediaController, speed: Float) {
                Log.d(PLAYER_TAG, "controllerCallback: onPlaybackSpeedChanged()")
                _playbackSpeed.value = speed
                scope.launch {
                    repository.setPlaybackSpeed(speed)
                }
            }
        }
    }

    fun play() {
        controller.play()
    }

    fun pause() {
        controller.pause()
    }

    fun seekTo(position: Long) {
        controller.currentMediaItem ?: return

        val mPosition = when {
            position <= 0L -> 0L
            position >= controller.duration -> controller.duration
            else -> position
        }
        controller.seekTo(mPosition)
        _currentPosition.value = mPosition
    }

    fun playEpisode(episodeId: String) {
        startPlaying = true
        _playerState.value = MediaPlayer.PLAYER_STATE_PAUSED
        controller.setMediaItem(episodeId)
    }

    fun playFromTimecode(episodeId: String, timecode: Long) {
        startFromTimecode = true
        if (currentEpisode.value.id == episodeId) {
            controller.seekTo(timecode)
        } else {
            this.timecode = timecode
            playEpisode(episodeId)
        }
    }

    fun restoreEpisode() {
        startPlaying = false
        scope.launch {
            val episodeId = repository.getLastEpisodeId().first() ?: return@launch
            val episode = repository.getEpisode(episodeId) ?: return@launch
            if (!episode.isCompleted) {
                controller.setMediaItem(episodeId)
            }
        }
    }

    fun markCompleted(episodeId: String) {
        controller.sendCustomCommand(
            SessionCommand(CustomSessionCommand.RESET_PLAYER, null),
            null
        )
        _currentEpisode.value = CurrentEpisode.empty()
        scope.launch {
            repository.markEpisodeCompleted(episodeId)
        }
    }

    fun changePlaybackSpeed() {
        controller.playbackSpeed = when (controller.playbackSpeed) {
            1.0F -> 1.2F
            1.2F -> 1.5F
            1.5F -> 2.0F
            else -> 1.0F
        }
    }

    private fun postCurrentPosition() {
        currentPositionJob?.cancel()
        if (isPlaying) {
            currentPositionJob = scope.launch {
                while (true) {
                    ensureActive()
                    _currentPosition.value = controller.currentPosition.coerceIn(0..duration.value)
                    delay(1000)
                }
            }
        } else {
            controller.currentPosition.coerceIn(0..duration.value).let { value ->
                // Make sure _currentPosition gets a new value that will pass the equality check
                // against the previous one due to the distinctUntilChanged() function applied
                // under the hood.
                val position = when {
                    value != currentPosition.value -> value
                    value < 1 -> value + 1 // Do not get below 0.
                    else -> value - 1
                }
                _currentPosition.value = position
            }
        }
    }

    @ExperimentalTime
    fun connect(context: Context, sessionToken: SessionToken) {
        Log.d(PLAYER_TAG, "PlayerServiceConnection.connect()")
        scope = CoroutineScope(SupervisorJob() + defaultDispatcher)

        controller = MediaController.Builder(context)
            .setSessionToken(sessionToken)
            .setControllerCallback(Executors.newSingleThreadExecutor(), controllerCallback)
            .build()
    }

    fun disconnect() {
        Log.d(PLAYER_TAG, "PlayerServiceConnection.disconnect()")
        controller.close()
        scope.cancel()
    }
}