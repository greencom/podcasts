package com.greencom.android.podcasts.player

import android.content.Context
import android.util.Log
import androidx.media2.common.MediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.session.MediaController
import androidx.media2.session.SessionCommandGroup
import androidx.media2.session.SessionToken
import com.google.android.exoplayer2.Player
import com.greencom.android.podcasts.repository.PlayerRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime

private const val PLAYER_SERVICE_CONNECTION_TAG = "PLAYER_SERVICE_CONNECTION_TAG"

// TODO
@Singleton
class PlayerServiceConnection @Inject constructor(
    private val playerRepository: PlayerRepository,
) {

    private var controller: MediaController? = null

    private var scope: CoroutineScope? = null

    private var currentPositionJob: Job? = null

    private val _currentEpisode = MutableStateFlow(CurrentEpisode.empty())
    val currentEpisode = _currentEpisode.asStateFlow()

    private val _forceCoverUpdate = MutableSharedFlow<Unit>()
    val forceCoverUpdate = _forceCoverUpdate.asSharedFlow()

    private val _duration = MutableStateFlow(Long.MAX_VALUE)
    val duration = _duration.asStateFlow()

    private val _currentPosition = MutableStateFlow<Long>(0)
    val currentPosition = _currentPosition.asStateFlow()

    private val _exoPlayerState = MutableStateFlow(Player.STATE_IDLE)
    val exoPlayerState = _exoPlayerState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    /**
     * Used to indicate whether the playback should start after current position reset
     * in [resetCurrentPosition]. `false` value (do not start after resetting) used to not
     * start playback for the restored last played episode on the app start. In any other case
     * the playback should start (e.g. user sets an episode to the player, or the next episode
     * in the playlist has started after the end of the previous one).
     */
    private var resetCurrentPositionAndPlay = false

    @ExperimentalTime
    private val controllerCallback: MediaController.ControllerCallback by lazy {
        object : MediaController.ControllerCallback() {
            override fun onConnected(
                controller: MediaController,
                allowedCommands: SessionCommandGroup
            ) {
                Log.d(PLAYER_SERVICE_CONNECTION_TAG, "controllerCallback: onConnected()")
                controller.currentMediaItem ?: return

                updateCurrentEpisode(controller)
                updateDuration(controller)
                updateCurrentPosition(controller)
            }

            override fun onCurrentMediaItemChanged(controller: MediaController, item: MediaItem?) {
                Log.d(PLAYER_SERVICE_CONNECTION_TAG, "controllerCallback: onCurrentMediaItemChanged()")
                updateCurrentEpisode(item)
                updateDuration(controller)
                resetCurrentPosition(controller)
            }

            override fun onPlayerStateChanged(controller: MediaController, state: Int) {
                Log.d(PLAYER_SERVICE_CONNECTION_TAG, "controllerCallback: onPlayerStateChanged() with state $state")
                updateDuration(controller)
                updateCurrentPosition(controller)

                forceCoverUpdate(state)

                // If an error occurred while loading a restored last played episode, it means
                // the restored episode has not started anyway. So set to true.
                if (state == MediaPlayer.PLAYER_STATE_ERROR) {
                    resetCurrentPositionAndPlay = true
                }
            }
        }
    }

    @ExperimentalTime
    fun connect(context: Context, sessionToken: SessionToken) {
        Log.d(PLAYER_SERVICE_CONNECTION_TAG, "connect()")
        // Set to false, so the restored last played episode does not start playing
        // on the application start.
        resetCurrentPositionAndPlay = false

        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        controller = MediaController.Builder(context)
            .setSessionToken(sessionToken)
            .setControllerCallback(Executors.newSingleThreadExecutor(), controllerCallback)
            .build()
    }

    fun disconnect() {
        Log.d(PLAYER_SERVICE_CONNECTION_TAG, "disconnect()")
        controller?.close()
        scope?.cancel()
    }

    fun setEpisode(episodeId: String) {
        // Episodes explicitly set by the user should start playing.
        resetCurrentPositionAndPlay = true
        controller?.setMediaItem(episodeId)
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position.coerceIn(0..duration.value))
    }

    fun setExoPlayerState(state: Int) {
        _exoPlayerState.value = state
    }

    fun setIsPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    @ExperimentalTime
    private fun updateCurrentEpisode(controller: MediaController) {
        _currentEpisode.value = CurrentEpisode.from(controller.currentMediaItem)
    }

    @ExperimentalTime
    private fun updateCurrentEpisode(mediaItem: MediaItem?) {
        _currentEpisode.value = CurrentEpisode.from(mediaItem)
    }

    private fun forceCoverUpdate(playerState: Int) {
        if (playerState == MediaPlayer.PLAYER_STATE_PLAYING) {
            scope?.launch(Dispatchers.Default) {
                _forceCoverUpdate.emit(Unit)
            }
        }
    }

    private fun updateDuration(controller: MediaController) {
        if (controller.duration > 0) {
            _duration.value = controller.duration
        } else {
            _duration.value = Long.MAX_VALUE
        }
    }

    private fun updateCurrentPosition(controller: MediaController) {
        currentPositionJob?.cancel()
        if (isPlaying.value) {
            // If the player is playing (including the application start), it means
            // there is no need for the player to restore last played episode. So set to true.
            resetCurrentPositionAndPlay = true
            currentPositionJob = scope?.launch(Dispatchers.Default) {
                while (true) {
                    ensureActive()
                    _currentPosition.value = controller.currentPosition.coerceIn(0..duration.value)
                    delay(1000)
                }
            }
        } else {
            _currentPosition.value = modifiedCurrentPosition(controller)
        }
    }

    private fun modifiedCurrentPosition(controller: MediaController): Long {
        return controller.currentPosition.coerceIn(0..duration.value).let { position ->
            when {
                position != currentPosition.value -> position
                position < 1000 -> position + 1
                else -> position - 1
            }
        }
    }

    private fun resetCurrentPosition(controller: MediaController) {
        controller.pause()
        // Do not start the playback on the application start for the restored
        // last played episode.
        if (resetCurrentPositionAndPlay) {
            controller.play()
        }
    }

