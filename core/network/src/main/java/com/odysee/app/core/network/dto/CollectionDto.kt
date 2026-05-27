package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CollectionListParams(
    val resolve: Boolean = true,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 50,
)

@Serializable
data class CollectionListResult(
    val items: List<CollectionClaimDto> = emptyList(),
    val page: Int? = null,
    @SerialName("page_size") val pageSize: Int? = null,
    @SerialName("total_pages") val totalPages: Int? = null,
    @SerialName("total_items") val totalItems: Int? = null,
)

@Serializable
data class CollectionClaimDto(
    @SerialName("claim_id") val claimId: String,
    val name: String? = null,
    @SerialName("permanent_url") val permanentUrl: String? = null,
    @SerialName("canonical_url") val canonicalUrl: String? = null,
    val timestamp: Long? = null,
    val value: CollectionValueDto? = null,
    @SerialName("signing_channel") val signingChannel: ClaimDto? = null,
)

@Serializable
data class CollectionValueDto(
    val title: String? = null,
    val description: String? = null,
    val thumbnail: ThumbnailDto? = null,
    val claims: List<String>? = null,
    val tags: List<String>? = null,
)
