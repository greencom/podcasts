package com.greencom.android.podcasts.player

import android.content.Context
import android.util.Log
import androidx.media2.common.MediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.session.MediaController
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

    private lateinit var job: Job

    private lateinit var scope: CoroutineScope

    private var currentPositionJob: Job? = null

    private lateinit var controller: MediaController

    private val _currentEpisode = MutableStateFlow(CurrentEpisode.empty())
    val currentEpisode = _currentEpisode.asStateFlow()

    private val _currentState = MutableStateFlow(MediaPlayer.PLAYER_STATE_IDLE)
    val currentState = _currentState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    val isPlaying: Boolean
        get() = if (::controller.isInitialized) controller.playerState.isPlayerPlaying() else false

    val isPaused: Boolean
        get() = if (::controller.isInitialized) controller.playerState.isPlayerPaused() else false

    val duration: Long
        get() = if (::controller.isInitialized && controller.duration >= 0) {
            controller.duration
        } else Long.MAX_VALUE

    private var startPlaying = true

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
                    _currentState.value = controller.playerState
                    postCurrentPosition()
                } else {
                    restoreEpisode()
                }
            }

            override fun onCurrentMediaItemChanged(controller: MediaController, item: MediaItem?) {
                Log.d(PLAYER_TAG, "controllerCallback: onCurrentMediaItemChanged()")
                val episode = CurrentEpisode.from(item)
                _currentEpisode.value = episode

                val startPosition: Long
                runBlocking {
                    startPosition = repository.getEpisodePosition(episode.id) ?: 0L
                }

                controller.prepare().get()
                controller.seekTo(startPosition).get()
                if (startPlaying) controller.play()
            }

            override fun onPlayerStateChanged(controller: MediaController, state: Int) {
                Log.d(PLAYER_TAG, "controllerCallback: onPlayerStateChanged(), state $state")
                _currentState.value = state
                postCurrentPosition()

                if (state.isPlayerError()) {
                    _currentEpisode.value = CurrentEpisode.empty()
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
        if (controller.currentMediaItem == null) return

        val newPosition = when {
            position <= 0L -> 0L
            position >= controller.duration -> controller.duration
            else -> position
        }
        controller.seekTo(newPosition)
        _currentPosition.value = newPosition
    }

    fun playEpisode(episodeId: String) {
        startPlaying = true
        _currentState.value = MediaPlayer.PLAYER_STATE_PAUSED
        controller.setMediaItem(episodeId)
    }

    fun restoreEpisode() {
        startPlaying = false
        scope.launch {
            val episodeId = repository.getLastEpisodeId().first() ?: return@launch
            controller.setMediaItem(episodeId)
        }
    }

    private fun postCurrentPosition() {
        currentPositionJob?.cancel()
        if (isPlaying) {
            currentPositionJob = scope.launch {
                while (true) {
                    ensureActive()
                    if (controller.currentPosition in 0..duration) {
                        _currentPosition.value = controller.currentPosition
                    }
                    delay(1000)
                }
            }
        } else if (controller.currentPosition in 0..duration) {
            val value = if (currentPosition.value == controller.currentPosition) {
                controller.currentPosition + 1
            } else {
                controller.currentPosition
            }
            _currentPosition.value = value
        }
    }

    @ExperimentalTime
    fun connect(context: Context, sessionToken: SessionToken) {
        Log.d(PLAYER_TAG, "PlayerServiceConnection.connect()")
        job = SupervisorJob()
        scope = CoroutineScope(job + defaultDispatcher)

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