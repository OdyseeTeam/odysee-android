package com.odysee.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.auth.AuthRepository
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.data.collections.CollectionEntry
import com.odysee.app.core.data.collections.FavoritesRepository
import com.odysee.app.core.data.collections.WatchLaterRepository
import com.odysee.app.core.data.moderation.BlockedChannelsRepository
import com.odysee.app.core.data.notifications.NotificationsRepository
import com.odysee.app.core.data.subscriptions.Subscription
import com.odysee.app.core.data.subscriptions.SubscriptionsRepository
import com.odysee.app.core.model.Claim
import com.odysee.app.core.model.HomepageCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.absoluteValue

private val SUPPORTED_HOMEPAGE_LANGS = setOf(
    "en", "fr", "es", "de", "it", "hi", "zh", "ru", "pt-BR",
)

private fun resolveHomepageLang(locale: Locale = Locale.getDefault()): String {
    val tag = locale.toLanguageTag()
    if (tag in SUPPORTED_HOMEPAGE_LANGS) return tag
    val primary = locale.language
    if (primary == "pt") return "pt-BR"
    return if (primary in SUPPORTED_HOMEPAGE_LANGS) primary else "en"
}

data class VideoUiModel(
    val id: String,
    val permanentUrl: String,
    val title: String,
    val description: String?,
    val channelClaimId: String?,
    val channelName: String,
    val channelTitle: String? = null,
    val channelInitial: Char,
    val channelAvatarUrl: String?,
    val channelAvatarTintIndex: Int,
    val thumbnailUrl: String?,
    val thumbnailTintIndex: Int,
    val ageLabel: String,
    val durationLabel: String,
    val viewCount: Long? = null,
    val isShort: Boolean = false,
    val isLivestream: Boolean = false,
    val isUpcoming: Boolean = false,
    val liveStreamUrl: String? = null,
    val paywall: com.odysee.app.core.model.Paywall = com.odysee.app.core.model.Paywall.Free,
    val isPurchased: Boolean = false,
    val isMembersOnly: Boolean = false,
)

data class CategoryChip(
    val id: String,
    val label: String,
    val iconName: String?,
)

sealed interface FeedState {
    data object Loading : FeedState
    data class Success(val videos: List<VideoUiModel>) : FeedState
    data class Error(val message: String) : FeedState
}

