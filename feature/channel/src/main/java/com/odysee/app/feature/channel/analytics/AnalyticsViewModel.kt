package com.odysee.app.feature.channel.analytics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.analytics.ChannelStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsState(
    val claimId: String,
    val name: String,
    val isLoading: Boolean = true,
    val stats: ChannelStats? = null,
    val error: String? = null,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val route: AnalyticsRoute = savedStateHandle.toRoute()
    private val _state = MutableStateFlow(AnalyticsState(claimId = route.claimId, name = route.name))
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = runCatching { contentRepository.getChannelStats(route.claimId) }
            result.onSuccess { stats ->
                _state.update { it.copy(isLoading = false, stats = stats) }
            }.onFailure { err ->
                _state.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }
}
