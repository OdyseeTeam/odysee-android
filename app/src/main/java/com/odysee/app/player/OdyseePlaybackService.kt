package com.odysee.app.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.odysee.app.MainActivity
import com.odysee.app.core.data.player.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OdyseePlaybackService : MediaSessionService() {

    @Inject lateinit var playerController: PlayerController

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        mediaSession = MediaSession.Builder(this, playerController.exoPlayer)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return super.onTaskRemoved(rootIntent)
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        // PlayerController owns the ExoPlayer lifecycle, so just release the session, not the player.
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
