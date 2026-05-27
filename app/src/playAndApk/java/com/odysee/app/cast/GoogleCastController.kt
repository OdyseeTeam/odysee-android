package com.odysee.app.cast

import android.content.Context
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import com.odysee.app.core.data.cast.CastController
import com.odysee.app.core.data.cast.CastQueueItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCastController @Inject constructor(
    @ApplicationContext private val context: Context,
) : CastController {

    private val _isSessionActive = MutableStateFlow(false)
    override val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val sessionManager get() = runCatching {
        CastContext.getSharedInstance(context).sessionManager
    }.getOrNull()

    private val listener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            _isSessionActive.value = true
        }
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            _isSessionActive.value = true
        }
        override fun onSessionEnded(session: CastSession, error: Int) {
            _isSessionActive.value = false
        }
        override fun onSessionSuspended(session: CastSession, reason: Int) {
            _isSessionActive.value = false
        }
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            _isSessionActive.value = false
        }
        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            _isSessionActive.value = false
        }
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionEnding(session: CastSession) = Unit
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
    }

    init {
        // Register the listener and seed the initial state.
        runCatching {
            sessionManager?.addSessionManagerListener(listener, CastSession::class.java)
            _isSessionActive.value = sessionManager?.currentCastSession?.isConnected == true
        }
    }

    private fun currentRemoteClient() =
        sessionManager?.currentCastSession?.takeIf { it.isConnected }?.remoteMediaClient

    override fun loadMedia(
        streamUrl: String,
        title: String,
        channelName: String?,
        thumbnailUrl: String?,
    ) {
        val client = currentRemoteClient() ?: return
        runCatching { client.load(MediaLoadRequestData.Builder().setMediaInfo(buildInfo(streamUrl, title, channelName, thumbnailUrl)).setAutoplay(true).build()) }
    }

    override fun queueAppend(
        streamUrl: String,
        title: String,
        channelName: String?,
        thumbnailUrl: String?,
    ) {
        val client = currentRemoteClient() ?: return
        val item = MediaQueueItem.Builder(buildInfo(streamUrl, title, channelName, thumbnailUrl))
            .setAutoplay(true)
            .build()
        runCatching { client.queueAppendItem(item, null) }
    }

    override fun queueLoad(items: List<CastQueueItem>, startIndex: Int) {
        val client = currentRemoteClient() ?: return
        if (items.isEmpty()) return
        val queueItems = items.map {
            MediaQueueItem.Builder(buildInfo(it.streamUrl, it.title, it.channelName, it.thumbnailUrl))
                .setAutoplay(true)
                .build()
        }.toTypedArray()
        runCatching {
            client.queueLoad(
                queueItems,
                startIndex.coerceIn(0, items.size - 1),
                com.google.android.gms.cast.MediaStatus.REPEAT_MODE_REPEAT_OFF,
                null,
            )
        }
    }

    override fun endSession() {
        runCatching { sessionManager?.endCurrentSession(true) }
    }

    private fun buildInfo(
        streamUrl: String,
        title: String,
        channelName: String?,
        thumbnailUrl: String?,
    ): MediaInfo {
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, title)
            if (!channelName.isNullOrBlank()) {
                putString(MediaMetadata.KEY_SUBTITLE, channelName)
            }
            if (!thumbnailUrl.isNullOrBlank()) {
                addImage(WebImage(android.net.Uri.parse(thumbnailUrl)))
            }
        }
        val contentType = if (streamUrl.contains(".m3u8", ignoreCase = true)) {
            "application/x-mpegurl"
        } else {
            "video/mp4"
        }
        return MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(contentType)
            .setMetadata(metadata)
            .build()
    }
}
