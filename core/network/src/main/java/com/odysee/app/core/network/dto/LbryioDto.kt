package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LbryioEnvelope<T>(
    val success: Boolean = false,
    val error: String? = null,
    val data: T? = null,
)

@Serializable
data class UserDto(
    val id: Long? = null,
    @SerialName("primary_email") val primaryEmail: String? = null,
    @SerialName("has_verified_email") val hasVerifiedEmail: Boolean = false,
    @SerialName("is_identity_verified") val isIdentityVerified: Boolean = false,
    @SerialName("is_reward_approved") val isRewardApproved: Boolean = false,
    val language: String? = null,
    @SerialName("auth_token") val authToken: String? = null,
)
