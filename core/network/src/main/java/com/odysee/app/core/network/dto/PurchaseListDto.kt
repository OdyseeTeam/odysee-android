package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseListParams(
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 10,
    val resolve: Boolean = true,
)

@Serializable
data class PurchaseListResult(
    val items: List<PurchaseItemDto> = emptyList(),
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 10,
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_items") val totalItems: Int = 0,
)

@Serializable
data class PurchaseItemDto(
    @SerialName("claim_id") val claimId: String? = null,
    @SerialName("claim") val claim: ClaimDto? = null,
    val amount: String? = null,
    @SerialName("normalized_name") val normalizedName: String? = null,
    @SerialName("permanent_url") val permanentUrl: String? = null,
    val timestamp: Long? = null,
)

@Serializable
data class PurchaseCreateParams(
    val url: String,
    val blocking: Boolean = true,
)

@Serializable
data class PurchaseCreateResult(
    val txid: String? = null,
    val height: Long? = null,
)
