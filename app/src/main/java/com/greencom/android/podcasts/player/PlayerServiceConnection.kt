package com.greencom.android.podcasts.player

import android.content.Context
import androidx.core.os.bundleOf
import androidx.media2.common.MediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.session.MediaController
import androidx.media2.session.SessionCommand
import androidx.media2.session.SessionCommandGroup
import androidx.media2.session.SessionToken
import com.google.android.exoplayer2.Player
import com.greencom.android.podcasts.repository.PlayerRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * PlayerServiceConnection contains [MediaController] that manages the connection with the
 * [PlayerService]'s [MediaSession][androidx.media2.session.MediaSession] and exposes
 * player related data to the application. Use [connect] to initialize properties and
 * connect to the MediaSession. Do not forget to [disconnect] when the connection is no longer
 * needed.
 *
 * Note: since [PlayerService] uses [ExoPlayer][com.google.android.exoplayer2.SimpleExoPlayer]
 * instead of Media2 [MediaPlayer], the connection contains bugs due to the difference
 * in the implementation of these players. [exoPlayerState] and [isPlaying] use ExoPlayer's
 * states directly but need to be updated from the outside using [setExoPlayerState] and
 * [setIsPlaying] methods. Pass the appropriate data using [PlayerService.PlayerServiceBinder]
 * binder class that provides access to the corresponding [PlayerService] properties.
 */
