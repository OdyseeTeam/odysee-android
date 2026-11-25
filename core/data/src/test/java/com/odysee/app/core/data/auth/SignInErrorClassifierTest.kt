package com.odysee.app.core.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInErrorClassifierTest {

    @Test
    fun passwordWrong_isBadCredentials() {
        val d = SignInErrorClassifier.classify(
            "Incorrect email and/or password",
            passwordProvided = true,
            httpCode = 409,
        )
        assertEquals(SignInErrorClassifier.Decision.BadCredentials, d)
    }

    @Test
    fun emailOnlyIncorrect_isMagicLink() {
        val d = SignInErrorClassifier.classify(
            "Incorrect email and/or password",
            passwordProvided = false,
            httpCode = 409,
        )
        assertEquals(SignInErrorClassifier.Decision.MagicLink, d)
    }

    @Test
    fun alreadySignedIn_isPromote() {
        val d = SignInErrorClassifier.classify(
            "User is already signed in. Please sign out first.",
            passwordProvided = true,
            httpCode = 400,
        )
        assertEquals(SignInErrorClassifier.Decision.Promote, d)
    }

    @Test
    fun unknownError_isFailure() {
        val d = SignInErrorClassifier.classify(
            "Rate limit exceeded",
            passwordProvided = true,
            httpCode = 429,
        )
        assertTrue(d is SignInErrorClassifier.Decision.Failure)
    }

    @Test
    fun emailOnly409_withoutMessage_isMagicLink() {
        val d = SignInErrorClassifier.classify(
            null,
            passwordProvided = false,
            httpCode = 409,
        )
        assertEquals(SignInErrorClassifier.Decision.MagicLink, d)
    }

    @Test
    fun needsVerification_isMagicLink() {
        val d = SignInErrorClassifier.classify(
            "Please verify your email",
            passwordProvided = true,
            httpCode = 403,
        )
        assertEquals(SignInErrorClassifier.Decision.MagicLink, d)
    }
}
