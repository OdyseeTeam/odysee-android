package com.odysee.app.core.model

data class User(
    val id: Long?,
    val email: String?,
    val isEmailVerified: Boolean,
    val isIdentityVerified: Boolean,
    val language: String?,
) {
    val isAnonymous: Boolean get() = email.isNullOrBlank() || !isEmailVerified
}
