package com.odysee.app.upload

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

enum class PostStep { Post, Content, Visibility, Publish }

data class PostFormState(
    val body: String = "",
    val title: String = "",
    val urlSlug: String = "",
    val urlEditedManually: Boolean = false,
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
    val visibility: UploadVisibility = UploadVisibility.Public,
    val scheduledTimestamp: Long? = null,
    val showOnUpcoming: Boolean = true,
    val paywall: UploadPaywall = UploadPaywall.Free,
    val lbcAmount: String = "1",
    val fiatPurchaseEnabled: Boolean = false,
    val fiatPurchaseAmount: String = "1.00",
    val fiatRentalEnabled: Boolean = false,
    val fiatRentalAmount: String = "1.00",
    val fiatRentalDuration: String = "1",
    val fiatRentalDurationUnit: RentalDurationUnit = RentalDurationUnit.Days,
    val bid: String = "0.001",
    val membersOnly: Boolean = false,
    val step: PostStep = PostStep.Post,
    val isPublishing: Boolean = false,
    val uploadProgress: Float = 0f,
    val publishSucceeded: Boolean = false,
    val errorMessage: String? = null,
) {
    val isPostStepValid: Boolean
        get() = title.isNotBlank() && urlSlug.isNotBlank() && body.trim().isNotBlank()
    val isContentStepValid: Boolean get() = thumbnailUrl.isNotBlank()
    val isVisibilityStepValid: Boolean
        get() {
            if (visibility == UploadVisibility.Scheduled && scheduledTimestamp == null) return false
            if (paywall == UploadPaywall.Lbc && (lbcAmount.toDoubleOrNull() ?: 0.0) <= 0.0) return false
            if (paywall == UploadPaywall.Fiat) {
                if (!fiatPurchaseEnabled && !fiatRentalEnabled) return false
                if (fiatPurchaseEnabled && (fiatPurchaseAmount.toDoubleOrNull() ?: 0.0) <= 0.0) return false
                if (fiatRentalEnabled) {
                    if ((fiatRentalAmount.toDoubleOrNull() ?: 0.0) <= 0.0) return false
                    if ((fiatRentalDuration.toIntOrNull() ?: 0) <= 0) return false
                }
            }
            return true
        }
    val canPublish: Boolean
        get() = isPostStepValid && isContentStepValid && isVisibilityStepValid &&
            !channelClaimId.isNullOrBlank() && (bid.toDoubleOrNull() ?: 0.0) > 0.0
}

