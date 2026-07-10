package com.odysee.app.feature.shorts

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.reactions.MyReaction
import com.odysee.app.core.data.reactions.Reactions
import com.odysee.app.core.data.reactions.ReactionsRepository
import com.odysee.app.core.data.subscriptions.SubscriptionsRepository
import com.odysee.app.core.datastore.AuthPreferences
import com.odysee.app.core.model.Claim
import com.odysee.app.core.model.Comment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue

data class ShortUi(
    val claimId: String,
    val permanentUrl: String,
    val title: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val releaseTime: Long? = null,
    val channelName: String,
    val channelTitle: String? = null,
    val channelClaimId: String?,
    val channelAvatarUrl: String?,
    val thumbnailUrl: String?,
    val channelInitial: Char,
    val videoAspect: Float? = null,
)

enum class ShortsViewMode { RELATED, CHANNEL }

data class ShortsUiState(
    val items: List<ShortUi> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentIndex: Int = 0,
    val reactionsByClaim: Map<String, Reactions> = emptyMap(),
    val subscribedChannelIds: Set<String> = emptySet(),
    val isSignedIn: Boolean = false,
    val autoplayNextShort: Boolean = true,
    val showInfo: Boolean = false,
    val showComments: Boolean = false,
    val isPlaying: Boolean = true,
    val playRequested: Boolean = true,
    val isBuffering: Boolean = false,
    val renderedClaimId: String? = null,
    val isMuted: Boolean = false,
    val viewMode: ShortsViewMode = ShortsViewMode.RELATED,
)

