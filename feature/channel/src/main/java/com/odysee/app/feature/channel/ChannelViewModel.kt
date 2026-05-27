package com.odysee.app.feature.channel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.auth.AuthRepository
import com.odysee.app.core.data.collections.CollectionEntry
import com.odysee.app.core.data.collections.FavoritesRepository
import com.odysee.app.core.data.collections.WatchLaterRepository
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.data.auth.PremiumTier
import com.odysee.app.core.data.moderation.BlockedChannelsRepository
import com.odysee.app.core.data.reactions.MyReaction
import com.odysee.app.core.data.subscriptions.SubscriptionsRepository
import com.odysee.app.core.data.tags.TagsRepository
import com.odysee.app.core.designsystem.comments.OdyseeComment
import com.odysee.app.core.designsystem.comments.OdyseeMembershipTier
import com.odysee.app.core.designsystem.comments.OdyseeReaction
import com.odysee.app.core.model.Channel
import com.odysee.app.core.model.Claim
import com.odysee.app.core.network.LbryioApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.absoluteValue

data class ChannelVideoUi(
    val id: String,
    val permanentUrl: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val thumbnailTintIndex: Int,
    val ageLabel: String,
    val durationLabel: String,
    val paywall: com.odysee.app.core.model.Paywall = com.odysee.app.core.model.Paywall.Free,
    val isPurchased: Boolean = false,
    val isMembersOnly: Boolean = false,
)

data class ChannelUiState(
    val claimId: String,
    val displayName: String,
    val channel: Channel? = null,
    val videos: List<ChannelVideoUi> = emptyList(),
    val videosPage: Int = 0,
    val videosExhausted: Boolean = false,
    val isLoadingChannel: Boolean = true,
    val isLoadingVideos: Boolean = true,
    val isLoadingMoreVideos: Boolean = false,
    val channelError: String? = null,
    val videosError: String? = null,
    val shorts: List<ChannelVideoUi> = emptyList(),
    val shortsPage: Int = 0,
    val shortsExhausted: Boolean = false,
    val isLoadingShorts: Boolean = false,
    val shortsError: String? = null,
    val playlists: List<ChannelPlaylistUi> = emptyList(),
    val isLoadingPlaylists: Boolean = false,
    val playlistsError: String? = null,
    val featuredChannels: List<Channel> = emptyList(),
    val isLoadingFeaturedChannels: Boolean = false,
    val featuredChannelsLoaded: Boolean = false,
    val isOwnChannel: Boolean = false,
    val isSubscribed: Boolean = false,
    val isBlocked: Boolean = false,
    val discussion: DiscussionState = DiscussionState.Idle,
    val followerCount: Long? = null,
    val followedTags: Set<String> = emptySet(),
)

data class ChannelPlaylistUi(
    val claimId: String,
    val name: String,
    val title: String,
    val thumbnailUrl: String?,
    val itemCount: Int,
)

sealed interface DiscussionState {
    data object Idle : DiscussionState
    data object Loading : DiscussionState
    data class Success(
        val comments: List<OdyseeComment>,
        val repliesByParent: Map<String, List<OdyseeComment>> = emptyMap(),
    ) : DiscussionState
    data class Error(val message: String) : DiscussionState
}

