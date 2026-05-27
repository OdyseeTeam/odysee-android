package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PremiumStatusDto(
    @SerialName("has_premium") val hasPremium: Boolean = false,
    @SerialName("has_premium_plus") val hasPremiumPlus: Boolean = false,
)
