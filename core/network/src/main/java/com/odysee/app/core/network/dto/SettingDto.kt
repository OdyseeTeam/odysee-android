package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingGetParams(
    @SerialName("channel_id") val channelId: String,
    @SerialName("channel_name") val channelName: String? = null,
)

@Serializable
data class SettingResponse(
    @SerialName("channel_sections") val channelSections: ChannelSectionsDto? = null,
    @SerialName("min_tip_amount_comment") val minTipAmountComment: Double? = null,
    @SerialName("min_tip_amount_super_chat") val minTipAmountSuperChat: Double? = null,
    @SerialName("slow_mode_min_gap") val slowModeMinGap: Int? = null,
    @SerialName("comments_members_only") val commentsMembersOnly: Boolean? = null,
    @SerialName("livestream_chat_members_only") val livestreamChatMembersOnly: Boolean? = null,
)

@Serializable
data class ChannelSectionsDto(
    val entries: Map<String, ChannelSectionEntryDto> = emptyMap(),
)

@Serializable
data class ChannelSectionEntryDto(
    val id: String? = null,
    @SerialName("value_type") val valueType: String? = null,
    val value: ChannelSectionValueDto? = null,
)

@Serializable
data class ChannelSectionValueDto(
    val title: String? = null,
    val uris: List<String> = emptyList(),
)
