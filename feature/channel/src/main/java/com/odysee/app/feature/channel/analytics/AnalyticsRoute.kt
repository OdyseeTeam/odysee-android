package com.odysee.app.feature.channel.analytics

import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsRoute(
    val claimId: String,
    val name: String,
)