data class HomeUiState(
    val isLoadingHomepage: Boolean = true,
    val homepageError: String? = null,
    val baseCategories: List<CategoryChip> = emptyList(),
    val selectedCategoryId: String? = null,
    val feed: FeedState = FeedState.Loading,
    val followingFeed: FeedState = FeedState.Success(emptyList()),
    val subscriptions: List<Subscription> = emptyList(),
    val isSignedIn: Boolean = false,
    val unseenNotifications: Int = 0,
    val selectedContentTypes: Set<String> = setOf("videos", "shorts", "live", "upcoming"),
    val livestreamFeed: List<VideoUiModel> = emptyList(),
    val upcomingFeed: List<VideoUiModel> = emptyList(),
    val isLivestreamFetching: Boolean = false,
) {
    val categories: List<CategoryChip>
        get() = if (isSignedIn || subscriptions.isNotEmpty()) {
            listOf(CategoryChip(id = HomeViewModel.FOLLOWING_ID, label = "Following", iconName = null)) + baseCategories
        } else {
            baseCategories
        }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val watchLaterRepository: WatchLaterRepository,
    private val favoritesRepository: FavoritesRepository,
    private val blockedChannelsRepository: BlockedChannelsRepository,
    private val notificationsRepository: NotificationsRepository,
    private val authPreferences: com.odysee.app.core.datastore.AuthPreferences,
    authRepository: AuthRepository,
) : ViewModel() {

    private var blockedIds: Set<String> = emptySet()

    fun saveToWatchLater(video: VideoUiModel) {
        viewModelScope.launch { watchLaterRepository.add(video.toCollectionEntry()) }
    }

    fun saveToFavorites(video: VideoUiModel) {
        viewModelScope.launch { favoritesRepository.add(video.toCollectionEntry()) }
    }

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val categories = mutableMapOf<String, HomepageCategory>()
    private val cache = mutableMapOf<String, List<VideoUiModel>>()
    private var currentJob: Job? = null
    private var followingJob: Job? = null
    private var lastFollowingIds: List<String>? = null
    private var userPickedCategory = false
    @Volatile private var homepageLanguage: String? = null

    init {
        loadHomepage()
        loadLivestreamFeeds()
        viewModelScope.launch {
            authPreferences.homeContentTypes.collect { stored ->
                if (stored != _state.value.selectedContentTypes) {
                    _state.update { it.copy(selectedContentTypes = stored) }
                }
            }
        }
        viewModelScope.launch {
            authPreferences.homepageLanguage.collect { lang ->
                val cleaned = lang?.takeIf { it.isNotBlank() }
                if (cleaned != homepageLanguage) {
                    homepageLanguage = cleaned
                    cache.clear()
                    pagesLoaded.clear()
                    exhausted.clear()
                    _state.value.selectedCategoryId?.let { loadCategory(it) }
                }
            }
        }
        viewModelScope.launch {
            blockedChannelsRepository.blocked.collect { list ->
                val newIds = list.map { it.claimId }.toSet()
                if (newIds != blockedIds) {
                    blockedIds = newIds
                    // Re-apply filter to current feeds
                    val s = _state.value
                    val newFeed = (s.feed as? FeedState.Success)
                        ?.let { FeedState.Success(it.videos.filter { v -> v.channelClaimId !in blockedIds }) }
                        ?: s.feed
                    val newFollowing = (s.followingFeed as? FeedState.Success)
                        ?.let { FeedState.Success(it.videos.filter { v -> v.channelClaimId !in blockedIds }) }
                        ?: s.followingFeed
                    _state.update { it.copy(feed = newFeed, followingFeed = newFollowing) }
                }
            }
        }
        viewModelScope.launch {
            authRepository.state.collect { auth ->
                val signedIn = auth is AuthState.SignedIn
                val before = _state.value
                _state.update { it.copy(isSignedIn = signedIn) }
                if (signedIn) {
                    runCatching { notificationsRepository.refreshUnseen() }
                }
                if (signedIn && !userPickedCategory && before.selectedCategoryId != FOLLOWING_ID) {
                    _state.update { it.copy(selectedCategoryId = FOLLOWING_ID) }
                }
            }
        }
        viewModelScope.launch {
            notificationsRepository.unseenCount.collect { c ->
                _state.update { it.copy(unseenNotifications = c) }
            }
        }
        viewModelScope.launch {
            subscriptionsRepository.subscriptions.collect { subs ->
                val before = _state.value
                _state.update { it.copy(subscriptions = subs) }
                val ids = subs.map { it.claimId }.sorted()
                if (ids != lastFollowingIds) {
                    lastFollowingIds = ids
                    loadFollowing(ids)
                }
                if (subs.isNotEmpty() && !userPickedCategory && before.selectedCategoryId != FOLLOWING_ID) {
                    _state.update { it.copy(selectedCategoryId = FOLLOWING_ID) }
                }
                applyLivestreamFilter()
            }
        }
    }

    companion object {
        const val FOLLOWING_ID = "__FOLLOWING__"
    }

    private var livestreamJob: Job? = null
    private var livestreamLoaded = false
    private var allLivestreams: List<VideoUiModel> = emptyList()
    private var allUpcoming: List<VideoUiModel> = emptyList()

    fun setContentTypes(types: Set<String>) {
        _state.update { it.copy(selectedContentTypes = types) }
        viewModelScope.launch { authPreferences.setHomeContentTypes(types) }
        if (("live" in types || "upcoming" in types) && !livestreamLoaded) {
            loadLivestreamFeeds()
        }
    }

    private fun loadLivestreamFeeds() {
        if (livestreamJob?.isActive == true) return
        _state.update { it.copy(isLivestreamFetching = true) }
        livestreamJob = viewModelScope.launch {
            val urls = runCatching { contentRepository.getLivestreamUrls() }
                .getOrNull().orEmpty()
            val live = runCatching { contentRepository.getLivestreams() }
                .getOrNull().orEmpty().map { it.toUi().copy(liveStreamUrl = urls[it.claimId]) }
            val upcoming = runCatching { contentRepository.getUpcoming() }
                .getOrNull().orEmpty().map { it.toUi() }
            livestreamLoaded = true
            allLivestreams = live
            allUpcoming = upcoming
            _state.update { it.copy(isLivestreamFetching = false) }
            applyLivestreamFilter()
        }
    }

    private fun applyLivestreamFilter() {
        val s = _state.value
        val channelIds: Set<String>? = when (val id = s.selectedCategoryId) {
            null -> null
            FOLLOWING_ID -> s.subscriptions.map { it.claimId }.toSet()
            else -> categories[id]?.channelIds?.takeIf { it.isNotEmpty() }?.toSet()
        }
        val live = if (channelIds == null) allLivestreams
            else allLivestreams.filter { it.channelClaimId in channelIds }
        val upcoming = if (channelIds == null) allUpcoming
            else allUpcoming.filter { it.channelClaimId in channelIds }
        _state.update { it.copy(livestreamFeed = live, upcomingFeed = upcoming) }
    }

    fun retryFollowing() {
        loadFollowing(lastFollowingIds ?: emptyList())
    }

    private fun loadFollowing(ids: List<String>) {
        followingPage = 1
        followingExhausted = false
        followingJob?.cancel()
        if (ids.isEmpty()) {
            _state.update { it.copy(followingFeed = FeedState.Success(emptyList())) }
            return
        }
        _state.update { it.copy(followingFeed = FeedState.Loading) }
        followingJob = viewModelScope.launch {
            runCatching { contentRepository.getFollowingFeed(ids) }
                .onSuccess { claims ->
                    val videos = claims.map { it.toUi() }.filter { it.channelClaimId !in blockedIds }
                    _state.update { it.copy(followingFeed = FeedState.Success(videos)) }
                    enrichViewCounts(videos) { updated ->
                        val current = _state.value.followingFeed
                        if (current is FeedState.Success) {
                            _state.update { it.copy(followingFeed = FeedState.Success(updated)) }
                        }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            followingFeed = FeedState.Error(
                                error.message ?: error::class.simpleName ?: "Unknown error",
                            ),
                        )
                    }
                }
        }
    }

    private fun enrichViewCounts(videos: List<VideoUiModel>, apply: (List<VideoUiModel>) -> Unit) {
        if (videos.isEmpty()) return
        viewModelScope.launch {
            runCatching { contentRepository.getViewCounts(videos.map { it.id }) }
                .onSuccess { counts ->
                    if (counts.isNotEmpty()) {
                        apply(videos.map { v -> v.copy(viewCount = counts[v.id] ?: v.viewCount) })
                    }
                }
        }
    }

    fun loadHomepage() {
        viewModelScope.launch {
            val lang = resolveHomepageLang()
            _state.update { it.copy(isLoadingHomepage = true, homepageError = null) }
            runCatching { contentRepository.getHomepage(lang) }
                .onSuccess { homepage ->
                    categories.clear()
                    homepage.categories.forEach { categories[it.id] = it }
                    val baseChips = homepage.categories.map {
                        CategoryChip(id = it.id, label = it.label, iconName = it.icon)
                    }
                    val showFollowing = _state.value.isSignedIn || _state.value.subscriptions.isNotEmpty()
                    val initialId = when {
                        userPickedCategory -> _state.value.selectedCategoryId
                        showFollowing -> FOLLOWING_ID
                        else -> baseChips.firstOrNull()?.id
                    }
                    _state.update {
                        it.copy(
                            isLoadingHomepage = false,
                            baseCategories = baseChips,
                            selectedCategoryId = initialId,
                        )
                    }
                    if (initialId != null && initialId != FOLLOWING_ID) {
                        loadCategory(initialId)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingHomepage = false,
                            homepageError = error.message ?: error::class.simpleName ?: "Unknown error",
                        )
                    }
                }
        }
    }

    fun selectCategory(categoryId: String) {
        userPickedCategory = true
        val current = _state.value.selectedCategoryId
        if (categoryId == FOLLOWING_ID) {
            _state.update { it.copy(selectedCategoryId = categoryId) }
            applyLivestreamFilter()
            return
        }
        if (current == categoryId) return
        val cached = cache[categoryId]
        _state.update {
            it.copy(
                selectedCategoryId = categoryId,
                feed = if (cached != null) FeedState.Success(cached) else FeedState.Loading,
            )
        }
        applyLivestreamFilter()
        if (cached == null) loadCategory(categoryId)
    }

    fun retry() {
        val id = _state.value.selectedCategoryId ?: return
        if (id == FOLLOWING_ID) {
            retryFollowing()
            return
        }
        cache.remove(id)
        _state.update { it.copy(feed = FeedState.Loading) }
        loadCategory(id)
    }

    private val pagesLoaded = mutableMapOf<String, Int>()
    private val loadingMore = mutableSetOf<String>()
    private val exhausted = mutableSetOf<String>()
    private var followingPage = 1
    private var followingLoadingMore = false
    private var followingExhausted = false

    private fun loadCategory(categoryId: String) {
        val category = categories[categoryId] ?: return
        currentJob?.cancel()
        pagesLoaded[categoryId] = 1
        exhausted.remove(categoryId)
        currentJob = viewModelScope.launch {
            runCatching { contentRepository.getCategoryFeed(category, page = 1, homepageLanguage = homepageLanguage) }
                .onSuccess { claims ->
                    val videos = claims.map { it.toUi() }.filter { it.channelClaimId !in blockedIds }
                    cache[categoryId] = videos
                    if (claims.size < category.pageSize) exhausted.add(categoryId)
                    if (_state.value.selectedCategoryId == categoryId) {
                        _state.update { it.copy(feed = FeedState.Success(videos)) }
                    }
                    enrichViewCounts(videos) { updated ->
                        cache[categoryId] = updated
                        if (_state.value.selectedCategoryId == categoryId) {
                            _state.update { it.copy(feed = FeedState.Success(updated)) }
                        }
                    }
                }
                .onFailure { error ->
                    if (_state.value.selectedCategoryId == categoryId) {
                        _state.update {
                            it.copy(
                                feed = FeedState.Error(
                                    error.message ?: error::class.simpleName ?: "Unknown error",
                                ),
                            )
                        }
                    }
                }
        }
    }

    fun loadMoreCurrent() {
        val id = _state.value.selectedCategoryId ?: return
        if (id == FOLLOWING_ID) {
            loadMoreFollowing()
            return
        }
        if (id in loadingMore || id in exhausted) return
        val category = categories[id] ?: return
        val nextPage = (pagesLoaded[id] ?: 1) + 1
        loadingMore.add(id)
        viewModelScope.launch {
            runCatching { contentRepository.getCategoryFeed(category, page = nextPage, homepageLanguage = homepageLanguage) }
                .onSuccess { claims ->
                    if (claims.size < category.pageSize) exhausted.add(id)
                    if (claims.isNotEmpty()) {
                        pagesLoaded[id] = nextPage
                        val newVideos = claims.map { it.toUi() }.filter { v -> v.channelClaimId !in blockedIds }
                        val existing = cache[id].orEmpty()
                        val seen = existing.map { it.id }.toHashSet()
                        val merged = existing + newVideos.filter { it.id !in seen }
                        cache[id] = merged
                        if (_state.value.selectedCategoryId == id) {
                            _state.update { it.copy(feed = FeedState.Success(merged)) }
                        }
                        enrichViewCounts(newVideos) { updatedBatch ->
                            val byId = updatedBatch.associateBy { it.id }
                            val finalMerged = merged.map { byId[it.id] ?: it }
                            cache[id] = finalMerged
                            if (_state.value.selectedCategoryId == id) {
                                _state.update { it.copy(feed = FeedState.Success(finalMerged)) }
                            }
                        }
                    }
                }
            loadingMore.remove(id)
        }
    }

    private fun loadMoreFollowing() {
        if (followingLoadingMore || followingExhausted) return
        val ids = lastFollowingIds ?: return
        if (ids.isEmpty()) return
        followingLoadingMore = true
        viewModelScope.launch {
            val nextPage = followingPage + 1
            runCatching { contentRepository.getFollowingFeed(ids, page = nextPage) }
                .onSuccess { claims ->
                    if (claims.size < 30) followingExhausted = true
                    if (claims.isNotEmpty()) {
                        followingPage = nextPage
                        val existing = (_state.value.followingFeed as? FeedState.Success)?.videos.orEmpty()
                        val seen = existing.map { it.id }.toHashSet()
                        val newVideos = claims.map { it.toUi() }
                            .filter { it.channelClaimId !in blockedIds && it.id !in seen }
                        val merged = existing + newVideos
                        _state.update { it.copy(followingFeed = FeedState.Success(merged)) }
                    }
                }
            followingLoadingMore = false
        }
    }
}

