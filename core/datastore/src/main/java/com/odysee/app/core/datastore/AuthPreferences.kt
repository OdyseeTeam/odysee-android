package com.odysee.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "odysee_auth")

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.authDataStore

    val authToken: Flow<String?> = store.data.map { it[KEY_AUTH_TOKEN] }
    val email: Flow<String?> = store.data.map { it[KEY_EMAIL] }
    val activeChannelClaimId: Flow<String?> = store.data.map { it[KEY_ACTIVE_CHANNEL_ID] }
    val subscriptions: Flow<List<String>> = store.data.map { prefs ->
        prefs[KEY_SUBSCRIPTIONS]?.takeIf { it.isNotBlank() }?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
    }
    val showMature: Flow<Boolean> = store.data.map { it[KEY_SHOW_MATURE] ?: false }
    val autoplay: Flow<Boolean> = store.data.map { it[KEY_AUTOPLAY] ?: true }
    val autoplayMedia: Flow<Boolean> = store.data.map { it[KEY_AUTOPLAY_MEDIA] ?: true }
    val autoplayNextShort: Flow<Boolean> = store.data.map { it[KEY_AUTOPLAY_NEXT_SHORT] ?: true }
    val hideShorts: Flow<Boolean> = store.data.map { it[KEY_HIDE_SHORTS] ?: false }
    val themePreference: Flow<String> = store.data.map { it[KEY_THEME] ?: "system" }
    val languageOverride: Flow<String?> = store.data.map { it[KEY_LANGUAGE] }
    val homepageLanguage: Flow<String?> = store.data.map { it[KEY_HOMEPAGE_LANGUAGE] }
    val searchInLanguage: Flow<Boolean> = store.data.map { it[KEY_SEARCH_IN_LANGUAGE] ?: false }
    val clock24h: Flow<Boolean> = store.data.map { it[KEY_CLOCK_24H] ?: false }
    val hideWalletBalance: Flow<Boolean> = store.data.map { it[KEY_HIDE_WALLET_BALANCE] ?: false }
    val hideNotificationCount: Flow<Boolean> = store.data.map { it[KEY_HIDE_NOTIFICATION_COUNT] ?: false }
    val hideMembersOnly: Flow<Boolean> = store.data.map { it[KEY_HIDE_MEMBERS_ONLY] ?: false }
    val hideReposts: Flow<Boolean> = store.data.map { it[KEY_HIDE_REPOSTS] ?: false }
    val hideLivestreams: Flow<Boolean> = store.data.map { it[KEY_HIDE_LIVESTREAMS] ?: false }
    val defaultPlaylistAction: Flow<String> = store.data.map { it[KEY_DEFAULT_PLAYLIST_ACTION] ?: "view" }
    val publishConfirmation: Flow<Boolean> = store.data.map { it[KEY_PUBLISH_CONFIRMATION] ?: true }
    val floatingPlayer: Flow<Boolean> = store.data.map { it[KEY_FLOATING_PLAYER] ?: true }
    val disableShortsView: Flow<Boolean> = store.data.map { it[KEY_DISABLE_SHORTS_VIEW] ?: false }
    val p2pDelivery: Flow<Boolean> = store.data.map { it[KEY_P2P_DELIVERY] ?: true }
    val purchaseTipConfirmation: Flow<Boolean> = store.data.map { it[KEY_PURCHASE_TIP_CONFIRM] ?: true }
    val defaultVideoQuality: Flow<String> = store.data.map { it[KEY_DEFAULT_VIDEO_QUALITY] ?: "auto" }
    val homeContentTypes: Flow<Set<String>> = store.data.map { prefs ->
        prefs[KEY_HOME_CONTENT_TYPES]?.split(',')?.filter { it.isNotBlank() }?.toSet()
            ?: setOf("videos", "shorts", "live", "upcoming")
    }
    val watchHistory: Flow<List<String>> = store.data.map { prefs ->
        prefs[KEY_WATCH_HISTORY]?.takeIf { it.isNotBlank() }?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
    }
    val watchLater: Flow<List<String>> = store.data.map { prefs ->
        prefs[KEY_WATCH_LATER]?.takeIf { it.isNotBlank() }?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
    }
    val favorites: Flow<List<String>> = store.data.map { prefs ->
        prefs[KEY_FAVORITES]?.takeIf { it.isNotBlank() }?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
    }
    val customPlaylists: Flow<String?> = store.data.map { prefs -> prefs[KEY_CUSTOM_PLAYLISTS] }
    val blockedChannels: Flow<List<String>> = store.data.map { prefs ->
        prefs[KEY_BLOCKED]?.takeIf { it.isNotBlank() }?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
    }
    val followedTags: Flow<List<String>> = store.data.map { prefs ->
        prefs[KEY_FOLLOWED_TAGS]?.takeIf { it.isNotBlank() }?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
    }
    val playbackPositions: Flow<Map<String, Long>> = store.data.map { prefs ->
        prefs[KEY_PLAYBACK_POSITIONS]?.takeIf { it.isNotBlank() }
            ?.split('\n')
            ?.mapNotNull { line ->
                val parts = line.split('|', limit = 2)
                if (parts.size == 2) parts[0] to (parts[1].toLongOrNull() ?: return@mapNotNull null) else null
            }?.toMap() ?: emptyMap()
    }

    suspend fun installationId(): String {
        val existing = readSingle(KEY_INSTALLATION_ID)
        // internal-apis requires `[A-Za-z0-9]{65,66}` on app_id.
        if (existing != null && existing.length in 65..66 && existing.all { it.isLetterOrDigit() }) {
            return existing
        }
        val fresh = generateAlphanumeric(66)
        store.edit { prefs -> prefs[KEY_INSTALLATION_ID] = fresh }
        return fresh
    }

    private fun generateAlphanumeric(length: Int): String {
        val pool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val random = java.security.SecureRandom()
        return buildString(length) {
            repeat(length) { append(pool[random.nextInt(pool.size)]) }
        }
    }

    suspend fun setAuthToken(token: String?) {
        store.edit { prefs ->
            if (token == null) prefs.remove(KEY_AUTH_TOKEN) else prefs[KEY_AUTH_TOKEN] = token
        }
    }

    suspend fun setEmail(email: String?) {
        store.edit { prefs ->
            if (email == null) prefs.remove(KEY_EMAIL) else prefs[KEY_EMAIL] = email
        }
    }

    suspend fun setActiveChannelClaimId(claimId: String?) {
        store.edit { prefs ->
            if (claimId == null) prefs.remove(KEY_ACTIVE_CHANNEL_ID) else prefs[KEY_ACTIVE_CHANNEL_ID] = claimId
        }
    }

    suspend fun setSubscriptions(entries: List<String>) {
        store.edit { prefs ->
            if (entries.isEmpty()) prefs.remove(KEY_SUBSCRIPTIONS)
            else prefs[KEY_SUBSCRIPTIONS] = entries.joinToString("\n")
        }
    }

    suspend fun setShowMature(value: Boolean) {
        store.edit { it[KEY_SHOW_MATURE] = value }
    }

    suspend fun setAutoplay(value: Boolean) {
        store.edit { it[KEY_AUTOPLAY] = value }
    }

    suspend fun setAutoplayMedia(value: Boolean) {
        store.edit { it[KEY_AUTOPLAY_MEDIA] = value }
    }

    suspend fun setSearchInLanguage(value: Boolean) {
        store.edit { it[KEY_SEARCH_IN_LANGUAGE] = value }
    }

    suspend fun setClock24h(value: Boolean) {
        store.edit { it[KEY_CLOCK_24H] = value }
    }

    suspend fun setHideWalletBalance(value: Boolean) {
        store.edit { it[KEY_HIDE_WALLET_BALANCE] = value }
    }

    suspend fun setHideNotificationCount(value: Boolean) {
        store.edit { it[KEY_HIDE_NOTIFICATION_COUNT] = value }
    }

    suspend fun setHideMembersOnly(value: Boolean) {
        store.edit { it[KEY_HIDE_MEMBERS_ONLY] = value }
    }

    suspend fun setHideReposts(value: Boolean) {
        store.edit { it[KEY_HIDE_REPOSTS] = value }
    }

    suspend fun setHideLivestreams(value: Boolean) {
        store.edit { it[KEY_HIDE_LIVESTREAMS] = value }
    }

    suspend fun setDefaultPlaylistAction(value: String) {
        store.edit { it[KEY_DEFAULT_PLAYLIST_ACTION] = value }
    }

    suspend fun setPublishConfirmation(value: Boolean) {
        store.edit { it[KEY_PUBLISH_CONFIRMATION] = value }
    }

    suspend fun setFloatingPlayer(value: Boolean) {
        store.edit { it[KEY_FLOATING_PLAYER] = value }
    }

    suspend fun setP2pDelivery(value: Boolean) {
        store.edit { it[KEY_P2P_DELIVERY] = value }
    }

    suspend fun setPurchaseTipConfirmation(value: Boolean) {
        store.edit { it[KEY_PURCHASE_TIP_CONFIRM] = value }
    }

    suspend fun setDisableShortsView(value: Boolean) {
        store.edit { it[KEY_DISABLE_SHORTS_VIEW] = value }
    }

    suspend fun setDefaultVideoQuality(value: String) {
        store.edit { it[KEY_DEFAULT_VIDEO_QUALITY] = value }
    }

    suspend fun setHomeContentTypes(value: Set<String>) {
        store.edit { it[KEY_HOME_CONTENT_TYPES] = value.joinToString(",") }
    }

    suspend fun setAutoplayNextShort(value: Boolean) {
        store.edit { it[KEY_AUTOPLAY_NEXT_SHORT] = value }
    }

    suspend fun setHideShorts(value: Boolean) {
        store.edit { it[KEY_HIDE_SHORTS] = value }
    }

    suspend fun setThemePreference(theme: String) {
        store.edit { it[KEY_THEME] = theme }
    }

    suspend fun setLanguageOverride(lang: String?) {
        store.edit { prefs ->
            if (lang == null) prefs.remove(KEY_LANGUAGE) else prefs[KEY_LANGUAGE] = lang
        }
    }

    suspend fun setHomepageLanguage(lang: String?) {
        store.edit { prefs ->
            if (lang == null) prefs.remove(KEY_HOMEPAGE_LANGUAGE) else prefs[KEY_HOMEPAGE_LANGUAGE] = lang
        }
    }

    suspend fun setWatchHistory(entries: List<String>) {
        store.edit { prefs ->
            if (entries.isEmpty()) prefs.remove(KEY_WATCH_HISTORY)
            else prefs[KEY_WATCH_HISTORY] = entries.joinToString("\n")
        }
    }

    suspend fun setWatchLater(entries: List<String>) {
        store.edit { prefs ->
            if (entries.isEmpty()) prefs.remove(KEY_WATCH_LATER)
            else prefs[KEY_WATCH_LATER] = entries.joinToString("\n")
        }
    }

    suspend fun setFavorites(entries: List<String>) {
        store.edit { prefs ->
            if (entries.isEmpty()) prefs.remove(KEY_FAVORITES)
            else prefs[KEY_FAVORITES] = entries.joinToString("\n")
        }
    }

    suspend fun setCustomPlaylists(json: String?) {
        store.edit { prefs ->
            if (json.isNullOrBlank()) prefs.remove(KEY_CUSTOM_PLAYLISTS)
            else prefs[KEY_CUSTOM_PLAYLISTS] = json
        }
    }

    suspend fun setBlockedChannels(entries: List<String>) {
        store.edit { prefs ->
            if (entries.isEmpty()) prefs.remove(KEY_BLOCKED)
            else prefs[KEY_BLOCKED] = entries.joinToString("\n")
        }
    }

    suspend fun setFollowedTags(tags: List<String>) {
        store.edit { prefs ->
            if (tags.isEmpty()) prefs.remove(KEY_FOLLOWED_TAGS)
            else prefs[KEY_FOLLOWED_TAGS] = tags.joinToString("\n")
        }
    }

    suspend fun setPlaybackPositions(positions: Map<String, Long>) {
        store.edit { prefs ->
            if (positions.isEmpty()) prefs.remove(KEY_PLAYBACK_POSITIONS)
            else prefs[KEY_PLAYBACK_POSITIONS] = positions.entries.joinToString("\n") { "${it.key}|${it.value}" }
        }
    }

    private suspend fun readSingle(key: Preferences.Key<String>): String? {
        var value: String? = null
        store.edit { value = it[key] }
        return value
    }

    private companion object {
        val KEY_INSTALLATION_ID = stringPreferencesKey("installation_id")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_EMAIL = stringPreferencesKey("email")
        val KEY_ACTIVE_CHANNEL_ID = stringPreferencesKey("active_channel_claim_id")
        val KEY_SUBSCRIPTIONS = stringPreferencesKey("subscriptions")
        val KEY_SHOW_MATURE = booleanPreferencesKey("show_mature")
        val KEY_AUTOPLAY = booleanPreferencesKey("autoplay")
        val KEY_AUTOPLAY_NEXT_SHORT = booleanPreferencesKey("autoplay_next_short")
        val KEY_HIDE_SHORTS = booleanPreferencesKey("hide_shorts")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_HOMEPAGE_LANGUAGE = stringPreferencesKey("homepage_language")
        val KEY_WATCH_HISTORY = stringPreferencesKey("watch_history")
        val KEY_WATCH_LATER = stringPreferencesKey("watch_later")
        val KEY_FAVORITES = stringPreferencesKey("favorites")
        val KEY_CUSTOM_PLAYLISTS = stringPreferencesKey("custom_playlists_json")
        val KEY_BLOCKED = stringPreferencesKey("blocked_channels")
        val KEY_FOLLOWED_TAGS = stringPreferencesKey("followed_tags")
        val KEY_PLAYBACK_POSITIONS = stringPreferencesKey("playback_positions")
        val KEY_AUTOPLAY_MEDIA = booleanPreferencesKey("autoplay_media")
        val KEY_SEARCH_IN_LANGUAGE = booleanPreferencesKey("search_in_language")
        val KEY_CLOCK_24H = booleanPreferencesKey("clock_24h")
        val KEY_HIDE_WALLET_BALANCE = booleanPreferencesKey("hide_wallet_balance")
        val KEY_HIDE_NOTIFICATION_COUNT = booleanPreferencesKey("hide_notification_count")
        val KEY_HIDE_MEMBERS_ONLY = booleanPreferencesKey("hide_members_only")
        val KEY_HIDE_REPOSTS = booleanPreferencesKey("hide_reposts")
        val KEY_HIDE_LIVESTREAMS = booleanPreferencesKey("hide_livestreams")
        val KEY_DEFAULT_PLAYLIST_ACTION = stringPreferencesKey("default_playlist_action")
        val KEY_PUBLISH_CONFIRMATION = booleanPreferencesKey("publish_confirmation")
        val KEY_FLOATING_PLAYER = booleanPreferencesKey("floating_player")
        val KEY_DISABLE_SHORTS_VIEW = booleanPreferencesKey("disable_shorts_view")
        val KEY_P2P_DELIVERY = booleanPreferencesKey("p2p_delivery")
        val KEY_PURCHASE_TIP_CONFIRM = booleanPreferencesKey("purchase_tip_confirmation")
        val KEY_DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
        val KEY_HOME_CONTENT_TYPES = stringPreferencesKey("home_content_types")
    }
}
