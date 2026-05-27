package com.odysee.app.feature.channel.featuredchannels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.featured.FeaturedChannelSection
import com.odysee.app.core.model.Channel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class FeaturedChannelsEditState(
    val channelClaimId: String,
    val channelName: String,
    val isLoading: Boolean = true,
    val sections: List<FeaturedChannelSection> = emptyList(),
    val resolved: Map<String, Channel> = emptyMap(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Channel> = emptyList(),
    val isSearching: Boolean = false,
    val activeSectionId: String? = null,
)

@HiltViewModel
class FeaturedChannelsEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val route: FeaturedChannelsEditRoute = savedStateHandle.toRoute()
    private val _state = MutableStateFlow(
        FeaturedChannelsEditState(channelClaimId = route.claimId, channelName = route.name),
    )
    val state: StateFlow<FeaturedChannelsEditState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val sections = runCatching {
                contentRepository.getFeaturedChannelSections(route.claimId)
            }.getOrNull().orEmpty()
            val ids = sections.flatMap { it.uris }.mapNotNull { extractClaimId(it) }.distinct()
            val resolved = if (ids.isEmpty()) emptyList() else runCatching {
                contentRepository.getChannels(ids)
            }.getOrNull().orEmpty()
            _state.update {
                it.copy(
                    isLoading = false,
                    sections = sections.ifEmpty {
                        listOf(FeaturedChannelSection(id = UUID.randomUUID().toString(), title = "Featured channels", uris = emptyList()))
                    },
                    resolved = resolved.associateBy { c -> c.claimId },
                    activeSectionId = sections.firstOrNull()?.id ?: UUID.randomUUID().toString(),
                )
            }
        }
    }

    fun setActiveSection(id: String) = _state.update { it.copy(activeSectionId = id) }

    fun addSection() {
        _state.update {
            val newId = UUID.randomUUID().toString()
            it.copy(
                sections = it.sections + FeaturedChannelSection(
                    id = newId,
                    title = "Featured channels",
                    uris = emptyList(),
                ),
                activeSectionId = newId,
            )
        }
    }

    fun renameSection(id: String, name: String) {
        _state.update {
            it.copy(
                sections = it.sections.map { s -> if (s.id == id) s.copy(title = name) else s },
            )
        }
    }

    fun deleteSection(id: String) {
        _state.update {
            val remaining = it.sections.filterNot { s -> s.id == id }
            it.copy(
                sections = remaining,
                activeSectionId = remaining.firstOrNull()?.id,
            )
        }
    }

    fun removeChannelFromActiveSection(uri: String) {
        val activeId = _state.value.activeSectionId ?: return
        _state.update {
            it.copy(
                sections = it.sections.map { s ->
                    if (s.id == activeId) s.copy(uris = s.uris.filterNot { u -> u == uri }) else s
                },
            )
        }
    }

    fun addChannelToActiveSection(channel: Channel) {
        val activeId = _state.value.activeSectionId ?: return
        val uri = channel.permanentUrl
        _state.update {
            it.copy(
                sections = it.sections.map { s ->
                    if (s.id == activeId && !s.uris.contains(uri)) s.copy(uris = s.uris + uri) else s
                },
                resolved = it.resolved + (channel.claimId to channel),
            )
        }
    }

    fun search(query: String) {
        _state.update { it.copy(searchQuery = query) }
        if (query.trim().length < 2) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            val results = runCatching {
                contentRepository.search(query.trim(), size = 12)
            }.getOrNull().orEmpty()
            val channels = results
                .filter { it.signingChannel == null }
                .mapNotNull { c ->
                    Channel(
                        claimId = c.claimId,
                        name = c.name,
                        title = c.title,
                        description = null,
                        thumbnailUrl = c.thumbnailUrl,
                        coverUrl = null,
                        permanentUrl = c.permanentUrl ?: "",
                        canonicalUrl = null,
                        tags = emptyList(),
                        languages = emptyList(),
                        email = null,
                        websiteUrl = null,
                        stakedAmount = 0.0,
                        claimsInChannel = null,
                        creationTimestamp = null,
                        modifiedAt = null,
                    )
                }
            _state.update { it.copy(searchResults = channels, isSearching = false) }
        }
    }

    fun save() {
        val st = _state.value
        if (st.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val result = runCatching {
                contentRepository.updateFeaturedChannelSections(
                    channelClaimId = st.channelClaimId,
                    channelName = st.channelName,
                    sections = st.sections,
                )
            }
            _state.update {
                if (result.isSuccess) it.copy(isSaving = false, saved = true)
                else it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Save failed")
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun extractClaimId(uri: String): String? {
        val hash = uri.lastIndexOf('#')
        if (hash < 0 || hash >= uri.length - 1) return null
        return uri.substring(hash + 1).takeWhile { it.isLetterOrDigit() }.takeIf { it.isNotBlank() }
    }
}
