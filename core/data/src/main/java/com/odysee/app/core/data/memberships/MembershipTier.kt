package com.odysee.app.core.data.memberships

data class MembershipTier(
    val id: String,
    val name: String,
    val description: String?,
    val priceUsd: Double,
)

data class MySubscription(
    val membershipId: String,
    val tierName: String,
    val tierDescription: String?,
    val priceUsd: Double,
    val creatorChannelId: String?,
    val creatorChannelName: String?,
    val creatorChannelUrl: String?,
    val status: String,
    val endsAt: String?,
)
