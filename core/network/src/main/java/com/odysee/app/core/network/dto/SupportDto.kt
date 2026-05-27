package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupportCreateParams(
    @SerialName("claim_id") val claimId: String,
    val amount: String,
    val tip: Boolean = true,
    val blocking: Boolean = true,
    @SerialName("channel_id") val channelId: String? = null,
)

@Serializable
data class SupportCreateResult(
    val txid: String? = null,
    val height: Long? = null,
)
