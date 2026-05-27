package com.odysee.app.feature.channel.memberships

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.memberships.MembershipTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MembershipsState(
    val claimId: String,
    val name: String,
    val isLoading: Boolean = true,
    val tiers: List<MembershipTier> = emptyList(),
    val isMutating: Boolean = false,
    val error: String? = null,
    val editing: TierDraft? = null,
)

data class TierDraft(
    val id: String? = null,
    val name: String = "",
    val description: String = "",
    val priceText: String = "",
) {
    val priceUsd: Double? get() = priceText.toDoubleOrNull()?.takeIf { it >= 0 }
    val canSubmit: Boolean get() = name.isNotBlank() && (priceUsd ?: -1.0) >= 0.0
}

@HiltViewModel
class MembershipsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val route: CreatorMembershipsRoute = savedStateHandle.toRoute()
    private val _state = MutableStateFlow(MembershipsState(claimId = route.claimId, name = route.name))
    val state: StateFlow<MembershipsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = runCatching { contentRepository.listMemberships(route.claimId) }
            result.onSuccess { list ->
                _state.update { it.copy(isLoading = false, tiers = list) }
            }.onFailure { err ->
                _state.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun startNew() = _state.update { it.copy(editing = TierDraft()) }
    fun startEdit(tier: MembershipTier) = _state.update {
        it.copy(
            editing = TierDraft(
                id = tier.id,
                name = tier.name,
                description = tier.description.orEmpty(),
                priceText = if (tier.priceUsd > 0) "%.2f".format(tier.priceUsd) else "",
            ),
        )
    }
    fun closeEdit() = _state.update { it.copy(editing = null) }

    fun onName(v: String) = _state.update {
        val e = it.editing ?: return@update it
        it.copy(editing = e.copy(name = v))
    }
    fun onDescription(v: String) = _state.update {
        val e = it.editing ?: return@update it
        it.copy(editing = e.copy(description = v))
    }
    fun onPrice(v: String) = _state.update {
        val e = it.editing ?: return@update it
        it.copy(editing = e.copy(priceText = v.filter { c -> c.isDigit() || c == '.' }))
    }

    fun submit() {
        val draft = _state.value.editing ?: return
        if (!draft.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isMutating = true, error = null) }
            val result = if (draft.id == null) {
                runCatching {
                    contentRepository.createMembership(
                        channelClaimId = route.claimId,
                        name = draft.name.trim(),
                        description = draft.description.trim().takeIf { it.isNotBlank() },
                        priceUsd = draft.priceUsd ?: 0.0,
                    )
                }
            } else {
                runCatching {
                    contentRepository.updateMembership(
                        id = draft.id,
                        name = draft.name.trim(),
                        description = draft.description.trim().takeIf { it.isNotBlank() },
                        priceUsd = draft.priceUsd,
                    )
                }
            }
            result.onSuccess {
                _state.update { it.copy(isMutating = false, editing = null) }
                load()
            }.onFailure { err ->
                _state.update { it.copy(isMutating = false, error = err.message ?: "Save failed") }
            }
        }
    }

    fun delete(tier: MembershipTier) {
        viewModelScope.launch {
            _state.update { it.copy(isMutating = true) }
            runCatching { contentRepository.deleteMembership(tier.id) }
            _state.update { it.copy(isMutating = false) }
            load()
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
