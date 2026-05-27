package com.odysee.app.feature.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.collections.PlaylistDraft
import com.odysee.app.core.data.collections.PlaylistsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PlaylistEditState(
    val id: String? = null,
    val name: String = "",
    val description: String = "",
    val thumbnailUrl: String? = null,
    val tags: List<String> = emptyList(),
    val isPublic: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingThumbnail: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PlaylistEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistsRepository: PlaylistsRepository,
    private val contentRepository: ContentRepository,
    private val application: Application,
) : ViewModel() {

    private val route: PlaylistEditRoute = savedStateHandle.toRoute()

    private val _state = MutableStateFlow(PlaylistEditState(id = route.id, isLoading = route.id != null))
    val state: StateFlow<PlaylistEditState> = _state.asStateFlow()

    init {
        route.id?.let { id ->
            viewModelScope.launch {
                val existing = playlistsRepository.getLocalPlaylist(id)
                if (existing != null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            id = existing.id,
                            name = existing.name,
                            description = existing.description.orEmpty(),
                            thumbnailUrl = existing.thumbnailUrl,
                            tags = existing.tags,
                            isPublic = existing.isPublic,
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v) }
    fun onDescription(v: String) = _state.update { it.copy(description = v) }
    fun setPublic(v: Boolean) = _state.update { it.copy(isPublic = v) }

    fun addTag(tag: String) {
        val clean = tag.trim().lowercase()
        if (clean.isBlank()) return
        _state.update { st ->
            if (st.tags.contains(clean)) st else st.copy(tags = st.tags + clean)
        }
    }

    fun removeTag(tag: String) = _state.update { it.copy(tags = it.tags - tag) }

    fun pickThumbnail(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingThumbnail = true) }
            val resolver = application.contentResolver
            val result = runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: error("Couldn't read image")
                val mime = resolver.getType(uri) ?: "image/jpeg"
                val fileName = (uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
                    ?: "image.jpg").ifBlank { "image.jpg" }
                contentRepository.uploadImage(bytes, fileName, mime)
            }
            result
                .onSuccess { url -> _state.update { it.copy(isUploadingThumbnail = false, thumbnailUrl = url) } }
                .onFailure { err ->
                    _state.update {
                        it.copy(isUploadingThumbnail = false, error = "Thumbnail upload: ${err.message}")
                    }
                }
        }
    }

    fun clearThumbnail() {
        _state.update { it.copy(thumbnailUrl = null) }
    }

    fun save() {
        val st = _state.value
        if (st.isSaving) return
        if (st.name.trim().isBlank()) {
            _state.update { it.copy(error = "Give it a name first.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val result = runCatching {
                playlistsRepository.upsertLocalPlaylist(
                    PlaylistDraft(
                        id = st.id,
                        name = st.name,
                        description = st.description,
                        thumbnailUrl = st.thumbnailUrl,
                        tags = st.tags,
                        isPublic = st.isPublic,
                    ),
                )
            }
            result.onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { err -> _state.update { it.copy(isSaving = false, error = err.message ?: "Save failed") } }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
