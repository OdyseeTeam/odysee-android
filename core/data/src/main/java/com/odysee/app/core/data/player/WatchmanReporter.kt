package com.odysee.app.core.data.player

import com.odysee.app.core.network.WatchmanApi
import com.odysee.app.core.network.WatchmanReportDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts playback telemetry to Odysee's Watchman backend, mirroring
 * `ui/analytics/watchman.ts` from the web client. Rebuffer counts/durations are
 * accumulated as ExoPlayer transitions in and out of STATE_BUFFERING during
 * playback, and a report is sent every 10 seconds while the video is playing.
 */
@Singleton
class WatchmanReporter @Inject constructor(
    private val watchmanApi: WatchmanApi,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    private var enabled: Boolean = true

    // Current playback context.
    private var permanentUrl: String? = null
    private var userId: String? = null
    private var isLivestream: Boolean = false
    private var isPreview: Boolean = false
    private var streamingMimeType: String? = null

    // Provided each tick by the player.
    @Volatile private var currentPositionMs: Long = 0
    @Volatile private var totalDurationMs: Long = 0
    @Volatile private var currentBitrateBps: Long? = null

    // Rebuffer accumulators (reset every send).
    @Volatile private var rebufCount: Int = 0
    @Volatile private var rebufDurationMs: Long = 0
    @Volatile private var lastBufferStartedAt: Long = 0

    @Volatile private var lastSendAtMs: Long = 0

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) stop()
    }

    fun onPlaybackStarted(
        permanentUrl: String,
        userId: String?,
        isLivestream: Boolean,
        isPreview: Boolean,
        mimeType: String?,
    ) {
        if (!enabled) return
        this.permanentUrl = permanentUrl
        this.userId = userId
        this.isLivestream = isLivestream
        this.isPreview = isPreview
        this.streamingMimeType = mimeType
        rebufCount = 0
        rebufDurationMs = 0
        lastBufferStartedAt = 0
        lastSendAtMs = System.currentTimeMillis()
    }

    fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!enabled) return
        if (isPlaying) ensureLoopRunning() else stopAndFlush()
    }

    fun onBufferingStateChanged(isBuffering: Boolean) {
        if (!enabled) return
        if (isBuffering) {
            // Don't double-count consecutive buffering events.
            if (lastBufferStartedAt == 0L) {
                lastBufferStartedAt = System.currentTimeMillis()
                rebufCount += 1
            }
        } else if (lastBufferStartedAt > 0L) {
            rebufDurationMs += System.currentTimeMillis() - lastBufferStartedAt
            lastBufferStartedAt = 0
        }
    }

    fun updateProgress(positionMs: Long, durationMs: Long, bitrateBps: Long?) {
        currentPositionMs = positionMs
        totalDurationMs = durationMs
        if (bitrateBps != null && bitrateBps > 0) currentBitrateBps = bitrateBps
    }

    fun onPlaybackEnded() {
        if (!enabled) return
        stopAndFlush()
        permanentUrl = null
    }

    private fun ensureLoopRunning() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch {
            while (isActive) {
                delay(INTERVAL_MS)
                send()
            }
        }
    }

    private fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    private fun stopAndFlush() {
        scope.launch {
            send()
            stop()
        }
    }

    private suspend fun send() {
        val url = permanentUrl ?: return
        val uid = userId ?: return
        val now = System.currentTimeMillis()
        val sinceLast = (now - lastSendAtMs).coerceAtLeast(0)
        lastSendAtMs = now

        // If we're still inside a buffer event, treat what we've seen so far as
        // a complete chunk and start a new one for the next tick.
        if (lastBufferStartedAt > 0L) {
            rebufDurationMs += now - lastBufferStartedAt
            lastBufferStartedAt = now
        }
        val bufferDuration = rebufDurationMs.coerceAtMost(sinceLast)
        val bufferCount = rebufCount
        rebufCount = 0
        rebufDurationMs = 0

        val protocol = when {
            isLivestream -> "lvs"
            streamingMimeType?.contains("mpegurl", ignoreCase = true) == true -> "hls"
            else -> "stb"
        }
        val totalSec = (totalDurationMs / 1000L).coerceAtLeast(0)
        val positionMs = if (isLivestream) 0 else currentPositionMs
        val relPosition = if (isLivestream || totalSec <= 0) 0
        else ((positionMs.toDouble() / (totalSec * 1000L).toDouble()) * 100.0).toInt().coerceIn(0, 100)

        val body = WatchmanReportDto(
            rebuf_count = bufferCount,
            rebuf_duration = bufferDuration,
            url = url.removePrefix("lbry://"),
            device = "android",
            duration = sinceLast,
            protocol = protocol,
            player = "media3",
            user_id = uid,
            position = positionMs,
            rel_position = relPosition,
            bitrate = currentBitrateBps,
            preview = if (isPreview) true else null,
        )
        runCatching { watchmanApi.report(body) }
    }

    companion object {
        private const val INTERVAL_MS = 10_000L
    }
}
