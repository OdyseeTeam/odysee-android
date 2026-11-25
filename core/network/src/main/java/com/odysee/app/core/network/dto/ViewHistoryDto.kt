package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ViewHistoryPageDto(
    val items: List<ViewHistoryItemDto> = emptyList(),
    val page: Int? = null,
    @SerialName("has_more") val hasMore: Boolean? = null,
)

@Serializable
data class ViewHistoryItemDto(
    @SerialName("claim_id") val claimId: String,
    @SerialName("updated_at") val updatedAt: String? = null,
)