private fun VideoUiModel.toCollectionEntry(): CollectionEntry = CollectionEntry(
    claimId = id,
    permanentUrl = permanentUrl,
    title = title,
    channelName = channelName,
    channelClaimId = channelClaimId,
    thumbnailUrl = thumbnailUrl,
    addedAt = System.currentTimeMillis(),
)

private fun Claim.toUi(): VideoUiModel {
    val channelName = signingChannel?.name ?: "@anonymous"
    val initial = (channelName.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar()
    val tintSeed = channelName.hashCode().absoluteValue
    return VideoUiModel(
        id = claimId,
        permanentUrl = permanentUrl,
        title = title,
        description = description,
        channelClaimId = signingChannel?.claimId,
        channelName = channelName,
        channelTitle = signingChannel?.title?.takeIf { it.isNotBlank() },
        channelInitial = initial,
        channelAvatarUrl = signingChannel?.thumbnailUrl,
        channelAvatarTintIndex = tintSeed,
        thumbnailUrl = thumbnailUrl,
        thumbnailTintIndex = claimId.hashCode().absoluteValue,
        ageLabel = formatAge(releaseTime),
        durationLabel = formatDuration(durationSeconds),
        isShort = isShort,
        isLivestream = isLivestream,
        isUpcoming = isUpcoming,
        paywall = paywall,
        isPurchased = isPurchased,
        isMembersOnly = isMembersOnly,
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

private fun formatAge(releaseTimeSeconds: Long?): String {
    if (releaseTimeSeconds == null || releaseTimeSeconds <= 0) return ""
    val nowSec = System.currentTimeMillis() / 1000
    val deltaSec = (nowSec - releaseTimeSeconds).coerceAtLeast(0)
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
