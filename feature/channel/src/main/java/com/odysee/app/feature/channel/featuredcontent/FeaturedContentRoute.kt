package com.odysee.app.feature.channel.featuredcontent

import kotlinx.serialization.Serializable

@Serializable
data class FeaturedContentRoute(
    val claimId: String,
    val name: String,
)
