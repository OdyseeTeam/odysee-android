package com.odysee.app.feature.channel.discussion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.discussion.DiscussionSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscussionSettingsState(
    val claimId: String,
    val name: String,
    val isLoading: Boolean = true,
    val settings: DiscussionSettings = DiscussionSettings(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DiscussionSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val route: DiscussionSettingsRoute = savedStateHandle.toRoute()
    private val _state = MutableStateFlow(DiscussionSettingsState(claimId = route.claimId, name = route.name))
    val state: StateFlow<DiscussionSettingsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = runCatching { contentRepository.getDiscussionSettings(route.claimId) }
            result.onSuccess { s ->
                _state.update { it.copy(isLoading = false, settings = s) }
            }.onFailure { err ->
                _state.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun onMinTipChange(text: String) {
        val v = text.toDoubleOrNull() ?: 0.0
        _state.update { it.copy(settings = it.settings.copy(minTipAmountComment = v.coerceAtLeast(0.0))) }
    }

    fun onMinSuperChatChange(text: String) {
        val v = text.toDoubleOrNull() ?: 0.0
        _state.update { it.copy(settings = it.settings.copy(minTipAmountSuperChat = v.coerceAtLeast(0.0))) }
    }

    fun onSlowModeChange(text: String) {
        val v = text.toIntOrNull() ?: 0
        _state.update { it.copy(settings = it.settings.copy(slowModeMinGap = v.coerceAtLeast(0))) }
    }

    fun setMembersOnlyComments(value: Boolean) {
        _state.update { it.copy(settings = it.settings.copy(commentsMembersOnly = value)) }
    }

    fun setMembersOnlyLivestreamChat(value: Boolean) {
        _state.update { it.copy(settings = it.settings.copy(livestreamChatMembersOnly = value)) }
    }

    fun save() {
        val st = _state.value
        if (st.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val result = runCatching {
                contentRepository.updateDiscussionSettings(st.claimId, st.name, st.settings)
            }
            _state.update {
                if (result.isSuccess) it.copy(isSaving = false, saved = true)
                else it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Save failed")
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
