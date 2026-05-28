package com.odysee.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.datastore.FlavorConfig
import com.odysee.app.core.datastore.NotificationDeliveryMode
import com.odysee.app.core.datastore.NotificationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsUiState(
    val pushSupported: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val mode: NotificationDeliveryMode = NotificationDeliveryMode.Poll,
    val pollIntervalMinutes: Int = NotificationPreferences.DEFAULT_POLL_INTERVAL_MIN,
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val prefs: NotificationPreferences,
    private val flavorConfig: FlavorConfig,
) : ViewModel() {

    private val _state = MutableStateFlow(
        NotificationSettingsUiState(
            pushSupported = flavorConfig.pushSupported,
            mode = flavorConfig.defaultDeliveryMode,
        ),
    )
    val state: StateFlow<NotificationSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val mode = prefs.deliveryMode.first() ?: flavorConfig.defaultDeliveryMode
            val interval = prefs.pollIntervalMinutes.first()
            val enabled = prefs.notificationsEnabled.first()
            _state.update {
                it.copy(
                    pushSupported = flavorConfig.pushSupported,
                    notificationsEnabled = enabled,
                    mode = mode,
                    pollIntervalMinutes = interval,
                )
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _state.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch { prefs.setNotificationsEnabled(enabled) }
    }

    fun setMode(mode: NotificationDeliveryMode) {
        if (mode == NotificationDeliveryMode.Push && !flavorConfig.pushSupported) return
        _state.update { it.copy(mode = mode) }
        viewModelScope.launch { prefs.setDeliveryMode(mode) }
    }

    fun setIntervalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(
            NotificationPreferences.MIN_POLL_INTERVAL_MIN,
            NotificationPreferences.MAX_POLL_INTERVAL_MIN,
        )
        _state.update { it.copy(pollIntervalMinutes = clamped) }
        viewModelScope.launch { prefs.setPollIntervalMinutes(clamped) }
    }
}
