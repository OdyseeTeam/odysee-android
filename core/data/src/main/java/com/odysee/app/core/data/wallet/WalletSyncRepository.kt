package com.odysee.app.core.data.wallet

import com.odysee.app.core.network.LbryioApi
import com.odysee.app.core.network.SdkProxyApi
import com.odysee.app.core.network.dto.SyncApplyParams
import com.odysee.app.core.network.dto.SyncHashParams
import com.odysee.app.core.network.jsonrpc.JsonRpcRequest
import com.odysee.app.core.network.jsonrpc.unwrap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted-wallet sync against Odysee's sync/get + sync/set lbryio endpoints, mirroring
 * the web frontend's flow in `ui/redux/actions/sync.ts`.
 *
 * Steps:
 * 1. `sync_hash` on the SDK → fingerprint of the local wallet blob.
 * 2. `sync/get` on lbryio with that hash → returns `(hash, data, changed)`.
 *    `changed=false` means the server's blob matches ours — nothing to do.
 *    `changed=true` means the server has a different / newer blob.
 * 3. `sync_apply` on the SDK with `{ password, data }` → SDK merges the
 *    server blob with our local wallet and emits a fresh `(hash, data)`.
 * 4. If the post-apply hash differs from what the server gave us, push it
 *    back via `sync/set { old_hash, new_hash, data }`.
 *
 * Triggers: sign-in success, post-purchase, post-tip, periodic refresh.
 */
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

    /**
     * Runs the full hash → get → apply → set chain. [password] is the wallet
     * encryption password — empty string for the default unencrypted wallet
     * (which is what Odysee normally uses).
     */
    suspend fun sync(password: String = ""): Result = runCatching {
        // 1. Local hash.
        val localHash = sdkProxyApi.syncHash(
            JsonRpcRequest(method = "sync_hash", params = SyncHashParams()),
        ).unwrap()

        // 2. Ask the server.
        val getResp = lbryioApi.syncGet(hash = localHash).data
            ?: return@runCatching Result.NotChanged
        if (!getResp.changed || getResp.data.isNullOrBlank()) {
            return@runCatching Result.NotChanged
        }

        // 3. Apply server blob to our wallet via the SDK.
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
            return@runCatching Result.NotChanged
        }

        // 4. If the post-apply hash diverged from the server's, push it back.
        if (appliedHash != getResp.hash) {
            lbryioApi.syncSet(
                oldHash = getResp.hash.orEmpty(),
                newHash = appliedHash,
                data = appliedData,
            )
        }
        Result.Synced(appliedHash)
    }.getOrElse { err ->
        Result.Failed(err.message ?: "Wallet sync failed")
    }
}
