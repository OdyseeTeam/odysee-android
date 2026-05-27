package com.odysee.app.core.data.wallet

import com.odysee.app.core.network.SdkProxyApi
import com.odysee.app.core.network.dto.AddressUnusedParams
import com.odysee.app.core.network.dto.TransactionListParams
import com.odysee.app.core.network.dto.WalletBalanceParams
import com.odysee.app.core.network.dto.WalletSendParams
import com.odysee.app.core.network.jsonrpc.JsonRpcRequest
import com.odysee.app.core.network.jsonrpc.unwrap
import javax.inject.Inject
import javax.inject.Singleton

data class WalletBalance(
    val available: Double,
    val total: Double,
    val reserved: Double,
    val reservedForClaims: Double,
    val reservedForSupports: Double,
    val reservedForTips: Double,
)

data class WalletTransaction(
    val txid: String,
    val timestamp: Long?,
    val valueLbc: Double,
    val fee: Double,
    val confirmations: Long,
    val description: String,
)

interface WalletRepository {
    suspend fun getBalance(): WalletBalance
    suspend fun getTransactions(page: Int = 1, pageSize: Int = 30): List<WalletTransaction>
    suspend fun getReceiveAddress(): String
    suspend fun sendLbc(address: String, amount: String): String
}

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val sdkProxyApi: SdkProxyApi,
) : WalletRepository {

    override suspend fun getBalance(): WalletBalance {
        val res = sdkProxyApi.walletBalance(
            JsonRpcRequest(method = "wallet_balance", params = WalletBalanceParams()),
        ).unwrap()
        return WalletBalance(
            available = res.available?.toDoubleOrNull() ?: 0.0,
            total = res.total?.toDoubleOrNull() ?: 0.0,
            reserved = res.reserved?.toDoubleOrNull() ?: 0.0,
            reservedForClaims = res.reservedSubtotals?.claims?.toDoubleOrNull() ?: 0.0,
            reservedForSupports = res.reservedSubtotals?.supports?.toDoubleOrNull() ?: 0.0,
            reservedForTips = res.reservedSubtotals?.tips?.toDoubleOrNull() ?: 0.0,
        )
    }

    override suspend fun getReceiveAddress(): String {
        return sdkProxyApi.addressUnused(
            JsonRpcRequest(method = "address_unused", params = AddressUnusedParams()),
        ).unwrap()
    }

    override suspend fun sendLbc(address: String, amount: String): String {
        val res = sdkProxyApi.walletSend(
            JsonRpcRequest(
                method = "wallet_send",
                params = WalletSendParams(addresses = listOf(address), amount = amount),
            ),
        ).unwrap()
        return res.txid ?: error("No transaction id returned")
    }

    override suspend fun getTransactions(page: Int, pageSize: Int): List<WalletTransaction> {
        val res = sdkProxyApi.transactionList(
            JsonRpcRequest(method = "transaction_list", params = TransactionListParams(page = page, pageSize = pageSize)),
        ).unwrap()
        return res.items.map { tx ->
            val firstName = tx.claimInfo?.firstOrNull()?.claimName
                ?: tx.supportInfo?.firstOrNull()?.claimName
                ?: tx.abandonInfo?.firstOrNull()?.claimName
            val desc = when {
                tx.claimInfo?.isNotEmpty() == true -> "Publish: ${firstName ?: ""}".trim().trimEnd(':')
                tx.supportInfo?.isNotEmpty() == true -> "Support: ${firstName ?: ""}".trim().trimEnd(':')
                tx.abandonInfo?.isNotEmpty() == true -> "Abandon: ${firstName ?: ""}".trim().trimEnd(':')
                else -> "Transaction"
            }
            WalletTransaction(
                txid = tx.txid.orEmpty(),
                timestamp = tx.timestamp,
                valueLbc = tx.value?.toDoubleOrNull() ?: 0.0,
                fee = tx.fee?.toDoubleOrNull() ?: 0.0,
                confirmations = tx.confirmations ?: 0L,
                description = desc,
            )
        }
    }
}
