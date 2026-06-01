package com.odysee.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationDataStore by preferencesDataStore(name = "odysee_notifications")

enum class NotificationDeliveryMode { Push, Poll }

@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.notificationDataStore

    val deliveryMode: Flow<NotificationDeliveryMode?> = store.data.map { prefs ->
        prefs[KEY_MODE]?.let { runCatching { NotificationDeliveryMode.valueOf(it) }.getOrNull() }
    }

    val pollIntervalMinutes: Flow<Int> = store.data.map { it[KEY_POLL_INTERVAL_MIN] ?: DEFAULT_POLL_INTERVAL_MIN }

    val lastSeenNotificationId: Flow<Long> = store.data.map { it[KEY_LAST_SEEN_ID] ?: 0L }

    val pushEnabled: Flow<Boolean> = store.data.map { it[KEY_PUSH_ENABLED] ?: true }

    /** Master switch — when false, neither push nor poll fires. */
    val notificationsEnabled: Flow<Boolean> = store.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setDeliveryMode(mode: NotificationDeliveryMode) {
        store.edit { it[KEY_MODE] = mode.name }
    }

    suspend fun setPollIntervalMinutes(minutes: Int) {
        store.edit { it[KEY_POLL_INTERVAL_MIN] = minutes.coerceIn(MIN_POLL_INTERVAL_MIN, MAX_POLL_INTERVAL_MIN) }
    }

    suspend fun setLastSeenNotificationId(id: Long) {
        store.edit { it[KEY_LAST_SEEN_ID] = id }
    }

    suspend fun setPushEnabled(value: Boolean) {
        store.edit { it[KEY_PUSH_ENABLED] = value }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        store.edit { it[KEY_NOTIFICATIONS_ENABLED] = value }
    }

    companion object {
        const val MIN_POLL_INTERVAL_MIN = 15
        const val MAX_POLL_INTERVAL_MIN = 24 * 60
        const val DEFAULT_POLL_INTERVAL_MIN = 60

        private val KEY_MODE = stringPreferencesKey("delivery_mode")
        private val KEY_POLL_INTERVAL_MIN = intPreferencesKey("poll_interval_min")
        private val KEY_LAST_SEEN_ID = longPreferencesKey("last_seen_id")
        private val KEY_PUSH_ENABLED = booleanPreferencesKey("push_enabled")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }
}
