package com.odysee.app.core.data.wallet

import com.odysee.app.core.network.LbryioApi
import com.odysee.app.core.network.SdkProxyApi
import com.odysee.app.core.network.dto.AccountListParams
import com.odysee.app.core.network.dto.AccountSetParams
import com.odysee.app.core.network.dto.SyncApplyParams
import com.odysee.app.core.network.dto.SyncHashParams
import com.odysee.app.core.network.jsonrpc.JsonRpcRequest
import com.odysee.app.core.network.jsonrpc.unwrap
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypted-wallet sync via lbryio `sync/get` + SDK `sync_apply` (mirrors odysee-frontend). */
@Singleton
class WalletSyncRepository @Inject constructor(
    private val sdkProxyApi: SdkProxyApi,
    private val lbryioApi: LbryioApi,
) {

    sealed class Result {
        data object NotChanged : Result()
        data class Synced(val newHash: String) : Result()
        data class Failed(val message: String) : Result()
    }

    suspend fun sync(password: String = ""): Result {
        val result = runCatching {
            val localHash = sdkProxyApi.syncHash(
                JsonRpcRequest(method = "sync_hash", params = SyncHashParams()),
            ).unwrap()
            android.util.Log.i(TAG, "sync: localHash=${localHash.take(12)}…")

            val envelope = lbryioApi.syncGet(hash = localHash)
            if (!envelope.success) {
                val err = envelope.error.orEmpty()
                android.util.Log.w(TAG, "sync/get failed: $err")
                if ("no wallet" in err.lowercase()) return@runCatching Result.NotChanged
                return@runCatching Result.Failed(err.ifBlank { "sync/get failed" })
            }
            val getResp = envelope.data
                ?: return@runCatching Result.NotChanged
            android.util.Log.i(
                TAG,
                "sync/get: changed=${getResp.changed} hasData=${!getResp.data.isNullOrBlank()}",
            )
            if (!getResp.changed || getResp.data.isNullOrBlank()) {
                return@runCatching Result.NotChanged
            }

            val applied = sdkProxyApi.syncApply(
                JsonRpcRequest(
                    method = "sync_apply",
                    params = SyncApplyParams(
                        password = password,
                        data = getResp.data,
                        blocking = true,
                    ),
                ),
            ).unwrap()
            val appliedHash = applied.hash
            val appliedData = applied.data
            if (appliedHash.isNullOrBlank() || appliedData.isNullOrBlank()) {
                android.util.Log.w(TAG, "sync_apply returned empty hash/data")
                return@runCatching Result.NotChanged
            }
            android.util.Log.i(TAG, "sync_apply: hash=${appliedHash.take(12)}…")

            if (appliedHash != getResp.hash) {
                lbryioApi.syncSet(
                    oldHash = getResp.hash.orEmpty(),
                    newHash = appliedHash,
                    data = appliedData,
                )
            }
            Result.Synced(appliedHash)
        }.getOrElse { err ->
            android.util.Log.e(TAG, "sync failed", err)
            Result.Failed(err.message ?: "Wallet sync failed")
        }

        runCatching { ensureDefaultAccount() }
            .onFailure { android.util.Log.e(TAG, "ensureDefaultAccount failed", it) }

        return result
    }

    /** Prefer funded account, else the post-sync account (usually index 1). */
    suspend fun ensureDefaultAccount() {
        val accounts = sdkProxyApi.accountList(
            JsonRpcRequest(method = "account_list", params = AccountListParams()),
        ).unwrap().lbcMainnet
        android.util.Log.i(
            TAG,
            "account_list: count=${accounts.size} " +
                "balances=${accounts.map { it.satoshiBalance() }} " +
                "defaults=${accounts.map { it.isDefault }}",
        )
        val preferred = pickDefaultAccount(accounts) ?: return
        if (preferred.isDefault == true) {
            android.util.Log.i(TAG, "default account already ${preferred.id.take(8)}…")
            return
        }
        android.util.Log.i(TAG, "account_set default=${preferred.id.take(8)}…")
        sdkProxyApi.accountSet(
            JsonRpcRequest(
                method = "account_set",
                params = AccountSetParams(accountId = preferred.id, default = true),
            ),
        ).unwrap()
    }

    private companion object {
        const val TAG = "WalletSync"
    }
}
