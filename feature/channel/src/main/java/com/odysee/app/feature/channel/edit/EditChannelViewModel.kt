package com.odysee.app.feature.channel.edit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.odysee.app.core.data.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class EditChannelState(
    val claimId: String,
    val handle: String,
    val title: String = "",
    val description: String = "",
    val websiteUrl: String = "",
    val email: String = "",
    val tags: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
    val coverUrl: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingThumbnail: Boolean = false,
    val isUploadingCover: Boolean = false,
    val pendingCrop: PendingCrop? = null,
    val error: String? = null,
    val savedTxId: String? = null,
)

data class PendingCrop(
    val uri: Uri,
    val isCover: Boolean,
)

@HiltViewModel
class EditChannelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val application: Application,
) : ViewModel() {

    private val route: EditChannelRoute = savedStateHandle.toRoute()
    private val _state = MutableStateFlow(EditChannelState(claimId = route.claimId, handle = route.name))
    val state: StateFlow<EditChannelState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val channel = runCatching { contentRepository.getChannel(route.claimId) }.getOrNull()
            if (channel == null) {
                _state.update { it.copy(isLoading = false, error = "Couldn't load channel") }
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    title = channel.title.orEmpty(),
                    description = channel.description.orEmpty(),
                    websiteUrl = channel.websiteUrl.orEmpty(),
                    email = channel.email.orEmpty(),
                    tags = channel.tags,
                    languages = channel.languages,
                    thumbnailUrl = channel.thumbnailUrl,
                    coverUrl = channel.coverUrl,
                )
            }
        }
    }

    fun onTitle(v: String) = _state.update { it.copy(title = v) }
    fun onDescription(v: String) = _state.update { it.copy(description = v) }
    fun onWebsite(v: String) = _state.update { it.copy(websiteUrl = v) }
    fun onEmail(v: String) = _state.update { it.copy(email = v) }
    fun addTag(tag: String) {
        val clean = tag.trim().lowercase()
        if (clean.isBlank()) return
        _state.update { st ->
            if (st.tags.contains(clean)) st else st.copy(tags = st.tags + clean)
        }
    }
    fun removeTag(tag: String) = _state.update { it.copy(tags = it.tags - tag) }
    fun addLanguage(lang: String) {
        val clean = lang.trim()
        if (clean.isBlank()) return
        _state.update { st ->
            if (st.languages.contains(clean)) st else st.copy(languages = st.languages + clean)
        }
    }
    fun removeLanguage(lang: String) = _state.update { it.copy(languages = it.languages - lang) }

    fun pickThumbnail(uri: Uri) {
        _state.update { it.copy(pendingCrop = PendingCrop(uri = uri, isCover = false)) }
    }
    fun pickCover(uri: Uri) {
        _state.update { it.copy(pendingCrop = PendingCrop(uri = uri, isCover = true)) }
    }
    fun cancelCrop() = _state.update { it.copy(pendingCrop = null) }

    fun applyCroppedImage(bytes: ByteArray, isCover: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    pendingCrop = null,
                    isUploadingCover = if (isCover) true else it.isUploadingCover,
                    isUploadingThumbnail = if (!isCover) true else it.isUploadingThumbnail,
                )
            }
            val result = runCatching {
                val fileName = if (isCover) "cover.jpg" else "thumbnail.jpg"
                contentRepository.uploadImage(bytes, fileName, "image/jpeg")
            }
            result
                .onSuccess { url ->
                    _state.update {
                        if (isCover) it.copy(isUploadingCover = false, coverUrl = url)
                        else it.copy(isUploadingThumbnail = false, thumbnailUrl = url)
                    }
                }
                .onFailure { err ->
                    _state.update {
                        if (isCover) it.copy(isUploadingCover = false, error = "Cover upload: ${err.message}")
                        else it.copy(isUploadingThumbnail = false, error = "Thumbnail upload: ${err.message}")
                    }
                }
        }
    }

    fun save() {
        val st = _state.value
        if (st.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, savedTxId = null) }
            val result = runCatching {
                contentRepository.updateChannel(
                    claimId = st.claimId,
                    title = st.title.takeIf { it.isNotBlank() },
                    description = st.description.takeIf { it.isNotBlank() },
                    thumbnailUrl = st.thumbnailUrl,
                    coverUrl = st.coverUrl,
                    websiteUrl = st.websiteUrl.takeIf { it.isNotBlank() },
                    email = st.email.takeIf { it.isNotBlank() },
                    tags = st.tags,
                    languages = st.languages,
                    replace = true,
                )
            }
            result.onSuccess { tx ->
                _state.update { it.copy(isSaving = false, savedTxId = tx) }
            }.onFailure { err ->
                _state.update { it.copy(isSaving = false, error = err.message ?: "Save failed") }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
