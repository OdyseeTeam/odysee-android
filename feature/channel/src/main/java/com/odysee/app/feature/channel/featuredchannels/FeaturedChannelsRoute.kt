package com.odysee.app.feature.channel.featuredchannels

import kotlinx.serialization.Serializable

@Serializable
data class FeaturedChannelsEditRoute(
    val claimId: String,
    val name: String,
)
