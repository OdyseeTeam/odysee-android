package com.odysee.app.upload

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.network.SdkProxyApi
import com.odysee.app.core.network.dto.ChannelSignParams
import com.odysee.app.core.network.jsonrpc.JsonRpcRequest
import com.odysee.app.core.network.jsonrpc.unwrap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

enum class LivestreamStep { Details, Visibility, Publish, Ready }

data class LivestreamFormState(
    val title: String = "",
    val urlSlug: String = "",
    val urlEditedManually: Boolean = false,
    val description: String = "",
    val thumbnailUrl: String = "",
    val isUploadingThumbnail: Boolean = false,
    val thumbnailError: String? = null,
    val tags: List<String> = emptyList(),
    val tagSearch: String = "",
    val language: String = "en",
    val license: UploadLicense = UploadLicense.None,
    val licenseDescription: String = "",
    val licenseUrl: String = "",
    val channelClaimId: String? = null,
    val channelName: String? = null,
    val visibility: UploadVisibility = UploadVisibility.Public,
    val scheduledTimestamp: Long? = null,
    val showOnUpcoming: Boolean = true,
    val bid: String = "0.001",
    val membersOnly: Boolean = false,
    val step: LivestreamStep = LivestreamStep.Details,
    val isPublishing: Boolean = false,
    val publishSucceeded: Boolean = false,
    val errorMessage: String? = null,
    val streamKey: String? = null,
    val rtmpUrl: String = "rtmp://publish.odysee.live/live",
) {
    val isDetailsStepValid: Boolean
        get() = title.isNotBlank() && urlSlug.isNotBlank() && thumbnailUrl.isNotBlank()
    val isVisibilityStepValid: Boolean
        get() = !(visibility == UploadVisibility.Scheduled && scheduledTimestamp == null)
    val canPublish: Boolean
        get() = isDetailsStepValid && isVisibilityStepValid &&
            !channelClaimId.isNullOrBlank() && (bid.toDoubleOrNull() ?: 0.0) > 0.0
}

