package com.greencom.android.podcasts.player

import android.content.Intent
import android.media.AudioAttributes
import android.os.Binder
import android.os.IBinder
import androidx.media.AudioAttributesCompat
import androidx.media2.player.MediaPlayer
import androidx.media2.session.MediaSession
import androidx.media2.session.MediaSessionService
import androidx.media2.session.SessionCommandGroup
import androidx.media2.session.SessionToken
import retrofit2.http.Url
import timber.log.Timber
import java.net.URL
import java.util.concurrent.Executors

class PlayerService : MediaSessionService() {

    private lateinit var mediaSession: MediaSession
    private lateinit var mediaPlayer: MediaPlayer

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
    }

    private val mediaPlayerCallback = object : MediaPlayer.PlayerCallback() {

    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("PlayerService: onCreate() called")

        mediaPlayer = MediaPlayer(this).apply {
            registerPlayerCallback(Executors.newSingleThreadExecutor(), mediaPlayerCallback)
            setAudioAttributes(
                AudioAttributesCompat.Builder()
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                    .build()
            )
        }

        mediaSession = MediaSession.Builder(this, mediaPlayer)
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
        mediaPlayer.unregisterPlayerCallback(mediaPlayerCallback)
        mediaPlayer.close()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Timber.d("PlayerService: onGetSession() called")
        return mediaSession
    }

    inner class PlayerServiceBinder : Binder() {
        fun getSessionToken(): SessionToken = mediaSession.token
    }
}