package com.odysee.app.core.data.cast

import kotlinx.coroutines.flow.StateFlow

/**
 * Flavor-aware facade over the Cast SDK. The full variant uses Google Play
 * Services + RemoteMediaClient. FOSS provides a no-op that always reports
 * "no session".
 */
interface CastController {
    /** True while a Chromecast session is connected and ready to receive media. */
    val isSessionActive: StateFlow<Boolean>

    /** Replace what the cast device is playing with this stream. */
    fun loadMedia(streamUrl: String, title: String, channelName: String?, thumbnailUrl: String?)

    /** Append to the cast device's queue (for playlist play-all). */
    fun queueAppend(streamUrl: String, title: String, channelName: String?, thumbnailUrl: String?)

    /**
     * Load a full queue of items at once. [startIndex] points to the item that
     * begins playback; the rest queue up on the device for autoplay.
     */
    fun queueLoad(items: List<CastQueueItem>, startIndex: Int)

    /** Disconnect from the cast device (returns playback to phone). */
    fun endSession()
}

data class CastQueueItem(
    val streamUrl: String,
    val title: String,
    val channelName: String?,
    val thumbnailUrl: String?,
)

/** No-op fallback. Bound when the cast SDK isn't available (FOSS variant). */
class NoOpCastController : CastController {
    private val _idle = kotlinx.coroutines.flow.MutableStateFlow(false)
    override val isSessionActive: StateFlow<Boolean> = _idle
    override fun loadMedia(streamUrl: String, title: String, channelName: String?, thumbnailUrl: String?) = Unit
    override fun queueAppend(streamUrl: String, title: String, channelName: String?, thumbnailUrl: String?) = Unit
    override fun queueLoad(items: List<CastQueueItem>, startIndex: Int) = Unit
    override fun endSession() = Unit
}
