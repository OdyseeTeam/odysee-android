package com.odysee.app.core.data.auth

/**
 * Pure branching for Lbryio sign-in error strings / HTTP codes.
 * Kept free of Android deps so unit tests can cover the magic-link vs bad-password split.
 */
object SignInErrorClassifier {

    sealed interface Decision {
        data object Promote : Decision
        data object BadCredentials : Decision
        data object MagicLink : Decision
        data class Failure(val message: String) : Decision
    }

    fun classify(
        apiError: String?,
        passwordProvided: Boolean,
        httpCode: Int? = null,
    ): Decision {
        val msg = apiError?.takeIf { it.isNotBlank() } ?: "Sign-in failed"
        val lower = msg.lowercase()
        val looksLikeBadCredentials =
            "incorrect email" in lower || ("incorrect" in lower && "password" in lower)
        val needsVerification =
            "verif" in lower ||
                "confirm your email" in lower ||
                "check your email" in lower ||
                "email not confirmed" in lower

        return when {
            "already signed" in lower || "sign out first" in lower -> Decision.Promote
            looksLikeBadCredentials && passwordProvided -> Decision.BadCredentials
            looksLikeBadCredentials && !passwordProvided -> Decision.MagicLink
            needsVerification -> Decision.MagicLink
            // Email-only sign-in historically returns HTTP 409 for magic-link.
            !passwordProvided && httpCode == 409 -> Decision.MagicLink
            else -> Decision.Failure(msg)
        }
    }
}
