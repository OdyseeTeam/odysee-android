package com.odysee.app.core.data.settings

import com.odysee.app.core.data.preferences.SharedPrefSlices
import com.odysee.app.core.data.preferences.SharedPreferencesStore
import com.odysee.app.core.datastore.AuthPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors the user's client settings between Android's [AuthPreferences]
 * DataStore and the LBRY shared-preferences "settings" slice that the web
 * (odysee-frontend) writes via `preference_set`. The web's
 * `CLIENT_SYNC_KEYS` list (see ui/constants/shared_preferences.ts) defines
 * exactly which keys participate in cross-device sync.
 *
 * Sync rules:
 * - On login (`syncFromServer`), values from the server overwrite local
 *   ones for every key present in the slice. Missing keys keep their local
 *   value (so a fresh signin doesn't wipe defaults that haven't been
 *   touched on web).
 * - Every local mutation to a synced key triggers a debounced push so
 *   changes propagate back to the server without spamming preference_set.
 */
@Singleton
class ClientSettingsRepository @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val sharedStore: SharedPreferencesStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pushMutex = Mutex()

    private val _syncedOnce = MutableStateFlow(false)
    val syncedOnce = _syncedOnce.asStateFlow()

    /**
     * Pull the `settings` slice and apply each known key into AuthPreferences.
     * Returns the number of keys applied. Safe to call multiple times.
     */
    suspend fun syncFromServer(): Result<Int> = runCatching {
        val slice = sharedStore.readSlice(
            SharedPrefSlices.SETTINGS,
            JsonObject.serializer(),
        ) ?: run {
            _syncedOnce.value = true
            return@runCatching 0
        }
        applyServerSettings(slice).also { _syncedOnce.value = true }
    }

    private suspend fun applyServerSettings(slice: JsonObject): Int {
        var count = 0
        fun bool(webKey: String): Boolean? = slice[webKey]?.asBoolOrNull()
        fun str(webKey: String): String? = slice[webKey]?.asStringOrNull()

        bool(W_SHOW_MATURE)?.let { authPreferences.setShowMature(it); count++ }
        bool(W_AUTOPLAY_NEXT)?.let { authPreferences.setAutoplay(it); count++ }
        bool(W_AUTOPLAY_MEDIA)?.let { authPreferences.setAutoplayMedia(it); count++ }
        bool(W_AUTOPLAY_NEXT_SHORTS)?.let { authPreferences.setAutoplayNextShort(it); count++ }
        bool(W_HIDE_SHORTS)?.let { authPreferences.setHideShorts(it); count++ }
        bool(W_HIDE_REPOSTS)?.let { authPreferences.setHideReposts(it); count++ }
        bool(W_HIDE_MEMBERS_ONLY)?.let { authPreferences.setHideMembersOnly(it); count++ }
        bool(W_HIDE_SCHEDULED_LIVESTREAMS)?.let { authPreferences.setHideLivestreams(it); count++ }
        bool(W_HIDE_BALANCE)?.let { authPreferences.setHideWalletBalance(it); count++ }
        bool(W_HIDE_TITLE_NOTIFICATION_COUNT)?.let { authPreferences.setHideNotificationCount(it); count++ }
        bool(W_FLOATING_PLAYER)?.let { authPreferences.setFloatingPlayer(it); count++ }
        bool(W_DISABLE_SHORTS_VIEW)?.let { authPreferences.setDisableShortsView(it); count++ }
        bool(W_CLOCK_24H)?.let { authPreferences.setClock24h(it); count++ }
        bool(W_SEARCH_IN_LANGUAGE)?.let { authPreferences.setSearchInLanguage(it); count++ }
        // Web stores `instant_purchase_enabled`; we model the inverse as
        // `purchase_tip_confirmation` (show confirmation = !instant).
        bool(W_INSTANT_PURCHASE_ENABLED)?.let { authPreferences.setPurchaseTipConfirmation(!it); count++ }
        str(W_THEME)?.let { authPreferences.setThemePreference(it); count++ }
        str(W_LANGUAGE)?.let { authPreferences.setLanguageOverride(it); count++ }
        str(W_DEFAULT_VIDEO_QUALITY)?.let { authPreferences.setDefaultVideoQuality(it); count++ }
        str(W_DEFAULT_COLLECTION_ACTION)?.let {
            // Web's redux key prefixes the value: "defaultCollectionActionView"
            // / "defaultCollectionActionPlay". Android stores "view"/"play"
            // (matching its picker option keys). Map both directions.
            val normalized = when (it) {
                "defaultCollectionActionView" -> "view"
                "defaultCollectionActionPlay" -> "play"
                else -> it
            }
            authPreferences.setDefaultPlaylistAction(normalized); count++
        }
        str(W_ACTIVE_CHANNEL_CLAIM)?.let { authPreferences.setActiveChannelClaimId(it); count++ }
        return count
    }

    /** Build the JSON object we'd push for the current local state. */
    private suspend fun snapshot(): JsonObject = buildJsonObject {
        put(W_SHOW_MATURE, JsonPrimitive(authPreferences.showMature.first()))
        put(W_AUTOPLAY_NEXT, JsonPrimitive(authPreferences.autoplay.first()))
        put(W_AUTOPLAY_MEDIA, JsonPrimitive(authPreferences.autoplayMedia.first()))
        put(W_AUTOPLAY_NEXT_SHORTS, JsonPrimitive(authPreferences.autoplayNextShort.first()))
        put(W_HIDE_SHORTS, JsonPrimitive(authPreferences.hideShorts.first()))
        put(W_HIDE_REPOSTS, JsonPrimitive(authPreferences.hideReposts.first()))
        put(W_HIDE_MEMBERS_ONLY, JsonPrimitive(authPreferences.hideMembersOnly.first()))
        put(W_HIDE_SCHEDULED_LIVESTREAMS, JsonPrimitive(authPreferences.hideLivestreams.first()))
        put(W_HIDE_BALANCE, JsonPrimitive(authPreferences.hideWalletBalance.first()))
        put(W_HIDE_TITLE_NOTIFICATION_COUNT, JsonPrimitive(authPreferences.hideNotificationCount.first()))
        put(W_FLOATING_PLAYER, JsonPrimitive(authPreferences.floatingPlayer.first()))
        put(W_DISABLE_SHORTS_VIEW, JsonPrimitive(authPreferences.disableShortsView.first()))
        put(W_CLOCK_24H, JsonPrimitive(authPreferences.clock24h.first()))
        put(W_SEARCH_IN_LANGUAGE, JsonPrimitive(authPreferences.searchInLanguage.first()))
        put(W_INSTANT_PURCHASE_ENABLED, JsonPrimitive(!authPreferences.purchaseTipConfirmation.first()))
        put(W_THEME, JsonPrimitive(authPreferences.themePreference.first()))
        authPreferences.languageOverride.first()?.let { put(W_LANGUAGE, JsonPrimitive(it)) }
        put(W_DEFAULT_VIDEO_QUALITY, JsonPrimitive(authPreferences.defaultVideoQuality.first()))
        // Local "view"/"play" → web's prefixed enum string (see syncFromServer).
        put(W_DEFAULT_COLLECTION_ACTION, JsonPrimitive(
            when (authPreferences.defaultPlaylistAction.first()) {
                "view" -> "defaultCollectionActionView"
                "play" -> "defaultCollectionActionPlay"
                else -> authPreferences.defaultPlaylistAction.first()
            }
        ))
        authPreferences.activeChannelClaimId.first()?.let { put(W_ACTIVE_CHANNEL_CLAIM, JsonPrimitive(it)) }
    }

    suspend fun pushNow() = pushMutex.withLock {
        val current = snapshot()
        sharedStore.patch(mapOf(SharedPrefSlices.SETTINGS to current))
    }

    /**
     * Wire change-detection on every synced AuthPreferences flow. Only call
     * once (e.g. from AuthRepository init). Drops the first emission of each
     * flow so we don't push on app start; debounces so a flurry of toggles
     * coalesces into a single preference_set.
     */
    @OptIn(FlowPreview::class)
    fun startAutoPush() {
        combine(
            authPreferences.showMature,
            authPreferences.autoplay,
            authPreferences.autoplayMedia,
            authPreferences.autoplayNextShort,
            authPreferences.hideShorts,
            authPreferences.hideReposts,
            authPreferences.hideMembersOnly,
            authPreferences.hideLivestreams,
            authPreferences.hideWalletBalance,
            authPreferences.hideNotificationCount,
            authPreferences.floatingPlayer,
            authPreferences.disableShortsView,
            authPreferences.clock24h,
            authPreferences.searchInLanguage,
            authPreferences.purchaseTipConfirmation,
            authPreferences.themePreference,
            authPreferences.languageOverride,
            authPreferences.defaultVideoQuality,
            authPreferences.defaultPlaylistAction,
            authPreferences.activeChannelClaimId,
        ) { it.toList() }
            .distinctUntilChanged()
            .drop(1) // skip the initial emission on app start
            .debounce(800)
            .onEach { runCatching { pushNow() } }
            .launchIn(scope)
    }

    private fun JsonElement.asBoolOrNull(): Boolean? = runCatching {
        (this as? JsonPrimitive)?.booleanOrNull
            ?: (this as? JsonPrimitive)?.contentOrNull?.let {
                when (it.lowercase()) { "true" -> true; "false" -> false; else -> null }
            }
    }.getOrNull()

    private fun JsonElement.asStringOrNull(): String? = runCatching {
        (this as? JsonPrimitive)?.contentOrNull
    }.getOrNull()

    companion object {
        // Web setting-name constants — see odysee-frontend/ui/constants/settings.ts
        private const val W_SHOW_MATURE = "show_mature"
        private const val W_AUTOPLAY_MEDIA = "autoplay"
        private const val W_AUTOPLAY_NEXT = "autoplay_next"
        private const val W_AUTOPLAY_NEXT_SHORTS = "autoplay_next_shorts"
        private const val W_HIDE_SHORTS = "hide_shorts"
        private const val W_HIDE_REPOSTS = "hide_reposts"
        private const val W_HIDE_MEMBERS_ONLY = "hide_members_only_content"
        private const val W_HIDE_SCHEDULED_LIVESTREAMS = "hide_scheduled_livestreams"
        private const val W_HIDE_BALANCE = "hide_balance"
        private const val W_HIDE_TITLE_NOTIFICATION_COUNT = "hide_title_notification_count"
        private const val W_FLOATING_PLAYER = "floating_player"
        private const val W_DISABLE_SHORTS_VIEW = "disable_shorts_view"
        private const val W_CLOCK_24H = "clock_24h"
        private const val W_SEARCH_IN_LANGUAGE = "search_in_language"
        private const val W_INSTANT_PURCHASE_ENABLED = "instant_purchase_enabled"
        private const val W_THEME = "theme"
        private const val W_LANGUAGE = "language"
        private const val W_DEFAULT_VIDEO_QUALITY = "default_video_quality"
        private const val W_DEFAULT_COLLECTION_ACTION = "default_collection_action"
        private const val W_ACTIVE_CHANNEL_CLAIM = "active_channel_claim"
    }
}
