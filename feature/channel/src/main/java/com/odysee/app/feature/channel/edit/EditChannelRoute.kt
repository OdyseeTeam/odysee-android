package com.odysee.app.feature.channel.edit

import kotlinx.serialization.Serializable

@Serializable
data class EditChannelRoute(
    val claimId: String,
    val name: String,
)
