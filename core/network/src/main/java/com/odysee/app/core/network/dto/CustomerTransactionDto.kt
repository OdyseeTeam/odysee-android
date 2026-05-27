package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerTransactionDto(
    @SerialName("claim_id") val claimId: String? = null,
    @SerialName("source_claim_id") val sourceClaimId: String? = null,
    @SerialName("target_claim_id") val targetClaimId: String? = null,
    val type: String? = null,
    val status: String? = null,
    @SerialName("validity_seconds") val validitySeconds: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)