@Singleton
class PlayerServiceConnection @Inject constructor(
    /** [PlayerRepository] provides access to the player-related Room and DataStore methods. */
    private val playerRepository: PlayerRepository,
) {

    /** The instance of [MediaController]. Uses [controllerCallback]. */
    private var controller: MediaController? = null

    /**
     * PlayerServiceConnection [CoroutineScope]. Uses [SupervisorJob] and [Dispatchers.Main].
     * Cancels on [disconnect].
     */
    private var scope: CoroutineScope? = null

    /**
     * The [Job] that manages the updating of the [_currentPosition], see [updateCurrentPosition].
     */
    private var currentPositionJob: Job? = null

    /**
     * [MutableStateFlow] that contains the current episode of the player. The episode is
     * represented as [MediaItemEpisode]. See [updateCurrentEpisode].
     */
    private val _currentEpisode = MutableStateFlow(MediaItemEpisode.empty())

    /**
     * [StateFlow] that contains the current episode of the player. The episode is
     * represented as [MediaItemEpisode].
     */
    val currentEpisode = _currentEpisode.asStateFlow()

    /**
     * [MutableStateFlow] that contains the duration of the current episode. `Long.MAX_VALUE`
     * used when the duration is unknown. See [updateDuration].
     */
    private val _duration = MutableStateFlow(Long.MAX_VALUE)

    /**
     * [StateFlow] that contains the duration of the current episode. `Long.MAX_VALUE`
     * used when the duration is unknown.
     */
    val duration = _duration.asStateFlow()

    /**
     * [MutableStateFlow] that contains the current position of the player. See
     * [updateCurrentPosition] and [updateCurrentPositionOneShot].
     */
    private val _currentPosition = MutableStateFlow<Long>(0)

    /** [StateFlow] that contains the current position of the player. */
    val currentPosition = _currentPosition.asStateFlow()

    /**
     * [MutableStateFlow] that contains [ExoPlayer][com.google.android.exoplayer2.SimpleExoPlayer]
     * state.
     *
     * This value should be updated from the outside due to connection bugs described in the
     * [PlayerServiceConnection] documentation, see [setExoPlayerState].
     */
    private val _exoPlayerState = MutableStateFlow(Player.STATE_IDLE)

    /**
     * [StateFlow] that contains [ExoPlayer][com.google.android.exoplayer2.SimpleExoPlayer]
     * state.
     *
     * Note: this value should be updated from the outside due to connection bugs described in the
     * [PlayerServiceConnection] documentation, see [setExoPlayerState].
     */
    val exoPlayerState = _exoPlayerState.asStateFlow()

    /**
     * [MutableStateFlow] that contains [ExoPlayer][com.google.android.exoplayer2.SimpleExoPlayer]
     * `isPlaying` state.
     *
     * This value should be updated from the outside due to connection bugs described in the
     * [PlayerServiceConnection] documentation, see [setIsPlaying].
     */
    private val _isPlaying = MutableStateFlow(false)

    /**
     * [StateFlow] that contains [ExoPlayer][com.google.android.exoplayer2.SimpleExoPlayer]
     * `isPlaying` state.
     *
     * Note: this value should be updated from the outside due to connection bugs described in the
     * [PlayerServiceConnection] documentation, see [setIsPlaying].
     */
    val isPlaying = _isPlaying.asStateFlow()

    /**
     * Indicates whether the playback should start after the reset of the [_currentPosition]
     * in [resetCurrentPosition]. `false` value (do not start after resetting) used to not
     * start playback for the restored last played episode on the app start. In any other case
     * the playback should start (e.g. the user sets an episode to the player, or the next episode
     * in the playlist has started after the end of the previous one).
     */
    private var resetCurrentPositionAndPlay = false

    /** The instance of [MediaController.ControllerCallback] used by [controller]. */
    @ExperimentalTime
    private val controllerCallback: MediaController.ControllerCallback by lazy {
        object : MediaController.ControllerCallback() {
            override fun onConnected(
                controller: MediaController,
                allowedCommands: SessionCommandGroup
            ) {
                // Collect `isPlaying` values and update _currentPosition depending on the value
                // while the controller is connected.
                scope?.launch(Dispatchers.Default) {
                    isPlaying.collect { isPlaying ->
                        updateCurrentPosition(controller, isPlaying)
                    }
                }

                // Update _currentEpisode and _currentDuration values if the controller was
                // connected when the MediaSession already had media item.
                if (controller.currentMediaItem != null) {
                    updateCurrentEpisode(controller)
                    updateDuration(controller)
                }
            }

            override fun onCurrentMediaItemChanged(controller: MediaController, item: MediaItem?) {
                updateCurrentEpisode(item)
                updateDuration(controller)
                // Reset _currentPosition value by setting the player on pause and
                // immediately resume. The reason is described in the method documentation.
                resetCurrentPosition(controller)
            }

            override fun onPlayerStateChanged(controller: MediaController, state: Int) {
                updateDuration(controller)

                // If an error occurred while loading the restored episode, it means
                // the restored episode has not started anyway. So set to true.
                if (state == MediaPlayer.PLAYER_STATE_ERROR) {
                    resetCurrentPositionAndPlay = true
                }
            }

            override fun onSeekCompleted(controller: MediaController, position: Long) {
                updateCurrentPositionOneShot(controller)
            }
        }
    }

    /**
     * Initialize [PlayerServiceConnection] properties and connect the [controller]
     * to the [PlayerService]'s [MediaSession][androidx.media2.session.MediaSession] with
     * a given [SessionToken].
     */
    @ExperimentalTime
    fun connect(context: Context, sessionToken: SessionToken) {
        // Set to false, so the restored last played episode does not start playing
        // on the application start.
        resetCurrentPositionAndPlay = false

        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        controller = MediaController.Builder(context)
            .setSessionToken(sessionToken)
            .setControllerCallback(Executors.newSingleThreadExecutor(), controllerCallback)
            .build()
    }

    /**
     * Disconnect the [controller] from the [PlayerService]'s
     * [MediaSession][androidx.media2.session.MediaSession] and clear [PlayerServiceConnection]
     * resources.
     */
    fun disconnect() {
        controller?.close()
        scope?.cancel()
    }

    /** Send `Play` command to the player. */
    fun play() {
        controller?.play()
    }

    /** Send `Pause` command to the player. */
    fun pause() {
        controller?.pause()
    }

    /** Set an episode to the player by episode ID and play. */
    fun setEpisode(episodeId: String) {
        // Episodes explicitly set by the user should start playing.
        resetCurrentPositionAndPlay = true
        controller?.setMediaItem(episodeId)
    }

    /** Seek through the current episode to a given [position] in milliseconds. */
    fun seekTo(position: Long) {
        controller?.seekTo(position.coerceIn(0..duration.value))
    }

    /**
     * Play an episode by ID from a given [timecode] in milliseconds.
     * If the given episode is already the current one, just seek to the [timecode].
     * Otherwise set the episode and play from the [timecode].
     */
    fun playFromTimecode(episodeId: String, timecode: Long) {
        // Episodes explicitly set by the user should start playing.
        resetCurrentPositionAndPlay = true
        if (episodeId == currentEpisode.value.id) {
            controller?.seekTo(timecode.coerceIn(0..duration.value))
        } else {
            controller?.sendCustomCommand(
                SessionCommand(CustomCommand.SET_EPISODE_AND_PLAY_FROM_TIMECODE, null),
                bundleOf(
                    CustomCommandKey.SET_EPISODE_AND_PLAY_FROM_TIMECODE_EPISODE_ID_KEY to episodeId,
                    CustomCommandKey.SET_EPISODE_AND_PLAY_FROM_TIMECODE_TIMECODE_KEY to timecode
                )
            )
        }
    }

    /**
     * Increase the playback speed. Four modes are available: `1.0`, `1.2`, `1.5` and `2.0`.
     * See [decreasePlaybackSpeed].
     */
    fun increasePlaybackSpeed() {
        scope?.launch(Dispatchers.IO) {
            val speed = when (playerRepository.getPlaybackSpeed().first() ?: 1.0F) {
                1.0F -> 1.2F
                1.2F -> 1.5F
                1.5F -> 2.0F
                else -> 1.0F
            }
            controller?.playbackSpeed = speed
            playerRepository.setPlaybackSpeed(speed)
        }
    }

    /**
     * Decrease the playback speed. Four modes are available: `1.0`, `1.2`, `1.5` and `2.0`.
     * See [increasePlaybackSpeed].
     */
    fun decreasePlaybackSpeed() {
        scope?.launch(Dispatchers.IO) {
            val speed = when (playerRepository.getPlaybackSpeed().first() ?: 1.0F) {
                2.0F -> 1.5F
                1.5F -> 1.2F
                1.2F -> 1.0F
                else -> 2.0F
            }
            controller?.playbackSpeed = speed
            playerRepository.setPlaybackSpeed(speed)
        }
    }

    /**
     * Set a Sleep Timer for a given [Duration]. Only one timer can be active at the same
     * time. See [removeSleepTimer].
     */
    @ExperimentalTime
    fun setSleepTimer(duration: Duration) {
        controller?.sendCustomCommand(
            SessionCommand(CustomCommand.SET_SLEEP_TIMER, null),
            bundleOf(
                CustomCommandKey.SET_SLEEP_TIMER_DURATION_KEY to duration.inWholeMilliseconds
            )
        )
    }

    /**
     * Remove the active Sleep Timer. Do nothing if there is no active Sleep Timer.
     * See [setSleepTimer].
     */
    fun removeSleepTimer() {
        controller?.sendCustomCommand(
            SessionCommand(CustomCommand.REMOVE_SLEEP_TIMER, null),
            null
        )
    }

    /** Mark the current episode as completed and close the player. */
    fun markCurrentEpisodeCompleted() {
        controller?.sendCustomCommand(
            SessionCommand(CustomCommand.MARK_CURRENT_EPISODE_COMPLETED, null),
            null
        )
        scope?.launch(Dispatchers.IO) {
            // When the player removes the current media item, `isPlaying` state updates
            // and triggers the update of the media item state. So delay a bit to ensure
            // that onEpisodeCompleted will not be overwritten.
            delay(250)
            playerRepository.onEpisodeCompleted(currentEpisode.value.id)
            _currentEpisode.value = MediaItemEpisode.empty()
        }
    }

    /**
     * Used to update [exoPlayerState] value that needs to be updated from the outside.
     * The reason is described in the [PlayerServiceConnection] documentation.
     */
    fun setExoPlayerState(state: Int) {
        _exoPlayerState.value = state
    }

    /**
     * Used to update [isPlaying] value that needs to be updated from the outside.
     * The reason is described in the [PlayerServiceConnection] documentation.
     */
    fun setIsPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    /** Update [_currentEpisode] value. */
    @ExperimentalTime
    private fun updateCurrentEpisode(controller: MediaController) {
        _currentEpisode.value = MediaItemEpisode.from(controller.currentMediaItem)
    }

    /** Update [_currentEpisode] value. */
    @ExperimentalTime
    private fun updateCurrentEpisode(mediaItem: MediaItem?) {
        _currentEpisode.value = MediaItemEpisode.from(mediaItem)
    }

    /** Update [_duration] value. If the duration is negative, set `Long.MAX_VALUE`. */
    private fun updateDuration(controller: MediaController) {
        if (controller.duration > 0) {
            _duration.value = controller.duration
        } else {
            _duration.value = Long.MAX_VALUE
        }
    }

    /**
     * Updates [_currentPosition] value. If [isPlaying] is `true`, updates the position
     * every second using [currentPositionJob]. If `false`, does one-shot update using
     * [updateCurrentPositionOneShot].
     */
    private fun updateCurrentPosition(controller: MediaController, isPlaying: Boolean) {
        currentPositionJob?.cancel()
        if (isPlaying) {
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
            updateCurrentPositionOneShot(controller)
        }
    }

    /** Performs one-shot update of [_currentPosition] value. See [updateCurrentPosition]. */
    private fun updateCurrentPositionOneShot(controller: MediaController) {
        _currentPosition.value = modifiedCurrentPosition(controller)
    }

    /**
     * Returns the modified current position from the [controller]. Used to get the current
     * position that will pass the `equals` check on the [distinctUntilChanged] operator that
     * applied to the [currentPosition] by default.
     */
    private fun modifiedCurrentPosition(controller: MediaController): Long {
        return controller.currentPosition.coerceIn(0..duration.value).let { position ->
            when {
                position != currentPosition.value -> position
                position < 1000 -> position + 1
                else -> position - 1
            }
        }
    }

    /**
     * Used to reset the updating of the [_currentPosition] value due to the bug of the
     * connection between the [controller] and [PlayerService] ExoPlayer. Because of this bug,
     * after executing internal [PlayerService] `SEEK_TO` commands, the controller starts
     * to return the current position incorrectly, counting from `0`, and not from the position
     * that the `SEEK_TO` command has led to. [MediaController.pause] fixes it, after that the
     * controller starts to return the correct current position. Use [resetCurrentPositionAndPlay]
     * value to choose whether to to start playing after the pause or not.
     */
    private fun resetCurrentPosition(controller: MediaController) {
        controller.pause()
        // Do not start the playback on the application start for the restored episode.
        if (resetCurrentPositionAndPlay) {
            controller.play()
        }
    }
}