package com.greencom.android.podcasts.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.os.bundleOf
import androidx.media2.common.MediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.session.MediaController
import androidx.media2.session.SessionCommandGroup
import androidx.media2.session.SessionToken
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.di.DispatcherModule.DefaultDispatcher
import com.greencom.android.podcasts.utils.PLAYER_TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

// TODO
@Singleton
class PlayerServiceConnection @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    private lateinit var job: Job
    private lateinit var scope: CoroutineScope

    private var currentPositionJob: Job? = null

    private lateinit var controller: MediaController

    private val _currentEpisode = MutableStateFlow(CurrentEpisode.empty())
    val currentEpisode = _currentEpisode.asStateFlow()

    private val _playerState = MutableStateFlow(MediaPlayer.PLAYER_STATE_IDLE)
    val playerState = _playerState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    val isPlaying: Boolean
        get() = if (::controller.isInitialized) {
            controller.playerState == MediaPlayer.PLAYER_STATE_PLAYING
        } else false

    val isPaused: Boolean
        get() = if (::controller.isInitialized) {
            controller.playerState == MediaPlayer.PLAYER_STATE_PAUSED
        } else false

    val duration: Long
        get() = if (::controller.isInitialized) controller.duration else Long.MAX_VALUE

    @ExperimentalTime
    private val controllerCallback: MediaController.ControllerCallback by lazy {
        object : MediaController.ControllerCallback() {
            override fun onConnected(
                controller: MediaController,
                allowedCommands: SessionCommandGroup
            ) {
                Log.d(PLAYER_TAG, "controllerCallback: onConnected()")
                _currentEpisode.value = CurrentEpisode.from(controller.currentMediaItem)
                _playerState.value = controller.playerState
                postCurrentPosition()
            }

            override fun onDisconnected(controller: MediaController) {
                Log.d(PLAYER_TAG, "controllerCallback: onDisconnected()")
                super.onDisconnected(controller)
            }

            override fun onCurrentMediaItemChanged(controller: MediaController, item: MediaItem?) {
                Log.d(PLAYER_TAG, "controllerCallback: onCurrentMediaItemChanged()")
                _currentEpisode.value = CurrentEpisode.from(item)
                controller.play()
            }

            override fun onPlayerStateChanged(controller: MediaController, state: Int) {
                Log.d(PLAYER_TAG, "controllerCallback: onPlayerStateChanged(), state $state")
                _playerState.value = state
                postCurrentPosition()
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
        val newPosition = when {
            position <= 0L -> 0L
            position >= controller.duration -> controller.duration
            else -> position
        }
        controller.seekTo(newPosition)
        _currentPosition.value = newPosition
    }

    @ExperimentalTime
    fun playEpisode(episode: Episode) {
        controller.setMediaUri(
            Uri.parse(episode.audio),
            bundleOf(
                Pair(PlayerService.EPISODE_ID, episode.id),
                Pair(PlayerService.EPISODE_TITLE, episode.title),
                Pair(PlayerService.EPISODE_PUBLISHER, episode.publisher),
                Pair(PlayerService.EPISODE_IMAGE, episode.image),
                Pair(PlayerService.EPISODE_DURATION, Duration.seconds(episode.audioLength).inWholeMilliseconds),
                Pair(PlayerService.EPISODE_START_POSITION, episode.position)
            )
        )
    }

    private fun postCurrentPosition() {
        currentPositionJob?.cancel()

        if (controller.playerState == MediaPlayer.PLAYER_STATE_PLAYING) {
            currentPositionJob = scope.launch {
                while (true) {
                    if (controller.currentPosition in 0..duration) {
                        _currentPosition.value = controller.currentPosition
                    }
                    delay(1000)
                }
            }
        } else {
            if (controller.currentPosition in 0..duration) {
                _currentPosition.value = controller.currentPosition
            }
        }
    }

    @ExperimentalTime
    fun initConnection(context: Context, sessionToken: SessionToken) {
        Log.d(PLAYER_TAG, "PlayerServiceConnection.initConnection()")
        job = SupervisorJob()
        scope = CoroutineScope(job + defaultDispatcher)

        controller = MediaController.Builder(context)
            .setSessionToken(sessionToken)
            .setControllerCallback(Executors.newSingleThreadExecutor(), controllerCallback)
            .build()
    }

    fun closeConnection() {
        Log.d(PLAYER_TAG, "PlayerServiceConnection.closeConnection()")
        controller.close()
        scope.cancel()
    }

    data class CurrentEpisode(
        val id: String,
        val title: String,
        val publisher: String,
        val image: String,
    ) {

        fun isEmpty(): Boolean = id.isBlank()

        fun isNotEmpty(): Boolean = !isEmpty()

        companion object {
            private const val EMPTY = ""

            fun empty(): CurrentEpisode = CurrentEpisode(EMPTY, EMPTY, EMPTY, EMPTY)

            @ExperimentalTime
            fun from(mediaItem: MediaItem?): CurrentEpisode {
                return CurrentEpisode(
                    id = mediaItem?.metadata?.getString(PlayerService.EPISODE_ID) ?: EMPTY,
                    title = mediaItem?.metadata?.getString(PlayerService.EPISODE_TITLE) ?: EMPTY,
                    publisher = mediaItem?.metadata?.getString(PlayerService.EPISODE_PUBLISHER) ?: EMPTY,
                    image = mediaItem?.metadata?.getString(PlayerService.EPISODE_IMAGE) ?: EMPTY,
                )
            }
        }
    }
}