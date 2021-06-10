package com.greencom.android.podcasts.player

import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.media.AudioAttributesCompat
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.session.*
import timber.log.Timber
import java.util.concurrent.Executors

class PlayerService : MediaSessionService() {

    private lateinit var mediaSession: MediaSession
    private lateinit var player: MediaPlayer

    private val audioAttrs = AudioAttributesCompat.Builder()
        .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
        .setUsage(AudioAttributesCompat.USAGE_MEDIA)
        .build()

    private val mediaSessionCallback = object : MediaSession.SessionCallback() {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): SessionCommandGroup? {
            Timber.d("mediaSessionCallback: onConnect() called")
            return super.onConnect(session, controller)
        }

        override fun onPostConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            Timber.d("mediaSessionCallback: onPostConnect() called")
            super.onPostConnect(session, controller)
        }

        override fun onDisconnected(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            Timber.d("mediaSessionCallback: onDisconnected() called")
            super.onDisconnected(session, controller)
        }

        override fun onSetMediaUri(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            uri: Uri,
            extras: Bundle?
        ): Int {
            Timber.d("mediaSessionCallback: onSetMediaUri() called")

            resetPlayer()

            val mediaItemBuilder = UriMediaItem.Builder(uri)
            if (extras != null) {
                mediaItemBuilder.setMetadata(
                    MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, extras.getString(TITLE))
                        .putString(MediaMetadata.METADATA_KEY_ART_URI, extras.getString(IMAGE_URL))
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, extras.getLong(DURATION))
                        .build()
                )
            }

            var result = player.setMediaItem(mediaItemBuilder.build()).get()
            if (result.resultCode != SessionPlayer.PlayerResult.RESULT_SUCCESS) {
                Timber.d("setMediaItem ERROR, code ${result.resultCode}")
                return result.resultCode
            }

            result = player.prepare().get()
            if (result.resultCode != SessionPlayer.PlayerResult.RESULT_SUCCESS) {
                Timber.d("prepare ERROR, code ${result.resultCode}")
                return result.resultCode
            }

            player.play()

            return SessionResult.RESULT_SUCCESS
        }
    }

    private val mediaPlayerCallback = object : MediaPlayer.PlayerCallback() {
        override fun onPlayerStateChanged(player: SessionPlayer, playerState: Int) {
            Timber.d("mediaPlayerCallback: onPlayerStateChanged() called, playerState is $playerState")
            super.onPlayerStateChanged(player, playerState)
        }

        override fun onBufferingStateChanged(
            player: SessionPlayer,
            item: MediaItem?,
            buffState: Int
        ) {
            Timber.d("mediaPlayerCallback: onBufferingStateChanged() called, buffState is $buffState")
            super.onBufferingStateChanged(player, item, buffState)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("PlayerService: onCreate() called")

        player = MediaPlayer(this).apply {
            registerPlayerCallback(Executors.newSingleThreadExecutor(), mediaPlayerCallback)
            setAudioAttributes(audioAttrs)
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionCallback(Executors.newSingleThreadExecutor(), mediaSessionCallback)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Timber.d("PlayerService: onBind() called")
        return PlayerServiceBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("PlayerService: onDestroy() called")

        mediaSession.close()
        player.unregisterPlayerCallback(mediaPlayerCallback)
        player.close()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Timber.d("PlayerService: onGetSession() called")
        return mediaSession
    }

    private fun resetPlayer() {
        player.reset()
        player.setAudioAttributes(audioAttrs)
    }

    inner class PlayerServiceBinder : Binder() {
        fun getSessionToken(): SessionToken = mediaSession.token
    }

    companion object {
        const val TITLE = "TITLE"
        const val IMAGE_URL = "IMAGE_URL"
        const val DURATION = "DURATION"
    }
}