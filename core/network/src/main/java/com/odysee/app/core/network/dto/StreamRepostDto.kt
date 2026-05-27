package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreamRepostParams(
    val name: String,
    val bid: String,
    @SerialName("claim_id") val claimId: String,
    @SerialName("channel_id") val channelId: String,
    val blocking: Boolean = true,
)
