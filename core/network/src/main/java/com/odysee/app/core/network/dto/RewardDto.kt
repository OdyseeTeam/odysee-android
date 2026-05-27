package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RewardDto(
    @SerialName("reward_type") val rewardType: String? = null,
    @SerialName("reward_title") val rewardTitle: String? = null,
    @SerialName("reward_description") val rewardDescription: String? = null,
    @SerialName("reward_amount") val rewardAmount: Double? = null,
    @SerialName("claim_code") val claimCode: String? = null,
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
