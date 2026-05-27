package com.odysee.app.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.memberships.MySubscription
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MembershipsState(
    val isLoading: Boolean = true,
    val subscriptions: List<MySubscription> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class MembershipsViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MembershipsState())
    val state: StateFlow<MembershipsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = runCatching { contentRepository.listMyMembershipSubscriptions() }
            result.onSuccess { list ->
                _state.update { it.copy(isLoading = false, subscriptions = list) }
            }.onFailure { err ->
                _state.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }
}
