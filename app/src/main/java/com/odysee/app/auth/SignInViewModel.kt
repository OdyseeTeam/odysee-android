package com.odysee.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.auth.AuthRepository
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.data.auth.SignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInUiState(
    val isSubmitting: Boolean = false,
    val statusMessage: String? = null,
    val statusIsError: Boolean = false,
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.state.value)

    private val _ui = MutableStateFlow(SignInUiState())
    val ui: StateFlow<SignInUiState> = _ui.asStateFlow()

    private val _dismiss = Channel<Unit>(capacity = Channel.BUFFERED)
    val dismissEvents: Flow<Unit> = _dismiss.receiveAsFlow()

    init {
        // When the background poll / deep-link flips us into SignedIn while this
        // screen is open, close it automatically — otherwise the user sits on
        // the "check your email" UI even though they're already signed in.
        viewModelScope.launch {
            var wasSignedIn = authRepository.state.value is AuthState.SignedIn
            authRepository.state.collect { st ->
                val nowSignedIn = st is AuthState.SignedIn
                if (nowSignedIn && !wasSignedIn) _dismiss.trySend(Unit)
                wasSignedIn = nowSignedIn
            }
        }
    }

    fun signIn(email: String, password: String?) {
        if (email.isBlank()) {
            _ui.update { it.copy(statusMessage = "Enter your email.", statusIsError = true) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true, statusMessage = null, statusIsError = false) }
            val result = authRepository.signIn(email.trim(), password?.takeIf { it.isNotBlank() })
            _ui.update { it.copy(isSubmitting = false, statusMessage = result.toMessage(), statusIsError = result is SignInResult.Failure) }
            if (result is SignInResult.Success) _dismiss.trySend(Unit)
        }
    }

    fun signUp(email: String, password: String?) {
        if (email.isBlank()) {
            _ui.update { it.copy(statusMessage = "Enter your email.", statusIsError = true) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true, statusMessage = null, statusIsError = false) }
            val result = authRepository.signUp(email.trim(), password?.takeIf { it.isNotBlank() })
            _ui.update { it.copy(isSubmitting = false, statusMessage = result.toMessage(), statusIsError = result is SignInResult.Failure) }
            if (result is SignInResult.Success) _dismiss.trySend(Unit)
        }
    }

    fun requestPasswordReset(email: String) {
        if (email.isBlank()) {
            _ui.update { it.copy(statusMessage = "Enter your email.", statusIsError = true) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true, statusMessage = null, statusIsError = false) }
            val result = authRepository.requestPasswordReset(email.trim())
            val msg = when (result) {
                is SignInResult.VerificationEmailSent -> "Check your inbox for a reset link."
                is SignInResult.Failure -> result.message
                else -> "Request sent."
            }
            _ui.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = msg,
                    statusIsError = result is SignInResult.Failure,
                )
            }
        }
    }

    fun confirmEmailFromDeepLink(email: String, verificationToken: String, authToken: String) {
        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true, statusMessage = null, statusIsError = false) }
            val result = authRepository.confirmEmail(email, verificationToken, authToken)
            _ui.update {
                it.copy(
                    isSubmitting = false,
                    statusMessage = result.toMessage(),
                    statusIsError = result is SignInResult.Failure,
                )
            }
            if (result is SignInResult.Success) _dismiss.trySend(Unit)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true, statusMessage = null, statusIsError = false) }
            authRepository.signOut()
            _ui.update { it.copy(isSubmitting = false, statusMessage = "Signed out.", statusIsError = false) }
        }
    }

    fun clearStatus() {
        _ui.update { it.copy(statusMessage = null, statusIsError = false) }
    }
}

private fun SignInResult.toMessage(): String = when (this) {
    is SignInResult.Success -> "Signed in."
    is SignInResult.VerificationEmailSent -> "Check your inbox for a verification email."
    is SignInResult.TwoFactorRequired -> "Two-factor authentication required. Check your inbox."
    is SignInResult.Failure -> message
}
