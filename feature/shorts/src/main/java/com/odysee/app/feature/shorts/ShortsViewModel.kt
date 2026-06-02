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
            // Default repeat — only loop when autoplay-next is off.
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED && _state.value.autoplayNextShort) {
                        advanceToNext()
                    }
                }
            })
        }

    private val streamUrlByClaim = mutableMapOf<String, String>()

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
                _state.update { it.copy(autoplayNextShort = v) }
            }
        }
    }

    fun toggleAutoplayNextShort() {
        viewModelScope.launch {
            val next = !_state.value.autoplayNextShort
            authPreferences.setAutoplayNextShort(next)
            // When autoplay-next is on we don't want to also loop the current short.
            exoPlayer.repeatMode = if (next) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
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
        // Clear the video surface immediately so the previous short's last frame
        // doesn't get shown over the next short's thumbnail.
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _state.update { it.copy(currentIndex = index) }
        playCurrent()
        // Pre-resolve the next short so swiping to it starts playback instantly.
        _state.value.items.getOrNull(index + 1)?.let { next ->
            if (next.claimId !in streamUrlByClaim) {
                viewModelScope.launch {
                    runCatching { contentRepository.resolveStreamUrl(next.permanentUrl) }
                        .onSuccess { streamUrlByClaim[next.claimId] = it }
                }
            }
        }
        val current = _state.value.items.getOrNull(index)
        if (current != null && current.claimId !in _state.value.reactionsByClaim) {
            fetchReactionsFor(current.claimId)
        }
        if (index >= _state.value.items.size - 3) loadMore()
    }

    private fun playCurrent() {
        val current = _state.value.items.getOrNull(_state.value.currentIndex) ?: return
        val cached = streamUrlByClaim[current.claimId]
        if (cached != null) {
            exoPlayer.setMediaItem(MediaItem.fromUri(cached))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            return
        }
        viewModelScope.launch {
            runCatching { contentRepository.resolveStreamUrl(current.permanentUrl) }
                .onSuccess { url ->
                    streamUrlByClaim[current.claimId] = url
                    if (_state.value.items.getOrNull(_state.value.currentIndex)?.claimId == current.claimId) {
                        exoPlayer.setMediaItem(MediaItem.fromUri(url))
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                }
        }
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
    )
}
