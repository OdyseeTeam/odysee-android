package com.odysee.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.collections.CollectionEntry
import com.odysee.app.core.data.collections.FavoritesRepository
import com.odysee.app.core.data.collections.WatchLaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchLaterViewModel @Inject constructor(
    private val repository: WatchLaterRepository,
) : ViewModel() {
    val items: StateFlow<List<CollectionEntry>> = repository.items
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { repository.syncFromServer() }
    }

    fun remove(claimId: String) {
        viewModelScope.launch { repository.remove(claimId) }
    }
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: FavoritesRepository,
) : ViewModel() {
    val items: StateFlow<List<CollectionEntry>> = repository.items
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { repository.syncFromServer() }
    }

    fun remove(claimId: String) {
        viewModelScope.launch { repository.remove(claimId) }
    }
}

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val repository: com.odysee.app.core.data.collections.PlaylistsRepository,
) : ViewModel() {
    val playlists: StateFlow<List<com.odysee.app.core.data.collections.PlaylistSummary>> =
        repository.playlists.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun refresh() {
        viewModelScope.launch { repository.syncFromServer() }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteLocalPlaylist(id) }
    }

    fun setAutoPublish(id: String, enabled: Boolean) {
        viewModelScope.launch { repository.setAutoPublish(id, enabled) }
    }
}
