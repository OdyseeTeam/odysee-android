package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalletBalanceParams(val placeholder: String? = null)

@Serializable
data class WalletBalanceResult(
    val available: String? = null,
    val total: String? = null,
    val reserved: String? = null,
    @SerialName("reserved_subtotals") val reservedSubtotals: ReservedSubtotals? = null,
)

@Serializable
data class ReservedSubtotals(
    val claims: String? = null,
    val supports: String? = null,
    val tips: String? = null,
)

@Serializable
data class TransactionListParams(
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 30,
)

@Serializable
data class TransactionListResult(
    val items: List<TransactionDto> = emptyList(),
    val page: Int? = null,
    @SerialName("page_size") val pageSize: Int? = null,
)

@Serializable
data class TransactionDto(
    val txid: String? = null,
    val timestamp: Long? = null,
    val value: String? = null,
    val fee: String? = null,
    val confirmations: Long? = null,
    @SerialName("claim_info") val claimInfo: List<TxClaimInfo>? = null,
    @SerialName("support_info") val supportInfo: List<TxClaimInfo>? = null,
    @SerialName("abandon_info") val abandonInfo: List<TxClaimInfo>? = null,
)

@Serializable
data class TxClaimInfo(
    @SerialName("claim_id") val claimId: String? = null,
    @SerialName("claim_name") val claimName: String? = null,
    val amount: String? = null,
)

@Serializable
data class AddressUnusedParams(val placeholder: String? = null)

@Serializable
data class WalletSendParams(
    val addresses: List<String>,
    val amount: String,
)

@Serializable
data class WalletSendResult(
    val txid: String? = null,
    val height: Long? = null,
    val total_fee: String? = null,
    val total_input: String? = null,
    val total_output: String? = null,
)
