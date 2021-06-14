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
import com.greencom.android.podcasts.utils.GLOBAL_TAG
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
class PlayerServiceConnection @Inject constructor() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    private lateinit var controller: MediaController

    private val _currentEpisode = MutableStateFlow(CurrentEpisode.empty())
    val currentEpisode = _currentEpisode.asStateFlow()

    private val _playerState = MutableStateFlow(MediaPlayer.PLAYER_STATE_IDLE)
    val playerState = _playerState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    val isPlaying: Boolean
        get() = controller.playerState == MediaPlayer.PLAYER_STATE_PLAYING

    val isPaused: Boolean
        get() = controller.playerState == MediaPlayer.PLAYER_STATE_PAUSED

    val duration: Long
        get() = controller.duration

    private var currentPositionJob: Job? = null

    private val controllerCallback: MediaController.ControllerCallback by lazy {
        object : MediaController.ControllerCallback() {
            override fun onConnected(
                controller: MediaController,
                allowedCommands: SessionCommandGroup
            ) {
                Log.d(GLOBAL_TAG, "controllerCallback: onConnected()")
                super.onConnected(controller, allowedCommands)
            }

            override fun onDisconnected(controller: MediaController) {
                Log.d(GLOBAL_TAG, "controllerCallback: onDisconnected()")
                super.onDisconnected(controller)
            }

            override fun onCurrentMediaItemChanged(controller: MediaController, item: MediaItem?) {
                Log.d(GLOBAL_TAG, "controllerCallback: onCurrentMediaItemChanged()")
                _currentEpisode.value = CurrentEpisode.from(item)
            }

            override fun onPlayerStateChanged(controller: MediaController, state: Int) {
                Log.d(GLOBAL_TAG, "controllerCallback: onPlayerStateChanged(), state $state")
                _playerState.value = state

                currentPositionJob?.cancel()
                if (state == MediaPlayer.PLAYER_STATE_PLAYING) {
                    currentPositionJob = scope.launch {
                        while (true) {
                            if (controller.currentPosition <= duration) {
                                _currentPosition.value = controller.currentPosition
                            }
                            delay(500)
                        }
                    }
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

    fun skipForward() {
        controller.skipForward()
    }

    fun skipBackward() {
        controller.skipBackward()
    }

    fun seekTo(position: Long) {
        controller.seekTo(position)
    }

    @ExperimentalTime
    fun playEpisode(episode: Episode) {
        controller.setMediaUri(
            Uri.parse(episode.audio),
            bundleOf(
                Pair(PlayerService.ID, episode.id),
                Pair(PlayerService.TITLE, episode.title),
                Pair(PlayerService.PUBLISHER, episode.publisher),
                Pair(PlayerService.IMAGE_URI, episode.image),
                Pair(PlayerService.DURATION, Duration.seconds(episode.audioLength).inWholeMilliseconds)
            )
        )
    }

    fun createMediaController(context: Context, sessionToken: SessionToken) {
        if (this::controller.isInitialized) return

        controller = MediaController.Builder(context)
            .setSessionToken(sessionToken)
            .setControllerCallback(Executors.newSingleThreadExecutor(), controllerCallback)
            .build()

        _currentEpisode.value = CurrentEpisode.from(controller.currentMediaItem)
        _playerState.value = controller.playerState
        _currentPosition.value = if (controller.currentPosition == MediaPlayer.UNKNOWN_TIME) {
            0L
        } else controller.currentPosition
    }

    fun close() {
        scope.cancel()
    }

    data class CurrentEpisode(
        val id: String,
        val title: String,
        val publisher: String,
        val image: String,
    ) {

        fun isEmpty(): Boolean {
            return id.isBlank() && title.isBlank() && publisher.isBlank() && image.isBlank()
        }

        fun isNotEmpty(): Boolean = !isEmpty()

        companion object {
            private const val EMPTY = ""

            fun empty(): CurrentEpisode = CurrentEpisode(EMPTY, EMPTY, EMPTY, EMPTY)

            fun from(mediaItem: MediaItem?): CurrentEpisode {
                return CurrentEpisode(
                    id = mediaItem?.metadata?.getString(PlayerService.ID) ?: EMPTY,
                    title = mediaItem?.metadata?.getString(PlayerService.TITLE) ?: EMPTY,
                    publisher = mediaItem?.metadata?.getString(PlayerService.PUBLISHER) ?: EMPTY,
                    image = mediaItem?.metadata?.getString(PlayerService.IMAGE_URI) ?: EMPTY,
                )
            }
        }
    }
}