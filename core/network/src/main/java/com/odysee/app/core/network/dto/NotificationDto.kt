package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: Long,
    @SerialName("notification_rule") val rule: String? = null,
    @SerialName("notification_parameters") val parameters: NotificationParameters? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("is_seen") val isSeen: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("active_at") val activeAt: String? = null,
    val type: String? = null,
    @SerialName("group_count") val groupCount: Int? = null,
)

@Serializable
data class NotificationParameters(
    val device: NotificationDevice? = null,
    val dynamic: NotificationDynamic? = null,
)

@Serializable
data class NotificationDevice(
    val target: String? = null,
    val title: String? = null,
    val text: String? = null,
)

/**
 * Per-rule notification payload. The server emits different fields
 * depending on the notification type (new-content vs comment vs
 * comment-reply, etc.), but the small set of fields the app actually
 * reads is stable. Add fields here as we surface more notification
 * types — every field is optional so unrelated rules just leave them
 * null.
 */
@Serializable
data class NotificationDynamic(
    @SerialName("channel_url") val channelUrl: String? = null,
    @SerialName("channel_thumbnail") val channelThumbnail: String? = null,
    @SerialName("comment_author") val commentAuthor: String? = null,
    @SerialName("comment_author_thumbnail") val commentAuthorThumbnail: String? = null,
    @SerialName("reply_author") val replyAuthor: String? = null,
)
