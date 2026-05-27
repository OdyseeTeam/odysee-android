package com.odysee.app.feature.shorts

import kotlinx.serialization.Serializable

@Serializable
data class ShortsRoute(
    val initialClaimId: String,
    val initialPermanentUrl: String,
    val initialTitle: String,
    val initialChannelName: String,
    val initialChannelClaimId: String? = null,
    val initialChannelAvatarUrl: String? = null,
    val initialThumbnailUrl: String? = null,
)
