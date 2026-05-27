package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentCreateParams(
    @SerialName("claim_id") val claimId: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("channel_name") val channelName: String,
    val comment: String,
    val signature: String,
    @SerialName("signing_ts") val signingTs: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("support_tx_id") val supportTxId: String? = null,
)

@Serializable
data class CommentListParams(
    @SerialName("claim_id") val claimId: String,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 50,
    @SerialName("top_level") val topLevel: Boolean = true,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("sort_by") val sortBy: Int = 3,
)

@Serializable
data class CommentListOwnParams(
    @SerialName("author_claim_id") val authorClaimId: String,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 10,
    @SerialName("sort_by") val sortBy: Int = 1,
    @SerialName("requestor_channel_id") val requestorChannelId: String,
    @SerialName("requestor_channel_name") val requestorChannelName: String,
    val signature: String,
    @SerialName("signing_ts") val signingTs: String,
)

@Serializable
data class CommentReactListParams(
    @SerialName("comment_ids") val commentIds: String,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("channel_name") val channelName: String? = null,
    val signature: String? = null,
    @SerialName("signing_ts") val signingTs: String? = null,
)

@Serializable
data class CommentReactListResult(
    @SerialName("others_reactions") val othersReactions: Map<String, Map<String, Int>> = emptyMap(),
    @SerialName("my_reactions") val myReactions: Map<String, Map<String, Int>> = emptyMap(),
)

@Serializable
data class CommentReactParams(
    @SerialName("comment_ids") val commentIds: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("channel_name") val channelName: String,
    val signature: String,
    @SerialName("signing_ts") val signingTs: String,
    val type: String,
    val remove: Boolean = false,
    val clear_types: String? = null,
)

@Serializable
data class CommentReactResult(
    @SerialName("comment_ids") val commentIds: List<String> = emptyList(),
)

@Serializable
data class CommentPinParams(
    @SerialName("comment_id") val commentId: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("channel_name") val channelName: String,
    val signature: String,
    @SerialName("signing_ts") val signingTs: String,
    val remove: Boolean = false,
)

@Serializable
data class CommentEditParams(
    @SerialName("comment_id") val commentId: String,
    val comment: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("channel_name") val channelName: String,
    val signature: String,
    @SerialName("signing_ts") val signingTs: String,
)

@Serializable
data class CommentAbandonParams(
    @SerialName("comment_id") val commentId: String,
    @SerialName("creator_channel_id") val creatorChannelId: String? = null,
    @SerialName("creator_channel_name") val creatorChannelName: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("channel_name") val channelName: String? = null,
    val signature: String? = null,
    @SerialName("signing_ts") val signingTs: String? = null,
)

@Serializable
data class CommentListResult(
    val items: List<CommentDto> = emptyList(),
    @SerialName("total_items") val totalItems: Int? = null,
    @SerialName("total_pages") val totalPages: Int? = null,
    val page: Int? = null,
    @SerialName("page_size") val pageSize: Int? = null,
)

@Serializable
data class CommentDto(
    @SerialName("comment_id") val commentId: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("claim_id") val claimId: String? = null,
    val comment: String = "",
    @SerialName("channel_name") val channelName: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("channel_url") val channelUrl: String? = null,
    val timestamp: Long = 0,
    @SerialName("signing_ts") val signingTs: String? = null,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("is_hidden") val isHidden: Boolean = false,
    @SerialName("is_moderator") val isModerator: Boolean = false,
    @SerialName("is_global_mod") val isGlobalMod: Boolean = false,
    @SerialName("is_fiat") val isFiat: Boolean = false,
    @SerialName("is_protected") val isProtected: Boolean = false,
    @SerialName("is_creator") val isCreator: Boolean = false,
    @SerialName("replies") val replies: Int? = null,
    @SerialName("support_amount") val supportAmount: Double? = null,
)

@Serializable
data class ModerationBlockParams(
    @SerialName("mod_channel_id") val modChannelId: String,
    @SerialName("mod_channel_name") val modChannelName: String,
    @SerialName("blocked_channel_id") val blockedChannelId: String,
    @SerialName("blocked_channel_name") val blockedChannelName: String,
    @SerialName("creator_channel_id") val creatorChannelId: String? = null,
    @SerialName("creator_channel_name") val creatorChannelName: String? = null,
    @SerialName("offending_comment_id") val offendingCommentId: String? = null,
    @SerialName("block_all") val blockAll: Boolean? = null,
    @SerialName("time_out") val timeOut: Long? = null,
    val signature: String,
    @SerialName("signing_ts") val signingTs: String,
)

@Serializable
data class ModerationAddDelegateParams(
    @SerialName("mod_channel_id") val modChannelId: String,
    @SerialName("mod_channel_name") val modChannelName: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("channel_name") val channelName: String,
    val signature: String,
    @SerialName("signing_ts") val signingTs: String,
)

@Serializable
data class ModerationRemoveDelegateParams(
    @SerialName("mod_channel_id") val modChannelId: String,
    @SerialName("mod_channel_name") val modChannelName: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("channel_name") val channelName: String,
    val signature: String,
    @SerialName("signing_ts") val signingTs: String,
)
