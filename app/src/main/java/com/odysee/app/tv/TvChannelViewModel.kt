package com.odysee.app.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.subscriptions.SubscriptionsRepository
import com.odysee.app.core.model.Channel
import com.odysee.app.core.model.Claim
import com.odysee.app.core.network.LbryioApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TvChannelUiState(
    val channel: Channel? = null,
    val claims: List<Claim> = emptyList(),
    val playlists: List<TvChannelPlaylist> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val followerCount: Long? = null,
    val isSubscribed: Boolean = false,
)

data class TvChannelPlaylist(
    val claimId: String,
    val title: String,
    val thumbnailUrl: String?,
    val itemCount: Int,
)

@HiltViewModel
class TvChannelViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val lbryioApi: LbryioApi,
) : ViewModel() {
    private val _state = MutableStateFlow(TvChannelUiState())
    val state: StateFlow<TvChannelUiState> = _state.asStateFlow()
    private var currentClaimId: String? = null

    fun load(channelClaimId: String) {
        currentClaimId = channelClaimId
        _state.update { TvChannelUiState(isLoading = true) }
        viewModelScope.launch {
            val ch = runCatching { contentRepository.getChannel(channelClaimId) }.getOrNull()
            val items = runCatching {
                contentRepository.getChannelVideos(channelClaimId, page = 1, pageSize = 40)
            }.getOrDefault(emptyList())
            val rawPlaylists = runCatching {
                contentRepository.getChannelCollections(channelClaimId)
            }.getOrDefault(emptyList())
            val playlists = rawPlaylists.map { c ->
                TvChannelPlaylist(
                    claimId = c.claimId,
                    title = c.value?.title ?: c.name ?: "Untitled",
                    thumbnailUrl = c.value?.thumbnail?.url?.takeIf { it.isNotBlank() },
                    itemCount = c.value?.claims?.size ?: 0,
                )
            }
            val followerCount = runCatching {
                lbryioApi.subCount(channelClaimId).data?.firstOrNull()
            }.getOrNull()
            val isSubscribed = subscriptionsRepository.isSubscribed(channelClaimId)
            _state.update {
                TvChannelUiState(
                    channel = ch,
                    claims = items,
                    playlists = playlists,
                    isLoading = false,
                    error = null,
                    followerCount = followerCount,
                    isSubscribed = isSubscribed,
                )
            }
        }
        viewModelScope.launch {
            subscriptionsRepository.subscriptions.collect { subs ->
                val sub = subs.any { it.claimId == channelClaimId }
                _state.update { it.copy(isSubscribed = sub) }
            }
        }
    }

    fun toggleSubscribe(name: String) {
        val cid = currentClaimId ?: return
        viewModelScope.launch {
            if (_state.value.isSubscribed) subscriptionsRepository.unsubscribe(cid)
            else subscriptionsRepository.subscribe(cid, name)
        }
    }
}
