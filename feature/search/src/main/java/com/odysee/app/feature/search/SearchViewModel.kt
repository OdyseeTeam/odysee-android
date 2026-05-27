package com.odysee.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.model.Claim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.absoluteValue

data class SearchResultUi(
    val claimId: String,
    val title: String,
    val name: String,
    val permanentUrl: String,
    val isChannel: Boolean,
    val thumbnailUrl: String?,
    val channelClaimId: String?,
    val channelName: String?,
    val channelAvatarUrl: String?,
    val ageLabel: String,
    val durationLabel: String,
    val tintIndex: Int,
    val description: String?,
    val paywall: com.odysee.app.core.model.Paywall = com.odysee.app.core.model.Paywall.Free,
    val isPurchased: Boolean = false,
    val isMembersOnly: Boolean = false,
)

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Success(val results: List<SearchResultUi>) : SearchState
    data class Error(val message: String) : SearchState
}

data class SearchUiState(
    val query: String = "",
    val state: SearchState = SearchState.Idle,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(state = SearchState.Idle) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            runSearch(query)
        }
    }

    fun submit() {
        searchJob?.cancel()
        val q = _state.value.query.trim()
        if (q.isBlank()) return
        searchJob = viewModelScope.launch { runSearch(q) }
    }

    private suspend fun runSearch(query: String) {
        _state.update { it.copy(state = SearchState.Loading) }
        runCatching { contentRepository.search(query.trim()) }
            .onSuccess { claims ->
                val results = claims.map { it.toUi() }
                _state.update { it.copy(state = SearchState.Success(results)) }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        state = SearchState.Error(
                            error.message ?: error::class.simpleName ?: "Search failed",
                        ),
                    )
                }
            }
    }
}

private fun Claim.toUi(): SearchResultUi {
    val isChannel = name.startsWith("@")
    return SearchResultUi(
        claimId = claimId,
        title = title,
        name = name,
        permanentUrl = permanentUrl,
        isChannel = isChannel,
        thumbnailUrl = thumbnailUrl,
        channelClaimId = signingChannel?.claimId,
        channelName = signingChannel?.name ?: name.takeIf { isChannel },
        channelAvatarUrl = signingChannel?.thumbnailUrl,
        ageLabel = formatAge(releaseTime),
        durationLabel = formatDuration(durationSeconds),
        tintIndex = claimId.hashCode().absoluteValue,
        description = description,
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
