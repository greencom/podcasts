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

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    private lateinit var mediaSession: MediaSession
    private lateinit var player: MediaPlayer

    private val audioAttrs: AudioAttributesCompat by lazy {
        AudioAttributesCompat.Builder()
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .build()
    }

    private val sessionCallback: MediaSession.SessionCallback by lazy {
        object : MediaSession.SessionCallback() {
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

            override fun onCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                command: SessionCommand
            ): Int {
                Log.d(GLOBAL_TAG,"sessionCallback: onCommandRequest() called, command ${command.commandCode}")
                return super.onCommandRequest(session, controller, command)
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
                            .putString(PUBLISHER, extras.getString(PUBLISHER))
                            .putString(IMAGE_URI, extras.getString(IMAGE_URI))
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

                return SessionResult.RESULT_SUCCESS
            }
        }
    }

    private val playerCallback: MediaPlayer.PlayerCallback by lazy {
        object : MediaPlayer.PlayerCallback() {
            override fun onPlayerStateChanged(player: SessionPlayer, playerState: Int) {
                Log.d(GLOBAL_TAG,"playerCallback: onPlayerStateChanged() called, playerState $playerState")
                super.onPlayerStateChanged(player, playerState)
            }

            override fun onBufferingStateChanged(
                player: SessionPlayer,
                item: MediaItem?,
                buffState: Int
            ) {
                Log.d(GLOBAL_TAG,"playerCallback: onBufferingStateChanged() called, buffState $buffState")
                super.onBufferingStateChanged(player, item, buffState)
            }

            override fun onError(mp: MediaPlayer, item: MediaItem, what: Int, extra: Int) {
                Log.d(GLOBAL_TAG,"playerCallback: onError() called, what $what, extra $extra")
                super.onError(mp, item, what, extra)
            }

            override fun onInfo(mp: MediaPlayer, item: MediaItem, what: Int, extra: Int) {
                Log.d("onInfo","playerCallback: onInfo() called, what $what, extra $extra")
                super.onInfo(mp, item, what, extra)
            }
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
        mediaSession.close()
        player.unregisterPlayerCallback(playerCallback)
        player.close()
        scope.cancel()
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
        const val PUBLISHER = MediaMetadata.METADATA_KEY_AUTHOR
        const val IMAGE_URI = MediaMetadata.METADATA_KEY_ART_URI
        const val DURATION = MediaMetadata.METADATA_KEY_DURATION

        const val REWIND_FORWARD_VALUE = 30_000
        const val REWIND_BACKWARD_VALUE = 10_000
    }
}