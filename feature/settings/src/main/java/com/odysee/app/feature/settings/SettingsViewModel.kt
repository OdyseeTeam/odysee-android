package com.odysee.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.datastore.AuthPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: String = "system",
    val language: String? = null,
    val homepageLanguage: String? = null,
    val searchInLanguage: Boolean = false,
    val clock24h: Boolean = false,
    val hideWalletBalance: Boolean = false,
    val hideNotificationCount: Boolean = false,
    val showMature: Boolean = false,
    val hideMembersOnly: Boolean = false,
    val hideReposts: Boolean = false,
    val hideShorts: Boolean = false,
    val hideLivestreams: Boolean = false,
    val defaultPlaylistAction: String = "view",
    val publishConfirmation: Boolean = true,
    val purchaseTipConfirmation: Boolean = true,
    val autoplayMedia: Boolean = true,
    val autoplay: Boolean = true,
    val floatingPlayer: Boolean = true,
    val disableShortsView: Boolean = false,
    val p2pDelivery: Boolean = true,
    val defaultVideoQuality: String = "auto",
    val hasChannels: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val appUpdater: com.odysee.app.core.data.updater.AppUpdater,
    private val authRepository: com.odysee.app.core.data.auth.AuthRepository,
    private val lbryioApi: com.odysee.app.core.network.LbryioApi,
) : ViewModel() {

    val updaterSupported: Boolean get() = appUpdater.isSupported
    val updateState = appUpdater.state

    fun checkForUpdates() {
        viewModelScope.launch { appUpdater.checkForUpdates(silent = false) }
    }
    fun downloadUpdate() {
        viewModelScope.launch { appUpdater.downloadAndInstall() }
    }

    fun requestAccountDeletion(onResult: (success: Boolean, message: String?) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.requestAccountDeletion()
            when (result) {
                is com.odysee.app.core.data.auth.SignInResult.Success ->
                    onResult(true, null)
                is com.odysee.app.core.data.auth.SignInResult.Failure ->
                    onResult(false, result.message)
                else -> onResult(false, "Couldn't send request")
            }
        }
    }

    private val appearanceFlow = combine(
        authPreferences.themePreference,
        authPreferences.languageOverride,
        authPreferences.homepageLanguage,
        authPreferences.searchInLanguage,
        authPreferences.clock24h,
        authPreferences.hideWalletBalance,
        authPreferences.hideNotificationCount,
    ) { values ->
        Appearance(
            theme = values[0] as String,
            language = values[1] as String?,
            homepageLanguage = values[2] as String?,
            searchInLanguage = values[3] as Boolean,
            clock24h = values[4] as Boolean,
            hideWalletBalance = values[5] as Boolean,
            hideNotificationCount = values[6] as Boolean,
        )
    }

    private val contentFlow = combine(
        authPreferences.showMature,
        authPreferences.hideMembersOnly,
        authPreferences.hideReposts,
        authPreferences.hideShorts,
        authPreferences.hideLivestreams,
        authPreferences.defaultPlaylistAction,
        authPreferences.publishConfirmation,
        authPreferences.purchaseTipConfirmation,
    ) { values ->
        Content(
            showMature = values[0] as Boolean,
            hideMembersOnly = values[1] as Boolean,
            hideReposts = values[2] as Boolean,
            hideShorts = values[3] as Boolean,
            hideLivestreams = values[4] as Boolean,
            defaultPlaylistAction = values[5] as String,
            publishConfirmation = values[6] as Boolean,
            purchaseTipConfirmation = values[7] as Boolean,
        )
    }

    private val playerFlow = combine(
        authPreferences.autoplayMedia,
        authPreferences.autoplay,
        authPreferences.defaultVideoQuality,
        authPreferences.floatingPlayer,
        authPreferences.disableShortsView,
        authPreferences.p2pDelivery,
    ) { values ->
        PlayerSec(
            autoplayMedia = values[0] as Boolean,
            autoplay = values[1] as Boolean,
            defaultVideoQuality = values[2] as String,
            floatingPlayer = values[3] as Boolean,
            disableShortsView = values[4] as Boolean,
            p2pDelivery = values[5] as Boolean,
        )
    }

    val state: StateFlow<SettingsUiState> = combine(
        appearanceFlow, contentFlow, playerFlow, authRepository.state,
    ) { app, ct, pl, auth ->
        SettingsUiState(
            theme = app.theme,
            language = app.language,
            homepageLanguage = app.homepageLanguage,
            searchInLanguage = app.searchInLanguage,
            clock24h = app.clock24h,
            hideWalletBalance = app.hideWalletBalance,
            hideNotificationCount = app.hideNotificationCount,
            showMature = ct.showMature,
            hideMembersOnly = ct.hideMembersOnly,
            hideReposts = ct.hideReposts,
            hideShorts = ct.hideShorts,
            hideLivestreams = ct.hideLivestreams,
            defaultPlaylistAction = ct.defaultPlaylistAction,
            publishConfirmation = ct.publishConfirmation,
            purchaseTipConfirmation = ct.purchaseTipConfirmation,
            autoplayMedia = pl.autoplayMedia,
            autoplay = pl.autoplay,
            defaultVideoQuality = pl.defaultVideoQuality,
            floatingPlayer = pl.floatingPlayer,
            disableShortsView = pl.disableShortsView,
            p2pDelivery = pl.p2pDelivery,
            hasChannels = (auth as? com.odysee.app.core.data.auth.AuthState.SignedIn)
                ?.channels?.isNotEmpty() == true,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun setTheme(v: String) = launch { authPreferences.setThemePreference(v) }
    fun setLanguage(v: String?) = launch {
        authPreferences.setLanguageOverride(v)
        // Mirror to lbryio so the preference follows the user across devices.
        val pref = v ?: java.util.Locale.getDefault().language.takeIf { it.isNotBlank() } ?: "en"
        runCatching { lbryioApi.userLanguage(pref) }
    }
    fun setHomepageLanguage(v: String?) = launch { authPreferences.setHomepageLanguage(v) }
    fun setSearchInLanguage(v: Boolean) = launch { authPreferences.setSearchInLanguage(v) }
    fun setClock24h(v: Boolean) = launch { authPreferences.setClock24h(v) }
    fun setHideWalletBalance(v: Boolean) = launch { authPreferences.setHideWalletBalance(v) }
    fun setHideNotificationCount(v: Boolean) = launch { authPreferences.setHideNotificationCount(v) }
    fun setShowMature(v: Boolean) = launch { authPreferences.setShowMature(v) }
    fun setHideMembersOnly(v: Boolean) = launch { authPreferences.setHideMembersOnly(v) }
    fun setHideReposts(v: Boolean) = launch { authPreferences.setHideReposts(v) }
    fun setHideShorts(v: Boolean) = launch { authPreferences.setHideShorts(v) }
    fun setHideLivestreams(v: Boolean) = launch { authPreferences.setHideLivestreams(v) }
    fun setDefaultPlaylistAction(v: String) = launch { authPreferences.setDefaultPlaylistAction(v) }
    fun setPublishConfirmation(v: Boolean) = launch { authPreferences.setPublishConfirmation(v) }
    fun setPurchaseTipConfirmation(v: Boolean) = launch { authPreferences.setPurchaseTipConfirmation(v) }
    fun setAutoplayMedia(v: Boolean) = launch { authPreferences.setAutoplayMedia(v) }
    fun setAutoplay(v: Boolean) = launch { authPreferences.setAutoplay(v) }
    fun setFloatingPlayer(v: Boolean) = launch { authPreferences.setFloatingPlayer(v) }
    fun setDisableShortsView(v: Boolean) = launch { authPreferences.setDisableShortsView(v) }
    fun setP2pDelivery(v: Boolean) = launch { authPreferences.setP2pDelivery(v) }
    fun setDefaultVideoQuality(v: String) = launch { authPreferences.setDefaultVideoQuality(v) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private data class Appearance(
        val theme: String,
        val language: String?,
        val homepageLanguage: String?,
        val searchInLanguage: Boolean,
        val clock24h: Boolean,
        val hideWalletBalance: Boolean,
        val hideNotificationCount: Boolean,
    )

    private data class Content(
        val showMature: Boolean,
        val hideMembersOnly: Boolean,
        val hideReposts: Boolean,
        val hideShorts: Boolean,
        val hideLivestreams: Boolean,
        val defaultPlaylistAction: String,
        val publishConfirmation: Boolean,
        val purchaseTipConfirmation: Boolean,
    )

    private data class PlayerSec(
        val autoplayMedia: Boolean,
        val autoplay: Boolean,
        val defaultVideoQuality: String,
        val floatingPlayer: Boolean,
        val disableShortsView: Boolean,
        val p2pDelivery: Boolean,
    )
}