@HiltViewModel
class LivestreamFormViewModel @Inject constructor(
    private val application: Application,
    private val okHttpClient: OkHttpClient,
    private val contentRepository: com.odysee.app.core.data.ContentRepository,
    private val authRepository: com.odysee.app.core.data.auth.AuthRepository,
    private val sdkProxyApi: SdkProxyApi,
    private val uploadManager: com.odysee.app.core.data.publish.UploadManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LivestreamFormState())
    val state: StateFlow<LivestreamFormState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        viewModelScope.launch {
            uploadManager.job.collect { job ->
                if (job == null) return@collect
                when (job.status) {
                    com.odysee.app.core.data.publish.UploadStatus.Running ->
                        _state.update { it.copy(isPublishing = true, errorMessage = null) }
                    com.odysee.app.core.data.publish.UploadStatus.Completed -> {
                        val current = _state.value
                        val cid = current.channelClaimId
                        val cname = current.channelName
                        val key = if (cid != null) runCatching { fetchStreamKey(cid, cname) }.getOrNull() else null
                        _state.update {
                            it.copy(
                                isPublishing = false,
                                publishSucceeded = true,
                                streamKey = key,
                                step = LivestreamStep.Ready,
                            )
                        }
                    }
                    com.odysee.app.core.data.publish.UploadStatus.Failed ->
                        _state.update {
                            it.copy(
                                isPublishing = false,
                                errorMessage = job.errorMessage ?: "Publish failed",
                            )
                        }
                    com.odysee.app.core.data.publish.UploadStatus.Idle -> Unit
                }
            }
        }
    }

    fun onTitleChange(v: String) {
        _state.update { s ->
            val newSlug = if (!s.urlEditedManually) slugify(v) else s.urlSlug
            s.copy(title = v.take(200), urlSlug = newSlug)
        }
    }

    fun onUrlSlugChange(v: String) {
        _state.update { it.copy(urlSlug = sanitizeSlug(v), urlEditedManually = true) }
    }

    fun onDescriptionChange(v: String) {
        _state.update { it.copy(description = v) }
    }

    fun onThumbnailChange(v: String) {
        _state.update { it.copy(thumbnailUrl = v, thumbnailError = null) }
    }

    fun uploadThumbnail(uri: String) {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return
        viewModelScope.launch {
            uploadImageBytes {
                val resolver = application.contentResolver
                val bytes = resolver.openInputStream(parsed)?.use { it.readBytes() }
                    ?: error("Couldn't read image")
                val mime = resolver.getType(parsed) ?: "image/*"
                val fileName = parsed.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
                    ?: "thumbnail.jpg"
                Triple(bytes, mime, fileName)
            }
        }
    }

    private suspend fun uploadImageBytes(provide: () -> Triple<ByteArray, String, String>) {
        _state.update { it.copy(isUploadingThumbnail = true, thumbnailError = null) }
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val (bytes, mime, fileName) = provide()
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file-input", fileName, bytes.toRequestBody(mime.toMediaType()))
                    .addFormDataPart("name", fileName.substringBeforeLast('.', fileName))
                    .build()
                val request = Request.Builder()
                    .url("https://thumbs.odycdn.com/upload")
                    .post(body)
                    .build()
                okHttpClient.newCall(request).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) error("HTTP ${resp.code}: $text")
                    val obj = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
                        ?: error("Unexpected response")
                    if (obj["type"]?.jsonPrimitive?.contentOrNull != "success") {
                        error(obj["message"]?.jsonPrimitive?.contentOrNull ?: "Upload failed")
                    }
                    obj["message"]?.jsonPrimitive?.contentOrNull
                        ?: error("Missing URL in response")
                }
            }
        }
        result.fold(
            onSuccess = { url ->
                _state.update { it.copy(thumbnailUrl = url, isUploadingThumbnail = false) }
            },
            onFailure = { e ->
                _state.update {
                    it.copy(
                        isUploadingThumbnail = false,
                        thumbnailError = e.message ?: "Thumbnail upload failed",
                    )
                }
            },
        )
    }

    fun addTag(tag: String) {
        val clean = tag.trim().lowercase()
        if (clean.isBlank()) return
        _state.update { s ->
            if (s.tags.contains(clean) || s.tags.size >= 5) s.copy(tagSearch = "")
            else s.copy(tags = s.tags + clean, tagSearch = "")
        }
    }

    fun removeTag(tag: String) {
        _state.update { it.copy(tags = it.tags - tag) }
    }

    fun onTagSearchChange(v: String) {
        _state.update { it.copy(tagSearch = v) }
    }

    fun onLanguageChange(v: String) {
        _state.update { it.copy(language = v) }
    }

    fun onLicenseChange(v: UploadLicense) {
        _state.update { it.copy(license = v) }
    }

    fun onLicenseDescriptionChange(v: String) {
        _state.update { it.copy(licenseDescription = v) }
    }

    fun onLicenseUrlChange(v: String) {
        _state.update { it.copy(licenseUrl = v) }
    }

    fun onSelectChannel(id: String) {
        val ch = (authRepository.state.value as? com.odysee.app.core.data.auth.AuthState.SignedIn)
            ?.channels?.firstOrNull { it.claimId == id }
        _state.update { it.copy(channelClaimId = id, channelName = ch?.name) }
    }

    fun onVisibilityChange(v: UploadVisibility) {
        _state.update { s ->
            val timestamp = if (v == UploadVisibility.Scheduled && s.scheduledTimestamp == null)
                System.currentTimeMillis() + 30 * 60 * 1000L
            else s.scheduledTimestamp
            s.copy(visibility = v, scheduledTimestamp = timestamp)
        }
    }

    fun onScheduledTimestampChange(ts: Long) {
        _state.update { it.copy(scheduledTimestamp = ts) }
    }

    fun onShowOnUpcomingChange(v: Boolean) {
        _state.update { it.copy(showOnUpcoming = v) }
    }

    fun onMembersOnlyChange(v: Boolean) {
        _state.update { it.copy(membersOnly = v) }
    }

    fun goToStep(step: LivestreamStep) {
        _state.update { it.copy(step = step) }
    }

    suspend fun publish(): Boolean {
        val current = _state.value
        if (!current.canPublish) {
            _state.update { it.copy(errorMessage = "Fill in the required fields") }
            return false
        }
        val authState = authRepository.state.value
        val authToken = when (authState) {
            is com.odysee.app.core.data.auth.AuthState.SignedIn -> authState.authToken
            is com.odysee.app.core.data.auth.AuthState.Anonymous -> authState.authToken
            else -> null
        }
        if (authToken.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "Not signed in.") }
            return false
        }
        _state.update { it.copy(isPublishing = true, errorMessage = null) }
        val bid = current.bid.toDoubleOrNull()?.let {
            "%.4f".format(it).trimEnd('0').trimEnd('.')
        } ?: "0.001"
        val params = com.odysee.app.core.data.publish.PublishParams(
            name = current.urlSlug.ifBlank { slugify(current.title) },
            title = current.title,
            description = current.description.takeIf { it.isNotBlank() },
            bid = bid,
            channelId = current.channelClaimId,
            thumbnailUrl = current.thumbnailUrl.takeIf { it.isNotBlank() },
            tags = current.tags + "c:scheduled-livestream",
            languages = listOf(current.language),
            license = current.license.display.takeIf { current.license != UploadLicense.None },
            releaseTime = current.scheduledTimestamp?.let { it / 1000 },
            visibility = when (current.visibility) {
                UploadVisibility.Public -> com.odysee.app.core.data.publish.PublishVisibility.Public
                UploadVisibility.Unlisted -> com.odysee.app.core.data.publish.PublishVisibility.Unlisted
                UploadVisibility.Scheduled -> com.odysee.app.core.data.publish.PublishVisibility.Scheduled
                UploadVisibility.Private -> com.odysee.app.core.data.publish.PublishVisibility.Private
            },
            scheduledShow = current.showOnUpcoming,
            membersOnly = current.membersOnly,
        )
        return try {
            // 1-byte placeholder file; livestream claims have no real media yet.
            val tempFile = withContext(Dispatchers.IO) {
                val f = java.io.File.createTempFile("live_", ".bin", application.cacheDir)
                f.writeBytes(byteArrayOf(0))
                f
            }
            UploadForegroundService.start(
                context = application,
                jobId = java.util.UUID.randomUUID().toString(),
                title = current.title,
                filePath = tempFile.absolutePath,
                authToken = authToken,
                params = params,
            )
            true
        } catch (t: Throwable) {
            _state.update {
                it.copy(isPublishing = false, errorMessage = t.message ?: "Failed to start upload")
            }
            false
        }
    }

    private suspend fun fetchStreamKey(channelId: String, channelName: String?): String? {
        val hex = (channelName ?: return null).toByteArray(Charsets.UTF_8).joinToString("") {
            "%02x".format(it)
        }
        val signed = runCatching {
            sdkProxyApi.channelSign(
                JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId, hex)),
            ).unwrap()
        }.getOrNull() ?: return null
        val signature = signed.signature ?: return null
        val ts = signed.signingTs ?: return null
        return "$channelId?d=$hex&s=$signature&t=$ts"
    }

    private fun slugify(text: String): String {
        if (text.isBlank()) return ""
        val cleaned = text.trim().lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
        return cleaned.take(100)
    }

    private fun sanitizeSlug(text: String): String =
        text.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(100)
}
