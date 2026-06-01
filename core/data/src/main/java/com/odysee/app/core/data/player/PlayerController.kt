package com.odysee.app.core.data.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.auth.AuthRepository
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.data.collections.CollectionEntry
import com.odysee.app.core.data.collections.FavoritesRepository
import com.odysee.app.core.data.collections.WatchLaterRepository
import com.odysee.app.core.data.history.WatchHistoryEntry
import com.odysee.app.core.data.history.WatchHistoryRepository
import com.odysee.app.core.datastore.AuthPreferences
import com.odysee.app.core.data.reactions.MyReaction
import com.odysee.app.core.data.reactions.Reactions
import com.odysee.app.core.data.reactions.ReactionsRepository
import com.odysee.app.core.data.subscriptions.SubscriptionsRepository
import com.odysee.app.core.network.LbryioApi
import com.odysee.app.core.model.Comment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class CurrentMedia(
    val claimId: String,
    val permanentUrl: String,
    val title: String,
    val description: String?,
    val channelClaimId: String?,
    val channelName: String,
    val channelTitle: String? = null,
    val channelInitial: Char,
    val channelAvatarUrl: String?,
    val thumbnailUrl: String?,
    val ageLabel: String?,
    val liveStreamUrl: String? = null,
    val mediaType: String? = null,
    val linkedCommentId: String? = null,
    /**
     * When true, `PlayerController.play(...)` does NOT prepare ExoPlayer and
     * instead emits to [PlayerController.openShortsEvents] so the host can
     * route to the dedicated shorts player.
     */
    val isShort: Boolean = false,
) {
    val renderMode: MediaRenderMode
        get() = when {
            liveStreamUrl != null -> MediaRenderMode.Video
            mediaType.isNullOrBlank() -> MediaRenderMode.Video
            mediaType.startsWith("video/") -> MediaRenderMode.Video
            mediaType.startsWith("audio/") -> MediaRenderMode.Audio
            mediaType.startsWith("image/") -> MediaRenderMode.Image
            mediaType == "application/pdf" -> MediaRenderMode.Pdf
            mediaType.startsWith("text/") -> MediaRenderMode.Text
            else -> MediaRenderMode.Download
        }
}

enum class MediaRenderMode { Video, Audio, Image, Pdf, Text, Download }

data class RelatedItemUi(
    val claimId: String,
    val permanentUrl: String,
    val title: String,
    val description: String?,
    val channelClaimId: String?,
    val channelName: String,
    val channelTitle: String? = null,
    val channelInitial: Char,
    val channelAvatarUrl: String?,
    val thumbnailUrl: String?,
    val ageLabel: String,
    val durationLabel: String,
)

sealed interface RelatedState {
    data object Idle : RelatedState
    data object Loading : RelatedState
    data class Success(val items: List<RelatedItemUi>) : RelatedState
    data class Error(val message: String) : RelatedState
}

enum class CommentSort(val raw: Int, val label: String) {
    Best(3, "Best"),
    Newest(0, "Newest"),
    Controversial(2, "Controversial"),
}

data class CommentUiModel(
    val id: String,
    val parentId: String? = null,
    val author: String,
    val authorTitle: String? = null,
    val authorChannelId: String? = null,
    val authorInitial: Char,
    val authorAvatarUrl: String?,
    val body: String,
    val ageLabel: String,
    val isPinned: Boolean,
    val pinnedByName: String? = null,
    val isEdited: Boolean = false,
    val isCreator: Boolean = false,
    val isMine: Boolean = false,
    val isModerator: Boolean = false,
    val isGlobalMod: Boolean = false,
    val replyCount: Int = 0,
    val supportAmount: Double = 0.0,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val myReaction: com.odysee.app.core.data.reactions.MyReaction = com.odysee.app.core.data.reactions.MyReaction.NONE,
    val creatorLiked: Boolean = false,
    val creatorAvatarUrl: String? = null,
    val authorPremiumTier: com.odysee.app.core.data.auth.PremiumTier = com.odysee.app.core.data.auth.PremiumTier.None,
    val creatorMembership: String? = null,
)

enum class BlockScope { Self, Creator, Admin }

sealed interface CommentsState {
    data object Idle : CommentsState
    data object Loading : CommentsState
    data class Success(val comments: List<CommentUiModel>) : CommentsState
    data class Error(val message: String) : CommentsState
}

data class PlayerState(
    val media: CurrentMedia? = null,
    val streamingUrl: String? = null,
    val isResolving: Boolean = false,
    val errorMessage: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val comments: CommentsState = CommentsState.Idle,
    val related: RelatedState = RelatedState.Idle,
    val moreFromChannel: RelatedState = RelatedState.Idle,
    val inWatchLater: Boolean = false,
    val inFavorites: Boolean = false,
    val reactions: Reactions? = null,
    val isChannelSubscribed: Boolean = false,
    val channelFollowerCount: Long? = null,
    val viewCount: Long? = null,
    val commentSort: CommentSort = CommentSort.Best,
    val repliesByParent: Map<String, List<CommentUiModel>> = emptyMap(),
    val playlist: PlaylistContext? = null,
    val paywall: com.odysee.app.core.model.Paywall = com.odysee.app.core.model.Paywall.Free,
    val isPurchased: Boolean = false,
    val isMembersOnly: Boolean = false,
    val isMemberOfChannel: Boolean = false,
    val purchaseStatus: PurchaseStatus = PurchaseStatus.Idle,
)

sealed class PurchaseStatus {
    data object Idle : PurchaseStatus()
    data object Processing : PurchaseStatus()
    data class Failed(val message: String) : PurchaseStatus()
}

data class PlaylistContext(
    val id: String,
    val name: String,
    val items: List<PlaylistContextItem>,
    val currentIndex: Int,
)

data class PlaylistContextItem(
    val claimId: String,
    val permanentUrl: String,
    val title: String,
    val description: String? = null,
    val channelName: String,
    val channelClaimId: String?,
    val channelAvatarUrl: String?,
    val thumbnailUrl: String?,
)

enum class PlayerOpenMode { Expanded, Minimized, Pip }