//    private var scope: CoroutineScope? = null
//
//    private var currentPositionJob: Job? = null
//
//    private var controller: MediaController? = null
//
//    private val _currentEpisode = MutableStateFlow(CurrentEpisode.empty())
//    val currentEpisode = _currentEpisode.asStateFlow()
//
//    private val _duration = MutableStateFlow(Long.MAX_VALUE)
//    val duration = _duration.asStateFlow()
//
//    private val _playerState = MutableStateFlow(MediaPlayer.PLAYER_STATE_IDLE)
//    val playerState = _playerState.asStateFlow()
//
//    private val _isBuffering = MutableStateFlow(false)
//    val isBuffering = _isBuffering.asStateFlow()
//
//    private val _currentPosition = MutableStateFlow(0L)
//    val currentPosition = _currentPosition.asStateFlow()
//
//    val isPlaying: Boolean
//        get() = controller?.playerState == MediaPlayer.PLAYER_STATE_PLAYING
//
//    val isPaused: Boolean
//        get() = controller?.playerState == MediaPlayer.PLAYER_STATE_PAUSED
//
//    private var startPlaying = true
//
//    private var startFromTimecode = false
//
//    private var timecode = 0L
//
//    @ExperimentalTime
//    private val controllerCallback: MediaController.ControllerCallback by lazy {
//        object : MediaController.ControllerCallback() {
//            override fun onConnected(
//                controller: MediaController,
//                allowedCommands: SessionCommandGroup
//            ) {
//                Log.d(PLAYER_TAG, "controllerCallback: onConnected()")
//                val currentEpisode = CurrentEpisode.from(controller.currentMediaItem)
//                if (currentEpisode.isNotEmpty()) {
//                    _currentEpisode.value = currentEpisode
//                    _duration.value = controller.duration
//                    _playerState.value = controller.playerState
////                    postCurrentPosition()
//                } else {
//                    restoreEpisode()
//                }
//
//                // Check for buffering state.
//                if (
//                    isBuffering.value && (controller.playerState == MediaPlayer.PLAYER_STATE_PAUSED ||
//                            controller.playerState == MediaPlayer.PLAYER_STATE_PLAYING)
//                ) {
//                    _isBuffering.value = false
//                }
//            }
//
//            override fun onCurrentMediaItemChanged(controller: MediaController, item: MediaItem?) {
//                Log.d(PLAYER_TAG, "controllerCallback: onCurrentMediaItemChanged()")
//                val episode = CurrentEpisode.from(item)
//                _isBuffering.value = true
//                _currentEpisode.value = episode
//                _duration.value = controller.duration
//
//                val startPosition: Long
//                if (startFromTimecode) {
//                    startPosition = timecode
//                    startFromTimecode = false
//                    timecode = 0L
//                } else {
//                    runBlocking {
//                        startPosition = repository.getEpisodePosition(episode.id) ?: 0L
//                    }
//                }
//
//                controller.prepare().get()
//                controller.seekTo(startPosition).get()
//                if (startPlaying) controller.play()
//            }
//
//            override fun onPlayerStateChanged(controller: MediaController, state: Int) {
//                Log.d(PLAYER_TAG, "controllerCallback: onPlayerStateChanged(), state $state")
//                _isBuffering.value = false
//                _playerState.value = state
//                _duration.value = controller.duration
////                postCurrentPosition()
//
//                when (state) {
//                    MediaPlayer.PLAYER_STATE_ERROR -> {
//                        _currentEpisode.value = CurrentEpisode.empty()
//                    }
//                }
//            }
//
//            override fun onSeekCompleted(controller: MediaController, position: Long) {
//                Log.d(PLAYER_TAG, "controllerCallback: onSeekCompleted()")
//                _currentPosition.value = position
//                if (startFromTimecode) {
//                    controller.play()
//                    startFromTimecode = false
//                }
//            }
//        }
//    }
//
//    fun play() {
//        controller?.play()
//    }
//
//    fun pause() {
//        controller?.pause()
//    }
//
//    fun seekTo(position: Long) {
//        controller?.let { controller ->
//            controller.currentMediaItem ?: return
//
//            val mPosition = position.coerceIn(0..controller.duration)
//            controller.seekTo(mPosition)
//            _currentPosition.value = mPosition
//        }
//    }
//
//    fun playEpisode(episodeId: String) {
//        startPlaying = true
//        controller?.pause() // Pause player to show the appropriate UI state while loading.
//        controller?.setMediaItem(episodeId)
//    }
//
//    fun playFromTimecode(episodeId: String, timecode: Long) {
//        startFromTimecode = true
//        if (currentEpisode.value.id == episodeId) {
//            controller?.seekTo(timecode)
//        } else {
//            this.timecode = timecode
//            playEpisode(episodeId)
//        }
//    }
//
//    fun restoreEpisode() {
//        startPlaying = false
//        scope?.launch {
//            val episodeId = repository.getLastEpisodeId().first() ?: return@launch
//            val episode = repository.getEpisode(episodeId) ?: return@launch
//            if (!episode.isCompleted) {
//                controller?.setMediaItem(episodeId)
//            } else {
//                // Reset last episode.
//                repository.setLastEpisodeId("")
//            }
//        }
//    }
//
//    fun markCompleted(episodeId: String) {
//        controller?.sendCustomCommand(
//            SessionCommand(CustomSessionCommand.RESET_PLAYER, null),
//            null
//        )
//        _currentEpisode.value = CurrentEpisode.empty()
//        scope?.launch {
//            repository.onEpisodeCompleted(episodeId)
//        }
//    }
//
//    fun changePlaybackSpeed() {
//        scope?.launch {
//            val newSpeed = when (repository.getPlaybackSpeed().first() ?: 1.0F) {
//                1.0F -> 1.2F
//                1.2F -> 1.5F
//                1.5F -> 2.0F
//                else -> 1.0F
//            }
//            controller?.playbackSpeed = newSpeed
//            repository.setPlaybackSpeed(newSpeed)
//        }
//    }
//
//    @ExperimentalTime
//    fun setSleepTimer(duration: Duration) {
//        controller?.sendCustomCommand(
//            SessionCommand(CustomSessionCommand.SET_SLEEP_TIMER, null),
//            bundleOf(PLAYER_SET_SLEEP_TIMER to duration.inWholeMilliseconds)
//        )
//    }
//
//    fun clearSleepTimer() {
//        controller?.sendCustomCommand(
//            SessionCommand(CustomSessionCommand.REMOVE_SLEEP_TIMER, null),
//            null
//        )
//    }
//
//    private fun postCurrentPosition() {
//        val mController = controller ?: return
//        currentPositionJob?.cancel()
//        if (isPlaying) {
//            currentPositionJob = scope?.launch {
//                while (true) {
//                    ensureActive()
//                    _currentPosition.value = mController.currentPosition.coerceIn(0..duration.value)
//                    delay(1000)
//                }
//            }
//        } else {
//            // Make sure that duration is not less than zero.
//            val duration = duration.value.coerceIn(0..Long.MAX_VALUE)
//            mController.currentPosition.coerceIn(0..duration).let { value ->
//                // Make sure _currentPosition gets a new value that will pass the equality check
//                // against the previous one due to the distinctUntilChanged() function applied
//                // under the hood.
//                val position = when {
//                    value != currentPosition.value -> value
//                    value < 1 -> value + 1 // Do not get below 0.
//                    else -> value - 1
//                }
//                _currentPosition.value = position
//            }
//        }
//    }
//
//    @ExperimentalTime
//    fun connect(context: Context, sessionToken: SessionToken) {
//        Log.d(PLAYER_TAG, "PlayerServiceConnection.connect()")
//        scope = CoroutineScope(SupervisorJob() + defaultDispatcher)
//
//        controller = MediaController.Builder(context)
//            .setSessionToken(sessionToken)
//            .setControllerCallback(Executors.newSingleThreadExecutor(), controllerCallback)
//            .build()
//    }
//
//    fun disconnect() {
//        Log.d(PLAYER_TAG, "PlayerServiceConnection.disconnect()")
//        controller?.close()
//        scope?.cancel()
//    }
}