@HiltViewModel
class PostFormViewModel @Inject constructor(
    private val application: Application,
    private val okHttpClient: OkHttpClient,
    private val contentRepository: com.odysee.app.core.data.ContentRepository,
    private val authRepository: com.odysee.app.core.data.auth.AuthRepository,
    private val uploadManager: com.odysee.app.core.data.publish.UploadManager,
) : ViewModel() {

    private val _state = MutableStateFlow(PostFormState())
    val state: StateFlow<PostFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            uploadManager.job.collect { job ->
                if (job == null) return@collect
                val pct = if (job.totalBytes > 0)
                    (job.uploadedBytes.toFloat() / job.totalBytes.toFloat()).coerceIn(0f, 1f)
                else 0f
                when (job.status) {
                    com.odysee.app.core.data.publish.UploadStatus.Running ->
                        _state.update { it.copy(isPublishing = true, uploadProgress = pct, errorMessage = null) }
                    com.odysee.app.core.data.publish.UploadStatus.Completed ->
                        _state.update { it.copy(isPublishing = false, uploadProgress = 1f, publishSucceeded = true) }
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

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun onBodyChange(v: String) {
        _state.update { it.copy(body = v) }
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

    fun uploadThumbnailBytes(bytes: ByteArray, name: String, mime: String = "image/jpeg") {
        viewModelScope.launch { uploadImageBytes { Triple(bytes, mime, name) } }
    }

    private suspend fun uploadImageBytes(provide: () -> Triple<ByteArray, String, String>) {
        _state.update { it.copy(isUploadingThumbnail = true, thumbnailError = null) }
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val (bytes, mime, fileName) = provide()
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file-input",
                        fileName,
                        bytes.toRequestBody(mime.toMediaType()),
                    )
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
        _state.update { it.copy(channelClaimId = id) }
    }

    fun onVisibilityChange(v: UploadVisibility) {
        _state.update { s ->
            val timestamp = if (v == UploadVisibility.Scheduled && s.scheduledTimestamp == null)
                System.currentTimeMillis() + 30 * 60 * 1000L
            else s.scheduledTimestamp
            val paywall = if (v != UploadVisibility.Public) UploadPaywall.Free else s.paywall
            s.copy(visibility = v, scheduledTimestamp = timestamp, paywall = paywall)
        }
    }

    fun onScheduledTimestampChange(ts: Long) {
        _state.update { it.copy(scheduledTimestamp = ts) }
    }

    fun onShowOnUpcomingChange(v: Boolean) {
        _state.update { it.copy(showOnUpcoming = v) }
    }

    fun onPaywallChange(p: UploadPaywall) {
        _state.update { it.copy(paywall = p) }
    }

    fun onLbcAmountChange(v: String) {
        _state.update { it.copy(lbcAmount = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    }

    fun onFiatPurchaseEnabledChange(v: Boolean) {
        _state.update { it.copy(fiatPurchaseEnabled = v) }
    }

    fun onFiatPurchaseAmountChange(v: String) {
        _state.update { it.copy(fiatPurchaseAmount = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    }

    fun onFiatRentalEnabledChange(v: Boolean) {
        _state.update { it.copy(fiatRentalEnabled = v) }
    }

    fun onFiatRentalAmountChange(v: String) {
        _state.update { it.copy(fiatRentalAmount = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    }

    fun onFiatRentalDurationChange(v: String) {
        _state.update { it.copy(fiatRentalDuration = v.filter { ch -> ch.isDigit() }) }
    }

    fun onFiatRentalDurationUnitChange(unit: RentalDurationUnit) {
        _state.update { it.copy(fiatRentalDurationUnit = unit) }
    }

    fun onMembersOnlyChange(v: Boolean) {
        _state.update { it.copy(membersOnly = v) }
    }

    fun goToStep(step: PostStep) {
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

        _state.update { it.copy(isPublishing = true, errorMessage = null, uploadProgress = 0f) }
        val bid = current.bid.toDoubleOrNull()?.let {
            "%.4f".format(it).trimEnd('0').trimEnd('.')
        } ?: "0.001"
        val params = com.odysee.app.core.data.publish.PublishParams(
            name = current.urlSlug.ifBlank { slugify(current.title) },
            title = current.title,
            description = current.body.takeIf { it.isNotBlank() },
            bid = bid,
            channelId = current.channelClaimId,
            thumbnailUrl = current.thumbnailUrl.takeIf { it.isNotBlank() },
            tags = current.tags,
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
            feeAmountLbc = if (current.paywall == UploadPaywall.Lbc) current.lbcAmount else null,
            fiatPurchaseUsd = if (current.paywall == UploadPaywall.Fiat && current.fiatPurchaseEnabled)
                current.fiatPurchaseAmount else null,
            fiatRentalUsd = if (current.paywall == UploadPaywall.Fiat && current.fiatRentalEnabled)
                current.fiatRentalAmount else null,
            fiatRentalSeconds = if (current.paywall == UploadPaywall.Fiat && current.fiatRentalEnabled)
                (current.fiatRentalDuration.toLongOrNull() ?: 0L) *
                    when (current.fiatRentalDurationUnit) {
                        RentalDurationUnit.Hours -> 3600L
                        RentalDurationUnit.Days -> 86_400L
                        RentalDurationUnit.Weeks -> 7L * 86_400L
                        RentalDurationUnit.Months -> 30L * 86_400L
                    }
            else null,
        )
        return try {
            val tempFile = withContext(Dispatchers.IO) {
                val f = java.io.File.createTempFile("post_", ".md", application.cacheDir)
                f.writeText(current.body)
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
