package com.odysee.app.feature.channel.memberships

import kotlinx.serialization.Serializable

@Serializable
data class CreatorMembershipsRoute(
    val claimId: String,
    val name: String,
)