sealed interface PlayerUiCommand {
    data class Show(val mode: PlayerOpenMode) : PlayerUiCommand
}

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val contentRepository: ContentRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val watchLaterRepository: WatchLaterRepository,
    private val favoritesRepository: FavoritesRepository,
    private val reactionsRepository: ReactionsRepository,
    private val authRepository: AuthRepository,
    private val authPreferences: AuthPreferences,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val lbryioApi: LbryioApi,
    private val okHttpClient: okhttp3.OkHttpClient,
    private val watchman: WatchmanReporter,
    val castController: com.odysee.app.core.data.cast.CastController,
) {
    private val context: Context = appContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var playbackServiceStarted = false

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    private val _uiCommands = Channel<PlayerUiCommand>(Channel.UNLIMITED)
    val uiCommands: Flow<PlayerUiCommand> = _uiCommands.receiveAsFlow()
    private val _openShortsEvents = Channel<CurrentMedia>(Channel.UNLIMITED)
    val openShortsEvents: Flow<CurrentMedia> = _openShortsEvents.receiveAsFlow()
    private val _isPipActive = MutableStateFlow(false)
    val isPipActive: StateFlow<Boolean> = _isPipActive.asStateFlow()
    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()
    private val _activeVideoHeight = MutableStateFlow(0)
    val activeVideoHeight: StateFlow<Int> = _activeVideoHeight.asStateFlow()
    private val savedPositions = mutableMapOf<String, Long>()

    private fun ensurePlaybackServiceStarted() {
        if (playbackServiceStarted) return
        playbackServiceStarted = true
        val intent = android.content.Intent().apply {
            component = android.content.ComponentName(
                context.packageName,
                "com.odysee.app.player.OdyseePlaybackService",
            )
        }
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }.onFailure {
            playbackServiceStarted = false
            android.util.Log.e("PlayerController", "Failed to start playback service", it)
        }
    }

    private fun stopPlaybackService() {
        if (!playbackServiceStarted) return
        playbackServiceStarted = false
        val intent = android.content.Intent().apply {
            component = android.content.ComponentName(
                context.packageName,
                "com.odysee.app.player.OdyseePlaybackService",
            )
        }
        runCatching { context.stopService(intent) }
    }

    init {
        scope.launch {
            // When the Chromecast session becomes active mid-playback, stop the
            // local ExoPlayer and hand the current stream off to the cast device.
            // When the session ends, the user can tap the in-app play overlay to
            // resume locally (resolve URL → exoPlayer.prepare path).
            castController.isSessionActive.collect { active ->
                if (!active) return@collect
                val media = _state.value.media ?: return@collect
                val url = _state.value.streamingUrl ?: return@collect
                runCatching { exoPlayer.stop() }
                castController.loadMedia(
                    streamUrl = url,
                    title = media.title,
                    channelName = media.channelName,
                    thumbnailUrl = media.thumbnailUrl,
                )
            }
        }
        scope.launch {
            watchLaterRepository.items.collect { items ->
                val current = _state.value.media?.claimId
                _state.update { it.copy(inWatchLater = current != null && items.any { e -> e.claimId == current }) }
            }
        }
        scope.launch {
            favoritesRepository.items.collect { items ->
                val current = _state.value.media?.claimId
                _state.update { it.copy(inFavorites = current != null && items.any { e -> e.claimId == current }) }
            }
        }
        scope.launch {
            subscriptionsRepository.subscriptions.collect { subs ->
                val channelId = _state.value.media?.channelClaimId
                _state.update {
                    it.copy(isChannelSubscribed = channelId != null && subs.any { s -> s.claimId == channelId })
                }
            }
        }
        scope.launch {
            authPreferences.playbackPositions.collect { positions ->
                savedPositions.clear()
                savedPositions.putAll(positions)
            }
        }
        // Periodic position save while playing.
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(15_000)
                if (exoPlayer.isPlaying) persistCurrentPosition()
            }
        }
    }

    private fun persistCurrentPosition() {
        val claimId = _state.value.media?.claimId ?: return
        val pos = exoPlayer.currentPosition
        val duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
        // Don't save if too close to the start or end.
        if (pos < 5_000L) return
        if (duration > 0 && pos > duration - 10_000L) {
            // Watched to near-end — clear instead of saving.
            scope.launch {
                val map = authPreferences.playbackPositions.firstOrNull()?.toMutableMap() ?: mutableMapOf()
                if (map.remove(claimId) != null) authPreferences.setPlaybackPositions(map)
            }
            return
        }
        scope.launch {
            val map = authPreferences.playbackPositions.firstOrNull()?.toMutableMap() ?: mutableMapOf()
            map[claimId] = pos
            // Keep only the most recent 500 entries to avoid unbounded growth.
            val trimmed = if (map.size > 500) {
                map.entries.sortedByDescending { it.value }.take(500).associate { it.key to it.value }
            } else map
            authPreferences.setPlaybackPositions(trimmed)
        }
    }

    private var watchmanProgressJob: Job? = null
    private fun startWatchmanProgressLoop() {
        if (watchmanProgressJob?.isActive == true) return
        watchmanProgressJob = scope.launch {
            while (true) {
                val bitrate = runCatching {
                    exoPlayer.videoFormat?.bitrate?.toLong()
                }.getOrNull()
                watchman.updateProgress(
                    positionMs = runCatching { exoPlayer.currentPosition }.getOrDefault(0L),
                    durationMs = runCatching { exoPlayer.duration }.getOrDefault(0L)
                        .coerceAtLeast(0L),
                    bitrateBps = bitrate,
                )
                kotlinx.coroutines.delay(1_000)
            }
        }
    }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                .setDataSourceFactory(
                    androidx.media3.datasource.DefaultHttpDataSource.Factory()
                        .setUserAgent("Mozilla/5.0 (Linux; Android) Chrome/120.0.0.0 Mobile Safari/537.36")
                        .setDefaultRequestProperties(mapOf("Referer" to "https://odysee.com/"))
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(30_000)
                        .setReadTimeoutMs(30_000),
                )
                .setLoadErrorHandlingPolicy(
                    object : androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy() {
                        override fun getRetryDelayMsFor(
                            loadErrorInfo: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo,
                        ): Long {
                            val ex = loadErrorInfo.exception
                            if (ex is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                                // The player CDN returns 429 while a video is still being
                                // transcoded; web waits and retries until it's ready. Retry with
                                // exponential backoff up to ~2 minutes total.
                                if (ex.responseCode == 429 || ex.responseCode in 500..599) {
                                    if (loadErrorInfo.errorCount > 24) return androidx.media3.common.C.TIME_UNSET
                                    val base = 2000L
                                    val backoff = base * (1L shl (loadErrorInfo.errorCount.coerceAtMost(4) - 1).coerceAtLeast(0))
                                    return backoff.coerceAtMost(15_000L)
                                }
                            }
                            return super.getRetryDelayMsFor(loadErrorInfo)
                        }

                        override fun getMinimumLoadableRetryCount(dataType: Int): Int = Int.MAX_VALUE
                    },
                ),
        )
        .build().apply {
        playWhenReady = true
        // Tell Media3 this is audio/video content so foreground notification + audio focus work.
        setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .build(),
            true,
        )
        setHandleAudioBecomingNoisy(true)
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                android.util.Log.i("PlayerController", "isPlaying=$isPlaying")
                _state.update { it.copy(isPlaying = isPlaying) }
                watchman.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    // Only start the foreground media-notification service once the player
                    // is actually playing — otherwise Android kills the app for not calling
                    // startForeground within 5s when playback errors out before we play.
                    ensurePlaybackServiceStarted()
                } else {
                    persistCurrentPosition()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                android.util.Log.i("PlayerController", "playbackState=$playbackState")
                _state.update {
                    it.copy(isBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING)
                }
                watchman.onBufferingStateChanged(
                    playbackState == androidx.media3.common.Player.STATE_BUFFERING,
                )
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    watchman.onPlaybackEnded()
                    advanceToNextPlaylistItem()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                persistCurrentPosition()
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                _activeVideoHeight.value = videoSize.height
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("PlayerController", "onPlayerError", error)
                _state.update {
                    it.copy(errorMessage = error.localizedMessage ?: error.message ?: "Playback error")
                }
                stopPlaybackService()
            }
        })
    }

    fun setPipActive(active: Boolean) {
        _isPipActive.value = active
    }

    fun setFullscreen(active: Boolean) {
        _isFullscreen.value = active
    }

    val autoplayNext: kotlinx.coroutines.flow.Flow<Boolean> = authPreferences.autoplay
    val autoplayMedia: kotlinx.coroutines.flow.Flow<Boolean> = authPreferences.autoplayMedia

    fun setAutoplayNext(value: Boolean) {
        scope.launch { authPreferences.setAutoplay(value) }
    }

    fun setAutoplayMedia(value: Boolean) {
        scope.launch { authPreferences.setAutoplayMedia(value) }
    }

    private var resolveJob: Job? = null
    private var commentsJob: Job? = null
    private var relatedJob: Job? = null
    private var reactionsJob: Job? = null

    private var chatSocket: okhttp3.WebSocket? = null

    private fun closeChatSocket() {
        chatSocket?.close(1000, null)
        chatSocket = null
    }

    private fun connectChatSocket(claimId: String, channelName: String, channelClaimId: String?) {
        closeChatSocket()
        val category = if (!channelClaimId.isNullOrBlank()) "$channelName:$channelClaimId" else channelName
        val safeCategory = java.net.URLEncoder.encode(category, "UTF-8")
        val url = "wss://sockety.odysee.tv/ws/commentron?id=$claimId&category=$safeCategory&sub_category=viewer"
        android.util.Log.i("PlayerController", "Chat WS connect → $url")
        val request = okhttp3.Request.Builder().url(url).build()
        chatSocket = okHttpClient.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                android.util.Log.i("PlayerController", "Chat WS open")
            }
            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                android.util.Log.e("PlayerController", "Chat WS failure", t)
            }
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                android.util.Log.d("PlayerController", "Chat WS msg: ${text.take(200)}")
                val element = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(text) }
                    .getOrNull() ?: return
                val obj = element as? kotlinx.serialization.json.JsonObject ?: return
                val type = (obj["type"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return
                if (type != "delta") return
                val data = obj["data"] as? kotlinx.serialization.json.JsonObject ?: return
                val comment = data["comment"] as? kotlinx.serialization.json.JsonObject ?: return
                fun field(name: String): String? =
                    (comment[name] as? kotlinx.serialization.json.JsonPrimitive)?.content
                val id = field("comment_id") ?: return
                val author = field("channel_name") ?: "anonymous"
                val body = field("comment").orEmpty()
                val timestampSec = field("timestamp")?.toLongOrNull() ?: (System.currentTimeMillis() / 1000)
                val ui = CommentUiModel(
                    id = id,
                    author = author,
                    authorInitial = (author.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
                    authorAvatarUrl = null,
                    body = body,
                    ageLabel = formatAge(timestampSec),
                    isPinned = false,
                )
                _state.update { s ->
                    val existing = (s.comments as? CommentsState.Success)?.comments ?: emptyList()
                    if (existing.any { it.id == ui.id }) s
                    else s.copy(comments = CommentsState.Success(listOf(ui) + existing))
                }
            }
        })
    }

    fun play(
        media: CurrentMedia,
        openMode: PlayerOpenMode = PlayerOpenMode.Expanded,
        playlist: PlaylistContext? = null,
    ) {
        if (media.isShort) {
            // Hand off to the shorts player; never let a short open in the
            // standard expanded surface, regardless of where the tap came from.
            runCatching { exoPlayer.stop() }
            _openShortsEvents.trySend(media)
            return
        }
        _uiCommands.trySend(PlayerUiCommand.Show(openMode))
        val current = _state.value
        val effectivePlaylist = playlist ?: current.playlist
        if (current.media?.claimId == media.claimId && current.streamingUrl != null) {
            exoPlayer.playWhenReady = true
            if (playlist != null && current.playlist?.id != playlist.id) {
                _state.update { it.copy(playlist = playlist) }
            }
            return
        }
        closeChatSocket()
        if (media.liveStreamUrl != null) {
            connectChatSocket(media.claimId, media.channelName, media.channelClaimId)
        }
        _state.value = PlayerState(
            media = media,
            isResolving = true,
            comments = CommentsState.Loading,
            related = RelatedState.Loading,
            moreFromChannel = RelatedState.Idle,
            reactions = null,
            playlist = effectivePlaylist?.copy(
                currentIndex = effectivePlaylist.items
                    .indexOfFirst { it.claimId == media.claimId }
                    .takeIf { it >= 0 } ?: effectivePlaylist.currentIndex,
            ),
        )
        media.channelClaimId?.let { channelId ->
            scope.launch {
                runCatching { lbryioApi.subCount(channelId) }.getOrNull()
                    ?.data?.firstOrNull()?.let { count ->
                        _state.update { it.copy(channelFollowerCount = count) }
                    }
            }
        }
        scope.launch {
            runCatching { contentRepository.getViewCounts(listOf(media.claimId)) }
                .getOrNull()
                ?.get(media.claimId)
                ?.let { count ->
                    _state.update { it.copy(viewCount = count) }
                }
        }
        reactionsJob?.cancel()
        reactionsJob = scope.launch {
            val r = reactionsRepository.fetch(media.claimId)
            if (r != null) _state.update { it.copy(reactions = r) }
        }
        scope.launch {
            val subscribed = media.channelClaimId?.let { id ->
                subscriptionsRepository.subscriptions.firstOrNull()?.any { it.claimId == id }
            } ?: false
            _state.update {
                it.copy(
                    inWatchLater = watchLaterRepository.isIn(media.claimId),
                    inFavorites = favoritesRepository.isIn(media.claimId),
                    isChannelSubscribed = subscribed,
                )
            }
        }
        resolveJob?.cancel()
        commentsJob?.cancel()

        resolveJob = scope.launch {
            // First: resolve claim metadata to learn paywall / purchase state.
            // Without this we'd happily request the stream URL for a paid claim
            // even though the SDK would refuse to serve until purchase.
            val claimMeta = runCatching {
                contentRepository.getClaimsByIds(listOf(media.claimId)).firstOrNull()
            }.getOrNull()
            claimMeta?.let { c ->
                _state.update { st ->
                    if (st.media?.claimId != media.claimId) return@update st
                    val newMedia = if (st.media.mediaType.isNullOrBlank() && !c.mediaType.isNullOrBlank())
                        st.media.copy(mediaType = c.mediaType) else st.media
                    st.copy(
                        media = newMedia,
                        paywall = c.paywall,
                        isPurchased = c.isPurchased,
                        isMembersOnly = c.isMembersOnly,
                    )
                }
            }
            // For fiat-paywalled content the SDK's purchase_receipt is never
            // populated — purchase records live on lbryio (Arweave-backed).
            // Hit customer/list to detect those.
            val fiatPaywall = claimMeta?.paywall is com.odysee.app.core.model.Paywall.FiatPurchase ||
                claimMeta?.paywall is com.odysee.app.core.model.Paywall.FiatRental
            if (fiatPaywall && claimMeta?.isPurchased != true) {
                val hasFiat = runCatching { contentRepository.hasFiatPurchase(media.claimId) }.getOrNull() == true
                if (hasFiat) {
                    _state.update {
                        if (it.media?.claimId != media.claimId) it else it.copy(isPurchased = true)
                    }
                }
            }
            // Check membership subscription if the content is members-only.
            if (claimMeta?.isMembersOnly == true && media.channelClaimId != null) {
                val isMember = runCatching {
                    contentRepository.listMyMembershipSubscriptions()
                }.getOrNull()
                    ?.any { it.creatorChannelId == media.channelClaimId && it.status == "active" }
                    ?: false
                _state.update {
                    if (it.media?.claimId != media.claimId) it
                    else it.copy(isMemberOfChannel = isMember)
                }
            }
            // If the claim is paywalled and not yet purchased, OR members-only and
            // user isn't a member, hold off resolving the stream URL — the UI
            // surfaces the buy/join dialog. After purchase the ViewModel
            // re-invokes play() which will pass this gate.
            val gateState = _state.value
            val needsPurchase = gateState.paywall !is com.odysee.app.core.model.Paywall.Free && !gateState.isPurchased
            val needsMembership = gateState.isMembersOnly && !gateState.isMemberOfChannel
            if (needsPurchase || needsMembership) {
                _state.update { it.copy(isResolving = false) }
                return@launch
            }
            val resolved = runCatching {
                media.liveStreamUrl?.takeIf { it.isNotBlank() }
                    ?: contentRepository.resolveStreamUrl(media.permanentUrl)
            }
            resolved
                .onSuccess { url ->
                    _state.update { it.copy(isResolving = false, streamingUrl = url) }
                    val resolvedMode = _state.value.media?.renderMode ?: MediaRenderMode.Video
                    if (resolvedMode != MediaRenderMode.Video && resolvedMode != MediaRenderMode.Audio) {
                        // Non-AV content: skip ExoPlayer entirely.
                        return@onSuccess
                    }
                    // If a Chromecast session is active, stream goes to the
                    // cast device, not the local ExoPlayer.
                    if (castController.isSessionActive.value) {
                        runCatching { exoPlayer.stop() }
                        castController.loadMedia(
                            streamUrl = url,
                            title = media.title,
                            channelName = media.channelName,
                            thumbnailUrl = media.thumbnailUrl,
                        )
                        return@onSuccess
                    }
                    val metadata = androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(media.title)
                        .setArtist(media.channelName)
                        .also { mb ->
                            media.thumbnailUrl?.let { thumb ->
                                runCatching { mb.setArtworkUri(android.net.Uri.parse(thumb)) }
                            }
                        }
                        .build()
                    val item = MediaItem.Builder()
                        .setUri(url)
                        .setMediaId(media.claimId)
                        .setMediaMetadata(metadata)
                        .build()
                    exoPlayer.setMediaItem(item)
                    exoPlayer.prepare()
                    // Resume from last known position. Local takes precedence
                    // (more recent); fall back to the cross-device resume point
                    // from lbryio for the first watch on this device.
                    val localPos = savedPositions[media.claimId]?.takeIf { it > 0 }
                    if (localPos != null) {
                        exoPlayer.seekTo(localPos)
                    } else if (media.liveStreamUrl == null) {
                        scope.launch {
                            val remoteSec = runCatching {
                                lbryioApi.fileLastPositions(media.claimId).data
                                    ?.get(media.claimId)
                            }.getOrNull()
                            val remoteMs = remoteSec?.takeIf { it > 0.0 }?.let { (it * 1000).toLong() }
                            if (remoteMs != null && _state.value.media?.claimId == media.claimId) {
                                exoPlayer.seekTo(remoteMs)
                                savedPositions[media.claimId] = remoteMs
                            }
                        }
                    }
                    exoPlayer.playWhenReady = true
                    val signedUserId = (authRepository.state.value as? AuthState.SignedIn)
                        ?.user?.id
                    watchman.onPlaybackStarted(
                        permanentUrl = media.permanentUrl,
                        userId = signedUserId?.toString(),
                        isLivestream = media.liveStreamUrl != null,
                        isPreview = false,
                        mimeType = "application/x-mpegURL",
                    )
                    // View-tracking ping for Odysee's per-claim view counter.
                    if (media.liveStreamUrl == null) {
                        scope.launch {
                            runCatching {
                                lbryioApi.fileView(
                                    uri = media.permanentUrl.removePrefix("lbry://"),
                                    outpoint = media.claimId,
                                    claimId = media.claimId,
                                )
                            }
                        }
                    }
                    startWatchmanProgressLoop()
                    // The playback service is started lazily from the isPlaying listener
                    // once the player actually starts playing (avoids foreground-service
                    // start timeouts when the stream errors before reaching play state).
                    runCatching {
                        watchHistoryRepository.add(
                            WatchHistoryEntry(
                                claimId = media.claimId,
                                permanentUrl = media.permanentUrl,
                                title = media.title,
                                channelName = media.channelName,
                                channelClaimId = media.channelClaimId,
                                thumbnailUrl = media.thumbnailUrl,
                                watchedAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isResolving = false,
                            errorMessage = error.message ?: error::class.simpleName ?: "Unknown error",
                        )
                    }
                }
        }

        commentsJob = scope.launch {
            loadAndEnrichComments(media.claimId, _state.value.commentSort, media.channelClaimId)
        }
        loadRelated(media)
    }

    private suspend fun fetchPremiumByChannel(
        channelIds: List<String>,
    ): Map<String, com.odysee.app.core.data.auth.PremiumTier> {
        if (channelIds.isEmpty()) return emptyMap()
        val response = runCatching {
            lbryioApi.userHasPremium(channelIds.joinToString(","))
        }.getOrNull()?.data ?: return emptyMap()
        return response.mapValues { (_, v) ->
            when {
                v.hasPremiumPlus -> com.odysee.app.core.data.auth.PremiumTier.PremiumPlus
                v.hasPremium -> com.odysee.app.core.data.auth.PremiumTier.Premium
                else -> com.odysee.app.core.data.auth.PremiumTier.None
            }
        }
    }

    private suspend fun loadAndEnrichComments(
        claimId: String,
        sort: CommentSort,
        creatorChannelId: String?,
    ) {
        val mineChannelId = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel?.claimId
        val mineChannelName = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel?.name
        runCatching { contentRepository.getComments(claimId, sortBy = sort.raw) }
            .onSuccess { items ->
                val creatorName = _state.value.media?.channelName?.takeIf { it.isNotBlank() }
                val base = items.map { it.toUi(creatorChannelId, mineChannelId, pinnedByName = creatorName) }
                _state.update { it.copy(comments = CommentsState.Success(base)) }
                if (items.isEmpty()) return@onSuccess

                // Fetch avatars + reactions in parallel.
                val channelIds = items.mapNotNull { it.authorChannelId }.distinct()
                val avatarsJob = scope.launch {
                    val channels = runCatching { contentRepository.getChannels(channelIds) }
                        .getOrNull().orEmpty()
                    val byId = channels.associateBy { it.claimId }
                    _state.update { st ->
                        val current = (st.comments as? CommentsState.Success)?.comments ?: base
                        val merged = current.map { c ->
                            val ch = c.authorChannelId?.let { byId[it] } ?: return@map c
                            c.copy(
                                authorAvatarUrl = ch.thumbnailUrl?.takeIf { it.isNotBlank() } ?: c.authorAvatarUrl,
                                authorTitle = ch.title?.takeIf { it.isNotBlank() } ?: c.authorTitle,
                            )
                        }
                        st.copy(comments = CommentsState.Success(merged))
                    }
                }
                val creatorAvatar = creatorChannelId?.let { id ->
                    runCatching { contentRepository.getChannels(listOf(id)) }
                        .getOrNull()?.firstOrNull()?.thumbnailUrl
                }?.takeIf { it.isNotBlank() } ?: _state.value.media?.channelAvatarUrl
                val reactions = runCatching {
                    contentRepository.getCommentReactions(
                        items.map { it.commentId }, mineChannelId, mineChannelName,
                    )
                }.getOrNull() ?: emptyMap()
                if (reactions.isNotEmpty()) {
                    _state.update { st ->
                        val current = (st.comments as? CommentsState.Success)?.comments ?: base
                        val updated = current.map { c ->
                            val r = reactions[c.id] ?: return@map c
                            c.copy(
                                likes = r.likes,
                                dislikes = r.dislikes,
                                myReaction = when {
                                    r.myLike -> com.odysee.app.core.data.reactions.MyReaction.LIKE
                                    r.myDislike -> com.odysee.app.core.data.reactions.MyReaction.DISLIKE
                                    else -> com.odysee.app.core.data.reactions.MyReaction.NONE
                                },
                                creatorLiked = r.creatorLike,
                                creatorAvatarUrl = if (r.creatorLike) creatorAvatar else null,
                            )
                        }
                        st.copy(comments = CommentsState.Success(updated))
                    }
                }

                val premiumByChannel = fetchPremiumByChannel(channelIds)
                if (premiumByChannel.isNotEmpty()) {
                    _state.update { st ->
                        val current = (st.comments as? CommentsState.Success)?.comments ?: base
                        val updated = current.map { c ->
                            val tier = c.authorChannelId?.let { premiumByChannel[it] } ?: return@map c
                            c.copy(authorPremiumTier = tier)
                        }
                        st.copy(comments = CommentsState.Success(updated))
                    }
                }
                if (!creatorChannelId.isNullOrBlank()) {
                    val creatorMembership = runCatching {
                        contentRepository.checkCreatorMembershipsForChannels(creatorChannelId, channelIds)
                    }.getOrNull().orEmpty()
                    if (creatorMembership.isNotEmpty()) {
                        _state.update { st ->
                            val current = (st.comments as? CommentsState.Success)?.comments ?: base
                            val updated = current.map { c ->
                                val name = c.authorChannelId?.let { creatorMembership[it] } ?: return@map c
                                c.copy(creatorMembership = name)
                            }
                            st.copy(comments = CommentsState.Success(updated))
                        }
                    }
                }
                avatarsJob.join()
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        comments = CommentsState.Error(
                            error.message ?: error::class.simpleName ?: "Unknown error",
                        ),
                    )
                }
            }
    }

    fun loadReplies(parentCommentId: String) {
        val media = _state.value.media ?: return
        if (_state.value.repliesByParent[parentCommentId] != null) return
        val mineChannelId = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel?.claimId
        val mineChannelName = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel?.name
        scope.launch {
            val items = runCatching {
                contentRepository.getComments(
                    media.claimId,
                    sortBy = CommentSort.Newest.raw,
                    parentId = parentCommentId,
                )
            }.getOrNull().orEmpty()
            if (items.isEmpty()) {
                _state.update {
                    it.copy(repliesByParent = it.repliesByParent + (parentCommentId to emptyList()))
                }
                return@launch
            }
            val base = items.map { it.toUi(media.channelClaimId, mineChannelId, pinnedByName = media.channelName) }
            _state.update {
                it.copy(repliesByParent = it.repliesByParent + (parentCommentId to base))
            }
            // Avatars + titles
            scope.launch {
                val channels = runCatching {
                    contentRepository.getChannels(items.mapNotNull { it.authorChannelId }.distinct())
                }.getOrNull().orEmpty().associateBy { it.claimId }
                _state.update { st ->
                    val current = st.repliesByParent[parentCommentId] ?: return@update st
                    val withMeta = current.map { c ->
                        val ch = c.authorChannelId?.let { channels[it] } ?: return@map c
                        c.copy(
                            authorAvatarUrl = ch.thumbnailUrl?.takeIf { it.isNotBlank() } ?: c.authorAvatarUrl,
                            authorTitle = ch.title?.takeIf { it.isNotBlank() } ?: c.authorTitle,
                        )
                    }
                    st.copy(repliesByParent = st.repliesByParent + (parentCommentId to withMeta))
                }
            }
            // Reactions
            val creatorAvatar = media.channelClaimId?.let { id ->
                runCatching { contentRepository.getChannels(listOf(id)) }
                    .getOrNull()?.firstOrNull()?.thumbnailUrl
            }?.takeIf { it.isNotBlank() } ?: media.channelAvatarUrl
            val reactions = runCatching {
                contentRepository.getCommentReactions(
                    items.map { it.commentId }, mineChannelId, mineChannelName,
                )
            }.getOrNull() ?: emptyMap()
            if (reactions.isNotEmpty()) {
                _state.update { st ->
                    val current = st.repliesByParent[parentCommentId] ?: return@update st
                    val updated = current.map { c ->
                        val r = reactions[c.id] ?: return@map c
                        c.copy(
                            likes = r.likes,
                            dislikes = r.dislikes,
                            myReaction = when {
                                r.myLike -> com.odysee.app.core.data.reactions.MyReaction.LIKE
                                r.myDislike -> com.odysee.app.core.data.reactions.MyReaction.DISLIKE
                                else -> com.odysee.app.core.data.reactions.MyReaction.NONE
                            },
                            creatorLiked = r.creatorLike,
                            creatorAvatarUrl = if (r.creatorLike) creatorAvatar else null,
                        )
                    }
                    st.copy(repliesByParent = st.repliesByParent + (parentCommentId to updated))
                }
            }
            val premiumByChannel = fetchPremiumByChannel(items.mapNotNull { it.authorChannelId }.distinct())
            if (premiumByChannel.isNotEmpty()) {
                _state.update { st ->
                    val current = st.repliesByParent[parentCommentId] ?: return@update st
                    val updated = current.map { c ->
                        val tier = c.authorChannelId?.let { premiumByChannel[it] } ?: return@map c
                        c.copy(authorPremiumTier = tier)
                    }
                    st.copy(repliesByParent = st.repliesByParent + (parentCommentId to updated))
                }
            }
        }
    }

    fun setCommentSort(sort: CommentSort) {
        val current = _state.value.commentSort
        if (current == sort) return
        _state.update { it.copy(commentSort = sort, comments = CommentsState.Loading) }
        val media = _state.value.media ?: return
        commentsJob?.cancel()
        commentsJob = scope.launch {
            loadAndEnrichComments(media.claimId, sort, media.channelClaimId)
        }
    }

    fun toggleCommentReaction(commentId: String, like: Boolean) {
        val signedIn = authRepository.state.value as? AuthState.SignedIn
        val active = signedIn?.activeChannel ?: return
        val current = (_state.value.comments as? CommentsState.Success)?.comments ?: return
        val target = current.firstOrNull { it.id == commentId } ?: return
        val wasLiked = target.myReaction == com.odysee.app.core.data.reactions.MyReaction.LIKE
        val wasDisliked = target.myReaction == com.odysee.app.core.data.reactions.MyReaction.DISLIKE
        val type = if (like) "like" else "dislike"
        val remove = if (like) wasLiked else wasDisliked
        // Optimistic update
        val newReaction = when {
            remove -> com.odysee.app.core.data.reactions.MyReaction.NONE
            like -> com.odysee.app.core.data.reactions.MyReaction.LIKE
            else -> com.odysee.app.core.data.reactions.MyReaction.DISLIKE
        }
        val newLikes = target.likes + when {
            like && !wasLiked -> 1
            like && wasLiked -> -1
            !like && wasLiked -> -1
            else -> 0
        }
        val newDislikes = target.dislikes + when {
            !like && !wasDisliked -> 1
            !like && wasDisliked -> -1
            like && wasDisliked -> -1
            else -> 0
        }
        val updated = current.map {
            if (it.id == commentId) it.copy(
                likes = newLikes.coerceAtLeast(0),
                dislikes = newDislikes.coerceAtLeast(0),
                myReaction = newReaction,
            ) else it
        }
        _state.update { it.copy(comments = CommentsState.Success(updated)) }
        scope.launch {
            runCatching {
                contentRepository.reactToComment(commentId, active.claimId, active.name, type, remove)
            }
        }
    }

    fun requestExpand() {
        _uiCommands.trySend(PlayerUiCommand.Show(PlayerOpenMode.Expanded))
    }

    fun requestPip() {
        _uiCommands.trySend(PlayerUiCommand.Show(PlayerOpenMode.Pip))
    }

    fun retryResolve() {
        val media = _state.value.media ?: return
        play(media)
    }

    fun retryRelated() {
        val media = _state.value.media ?: return
        loadRelated(media)
    }

    private fun loadRelated(media: CurrentMedia) {
        relatedJob?.cancel()
        _state.update {
            it.copy(
                related = RelatedState.Loading,
                moreFromChannel = if (media.channelClaimId != null) RelatedState.Loading else RelatedState.Idle,
            )
        }
        relatedJob = scope.launch {
            runCatching {
                contentRepository.getRelated(
                    channelClaimId = media.channelClaimId,
                    excludeClaimId = media.claimId,
                    query = media.title,
                )
            }
                .onSuccess { claims ->
                    val items = claims.map { it.toRelated() }
                    _state.update { it.copy(related = RelatedState.Success(items)) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            related = RelatedState.Error(
                                error.message ?: error::class.simpleName ?: "Unknown error",
                            ),
                        )
                    }
                }
        }
        media.channelClaimId?.let { channelClaimId ->
            scope.launch {
                runCatching {
                    contentRepository.getChannelVideos(channelClaimId, page = 1, pageSize = 30)
                }
                    .onSuccess { claims ->
                        val items = claims
                            .filter { it.claimId != media.claimId }
                            .map { it.toRelated() }
                        _state.update { it.copy(moreFromChannel = RelatedState.Success(items)) }
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                moreFromChannel = RelatedState.Error(
                                    error.message ?: error::class.simpleName ?: "Unknown error",
                                ),
                            )
                        }
                    }
            }
        }
    }

    fun retryComments() {
        val media = _state.value.media ?: return
        commentsJob?.cancel()
        _state.update { it.copy(comments = CommentsState.Loading) }
        commentsJob = scope.launch {
            loadAndEnrichComments(media.claimId, _state.value.commentSort, media.channelClaimId)
        }
    }

    fun togglePlayPause() {
        exoPlayer.playWhenReady = !exoPlayer.playWhenReady
    }

    fun toggleWatchLater() {
        val media = _state.value.media ?: return
        scope.launch {
            watchLaterRepository.toggle(media.toCollectionEntry())
        }
    }

    fun toggleFavorite() {
        val media = _state.value.media ?: return
        scope.launch {
            favoritesRepository.toggle(media.toCollectionEntry())
        }
    }

    fun saveToWatchLater(media: CurrentMedia) {
        scope.launch { watchLaterRepository.add(media.toCollectionEntry()) }
    }

    fun saveToFavorites(media: CurrentMedia) {
        scope.launch { favoritesRepository.add(media.toCollectionEntry()) }
    }

    fun toggleChannelSubscription() {
        val media = _state.value.media ?: return
        val channelId = media.channelClaimId ?: return
        scope.launch {
            subscriptionsRepository.toggle(channelId, media.channelName)
        }
    }

    fun like() {
        val media = _state.value.media ?: return
        val current = _state.value.reactions
        val isLiked = current?.myReaction == MyReaction.LIKE
        val newReactions = if (isLiked) {
            current.copy(likes = (current.likes - 1).coerceAtLeast(0), myReaction = MyReaction.NONE)
        } else {
            val oldWasDislike = current?.myReaction == MyReaction.DISLIKE
            Reactions(
                likes = (current?.likes ?: 0) + 1,
                dislikes = if (oldWasDislike) (current?.dislikes ?: 1) - 1 else (current?.dislikes ?: 0),
                myReaction = MyReaction.LIKE,
            )
        }
        _state.update { it.copy(reactions = newReactions) }
        scope.launch { reactionsRepository.like(media.claimId, remove = isLiked) }
    }

    suspend fun sendTip(amountLbc: Double): Result<String> {
        val media = _state.value.media ?: return Result.failure(IllegalStateException("No media"))
        val activeChannelId = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel?.claimId
        return runCatching {
            contentRepository.sendTip(media.claimId, amountLbc, activeChannelId)
        }
    }

    fun postComment(text: String, parentId: String? = null) {
        val media = _state.value.media ?: return
        val active = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel ?: return
        if (text.isBlank()) return
        scope.launch {
            val result = runCatching {
                contentRepository.postComment(
                    claimId = media.claimId,
                    channelId = active.claimId,
                    channelName = active.name,
                    text = text,
                    parentId = parentId,
                )
            }
            result.onSuccess { posted ->
                _state.update { st ->
                    val current = (st.comments as? CommentsState.Success)?.comments ?: emptyList()
                    st.copy(comments = CommentsState.Success(listOf(posted.toUi()) + current))
                }
            }
        }
    }

    suspend fun postHyperchat(text: String, amountLbc: Double): Result<Unit> {
        val media = _state.value.media ?: return Result.failure(IllegalStateException("No media"))
        val active = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel
            ?: return Result.failure(IllegalStateException("Not signed in"))
        if (amountLbc <= 0.0) return Result.failure(IllegalArgumentException("Amount must be > 0"))
        return runCatching {
            val txid = contentRepository.sendTip(media.claimId, amountLbc, active.claimId)
            val posted = contentRepository.postComment(
                claimId = media.claimId,
                channelId = active.claimId,
                channelName = active.name,
                text = text,
                parentId = null,
                supportTxId = txid,
            )
            _state.update { st ->
                val current = (st.comments as? CommentsState.Success)?.comments ?: emptyList()
                val ui = posted.toUi().copy(supportAmount = amountLbc)
                st.copy(comments = CommentsState.Success(listOf(ui) + current))
            }
        }
    }

    fun togglePinComment(commentId: String, currentlyPinned: Boolean) {
        val active = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel ?: return
        scope.launch {
            runCatching {
                contentRepository.pinComment(
                    commentId = commentId,
                    channelId = active.claimId,
                    channelName = active.name,
                    remove = currentlyPinned,
                )
            }.onSuccess {
                _state.update { st ->
                    val current = (st.comments as? CommentsState.Success)?.comments ?: return@update st
                    val updated = current.map { c ->
                        if (c.id == commentId) c.copy(isPinned = !currentlyPinned) else c
                    }
                    st.copy(comments = CommentsState.Success(updated))
                }
            }
        }
    }

    fun editCommentText(commentId: String, newText: String) {
        val active = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel ?: return
        if (newText.isBlank()) return
        scope.launch {
            runCatching {
                contentRepository.editComment(
                    commentId = commentId,
                    text = newText,
                    channelId = active.claimId,
                    channelName = active.name,
                )
            }.onSuccess {
                _state.update { st ->
                    val current = (st.comments as? CommentsState.Success)?.comments ?: return@update st
                    val updated = current.map { c ->
                        if (c.id == commentId) c.copy(body = newText) else c
                    }
                    st.copy(comments = CommentsState.Success(updated))
                }
            }
        }
    }

    fun deleteCommentById(commentId: String) {
        val active = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel ?: return
        scope.launch {
            runCatching {
                contentRepository.deleteComment(
                    commentId = commentId,
                    channelId = active.claimId,
                    channelName = active.name,
                )
            }.onSuccess {
                _state.update { st ->
                    val current = (st.comments as? CommentsState.Success)?.comments ?: return@update st
                    val updated = current.filterNot { it.id == commentId }
                    st.copy(comments = CommentsState.Success(updated))
                }
            }
        }
    }

    fun blockCommenter(commentId: String, scope: BlockScope) {
        val active = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel ?: return
        val media = _state.value.media ?: return
        val comment = (_state.value.comments as? CommentsState.Success)?.comments?.firstOrNull { it.id == commentId }
            ?: return
        val blockedId = comment.authorChannelId ?: return
        val blockedName = comment.author
        this.scope.launch {
            runCatching {
                contentRepository.blockCommenter(
                    modChannelId = active.claimId,
                    modChannelName = active.name,
                    blockedChannelId = blockedId,
                    blockedChannelName = blockedName,
                    creatorChannelId = if (scope == BlockScope.Creator) media.channelClaimId else null,
                    creatorChannelName = if (scope == BlockScope.Creator) media.channelName else null,
                    offendingCommentId = commentId,
                    blockAll = scope == BlockScope.Admin,
                    timeoutSec = null,
                )
            }.onSuccess {
                _state.update { st ->
                    val current = (st.comments as? CommentsState.Success)?.comments ?: return@update st
                    st.copy(comments = CommentsState.Success(current.filterNot { it.authorChannelId == blockedId }))
                }
            }
        }
    }

    fun addCommentModerator(commentId: String) {
        val active = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel ?: return
        val comment = (_state.value.comments as? CommentsState.Success)?.comments?.firstOrNull { it.id == commentId }
            ?: return
        val modId = comment.authorChannelId ?: return
        scope.launch {
            runCatching {
                contentRepository.addCommentModerator(
                    modChannelId = modId,
                    modChannelName = comment.author,
                    creatorChannelId = active.claimId,
                    creatorChannelName = active.name,
                )
            }
        }
    }

    fun removeCommentModerator(commentId: String) {
        val active = (authRepository.state.value as? AuthState.SignedIn)?.activeChannel ?: return
        val comment = (_state.value.comments as? CommentsState.Success)?.comments?.firstOrNull { it.id == commentId }
            ?: return
        val modId = comment.authorChannelId ?: return
        scope.launch {
            runCatching {
                contentRepository.removeCommentModerator(
                    modChannelId = modId,
                    modChannelName = comment.author,
                    creatorChannelId = active.claimId,
                    creatorChannelName = active.name,
                )
            }
        }
    }

    fun dislike() {
        val media = _state.value.media ?: return
        val current = _state.value.reactions
        val isDisliked = current?.myReaction == MyReaction.DISLIKE
        val newReactions = if (isDisliked) {
            current.copy(dislikes = (current.dislikes - 1).coerceAtLeast(0), myReaction = MyReaction.NONE)
        } else {
            val oldWasLike = current?.myReaction == MyReaction.LIKE
            Reactions(
                likes = if (oldWasLike) (current?.likes ?: 1) - 1 else (current?.likes ?: 0),
                dislikes = (current?.dislikes ?: 0) + 1,
                myReaction = MyReaction.DISLIKE,
            )
        }
        _state.update { it.copy(reactions = newReactions) }
        scope.launch { reactionsRepository.dislike(media.claimId, remove = isDisliked) }
    }

    fun playPlaylistItem(index: Int) {
        val ctx = _state.value.playlist ?: return
        val item = ctx.items.getOrNull(index) ?: return
        val initial = (item.channelName.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar()
        play(
            media = CurrentMedia(
                claimId = item.claimId,
                permanentUrl = item.permanentUrl,
                title = item.title,
                description = item.description,
                channelClaimId = item.channelClaimId,
                channelName = item.channelName,
                channelInitial = initial,
                channelAvatarUrl = item.channelAvatarUrl,
                thumbnailUrl = item.thumbnailUrl,
                ageLabel = null,
            ),
            playlist = ctx.copy(currentIndex = index),
        )
        // If a cast session is active, push the whole playlist to the device so
        // it can auto-advance natively instead of round-tripping through the app
        // for every transition.
        if (castController.isSessionActive.value) {
            scope.launch {
                val resolved = ctx.items.map { i ->
                    val url = runCatching { contentRepository.resolveStreamUrl(i.permanentUrl) }.getOrNull()
                    if (url.isNullOrBlank()) null
                    else com.odysee.app.core.data.cast.CastQueueItem(
                        streamUrl = url,
                        title = i.title,
                        channelName = i.channelName,
                        thumbnailUrl = i.thumbnailUrl,
                    )
                }
                val valid = resolved.filterNotNull()
                if (valid.isNotEmpty()) {
                    // Map the start index from the original list into the filtered list.
                    val targetClaimId = item.claimId
                    val startIdx = valid.indexOfFirst { it.title == item.title }
                        .takeIf { it >= 0 } ?: 0
                    castController.queueLoad(valid, startIdx)
                    // Avoid the per-item loadMedia duplicate from the URL resolve path.
                    _state.update { it.copy(streamingUrl = null) }
                }
            }
        }
    }

    private fun advanceToNextPlaylistItem() {
        val ctx = _state.value.playlist ?: return
        val next = ctx.currentIndex + 1
        if (next >= 0 && next < ctx.items.size) {
            playPlaylistItem(next)
        }
    }

    /**
     * Triggered by the player UI when the user accepts the LBC paywall price.
     * On success the claim is re-resolved (purchase_receipt populated) and the
     * gate falls through, kicking off the stream URL resolve path.
     */
    fun purchaseWithLbc() {
        val media = _state.value.media ?: return
        if (_state.value.purchaseStatus is PurchaseStatus.Processing) return
        _state.update { it.copy(purchaseStatus = PurchaseStatus.Processing) }
        scope.launch {
            runCatching { contentRepository.purchaseClaimWithLbc(media.claimId) }
                .onSuccess {
                    _state.update { it.copy(purchaseStatus = PurchaseStatus.Idle, isPurchased = true) }
                    // Re-invoke play() to refresh the claim + resolve the stream now
                    // that the receipt should be visible.
                    play(media, openMode = PlayerOpenMode.Expanded, playlist = _state.value.playlist)
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(purchaseStatus = PurchaseStatus.Failed(err.message ?: "Purchase failed"))
                    }
                }
        }
    }

    /**
     * Called by the player UI after the user completes a fiat (Stripe/Arweave)
     * purchase OR membership join via a WebView pointed at odysee.com. Refreshes
     * the claim — if the receipt is now present, OR if the user is now an active
     * member of the channel, gate falls through.
     */
    fun onFiatPurchaseCompleted() {
        val media = _state.value.media ?: return
        scope.launch {
            val refreshed = runCatching {
                contentRepository.getClaimsByIds(listOf(media.claimId)).firstOrNull()
            }.getOrNull()
            val isPurchased = refreshed?.isPurchased == true ||
                runCatching { contentRepository.hasFiatPurchase(media.claimId) }.getOrNull() == true
            val isMember = media.channelClaimId?.let { cid ->
                runCatching { contentRepository.listMyMembershipSubscriptions() }
                    .getOrNull()
                    ?.any { it.creatorChannelId == cid && it.status == "active" }
            } ?: false
            if (isPurchased || isMember) {
                _state.update {
                    it.copy(
                        isPurchased = isPurchased || it.isPurchased,
                        isMemberOfChannel = isMember || it.isMemberOfChannel,
                        purchaseStatus = PurchaseStatus.Idle,
                    )
                }
                play(media, openMode = PlayerOpenMode.Expanded, playlist = _state.value.playlist)
            } else {
                _state.update {
                    it.copy(
                        purchaseStatus = PurchaseStatus.Failed("Purchase not yet recorded — try again in a moment."),
                    )
                }
            }
        }
    }

    fun clearPurchaseError() {
        _state.update { it.copy(purchaseStatus = PurchaseStatus.Idle) }
    }

    fun close() {
        persistCurrentPosition()
        resolveJob?.cancel()
        commentsJob?.cancel()
        closeChatSocket()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _state.value = PlayerState()
        stopPlaybackService()
    }
}

