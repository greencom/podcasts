package com.greencom.android.podcasts.player

import android.content.Context
import android.net.Uri
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.player.MediaPlayer
import androidx.media2.session.MediaController
import androidx.media2.session.SessionCommandGroup
import androidx.media2.session.SessionToken
import com.greencom.android.podcasts.data.domain.Episode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class PlayerViewModel : ViewModel() {

    private lateinit var mediaController: MediaController

    private val _currentEpisode = MutableSharedFlow<EpisodeMetadata>(1)
    val currentEpisode = _currentEpisode.asSharedFlow()

    private val _playerState = MutableSharedFlow<Int>(1)
    val playerState = _playerState.asSharedFlow()

    val isPlaying: Boolean
        get() = mediaController.playerState == MediaPlayer.PLAYER_STATE_PLAYING

    val isPaused: Boolean
        get() = mediaController.playerState == MediaPlayer.PLAYER_STATE_PAUSED

    private val mediaControllerCallback = object : MediaController.ControllerCallback() {
        override fun onConnected(
            controller: MediaController,
            allowedCommands: SessionCommandGroup
        ) {
            Timber.d("mediaControllerCallback: onConnected() called")
            super.onConnected(controller, allowedCommands)
        }

        override fun onDisconnected(controller: MediaController) {
            Timber.d("mediaControllerCallback: onDisconnected() called")
            super.onDisconnected(controller)
        }

        override fun onCurrentMediaItemChanged(controller: MediaController, item: MediaItem?) {
            Timber.d("mediaControllerCallback: onCurrentMediaItemChanged() called")
            viewModelScope.launch {
                val episodeMetadata = EpisodeMetadata(
                    title = item?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
                    image = item?.metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI) ?: "",
                )
                _currentEpisode.emit(episodeMetadata)
            }
        }

        override fun onPlayerStateChanged(controller: MediaController, state: Int) {
            Timber.d("mediaControllerCallback: onPlayerStateChanged() called")
            viewModelScope.launch { _playerState.emit(state) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController.close()
    }

    fun play() {
        mediaController.play()
    }

    fun pause() {
        mediaController.pause()
    }

    @ExperimentalTime
    fun playEpisode(episode: Episode) {
        mediaController.setMediaUri(
            Uri.parse(episode.audio),
            bundleOf(
                Pair(PlayerService.TITLE, episode.title),
                Pair(PlayerService.IMAGE_URL, episode.image),
                Pair(PlayerService.DURATION, Duration.seconds(episode.audioLength).inWholeMilliseconds),
            )
        )
    }

    fun createMediaController(context: Context, mediaSessionToken: SessionToken) {
        if (this::mediaController.isInitialized) return

        mediaController = MediaController.Builder(context)
            .setSessionToken(mediaSessionToken)
            .setControllerCallback(Executors.newSingleThreadExecutor(), mediaControllerCallback)
            .build()
    }

    data class EpisodeMetadata(
        val title: String,
        val image: String,
    )
}