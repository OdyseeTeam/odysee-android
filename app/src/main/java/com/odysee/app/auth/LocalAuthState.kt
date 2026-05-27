package com.odysee.app.auth

import androidx.compose.runtime.compositionLocalOf
import com.odysee.app.core.data.auth.AuthState

val LocalAuthState = compositionLocalOf<AuthState> { AuthState.Loading }