private fun com.odysee.app.core.model.Claim.toRelated(): RelatedItemUi {
    val channelName = signingChannel?.name ?: "@anonymous"
    val initial = (channelName.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar()
    return RelatedItemUi(
        claimId = claimId,
        permanentUrl = permanentUrl,
        title = title,
        description = description,
        channelClaimId = signingChannel?.claimId,
        channelName = channelName,
        channelTitle = signingChannel?.title,
        channelInitial = initial,
        channelAvatarUrl = signingChannel?.thumbnailUrl,
        thumbnailUrl = thumbnailUrl,
        ageLabel = formatAge(releaseTime ?: 0L),
        durationLabel = formatDuration(durationSeconds),
    )
}

private fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds <= 0) return ""
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs)
    else "%d:%02d".format(minutes, secs)
}

private fun CurrentMedia.toCollectionEntry(): CollectionEntry = CollectionEntry(
    claimId = claimId,
    permanentUrl = permanentUrl,
    title = title,
    channelName = channelName,
    channelClaimId = channelClaimId,
    thumbnailUrl = thumbnailUrl,
    addedAt = System.currentTimeMillis(),
)

private fun Comment.toUi(
    creatorChannelId: String? = null,
    mineChannelId: String? = null,
    pinnedByName: String? = null,
): CommentUiModel = CommentUiModel(
    id = commentId,
    parentId = parentId,
    author = authorName,
    authorChannelId = authorChannelId,
    authorInitial = (authorName.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
    authorAvatarUrl = authorAvatarUrl,
    body = body,
    ageLabel = formatAge(timestamp),
    isPinned = isPinned,
    pinnedByName = if (isPinned) pinnedByName else null,
    isEdited = signingTimestamp?.let { (it - timestamp) > 5L } == true,
    isCreator = isCreator || (creatorChannelId != null && authorChannelId == creatorChannelId),
    isMine = mineChannelId != null && authorChannelId == mineChannelId,
    isModerator = isModerator,
    isGlobalMod = isGlobalMod,
    replyCount = replyCount,
    supportAmount = supportAmount,
)

private fun formatAge(unixSeconds: Long): String {
    if (unixSeconds <= 0) return ""
    val nowSec = System.currentTimeMillis() / 1000
    val deltaSec = (nowSec - unixSeconds).coerceAtLeast(0)
    return when {
        deltaSec < 60 -> "Just now"
        deltaSec < TimeUnit.HOURS.toSeconds(1) -> "${deltaSec / 60}m ago"
        deltaSec < TimeUnit.DAYS.toSeconds(1) -> "${deltaSec / 3600}h ago"
        deltaSec < TimeUnit.DAYS.toSeconds(7) -> "${deltaSec / 86_400}d ago"
        deltaSec < TimeUnit.DAYS.toSeconds(30) -> "${deltaSec / (7 * 86_400)}w ago"
        deltaSec < TimeUnit.DAYS.toSeconds(365) -> "${deltaSec / (30 * 86_400)}mo ago"
        else -> "${deltaSec / (365 * 86_400)}y ago"
    }
}
