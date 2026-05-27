package com.odysee.app.feature.channel.moderation

import kotlinx.serialization.Serializable

@Serializable
data class ModerationRoute(
    val claimId: String,
    val name: String,
)
