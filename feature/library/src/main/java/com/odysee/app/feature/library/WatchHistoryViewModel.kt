package com.odysee.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.history.WatchHistoryEntry
import com.odysee.app.core.data.history.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchHistoryViewModel @Inject constructor(
    private val watchHistoryRepository: WatchHistoryRepository,
) : ViewModel() {

    val history: StateFlow<List<WatchHistoryEntry>> = watchHistoryRepository.history
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun remove(claimId: String) {
        viewModelScope.launch { watchHistoryRepository.remove(claimId) }
    }

    fun clear() {
        viewModelScope.launch { watchHistoryRepository.clear() }
    }
}
