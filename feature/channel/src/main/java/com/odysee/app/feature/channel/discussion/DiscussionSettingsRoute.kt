package com.odysee.app.feature.channel.discussion

import kotlinx.serialization.Serializable

@Serializable
data class DiscussionSettingsRoute(
    val claimId: String,
    val name: String,
)
