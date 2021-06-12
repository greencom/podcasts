package com.greencom.android.podcasts.player

import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.media.AudioAttributesCompat
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.session.*
import com.greencom.android.podcasts.utils.GLOBAL_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.Executors

// TODO
class PlayerService : MediaSessionService() {

    private lateinit var mediaSession: MediaSession
    private lateinit var player: MediaPlayer

    private val audioAttrs = AudioAttributesCompat.Builder()
        .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
        .setUsage(AudioAttributesCompat.USAGE_MEDIA)
        .build()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main)

    private val sessionCallback = object : MediaSession.SessionCallback() {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): SessionCommandGroup? {
            Log.d(GLOBAL_TAG,"sessionCallback: onConnect() called")
            return super.onConnect(session, controller)
        }

        override fun onPostConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            Log.d(GLOBAL_TAG,"sessionCallback: onPostConnect() called")
            super.onPostConnect(session, controller)
        }

        override fun onDisconnected(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            Log.d(GLOBAL_TAG,"sessionCallback: onDisconnected() called")
            super.onDisconnected(session, controller)
        }

        override fun onSkipForward(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): Int {
            Log.d(GLOBAL_TAG,"sessionCallback: onSkipForward() called")
            val result = player.seekTo(player.currentPosition + SKIP_FORWARD_VALUE).get()
            if (result.resultCode != SessionResult.RESULT_SUCCESS) Log.d(GLOBAL_TAG, "onSkipForward ERROR, code ${result.resultCode}")
            return result.resultCode
        }

        override fun onSkipBackward(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): Int {
            Log.d(GLOBAL_TAG,"sessionCallback: onSkipBackward() called")
            val result = player.seekTo(player.currentPosition - SKIP_BACKWARD_VALUE).get()
            if (result.resultCode != SessionResult.RESULT_SUCCESS) Log.d(GLOBAL_TAG, "onSkipBackward ERROR, code ${result.resultCode}")
            return result.resultCode
        }

        override fun onSetMediaUri(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            uri: Uri,
            extras: Bundle?
        ): Int {
            Log.d(GLOBAL_TAG,"sessionCallback: onSetMediaUri() called")
            resetPlayer()

            val mediaItemBuilder = UriMediaItem.Builder(uri)
            if (extras != null) {
                mediaItemBuilder.setMetadata(
                    MediaMetadata.Builder()
                        .putString(ID, extras.getString(ID))
                        .putString(TITLE, extras.getString(TITLE))
                        .putString(ART_URI, extras.getString(ART_URI))
                        .putLong(DURATION, extras.getLong(DURATION))
                        .build()
                )
            }

            var result = player.setMediaItem(mediaItemBuilder.build()).get()
            if (result.resultCode != SessionPlayer.PlayerResult.RESULT_SUCCESS) {
                Log.d(GLOBAL_TAG, "setMediaItem ERROR, code ${result.resultCode}")
                return result.resultCode
            }

            result = player.prepare().get()
            if (result.resultCode != SessionPlayer.PlayerResult.RESULT_SUCCESS) {
                Log.d(GLOBAL_TAG, "prepare ERROR, code ${result.resultCode}")
                return result.resultCode
            }

            player.play()
            return SessionResult.RESULT_SUCCESS
        }
    }

    private val playerCallback = object : MediaPlayer.PlayerCallback() {
        override fun onPlayerStateChanged(player: SessionPlayer, playerState: Int) {
            Log.d(GLOBAL_TAG,"playerCallback: onPlayerStateChanged() called, playerState $playerState")
            super.onPlayerStateChanged(player, playerState)
        }

        override fun onBufferingStateChanged(
            player: SessionPlayer,
            item: MediaItem?,
            buffState: Int
        ) {
            Log.d(GLOBAL_TAG,"mediaPlayerCallback: onBufferingStateChanged() called, buffState $buffState")
            super.onBufferingStateChanged(player, item, buffState)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(GLOBAL_TAG,"PlayerService: onCreate() called")

        player = MediaPlayer(this).apply {
            registerPlayerCallback(Executors.newSingleThreadExecutor(), playerCallback)
            setAudioAttributes(audioAttrs)
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionCallback(Executors.newSingleThreadExecutor(), sessionCallback)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(GLOBAL_TAG,"PlayerService: onBind() called")
        return PlayerServiceBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(GLOBAL_TAG,"PlayerService: onDestroy() called")

        serviceScope.cancel()

        mediaSession.close()
        player.unregisterPlayerCallback(playerCallback)
        player.close()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d(GLOBAL_TAG,"PlayerService: onGetSession() called")
        return mediaSession
    }

    private fun resetPlayer() {
        player.reset()
        player.setAudioAttributes(audioAttrs)
    }

    inner class PlayerServiceBinder : Binder() {
        val sessionToken: SessionToken
            get() = mediaSession.token
    }

    companion object {
        const val ID = MediaMetadata.METADATA_KEY_MEDIA_ID
        const val TITLE = MediaMetadata.METADATA_KEY_TITLE
        const val ART_URI = MediaMetadata.METADATA_KEY_ART_URI
        const val DURATION = MediaMetadata.METADATA_KEY_DURATION

        const val SKIP_FORWARD_VALUE = 30_000
        const val SKIP_BACKWARD_VALUE = 10_000
    }
}