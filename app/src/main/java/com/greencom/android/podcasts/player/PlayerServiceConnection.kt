package com.greencom.android.podcasts.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.os.bundleOf
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.player.MediaPlayer
import androidx.media2.session.MediaController
import androidx.media2.session.SessionCommandGroup
import androidx.media2.session.SessionToken
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.utils.GLOBAL_TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

// TODO
@Singleton
class PlayerServiceConnection @Inject constructor() {

    private lateinit var controller: MediaController

    private val _currentEpisode = MutableSharedFlow<CurrentEpisode>(1)
    val currentEpisode = _currentEpisode.asSharedFlow()

    private val _playerState = MutableSharedFlow<Int>(1)
    val playerState = _playerState.asSharedFlow()

    private val _currentPosition = MutableSharedFlow<Long>(1)
    val currentPosition = _currentPosition.asSharedFlow()

    val isPlaying: Boolean
        get() = controller.playerState == MediaPlayer.PLAYER_STATE_PLAYING

    val isPaused: Boolean
        get() = controller.playerState == MediaPlayer.PLAYER_STATE_PAUSED

    val duration: Long
        get() = controller.duration

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    private var currentPositionJob: Job? = null

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
                Pair(PlayerService.ART_URI, episode.image),
                Pair(PlayerService.DURATION, Duration.seconds(episode.audioLength).inWholeMilliseconds)
            )
        )
    }

    private val controllerCallback = object : MediaController.ControllerCallback() {

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
            scope.launch {
                val id = item?.metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) ?: ""
                val title = item?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                val image = item?.metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI) ?: ""
                val episodeMetadata = CurrentEpisode(id, title, image)
                _currentEpisode.emit(episodeMetadata)
            }
        }

        override fun onPlayerStateChanged(controller: MediaController, state: Int) {
            Log.d(GLOBAL_TAG, "controllerCallback: onPlayerStateChanged(), state $state")
            scope.launch { _playerState.emit(state) }

            currentPositionJob?.cancel()
            if (state == MediaPlayer.PLAYER_STATE_PLAYING) {
                currentPositionJob = scope.launch {
                    while (true) {
                        if (controller.currentPosition <= duration) {
                            _currentPosition.emit(controller.currentPosition)
                        }
                        delay(500)
                    }
                }
            }
        }
    }

    fun createMediaController(context: Context, sessionToken: SessionToken) {
        if (this::controller.isInitialized) return

        controller = MediaController.Builder(context)
            .setSessionToken(sessionToken)
            .setControllerCallback(Executors.newSingleThreadExecutor(), controllerCallback)
            .build()
    }

    fun close() {
        scope.cancel()
    }

    data class CurrentEpisode(
        val id: String,
        val title: String,
        val image: String,
    )
}