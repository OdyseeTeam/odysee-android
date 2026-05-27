package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull

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
    val dynamic: JsonObject? = null,
)

@Serializable
data class NotificationDevice(
    val target: String? = null,
    val title: String? = null,
    val text: String? = null,
)

fun JsonObject.stringOrNull(key: String): String? =
    (this[key] as? JsonElement)?.jsonPrimitive?.contentOrNull