@HiltViewModel
class ChannelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val blockedChannelsRepository: BlockedChannelsRepository,
    private val tagsRepository: TagsRepository,
    private val lbryioApi: LbryioApi,
    private val authRepoRef: AuthRepository,
    private val watchLaterRepository: WatchLaterRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {
    private val authRepository: AuthRepository get() = authRepoRef

    private val route: ChannelRoute = savedStateHandle.toRoute()

    private val _state = MutableStateFlow(
        ChannelUiState(claimId = route.claimId, displayName = route.name),
    )
    val state: StateFlow<ChannelUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.state.collect { auth ->
                val mine = (auth as? AuthState.SignedIn)?.channels?.any { it.claimId == route.claimId } == true
                _state.update { it.copy(isOwnChannel = mine) }
            }
        }
        viewModelScope.launch {
            subscriptionsRepository.subscriptions.collect { subs ->
                _state.update { it.copy(isSubscribed = subs.any { s -> s.claimId == route.claimId }) }
            }
        }
        viewModelScope.launch {
            blockedChannelsRepository.blocked.collect { list ->
                _state.update { it.copy(isBlocked = list.any { b -> b.claimId == route.claimId }) }
            }
        }
        viewModelScope.launch {
            tagsRepository.tags.collect { tags ->
                _state.update { it.copy(followedTags = tags.toSet()) }
            }
        }
        load()
    }

    fun toggleBlock() {
        viewModelScope.launch {
            blockedChannelsRepository.toggle(route.claimId, route.name)
        }
    }

    fun toggleTag(tag: String) {
        viewModelScope.launch { tagsRepository.toggle(tag) }
    }

    fun toggleSubscription() {
        viewModelScope.launch {
            subscriptionsRepository.toggle(route.claimId, route.name)
        }
    }

    fun load() {
        loadChannel()
        loadVideos()
        loadFollowerCount()
    }

    private fun loadFollowerCount() {
        viewModelScope.launch {
            runCatching { lbryioApi.subCount(route.claimId) }
                .onSuccess { env ->
                    val count = env.data?.firstOrNull()
                    _state.update { it.copy(followerCount = count) }
                }
        }
    }

    fun loadChannel() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingChannel = true, channelError = null) }
            runCatching { contentRepository.getChannel(route.claimId) }
                .onSuccess { channel ->
                    _state.update { it.copy(isLoadingChannel = false, channel = channel) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingChannel = false,
                            channelError = error.message
                                ?: error::class.simpleName
                                ?: "Unknown error",
                        )
                    }
                }
        }
    }

    fun loadDiscussion() {
        viewModelScope.launch {
            _state.update { it.copy(discussion = DiscussionState.Loading) }
            val signed = authRepoRef.state.value as? AuthState.SignedIn
            val mineChannelId = signed?.activeChannel?.claimId
            val mineChannelName = signed?.activeChannel?.name
            runCatching { contentRepository.getComments(route.claimId) }
                .onSuccess { comments ->
                    val creatorName = _state.value.channel?.name ?: route.name
                    val base = comments.map { c ->
                        val handle = c.authorName
                        val initial = (handle.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar()
                        OdyseeComment(
                            id = c.commentId,
                            parentId = c.parentId,
                            authorDisplayName = handle,
                            authorHandle = handle,
                            authorChannelId = c.authorChannelId,
                            authorAvatarUrl = c.authorAvatarUrl,
                            authorInitial = initial,
                            ageLabel = formatAge(c.timestamp),
                            body = c.body,
                            isPinned = c.isPinned,
                            pinnedByName = if (c.isPinned) creatorName else null,
                            isEdited = c.signingTimestamp?.let { (it - c.timestamp) > 5L } == true,
                            isCreator = c.authorChannelId == route.claimId,
                            isMine = !mineChannelId.isNullOrBlank() && c.authorChannelId == mineChannelId,
                            isModerator = c.isModerator,
                            isGlobalMod = c.isGlobalMod,
                            supportAmount = c.supportAmount,
                            replyCount = c.replyCount,
                        )
                    }
                    _state.update { it.copy(discussion = DiscussionState.Success(base)) }

                    val channelIds = base.mapNotNull { it.authorChannelId }.distinct()
                    if (channelIds.isNotEmpty()) {
                        val channels = runCatching { contentRepository.getChannels(channelIds) }
                            .getOrNull()
                            .orEmpty()
                            .associateBy { it.claimId }
                        if (channels.isNotEmpty()) {
                            _state.update { st ->
                                val current = (st.discussion as? DiscussionState.Success)?.comments ?: return@update st
                                val merged = current.map { c ->
                                    val ch = c.authorChannelId?.let { channels[it] } ?: return@map c
                                    val title = ch.title?.takeIf { it.isNotBlank() }
                                    c.copy(
                                        authorDisplayName = title ?: c.authorDisplayName,
                                        authorAvatarUrl = ch.thumbnailUrl?.takeIf { it.isNotBlank() }
                                            ?: c.authorAvatarUrl,
                                    )
                                }
                                st.copy(discussion = DiscussionState.Success(merged))
                            }
                        }
                    }

                    val reactionMap = runCatching {
                        contentRepository.getCommentReactions(
                            base.map { it.id },
                            mineChannelId,
                            mineChannelName,
                        )
                    }.getOrNull().orEmpty()
                    if (reactionMap.isNotEmpty()) {
                        val creatorAvatar = _state.value.channel?.thumbnailUrl?.takeIf { it.isNotBlank() }
                        _state.update { st ->
                            val current = (st.discussion as? DiscussionState.Success)?.comments ?: return@update st
                            val merged = current.map { c ->
                                val r = reactionMap[c.id] ?: return@map c
                                c.copy(
                                    likes = r.likes,
                                    dislikes = r.dislikes,
                                    myReaction = when {
                                        r.myLike -> OdyseeReaction.Like
                                        r.myDislike -> OdyseeReaction.Dislike
                                        else -> OdyseeReaction.None
                                    },
                                    creatorLiked = r.creatorLike,
                                    creatorAvatarUrl = if (r.creatorLike) creatorAvatar else null,
                                )
                            }
                            st.copy(discussion = DiscussionState.Success(merged))
                        }
                    }

                    val premiumByChannel = runCatching {
                        lbryioApi.userHasPremium(channelIds.joinToString(","))
                    }.getOrNull()?.data.orEmpty().mapValues { (_, v) ->
                        when {
                            v.hasPremiumPlus -> OdyseeMembershipTier.PremiumPlus
                            v.hasPremium -> OdyseeMembershipTier.Premium
                            else -> OdyseeMembershipTier.None
                        }
                    }
                    if (premiumByChannel.isNotEmpty()) {
                        _state.update { st ->
                            val current = (st.discussion as? DiscussionState.Success)?.comments ?: return@update st
                            val merged = current.map { c ->
                                val tier = c.authorChannelId?.let { premiumByChannel[it] } ?: return@map c
                                c.copy(membership = tier)
                            }
                            st.copy(discussion = DiscussionState.Success(merged))
                        }
                    }

                    val creatorMembership = runCatching {
                        contentRepository.checkCreatorMembershipsForChannels(route.claimId, channelIds)
                    }.getOrNull().orEmpty()
                    if (creatorMembership.isNotEmpty()) {
                        _state.update { st ->
                            val current = (st.discussion as? DiscussionState.Success)?.comments ?: return@update st
                            val merged = current.map { c ->
                                val name = c.authorChannelId?.let { creatorMembership[it] } ?: return@map c
                                c.copy(creatorMembership = name)
                            }
                            st.copy(discussion = DiscussionState.Success(merged))
                        }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            discussion = DiscussionState.Error(
                                error.message ?: error::class.simpleName ?: "Unknown error",
                            ),
                        )
                    }
                }
        }
    }

    fun reactToDiscussionComment(commentId: String, type: OdyseeReaction) {
        val signed = authRepoRef.state.value as? AuthState.SignedIn ?: return
        val active = signed.activeChannel ?: return
        viewModelScope.launch {
            val current = (_state.value.discussion as? DiscussionState.Success)?.comments ?: return@launch
            val target = current.firstOrNull { it.id == commentId } ?: return@launch
            val typeName = when (type) {
                OdyseeReaction.Like -> "like"
                OdyseeReaction.Dislike -> "dislike"
                OdyseeReaction.None -> return@launch
            }
            val remove = (type == OdyseeReaction.Like && target.myReaction == OdyseeReaction.Like) ||
                (type == OdyseeReaction.Dislike && target.myReaction == OdyseeReaction.Dislike)
            // Optimistic update
            _state.update { st ->
                val list = (st.discussion as? DiscussionState.Success)?.comments ?: return@update st
                val updated = list.map { c ->
                    if (c.id != commentId) return@map c
                    val (newLikes, newDislikes, newMy) = when (type) {
                        OdyseeReaction.Like ->
                            if (remove) Triple((c.likes - 1).coerceAtLeast(0), c.dislikes, OdyseeReaction.None)
                            else Triple(
                                c.likes + (if (c.myReaction == OdyseeReaction.Like) 0 else 1),
                                if (c.myReaction == OdyseeReaction.Dislike) (c.dislikes - 1).coerceAtLeast(0) else c.dislikes,
                                OdyseeReaction.Like,
                            )
                        OdyseeReaction.Dislike ->
                            if (remove) Triple(c.likes, (c.dislikes - 1).coerceAtLeast(0), OdyseeReaction.None)
                            else Triple(
                                if (c.myReaction == OdyseeReaction.Like) (c.likes - 1).coerceAtLeast(0) else c.likes,
                                c.dislikes + (if (c.myReaction == OdyseeReaction.Dislike) 0 else 1),
                                OdyseeReaction.Dislike,
                            )
                        OdyseeReaction.None -> Triple(c.likes, c.dislikes, OdyseeReaction.None)
                    }
                    c.copy(likes = newLikes, dislikes = newDislikes, myReaction = newMy)
                }
                st.copy(discussion = DiscussionState.Success(updated))
            }
            runCatching {
                contentRepository.reactToComment(
                    commentId = commentId,
                    channelId = active.claimId,
                    channelName = active.name,
                    type = typeName,
                    remove = remove,
                )
            }
        }
    }

    fun postDiscussionReply(parentId: String, text: String) {
        if (text.isBlank()) return
        val signed = authRepoRef.state.value as? AuthState.SignedIn ?: return
        val active = signed.activeChannel ?: return
        viewModelScope.launch {
            runCatching {
                contentRepository.postComment(
                    claimId = route.claimId,
                    channelId = active.claimId,
                    channelName = active.name,
                    text = text,
                    parentId = parentId,
                    supportTxId = null,
                )
            }.onSuccess { loadDiscussion() }
        }
    }

    fun loadFeaturedChannels() {
        if (_state.value.isLoadingFeaturedChannels || _state.value.featuredChannelsLoaded) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingFeaturedChannels = true) }
            val ids = runCatching {
                contentRepository.getFeaturedChannelClaimIds(route.claimId)
            }.getOrNull().orEmpty()
            val channels = if (ids.isEmpty()) emptyList() else runCatching {
                contentRepository.getChannels(ids)
            }.getOrNull().orEmpty()
            _state.update {
                it.copy(
                    featuredChannels = channels,
                    isLoadingFeaturedChannels = false,
                    featuredChannelsLoaded = true,
                )
            }
        }
    }

    fun loadDiscussionReplies(parentId: String) {
        val signed = authRepoRef.state.value as? AuthState.SignedIn
        val mineChannelId = signed?.activeChannel?.claimId
        val mineChannelName = signed?.activeChannel?.name
        viewModelScope.launch {
            val current = _state.value.discussion as? DiscussionState.Success ?: return@launch
            if (current.repliesByParent.containsKey(parentId)) return@launch
            val items = runCatching {
                contentRepository.getComments(route.claimId, parentId = parentId)
            }.getOrNull().orEmpty()
            if (items.isEmpty()) {
                _state.update { st ->
                    val s = st.discussion as? DiscussionState.Success ?: return@update st
                    st.copy(discussion = s.copy(repliesByParent = s.repliesByParent + (parentId to emptyList())))
                }
                return@launch
            }
            val base = items.map { c ->
                val handle = c.authorName
                val initial = (handle.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar()
                OdyseeComment(
                    id = c.commentId,
                    parentId = c.parentId,
                    authorDisplayName = handle,
                    authorHandle = handle,
                    authorChannelId = c.authorChannelId,
                    authorAvatarUrl = c.authorAvatarUrl,
                    authorInitial = initial,
                    ageLabel = formatAge(c.timestamp),
                    body = c.body,
                    isPinned = c.isPinned,
                    isCreator = c.authorChannelId == route.claimId,
                    isMine = !mineChannelId.isNullOrBlank() && c.authorChannelId == mineChannelId,
                    isModerator = c.isModerator,
                    isGlobalMod = c.isGlobalMod,
                    supportAmount = c.supportAmount,
                    replyCount = c.replyCount,
                )
            }
            _state.update { st ->
                val s = st.discussion as? DiscussionState.Success ?: return@update st
                st.copy(discussion = s.copy(repliesByParent = s.repliesByParent + (parentId to base)))
            }
            val channelIds = base.mapNotNull { it.authorChannelId }.distinct()
            if (channelIds.isNotEmpty()) {
                val channels = runCatching { contentRepository.getChannels(channelIds) }
                    .getOrNull().orEmpty().associateBy { it.claimId }
                if (channels.isNotEmpty()) {
                    _state.update { st ->
                        val s = st.discussion as? DiscussionState.Success ?: return@update st
                        val cur = s.repliesByParent[parentId] ?: return@update st
                        val merged = cur.map { c ->
                            val ch = c.authorChannelId?.let { channels[it] } ?: return@map c
                            c.copy(
                                authorDisplayName = ch.title?.takeIf { it.isNotBlank() } ?: c.authorDisplayName,
                                authorAvatarUrl = ch.thumbnailUrl?.takeIf { it.isNotBlank() } ?: c.authorAvatarUrl,
                            )
                        }
                        st.copy(discussion = s.copy(repliesByParent = s.repliesByParent + (parentId to merged)))
                    }
                }
            }
            val reactionMap = runCatching {
                contentRepository.getCommentReactions(base.map { it.id }, mineChannelId, mineChannelName)
            }.getOrNull().orEmpty()
            if (reactionMap.isNotEmpty()) {
                val creatorAvatar = _state.value.channel?.thumbnailUrl?.takeIf { it.isNotBlank() }
                _state.update { st ->
                    val s = st.discussion as? DiscussionState.Success ?: return@update st
                    val cur = s.repliesByParent[parentId] ?: return@update st
                    val merged = cur.map { c ->
                        val r = reactionMap[c.id] ?: return@map c
                        c.copy(
                            likes = r.likes,
                            dislikes = r.dislikes,
                            myReaction = when {
                                r.myLike -> OdyseeReaction.Like
                                r.myDislike -> OdyseeReaction.Dislike
                                else -> OdyseeReaction.None
                            },
                            creatorLiked = r.creatorLike,
                            creatorAvatarUrl = if (r.creatorLike) creatorAvatar else null,
                        )
                    }
                    st.copy(discussion = s.copy(repliesByParent = s.repliesByParent + (parentId to merged)))
                }
            }
        }
    }

    fun saveVideoToWatchLater(video: ChannelVideoUi) {
        viewModelScope.launch { watchLaterRepository.add(video.toCollectionEntry()) }
    }

    fun saveVideoToFavorites(video: ChannelVideoUi) {
        viewModelScope.launch { favoritesRepository.add(video.toCollectionEntry()) }
    }

    private fun ChannelVideoUi.toCollectionEntry() = CollectionEntry(
        claimId = id,
        permanentUrl = permanentUrl,
        title = title,
        channelName = _state.value.displayName,
        channelClaimId = _state.value.claimId,
        thumbnailUrl = thumbnailUrl,
        addedAt = System.currentTimeMillis(),
    )

    fun blockDiscussionCommentAuthor(commentId: String) {
        viewModelScope.launch {
            val target = (_state.value.discussion as? DiscussionState.Success)
                ?.comments?.firstOrNull { it.id == commentId } ?: return@launch
            val id = target.authorChannelId ?: return@launch
            blockedChannelsRepository.toggle(id, target.authorHandle)
            _state.update { st ->
                val list = (st.discussion as? DiscussionState.Success)?.comments ?: return@update st
                st.copy(discussion = DiscussionState.Success(list.filterNot { it.authorChannelId == id }))
            }
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoadingVideos = true,
                    videosError = null,
                    videos = emptyList(),
                    videosPage = 0,
                    videosExhausted = false,
                )
            }
            runCatching { contentRepository.getChannelVideos(route.claimId, page = 1, pageSize = PAGE_SIZE) }
                .onSuccess { claims ->
                    val items = claims.filterNot { it.isShort }.map { c -> c.toUi() }
                    _state.update {
                        it.copy(
                            isLoadingVideos = false,
                            videos = items,
                            videosPage = 1,
                            videosExhausted = claims.size < PAGE_SIZE,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingVideos = false,
                            videosError = error.message ?: error::class.simpleName ?: "Unknown error",
                        )
                    }
                }
        }
    }

    fun loadMoreVideos() {
        val cur = _state.value
        if (cur.isLoadingVideos || cur.isLoadingMoreVideos || cur.videosExhausted) return
        val next = cur.videosPage + 1
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMoreVideos = true) }
            runCatching { contentRepository.getChannelVideos(route.claimId, page = next, pageSize = PAGE_SIZE) }
                .onSuccess { claims ->
                    val items = claims.filterNot { it.isShort }.map { c -> c.toUi() }
                    val existingIds = cur.videos.mapTo(mutableSetOf()) { it.id }
                    val newOnes = items.filterNot { existingIds.contains(it.id) }
                    _state.update {
                        it.copy(
                            isLoadingMoreVideos = false,
                            videos = it.videos + newOnes,
                            videosPage = next,
                            videosExhausted = claims.size < PAGE_SIZE,
                        )
                    }
                }
                .onFailure {
                    _state.update { st -> st.copy(isLoadingMoreVideos = false) }
                }
        }
    }

    fun loadShorts() {
        val cur = _state.value
        if (cur.isLoadingShorts) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingShorts = true, shortsError = null) }
            runCatching { contentRepository.getChannelVideos(route.claimId, page = 1, pageSize = PAGE_SIZE) }
                .onSuccess { claims ->
                    val items = claims.filter { it.isShort }.map { c -> c.toUi() }
                    _state.update {
                        it.copy(
                            isLoadingShorts = false,
                            shorts = items,
                            shortsPage = 1,
                            shortsExhausted = claims.size < PAGE_SIZE,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingShorts = false,
                            shortsError = error.message ?: "Couldn't load shorts",
                        )
                    }
                }
        }
    }

    fun loadMoreShorts() {
        val cur = _state.value
        if (cur.isLoadingShorts || cur.shortsExhausted) return
        val next = cur.shortsPage + 1
        viewModelScope.launch {
            runCatching { contentRepository.getChannelVideos(route.claimId, page = next, pageSize = PAGE_SIZE) }
                .onSuccess { claims ->
                    val items = claims.filter { it.isShort }.map { c -> c.toUi() }
                    val existing = cur.shorts.mapTo(mutableSetOf()) { it.id }
                    _state.update { st ->
                        st.copy(
                            shorts = st.shorts + items.filterNot { existing.contains(it.id) },
                            shortsPage = next,
                            shortsExhausted = claims.size < PAGE_SIZE,
                        )
                    }
                }
        }
    }

    fun loadChannelPlaylists() {
        val cur = _state.value
        if (cur.isLoadingPlaylists) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPlaylists = true, playlistsError = null) }
            runCatching { contentRepository.getChannelCollections(route.claimId) }
                .onSuccess { rawList ->
                    val items = rawList.map { c ->
                        ChannelPlaylistUi(
                            claimId = c.claimId,
                            name = c.name ?: "",
                            title = c.value?.title ?: c.name ?: "Untitled",
                            thumbnailUrl = c.value?.thumbnail?.url?.takeIf { it.isNotBlank() },
                            itemCount = c.value?.claims?.size ?: 0,
                        )
                    }
                    _state.update { it.copy(isLoadingPlaylists = false, playlists = items) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoadingPlaylists = false, playlistsError = error.message ?: "Couldn't load playlists")
                    }
                }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}

private fun Claim.toUi(): ChannelVideoUi = ChannelVideoUi(
    id = claimId,
    permanentUrl = permanentUrl,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    thumbnailTintIndex = claimId.hashCode().absoluteValue,
    ageLabel = formatAge(releaseTime),
    durationLabel = formatDuration(durationSeconds),
    paywall = paywall,
    isPurchased = isPurchased,
    isMembersOnly = isMembersOnly,
)

private fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds <= 0) return ""
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs)
    else "%d:%02d".format(minutes, secs)
}

private fun formatAge(unixSeconds: Long?): String {
    if (unixSeconds == null || unixSeconds <= 0) return ""
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
