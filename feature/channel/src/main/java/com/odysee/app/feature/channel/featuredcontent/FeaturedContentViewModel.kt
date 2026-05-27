package com.odysee.app.feature.channel.featuredcontent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.model.Claim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeaturedContentState(
    val claimId: String,
    val name: String,
    val isLoading: Boolean = true,
    val claims: List<Claim> = emptyList(),
    val featuredUris: Set<String> = emptySet(),
    val initialFeatured: Set<String> = emptySet(),
    val isLoadingMore: Boolean = false,
    val page: Int = 0,
    val hasMore: Boolean = true,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FeaturedContentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val route: FeaturedContentRoute = savedStateHandle.toRoute()
    private val _state = MutableStateFlow(FeaturedContentState(claimId = route.claimId, name = route.name))
    val state: StateFlow<FeaturedContentState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val channel = runCatching { contentRepository.getChannel(route.claimId) }.getOrNull()
            val current = channel?.featuredUris.orEmpty().toSet()
            val firstPage = runCatching {
                contentRepository.getChannelVideos(route.claimId, page = 1, pageSize = 40)
            }.getOrNull().orEmpty()
            _state.update {
                it.copy(
                    isLoading = false,
                    claims = firstPage,
                    featuredUris = current,
                    initialFeatured = current,
                    page = 1,
                    hasMore = firstPage.size == 40,
                )
            }
        }
    }

    fun loadMore() {
        val st = _state.value
        if (st.isLoading || st.isLoadingMore || !st.hasMore) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            val next = st.page + 1
            val more = runCatching {
                contentRepository.getChannelVideos(route.claimId, page = next, pageSize = 40)
            }.getOrNull().orEmpty()
            _state.update {
                it.copy(
                    isLoadingMore = false,
                    claims = it.claims + more,
                    page = next,
                    hasMore = more.size == 40,
                )
            }
        }
    }

    fun toggle(claim: Claim) {
        val uri = claim.permanentUrl
        _state.update {
            val set = it.featuredUris
            val newSet = if (set.contains(uri)) set - uri else set + uri
            it.copy(featuredUris = newSet, saved = false)
        }
    }

    val hasChanges: Boolean
        get() = _state.value.featuredUris != _state.value.initialFeatured

    fun save() {
        val st = _state.value
        if (st.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val result = runCatching {
                contentRepository.updateChannel(
                    claimId = st.claimId,
                    featured = st.featuredUris.toList(),
                )
            }
            _state.update {
                if (result.isSuccess) it.copy(isSaving = false, saved = true, initialFeatured = it.featuredUris)
                else it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Save failed")
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
