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
        // When the user swipes the app away from Recents, stop playback and
        // tear down the service — otherwise Media3's default behavior is to
        // KEEP playing (background audio), which for a video app means the
        // user hears a stuck stream with no way to pause it short of force-
        // stopping the app. Users who explicitly want audio to continue
        // after leaving the video screen have the "Play in background"
        // menu action; app dismissal should not be a hidden opt-in for it.
        mediaSession?.player?.let { player ->
            player.pause()
            player.clearMediaItems()
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
