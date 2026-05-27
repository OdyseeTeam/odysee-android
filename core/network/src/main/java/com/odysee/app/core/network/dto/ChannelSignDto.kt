package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelSignParams(
    @SerialName("channel_id") val channelId: String,
    val hexdata: String,
)

@Serializable
data class ChannelSignResult(
    val signature: String? = null,
    @SerialName("signing_ts") val signingTs: String? = null,
)
