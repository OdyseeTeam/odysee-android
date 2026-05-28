package com.odysee.app.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.notifications.NotificationItem
import com.odysee.app.core.data.notifications.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val items: List<NotificationItem> = emptyList(),
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
    private val lbryioApi: com.odysee.app.core.network.LbryioApi,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { notificationsRepository.list() }
                .onSuccess { items ->
                    _state.update { it.copy(isLoading = false, items = items) }
                    val unseen = items.filter { !it.isSeen }.map { it.id }
                    if (unseen.isNotEmpty()) {
                        runCatching { notificationsRepository.markSeen(unseen) }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: error::class.simpleName ?: "Couldn't load",
                        )
                    }
                }
        }
    }

    fun markRead(id: Long) {
        viewModelScope.launch {
            runCatching { notificationsRepository.markRead(listOf(id)) }
            _state.update { state ->
                state.copy(items = state.items.map { if (it.id == id) it.copy(isRead = true) else it })
            }
        }
    }

    /** Permanently dismiss a notification (matches web's `notification/delete`). */
    fun dismiss(id: Long) {
        // Optimistically remove from UI first; server roundtrip can fail and
        // we still want a responsive list.
        _state.update { state -> state.copy(items = state.items.filterNot { it.id == id }) }
        viewModelScope.launch {
            runCatching { lbryioApi.notificationDelete(notificationIds = id.toString()) }
        }
    }
}
