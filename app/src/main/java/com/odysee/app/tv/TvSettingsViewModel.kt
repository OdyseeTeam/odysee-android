package com.odysee.app.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.datastore.AuthPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TvSettingsUiState(
    val homepageLanguage: String? = null,
    val autoplay: Boolean = true,
)

@HiltViewModel
class TvSettingsViewModel @Inject constructor(
    private val authPreferences: AuthPreferences,
) : ViewModel() {
    val state: StateFlow<TvSettingsUiState> = combine(
        authPreferences.homepageLanguage,
        authPreferences.autoplay,
    ) { lang, autoplay ->
        TvSettingsUiState(homepageLanguage = lang, autoplay = autoplay)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TvSettingsUiState())

    fun setHomepageLanguage(lang: String?) = viewModelScope.launch {
        authPreferences.setHomepageLanguage(lang)
    }

    fun setAutoplay(value: Boolean) = viewModelScope.launch {
        authPreferences.setAutoplay(value)
    }
}
