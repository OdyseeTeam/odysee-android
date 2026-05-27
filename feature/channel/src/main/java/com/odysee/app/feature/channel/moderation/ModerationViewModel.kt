package com.odysee.app.feature.channel.moderation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.auth.AuthRepository
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.data.moderation.BlockScope
import com.odysee.app.core.data.moderation.BlockedCommenter
import com.odysee.app.core.data.moderation.CommentModerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModerationState(
    val claimId: String,
    val name: String,
    val blockedLoading: Boolean = true,
    val blocked: List<BlockedCommenter> = emptyList(),
    val moderatorsLoading: Boolean = true,
    val moderators: List<CommentModerator> = emptyList(),
    val error: String? = null,
    val mutatingId: String? = null,
)

@HiltViewModel
class ModerationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val route: ModerationRoute = savedStateHandle.toRoute()
    private val _state = MutableStateFlow(ModerationState(claimId = route.claimId, name = route.name))
    val state: StateFlow<ModerationState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadBlocked()
        loadModerators()
    }

    private fun loadBlocked() {
        viewModelScope.launch {
            _state.update { it.copy(blockedLoading = true, error = null) }
            val result = runCatching {
                contentRepository.listBlockedCommenters(route.claimId, route.name)
            }
            result.onSuccess { list ->
                _state.update { it.copy(blockedLoading = false, blocked = list) }
            }.onFailure { err ->
                _state.update { it.copy(blockedLoading = false, error = err.message ?: "Couldn't load blocked list") }
            }
        }
    }

    private fun loadModerators() {
        viewModelScope.launch {
            _state.update { it.copy(moderatorsLoading = true) }
            val result = runCatching {
                contentRepository.listCommentModerators(route.claimId, route.name)
            }
            result.onSuccess { list ->
                _state.update { it.copy(moderatorsLoading = false, moderators = list) }
            }.onFailure { err ->
                _state.update { it.copy(moderatorsLoading = false, error = err.message ?: "Couldn't load mods") }
            }
        }
    }

    fun unblock(commenter: BlockedCommenter) {
        viewModelScope.launch {
            _state.update { it.copy(mutatingId = commenter.claimId) }
            val result = runCatching {
                contentRepository.unblockCommenter(
                    modChannelId = route.claimId,
                    modChannelName = route.name,
                    blockedChannelId = commenter.claimId,
                    blockedChannelName = commenter.name,
                    creatorChannelId = route.claimId,
                    creatorChannelName = route.name,
                )
            }
            _state.update {
                if (result.isSuccess) {
                    it.copy(mutatingId = null, blocked = it.blocked.filterNot { c -> c.claimId == commenter.claimId })
                } else {
                    it.copy(mutatingId = null, error = result.exceptionOrNull()?.message ?: "Unblock failed")
                }
            }
        }
    }

    fun removeModerator(mod: CommentModerator) {
        viewModelScope.launch {
            _state.update { it.copy(mutatingId = mod.claimId) }
            val result = runCatching {
                contentRepository.removeCommentModerator(
                    modChannelId = mod.claimId,
                    modChannelName = mod.name,
                    creatorChannelId = route.claimId,
                    creatorChannelName = route.name,
                )
            }
            _state.update {
                if (result.isSuccess) {
                    it.copy(mutatingId = null, moderators = it.moderators.filterNot { m -> m.claimId == mod.claimId })
                } else {
                    it.copy(mutatingId = null, error = result.exceptionOrNull()?.message ?: "Remove failed")
                }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
