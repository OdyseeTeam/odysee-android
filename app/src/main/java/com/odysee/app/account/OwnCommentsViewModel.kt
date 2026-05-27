package com.odysee.app.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.auth.AuthRepository
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.model.Claim
import com.odysee.app.core.model.Comment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OwnCommentsState(
    val channels: List<OwnCommentsChannel> = emptyList(),
    val activeChannelClaimId: String? = null,
    val activeChannelName: String? = null,
    val comments: List<Comment> = emptyList(),
    val claimsById: Map<String, Claim> = emptyMap(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalItems: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

data class OwnCommentsChannel(
    val claimId: String,
    val name: String,
    val title: String?,
    val thumbnailUrl: String?,
) {
    val displayName: String get() = title?.takeIf { it.isNotBlank() } ?: name.removePrefix("@")
}

@HiltViewModel
class OwnCommentsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val contentRepository: ContentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(OwnCommentsState())
    val state: StateFlow<OwnCommentsState> = _state.asStateFlow()

    init {
        val signedIn = authRepository.state.value as? AuthState.SignedIn
        val channels = signedIn?.channels.orEmpty()
            .map { OwnCommentsChannel(it.claimId, it.name, it.title, it.thumbnailUrl) }
        val active = signedIn?.activeChannel ?: signedIn?.channels?.firstOrNull()
        _state.update {
            it.copy(
                channels = channels,
                activeChannelClaimId = active?.claimId,
                activeChannelName = active?.name,
            )
        }
        if (active != null) loadFirstPage()
    }

    fun selectChannel(claimId: String) {
        val ch = _state.value.channels.firstOrNull { it.claimId == claimId } ?: return
        if (ch.claimId == _state.value.activeChannelClaimId) return
        _state.update {
            it.copy(
                activeChannelClaimId = ch.claimId,
                activeChannelName = ch.name,
                comments = emptyList(),
                claimsById = emptyMap(),
                page = 1,
                totalPages = 1,
                totalItems = 0,
            )
        }
        loadFirstPage()
    }

    fun refresh() = loadFirstPage()

    private fun loadFirstPage() {
        val s = _state.value
        val cid = s.activeChannelClaimId ?: return
        val name = s.activeChannelName ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { contentRepository.listOwnComments(cid, name, page = 1, pageSize = PAGE_SIZE) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            comments = result.comments,
                            claimsById = result.claimsById,
                            page = result.page,
                            totalPages = result.totalPages,
                            totalItems = result.totalItems,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoading = false, error = err.message ?: "Failed to load comments") }
                }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || s.page >= s.totalPages) return
        val cid = s.activeChannelClaimId ?: return
        val name = s.activeChannelName ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, error = null) }
            val next = s.page + 1
            runCatching { contentRepository.listOwnComments(cid, name, page = next, pageSize = PAGE_SIZE) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            comments = it.comments + result.comments,
                            claimsById = it.claimsById + result.claimsById,
                            page = result.page,
                            totalPages = result.totalPages,
                            totalItems = result.totalItems,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoadingMore = false, error = err.message ?: "Failed to load more") }
                }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    fun deleteComment(commentId: String) {
        val s = _state.value
        val cid = s.activeChannelClaimId ?: return
        val name = s.activeChannelName ?: return
        viewModelScope.launch {
            runCatching { contentRepository.deleteOwnComment(commentId, cid, name) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            comments = it.comments.filterNot { c -> c.commentId == commentId },
                            totalItems = (it.totalItems - 1).coerceAtLeast(0),
                        )
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(error = err.message ?: "Delete failed") }
                }
        }
    }

    fun editComment(commentId: String, newBody: String) {
        val s = _state.value
        val cid = s.activeChannelClaimId ?: return
        val name = s.activeChannelName ?: return
        if (newBody.isBlank()) return
        viewModelScope.launch {
            runCatching { contentRepository.editOwnComment(commentId, newBody, cid, name) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            comments = it.comments.map { c ->
                                if (c.commentId == commentId) c.copy(body = newBody) else c
                            },
                        )
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(error = err.message ?: "Edit failed") }
                }
        }
    }

    private companion object { const val PAGE_SIZE = 10 }
}
