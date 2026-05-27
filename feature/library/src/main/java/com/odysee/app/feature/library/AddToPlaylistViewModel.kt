package com.odysee.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.collections.CollectionEntry
import com.odysee.app.core.data.collections.FavoritesRepository
import com.odysee.app.core.data.collections.PlaylistDraft
import com.odysee.app.core.data.collections.PlaylistSummary
import com.odysee.app.core.data.collections.PlaylistsRepository
import com.odysee.app.core.data.collections.WatchLaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuickTargetClaim(
    val claimId: String,
    val permanentUrl: String,
    val title: String,
    val channelName: String,
    val channelClaimId: String?,
    val thumbnailUrl: String?,
)

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val repository: PlaylistsRepository,
    private val watchLaterRepository: WatchLaterRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistSummary>> =
        repository.playlists.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val watchLaterIds: StateFlow<Set<String>> =
        watchLaterRepository.items.map { list -> list.map { it.claimId }.toSet() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val favoriteIds: StateFlow<Set<String>> =
        favoritesRepository.items.map { list -> list.map { it.claimId }.toSet() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        viewModelScope.launch { repository.syncFromServer() }
    }

    fun toggle(playlistId: String, permanentUrl: String, alreadyIn: Boolean) {
        viewModelScope.launch {
            if (alreadyIn) repository.removeItem(playlistId, permanentUrl)
            else repository.addItem(playlistId, permanentUrl)
        }
    }

    fun createAndAdd(name: String, permanentUrl: String) {
        viewModelScope.launch {
            val id = repository.upsertLocalPlaylist(PlaylistDraft(name = name))
            repository.addItem(id, permanentUrl)
        }
    }

    fun toggleWatchLater(claim: QuickTargetClaim) {
        viewModelScope.launch {
            watchLaterRepository.toggle(claim.toEntry())
        }
    }

    fun toggleFavorite(claim: QuickTargetClaim) {
        viewModelScope.launch {
            favoritesRepository.toggle(claim.toEntry())
        }
    }

    private fun QuickTargetClaim.toEntry(): CollectionEntry = CollectionEntry(
        claimId = claimId,
        permanentUrl = permanentUrl,
        title = title,
        channelName = channelName,
        channelClaimId = channelClaimId,
        thumbnailUrl = thumbnailUrl,
        addedAt = System.currentTimeMillis(),
    )
}
