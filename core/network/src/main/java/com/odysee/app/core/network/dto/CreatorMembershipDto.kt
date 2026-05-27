package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatorMembershipDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("channel_name") val channelName: String? = null,
    val price: MembershipPriceDto? = null,
    val perks: List<MembershipPerkDto> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class MembershipPriceDto(
    val amount: Double = 0.0,
    val currency: String = "usd",
)

@Serializable
data class MembershipPerkDto(
    val id: String,
    val name: String,
    val description: String? = null,
)

@Serializable
data class MembershipSubscriptionDto(
    val membership: SubMembershipDto? = null,
    val subscription: SubInfoDto? = null,
    @SerialName("current_price") val currentPrice: MembershipPriceDto? = null,
    @SerialName("creator_channel_url") val creatorChannelUrl: String? = null,
    @SerialName("creator_channel_name") val creatorChannelName: String? = null,
    @SerialName("creator_channel_id") val creatorChannelId: String? = null,
)

@Serializable
data class SubMembershipDto(
    val id: String? = null,
    val name: String = "",
    val description: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("channel_claim_id") val channelClaimId: String? = null,
    @SerialName("channel_name") val channelName: String? = null,
)

@Serializable
data class SubInfoDto(
    val id: String? = null,
    val status: String = "",
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
