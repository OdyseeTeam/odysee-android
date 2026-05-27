package com.odysee.app.feature.channel

import kotlinx.serialization.Serializable

@Serializable
data class ChannelRoute(
    val claimId: String,
    val name: String,
)
