package com.odysee.app.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.collections.CollectionEntry
import com.odysee.app.core.data.collections.FavoritesRepository
import com.odysee.app.core.data.collections.PlaylistsRepository
import com.odysee.app.core.data.collections.WatchLaterRepository
import com.odysee.app.core.data.history.WatchHistoryEntry
import com.odysee.app.core.data.history.WatchHistoryRepository
import com.odysee.app.core.data.subscriptions.Subscription
import com.odysee.app.core.data.subscriptions.SubscriptionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TvLibraryUiState(
    val watchLater: List<CollectionEntry> = emptyList(),
    val favorites: List<CollectionEntry> = emptyList(),
    val history: List<WatchHistoryEntry> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
)

@HiltViewModel
class TvLibraryViewModel @Inject constructor(
    watchLater: WatchLaterRepository,
    favorites: FavoritesRepository,
    private val historyRepo: WatchHistoryRepository,
    subscriptions: SubscriptionsRepository,
    playlists: PlaylistsRepository,
) : ViewModel() {
    val state: StateFlow<TvLibraryUiState> = combine(
        watchLater.items,
        favorites.items,
        historyRepo.history,
        subscriptions.subscriptions,
    ) { wl, fav, hist, subs ->
        TvLibraryUiState(
            watchLater = wl,
            favorites = fav,
            history = hist,
            subscriptions = subs,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TvLibraryUiState())

    fun clearHistory() = viewModelScope.launch {
        historyRepo.clear()
    }
}
