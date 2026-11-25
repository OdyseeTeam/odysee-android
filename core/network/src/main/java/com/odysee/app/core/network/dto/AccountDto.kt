package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

@Serializable
data class AccountListParams(
    val placeholder: String? = null,
)

@Serializable
data class AccountListResult(
    @SerialName("lbc_mainnet") val lbcMainnet: List<AccountDto> = emptyList(),
)

@Serializable
data class AccountDto(
    val id: String,
    val name: String? = null,
    val satoshis: JsonElement? = null,
    @SerialName("is_default") val isDefault: Boolean? = null,
) {
    fun satoshiBalance(): Long = when (val s = satoshis) {
        is JsonPrimitive -> s.longOrNull
            ?: s.contentOrNull?.toLongOrNull()
            ?: s.contentOrNull?.toDoubleOrNull()?.toLong()
            ?: 0L
        else -> 0L
    }
}

@Serializable
data class AccountSetParams(
    @SerialName("account_id") val accountId: String,
    @SerialName("default") val default: Boolean = true,
)