@HiltViewModel
class ShortsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val reactionsRepository: ReactionsRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val authPreferences: AuthPreferences,
    private val authRepository: com.odysee.app.core.data.auth.AuthRepository,
    private val lbryioApi: com.odysee.app.core.network.LbryioApi,
    private val watchLaterRepository: com.odysee.app.core.data.collections.WatchLaterRepository,
    private val favoritesRepository: com.odysee.app.core.data.collections.FavoritesRepository,
    @javax.inject.Named("media") private val mediaOkHttpClient: okhttp3.OkHttpClient,
    private val mediaCache: androidx.media3.datasource.cache.Cache,
) : ViewModel() {

    val canComment: StateFlow<Boolean> = authRepository.state
        .map { (it as? com.odysee.app.core.data.auth.AuthState.SignedIn)?.activeChannel != null }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)

    val watchLaterIds: StateFlow<Set<String>> = watchLaterRepository.items
        .map { items -> items.map { it.claimId }.toSet() }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptySet())
    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.items
        .map { items -> items.map { it.claimId }.toSet() }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptySet())

    fun toggleWatchLater(item: ShortUi) = viewModelScope.launch {
        val entry = com.odysee.app.core.data.collections.CollectionEntry(
            claimId = item.claimId,
            permanentUrl = item.permanentUrl,
            title = item.title,
            channelName = item.channelName,
            channelClaimId = item.channelClaimId,
            thumbnailUrl = item.thumbnailUrl,
            addedAt = System.currentTimeMillis(),
        )
        if (item.claimId in watchLaterIds.value) watchLaterRepository.remove(item.claimId)
        else watchLaterRepository.add(entry)
    }

    fun toggleFavorite(item: ShortUi) = viewModelScope.launch {
        val entry = com.odysee.app.core.data.collections.CollectionEntry(
            claimId = item.claimId,
            permanentUrl = item.permanentUrl,
            title = item.title,
            channelName = item.channelName,
            channelClaimId = item.channelClaimId,
            thumbnailUrl = item.thumbnailUrl,
            addedAt = System.currentTimeMillis(),
        )
        if (item.claimId in favoriteIds.value) favoritesRepository.remove(item.claimId)
        else favoritesRepository.add(entry)
    }

    fun postComment(claimId: String, text: String, onPosted: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val auth = authRepository.state.value as? com.odysee.app.core.data.auth.AuthState.SignedIn
        val ch = auth?.activeChannel
        if (ch == null) { onPosted(false); return@launch }
        val result = runCatching {
            contentRepository.postComment(
                claimId = claimId,
                channelId = ch.claimId,
                channelName = ch.name,
                text = text,
            )
        }
        onPosted(result.isSuccess)
    }

    private val route: ShortsRoute = savedStateHandle.toRoute()

    private val _state = MutableStateFlow(
        ShortsUiState(
            items = listOf(
                ShortUi(
                    claimId = route.initialClaimId,
                    permanentUrl = route.initialPermanentUrl,
                    title = route.initialTitle,
                    channelName = route.initialChannelName,
                    channelTitle = null,
                    channelClaimId = route.initialChannelClaimId,
                    channelAvatarUrl = route.initialChannelAvatarUrl,
                    thumbnailUrl = route.initialThumbnailUrl,
                    channelInitial = (route.initialChannelName.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
                ),
            ),
        ),
    )

    private fun enrichInitialChannelMetadata() {
        viewModelScope.launch {
            val item = _state.value.items.firstOrNull() ?: return@launch
            val cid = item.channelClaimId ?: return@launch
            val channel = runCatching { contentRepository.getChannel(cid) }.getOrNull() ?: return@launch
            _state.update { st ->
                val updated = st.items.map {
                    if (it.claimId == item.claimId) it.copy(
                        channelTitle = channel.title?.takeIf { t -> t.isNotBlank() } ?: it.channelTitle,
                        channelAvatarUrl = channel.thumbnailUrl ?: it.channelAvatarUrl,
                    ) else it
                }
                st.copy(items = updated)
            }
        }
    }
    val state: StateFlow<ShortsUiState> = _state.asStateFlow()

    private var advanceToNext: () -> Unit = {}

    fun setAdvanceCallback(cb: () -> Unit) { advanceToNext = cb }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                .setDataSourceFactory(
                    androidx.media3.datasource.cache.CacheDataSource.Factory()
                        .setCache(mediaCache)
                        .setUpstreamDataSourceFactory(
                            androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(mediaOkHttpClient)
                                .setUserAgent("Mozilla/5.0 (Linux; Android) Chrome/120.0.0.0 Mobile Safari/537.36")
                                .setDefaultRequestProperties(mapOf("Referer" to "https://odysee.com/")),
                        )
                        .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR),
                )
                .setLoadErrorHandlingPolicy(
                    com.odysee.app.core.data.player.odyseeLoadErrorHandlingPolicy(maxHttpRetries = 1),
                ),
        )
        .build().apply {
            addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
                override fun onLoadError(
                    eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                    loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo,
                    mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData,
                    error: java.io.IOException,
                    wasCanceled: Boolean,
                ) {
                    if (wasCanceled) return
                    val uriString = loadEventInfo.uri.toString()
                    val claimId = streamUrlByClaim.entries.firstOrNull { it.value == uriString }?.key ?: return
                    val currentId = _state.value.items.getOrNull(_state.value.currentIndex)?.claimId
                    if (claimId != currentId) {
                        val count = (loadErrorCounts[claimId] ?: 0) + 1
                        loadErrorCounts[claimId] = count
                        if (count >= 2) removeFailedShort(claimId)
                    }
                }
            })
            playWhenReady = true
            // Keep CPU/network alive while screen dims so shorts don't stutter.
            setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
            // Default repeat — only loop when autoplay-next is off.
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) maybeEnqueueNext()
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    _state.update { it.copy(playRequested = playWhenReady) }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _state.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
                    if (playbackState == Player.STATE_ENDED) {
                        val st = _state.value
                        val current = st.items.getOrNull(st.currentIndex)
                        if (current != null && st.renderedClaimId != current.claimId) {
                            retryCurrent(current)
                        } else if (st.autoplayNextShort) {
                            advanceToNext()
                        }
                    }
                }

                override fun onRenderedFirstFrame() {
                    val id = exoPlayer.currentMediaItem?.mediaId
                    _state.update { st ->
                        st.copy(renderedClaimId = id ?: st.items.getOrNull(st.currentIndex)?.claimId)
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        _state.update { it.copy(renderedClaimId = null) }
                        if (_state.value.autoplayNextShort) {
                            advanceToNext()
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val current = _state.value.items.getOrNull(_state.value.currentIndex)
                    _state.update { it.copy(isPlaying = false, isBuffering = false, renderedClaimId = null) }
                    if (current == null) return
                    retryCurrent(current)
                }
            })
        }

    private val streamUrlByClaim = mutableMapOf<String, String>()
    private val resolving = mutableSetOf<String>()
    private val errorRetries = mutableMapOf<String, Int>()
    private val loadErrorCounts = mutableMapOf<String, Int>()

    init {
        playCurrent()
        loadMore()
        fetchReactionsFor(route.initialClaimId)
        enrichInitialChannelMetadata()
        viewModelScope.launch {
            subscriptionsRepository.subscriptions.collect { subs ->
                _state.update { it.copy(subscribedChannelIds = subs.map { s -> s.claimId }.toSet()) }
            }
        }
        viewModelScope.launch {
            authRepository.state.collect { auth ->
                val signedIn = auth is com.odysee.app.core.data.auth.AuthState.SignedIn
                _state.update { it.copy(isSignedIn = signedIn) }
            }
        }
        viewModelScope.launch {
            authPreferences.autoplayNextShort.collect { v ->
                // When autoplay-next is on we don't want to also loop the current short.
                exoPlayer.repeatMode = if (v) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
                _state.update { it.copy(autoplayNextShort = v) }
            }
        }
    }

    fun toggleAutoplayNextShort() {
        viewModelScope.launch {
            authPreferences.setAutoplayNextShort(!_state.value.autoplayNextShort)
        }
    }

    fun setViewMode(mode: ShortsViewMode) {
        if (_state.value.viewMode == mode) return
        // Keep the currently playing item at index 0 so the user doesn't jump pages.
        val current = _state.value.items.getOrNull(_state.value.currentIndex) ?: return
        _state.update { it.copy(viewMode = mode, items = listOf(current), currentIndex = 0, isLoading = true) }
        nextPage = 1
        loadMore()
    }

    fun setShowInfo(value: Boolean) {
        _state.update { it.copy(showInfo = value) }
    }

    fun setShowComments(value: Boolean) {
        _state.update { it.copy(showComments = value) }
    }

    fun togglePlayPause() {
        if (exoPlayer.mediaItemCount == 0 || exoPlayer.playbackState == Player.STATE_IDLE) {
            _state.value.items.getOrNull(_state.value.currentIndex)?.let { current ->
                errorRetries.remove(current.claimId)
                streamUrlByClaim.remove(current.claimId)
            }
            exoPlayer.playWhenReady = true
            playCurrent()
            return
        }
        val playing = !exoPlayer.playWhenReady
        exoPlayer.playWhenReady = playing
        _state.update { it.copy(isPlaying = playing) }
    }

    fun toggleMute() {
        val nextMuted = !_state.value.isMuted
        exoPlayer.volume = if (nextMuted) 0f else 1f
        _state.update { it.copy(isMuted = nextMuted) }
    }

    fun commentsFor(claimId: String): Flow<List<Comment>> = flow {
        emit(emptyList())
        runCatching { contentRepository.getComments(claimId) }
            .onSuccess { emit(it) }
    }

    fun descriptionFor(claimId: String): Flow<String?> = flow {
        emit(null)
        runCatching { contentRepository.search(claimId, size = 1) }
            .getOrNull()
            ?.firstOrNull { it.claimId == claimId }
            ?.let { emit(it.description) }
    }

    private val followerCache = mutableMapOf<String, Long>()
    suspend fun fetchFollowerCount(channelClaimId: String): Long? {
        followerCache[channelClaimId]?.let { return it }
        val v = runCatching { lbryioApi.subCount(channelClaimId) }.getOrNull()
            ?.data?.firstOrNull()
        if (v != null) followerCache[channelClaimId] = v
        return v
    }

    private fun fetchReactionsFor(claimId: String) {
        viewModelScope.launch {
            val r = reactionsRepository.fetch(claimId) ?: return@launch
            _state.update { it.copy(reactionsByClaim = it.reactionsByClaim + (claimId to r)) }
        }
    }

    fun toggleLike(claimId: String) {
        val current = _state.value.reactionsByClaim[claimId]
        val isLiked = current?.myReaction == MyReaction.LIKE
        val newReactions = if (isLiked) {
            current.copy(likes = (current.likes - 1).coerceAtLeast(0), myReaction = MyReaction.NONE)
        } else {
            val wasDislike = current?.myReaction == MyReaction.DISLIKE
            Reactions(
                likes = (current?.likes ?: 0) + 1,
                dislikes = if (wasDislike) (current?.dislikes ?: 1) - 1 else (current?.dislikes ?: 0),
                myReaction = MyReaction.LIKE,
            )
        }
        _state.update { it.copy(reactionsByClaim = it.reactionsByClaim + (claimId to newReactions)) }
        viewModelScope.launch { reactionsRepository.like(claimId, remove = isLiked) }
    }

    fun toggleDislike(claimId: String) {
        val current = _state.value.reactionsByClaim[claimId]
        val isDisliked = current?.myReaction == MyReaction.DISLIKE
        val newReactions = if (isDisliked) {
            current.copy(dislikes = (current.dislikes - 1).coerceAtLeast(0), myReaction = MyReaction.NONE)
        } else {
            val wasLike = current?.myReaction == MyReaction.LIKE
            Reactions(
                likes = if (wasLike) (current?.likes ?: 1) - 1 else (current?.likes ?: 0),
                dislikes = (current?.dislikes ?: 0) + 1,
                myReaction = MyReaction.DISLIKE,
            )
        }
        _state.update { it.copy(reactionsByClaim = it.reactionsByClaim + (claimId to newReactions)) }
        viewModelScope.launch { reactionsRepository.dislike(claimId, remove = isDisliked) }
    }

    fun toggleSubscribe(claimId: String, name: String) {
        if (!_state.value.isSignedIn) return
        viewModelScope.launch { subscriptionsRepository.toggle(claimId, name) }
    }

    fun onPageChanged(index: Int) {
        if (index !in _state.value.items.indices) return
        currentRetryJob?.cancel()
        val claimId = _state.value.items[index].claimId
        val playingThis = exoPlayer.currentMediaItem?.mediaId == claimId &&
            exoPlayer.playbackState != Player.STATE_IDLE
        val queuedNext = exoPlayer.mediaItemCount > 1 &&
            exoPlayer.currentMediaItemIndex == 0 &&
            exoPlayer.getMediaItemAt(1).mediaId == claimId
        val resolvingThis = index == _state.value.currentIndex && claimId in resolving
        when {
            playingThis -> {
                while (exoPlayer.currentMediaItemIndex > 0) exoPlayer.removeMediaItem(0)
                _state.update { it.copy(currentIndex = index) }
            }
            queuedNext -> {
                exoPlayer.seekToNextMediaItem()
                exoPlayer.removeMediaItem(0)
                _state.update { it.copy(currentIndex = index, renderedClaimId = null) }
            }
            resolvingThis -> Unit
            else -> {
                // Clear the video surface immediately so the previous short's last frame
                // doesn't get shown over the next short's thumbnail.
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                _state.update { it.copy(currentIndex = index, renderedClaimId = null, isBuffering = true) }
                playCurrent()
            }
        }
        // Pre-resolve the next short so swiping to it starts playback instantly.
        _state.value.items.getOrNull(index + 1)?.let { next ->
            if (next.claimId !in streamUrlByClaim) resolveAndMaybePlay(next)
        }
        maybeEnqueueNext()
        val current = _state.value.items.getOrNull(index)
        if (current != null && current.claimId !in _state.value.reactionsByClaim) {
            fetchReactionsFor(current.claimId)
        }
        if (index >= _state.value.items.size - 3) loadMore()
    }

    private fun playCurrent() {
        val current = _state.value.items.getOrNull(_state.value.currentIndex) ?: return
        val cached = streamUrlByClaim[current.claimId]
        if (cached != null) startPlayback(cached, current.claimId) else resolveAndMaybePlay(current)
    }

    private fun maybeEnqueueNext() {
        if (!exoPlayer.isPlaying) return
        val st = _state.value
        val next = st.items.getOrNull(st.currentIndex + 1) ?: return
        val url = streamUrlByClaim[next.claimId] ?: return
        if (exoPlayer.mediaItemCount != 1) return
        if (exoPlayer.currentMediaItem?.mediaId == next.claimId) return
        exoPlayer.addMediaItem(
            MediaItem.Builder().setUri(url).setMediaId(next.claimId).build(),
        )
    }

    private fun retryCurrent(current: ShortUi) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        val attempts = errorRetries[current.claimId] ?: 0
        if (attempts < 1) {
            errorRetries[current.claimId] = attempts + 1
            streamUrlByClaim.remove(current.claimId)
            resolveAndMaybePlay(current)
        } else {
            _state.update { it.copy(isBuffering = true) }
            scheduleCurrentRetry(current.claimId)
        }
    }

    private var currentRetryJob: kotlinx.coroutines.Job? = null

    private fun scheduleCurrentRetry(claimId: String) {
        if (currentRetryJob?.isActive == true) return
        currentRetryJob = viewModelScope.launch {
            var delayMs = 3_000L
            while (_state.value.items.getOrNull(_state.value.currentIndex)?.claimId == claimId) {
                kotlinx.coroutines.delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(15_000L)
                val current = _state.value.items.getOrNull(_state.value.currentIndex) ?: break
                if (current.claimId != claimId) break
                if (exoPlayer.mediaItemCount > 0 && exoPlayer.playbackState != Player.STATE_IDLE) break
                streamUrlByClaim.remove(claimId)
                resolveAndMaybePlay(current)
            }
        }
    }

    private fun removeFailedShort(claimId: String) {
        val idx = _state.value.items.indexOfFirst { it.claimId == claimId }
        when {
            idx < 0 || idx <= _state.value.currentIndex -> return
            else -> _state.update { st ->
                val i = st.items.indexOfFirst { it.claimId == claimId }
                if (i > st.currentIndex) st.copy(items = st.items.filterIndexed { j, _ -> j != i }) else st
            }
        }
        if (exoPlayer.mediaItemCount > 1 && exoPlayer.getMediaItemAt(1).mediaId == claimId) {
            exoPlayer.removeMediaItem(1)
        }
        _state.value.items.getOrNull(_state.value.currentIndex + 1)?.let { next ->
            if (next.claimId !in streamUrlByClaim) resolveAndMaybePlay(next)
        }
        maybeEnqueueNext()
        if (_state.value.currentIndex >= _state.value.items.size - 3) loadMore()
    }

    private fun startPlayback(url: String, claimId: String) {
        exoPlayer.setMediaItem(MediaItem.Builder().setUri(url).setMediaId(claimId).build())
        exoPlayer.prepare()
        maybeEnqueueNext()
    }

    private fun resolveAndMaybePlay(item: ShortUi) {
        if (item.claimId in resolving) {
            return
        }
        resolving += item.claimId
        viewModelScope.launch {
            val url = resolveWithRetry(item.permanentUrl)
            resolving -= item.claimId
            val isCurrent = _state.value.items.getOrNull(_state.value.currentIndex)?.claimId == item.claimId
            if (url != null) {
                streamUrlByClaim[item.claimId] = url
                if (isCurrent && exoPlayer.mediaItemCount == 0) {
                    startPlayback(url, item.claimId)
                } else {
                    maybeEnqueueNext()
                }
            } else if (isCurrent) {
                _state.update { it.copy(isBuffering = true) }
                scheduleCurrentRetry(item.claimId)
            } else {
                removeFailedShort(item.claimId)
            }
        }
    }

    private suspend fun resolveWithRetry(permanentUrl: String): String? {
        repeat(2) { attempt ->
            runCatching { contentRepository.resolveStreamUrl(permanentUrl) }
                .onSuccess { return it }
            if (attempt == 0) kotlinx.coroutines.delay(500)
        }
        return null
    }

    private var loadingMore = false
    private var nextPage = 1

    private fun loadMore() {
        if (loadingMore) return
        loadingMore = true
        viewModelScope.launch {
            val mode = _state.value.viewMode
            val channelId = _state.value.items.firstOrNull()?.channelClaimId
            val resp = runCatching {
                when (mode) {
                    ShortsViewMode.RELATED -> contentRepository.getShortsFeed(page = nextPage)
                    ShortsViewMode.CHANNEL -> {
                        if (channelId == null) emptyList()
                        else contentRepository.getChannelVideos(channelId, page = nextPage, pageSize = 30)
                            .filter { it.isShort }
                    }
                }
            }
            resp.onSuccess { claims ->
                val existing = _state.value.items.map { it.claimId }.toSet()
                val newOnes = claims.filter { it.claimId !in existing }.map { it.toShortUi() }
                if (newOnes.isNotEmpty()) {
                    _state.update { it.copy(items = it.items + newOnes, isLoading = false) }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
                nextPage++
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
            loadingMore = false
        }
    }

    fun pauseExternal() {
        exoPlayer.playWhenReady = false
    }

    fun resumeExternal() {
        exoPlayer.playWhenReady = true
    }

    override fun onCleared() {
        exoPlayer.release()
        super.onCleared()
    }
}

private fun Claim.toShortUi(): ShortUi {
    val channelName = signingChannel?.name ?: "@anonymous"
    return ShortUi(
        claimId = claimId,
        permanentUrl = permanentUrl,
        title = title,
        description = description,
        releaseTime = releaseTime,
        channelName = channelName,
        channelTitle = signingChannel?.title?.takeIf { it.isNotBlank() },
        channelClaimId = signingChannel?.claimId,
        channelAvatarUrl = signingChannel?.thumbnailUrl,
        thumbnailUrl = thumbnailUrl,
        channelInitial = (channelName.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
        videoAspect = videoWidth?.let { w ->
            videoHeight?.let { h -> if (w > 0 && h > 0) w.toFloat() / h.toFloat() else null }
        },
    )
}
