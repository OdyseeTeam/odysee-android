package com.odysee.app.account

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class NewChannelState(
    val handle: String = "",
    val title: String = "",
    val description: String = "",
    val websiteUrl: String = "",
    val email: String = "",
    val tags: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val bidLbc: String = "0.01",
    val thumbnailUrl: String? = null,
    val coverUrl: String? = null,
    val isUploadingThumbnail: Boolean = false,
    val isUploadingCover: Boolean = false,
    val isCreating: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean
        get() = handle.trim().isNotBlank() &&
            (bidLbc.toDoubleOrNull() ?: 0.0) > 0.0 &&
            !isCreating
}

@HiltViewModel
class NewChannelViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val application: Application,
) : ViewModel() {

    private val _state = MutableStateFlow(NewChannelState())
    val state: StateFlow<NewChannelState> = _state.asStateFlow()

    fun onHandle(v: String) {
        val cleaned = v.trim().removePrefix("@").filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        _state.update { it.copy(handle = cleaned) }
    }

    fun onTitle(v: String) = _state.update { it.copy(title = v) }
    fun onDescription(v: String) = _state.update { it.copy(description = v) }
    fun onWebsite(v: String) = _state.update { it.copy(websiteUrl = v) }
    fun onEmail(v: String) = _state.update { it.copy(email = v) }
    fun onBid(v: String) = _state.update {
        it.copy(bidLbc = v.filter { c -> c.isDigit() || c == '.' })
    }

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

    fun pickThumbnail(uri: Uri) = uploadFromUri(uri, isCover = false)
    fun pickCover(uri: Uri) = uploadFromUri(uri, isCover = true)

    private fun uploadFromUri(uri: Uri, isCover: Boolean) {
        viewModelScope.launch {
            _state.update { if (isCover) it.copy(isUploadingCover = true) else it.copy(isUploadingThumbnail = true) }
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

    fun submit() {
        val st = _state.value
        if (!st.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null) }
            val result = runCatching {
                contentRepository.createChannel(
                    name = st.handle,
                    bidLbc = st.bidLbc.toDoubleOrNull() ?: 0.01,
                    title = st.title.takeIf { it.isNotBlank() },
                    description = st.description.takeIf { it.isNotBlank() },
                    thumbnailUrl = st.thumbnailUrl,
                    coverUrl = st.coverUrl,
                    websiteUrl = st.websiteUrl.takeIf { it.isNotBlank() },
                    email = st.email.takeIf { it.isNotBlank() },
                    tags = st.tags,
                    languages = st.languages,
                )
            }
            result
                .onSuccess { _state.update { it.copy(isCreating = false, created = true) } }
                .onFailure { err ->
                    _state.update { it.copy(isCreating = false, error = err.message ?: "Channel creation failed") }
                }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
