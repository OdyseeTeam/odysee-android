package com.odysee.app.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.auth.AuthRepository
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.data.auth.SignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TvSignInUiState(
    val email: String = "",
    val isSending: Boolean = false,
    val emailSent: Boolean = false,
    val errorMessage: String? = null,
    val isSignedIn: Boolean = false,
    val signedInEmail: String? = null,
)

@HiltViewModel
class TvSignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _local = MutableStateFlow(TvSignInUiState())

    val state: StateFlow<TvSignInUiState> = combine(_local, authRepository.state) { local, auth ->
        val signed = auth as? AuthState.SignedIn
        local.copy(
            isSignedIn = signed != null,
            signedInEmail = signed?.user?.email,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TvSignInUiState())

    fun setEmail(value: String) {
        _local.value = _local.value.copy(email = value, errorMessage = null)
    }

    fun submit() {
        val email = _local.value.email.trim()
        if (email.isBlank() || !email.contains("@")) {
            _local.value = _local.value.copy(errorMessage = "Enter a valid email.")
            return
        }
        _local.value = _local.value.copy(isSending = true, errorMessage = null, emailSent = false)
        viewModelScope.launch {
            val result = authRepository.signIn(email, password = null)
            _local.value = when (result) {
                is SignInResult.Success -> _local.value.copy(isSending = false, emailSent = true)
                is SignInResult.VerificationEmailSent -> _local.value.copy(isSending = false, emailSent = true)
                is SignInResult.TwoFactorRequired -> _local.value.copy(
                    isSending = false,
                    errorMessage = "Two-factor sign-in isn't supported on TV yet — sign in on phone first.",
                )
                is SignInResult.Failure -> _local.value.copy(
                    isSending = false,
                    errorMessage = result.message,
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun reset() {
        _local.value = TvSignInUiState()
    }
}
