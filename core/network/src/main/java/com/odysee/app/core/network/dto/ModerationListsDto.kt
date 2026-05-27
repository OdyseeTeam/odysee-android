package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModerationBlockedListParams(
    @SerialName("mod_channel_id") val modChannelId: String,
    @SerialName("mod_channel_name") val modChannelName: String,
    @SerialName("creator_channel_id") val creatorChannelId: String? = null,
    @SerialName("creator_channel_name") val creatorChannelName: String? = null,
    @SerialName("blocked_list_type") val blockedListType: Int = 1,
    val signature: String,
    @SerialName("signing_ts") val signingTs: String,
)

@Serializable
data class ModerationBlockedListResult(
    @SerialName("blocked_channels") val blockedChannels: List<BlockedChannelEntryDto> = emptyList(),
    @SerialName("globally_blocked_channels") val globallyBlockedChannels: List<BlockedChannelEntryDto> = emptyList(),
    @SerialName("delegated_blocked_channels") val delegatedBlockedChannels: List<BlockedChannelEntryDto> = emptyList(),
)

@Serializable
data class BlockedChannelEntryDto(
    @SerialName("blocked_channel_id") val blockedChannelId: String,
    @SerialName("blocked_channel_name") val blockedChannelName: String,
    @SerialName("blocked_at") val blockedAt: String? = null,
    @SerialName("banned_until") val bannedUntil: String? = null,
)

@Serializable
data class ModerationListDelegatesParams(
    @SerialName("creator_channel_id") val creatorChannelId: String,
    @SerialName("creator_channel_name") val creatorChannelName: String,
    val signature: String,
    @SerialName("signing_ts") val signingTs: String,
)

@Serializable
data class ModerationListDelegatesResult(
    val delegates: List<DelegateEntryDto> = emptyList(),
)

@Serializable
data class DelegateEntryDto(
    @SerialName("channel_id") val channelId: String,
    @SerialName("channel_name") val channelName: String,
)
