package com.odysee.app.core.data.moderation

import com.odysee.app.core.data.subscriptions.parseLbryUrl
import com.odysee.app.core.datastore.AuthPreferences
import com.odysee.app.core.network.SdkProxyApi
import com.odysee.app.core.network.dto.PreferenceGetParams
import com.odysee.app.core.network.dto.PreferenceSetParams
import com.odysee.app.core.network.jsonrpc.JsonRpcRequest
import com.odysee.app.core.network.jsonrpc.unwrap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

data class BlockedChannel(val claimId: String, val name: String)

interface BlockedChannelsRepository {
    val blocked: Flow<List<BlockedChannel>>
    suspend fun isBlocked(claimId: String): Boolean
    suspend fun block(claimId: String, name: String)
    suspend fun unblock(claimId: String)
    suspend fun toggle(claimId: String, name: String): Boolean
    suspend fun syncFromServer(): Result<Int>
}

@Singleton
class BlockedChannelsRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val sdkProxyApi: SdkProxyApi,
) : BlockedChannelsRepository {

    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val blocked: Flow<List<BlockedChannel>> = authPreferences.blockedChannels.map { raw ->
        raw.mapNotNull { entry ->
            val parts = entry.split('|', limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) BlockedChannel(parts[0], parts[1]) else null
        }
    }

    override suspend fun isBlocked(claimId: String): Boolean =
        blocked.first().any { it.claimId == claimId }

    override suspend fun block(claimId: String, name: String) {
        val current = authPreferences.blockedChannels.first().toMutableList()
        if (current.none { it.startsWith("$claimId|") }) {
            current.add("$claimId|$name")
            authPreferences.setBlockedChannels(current)
        }
        bgScope.launch { runCatching { writeToSharedPreference() } }
    }

    override suspend fun unblock(claimId: String) {
        val current = authPreferences.blockedChannels.first()
        val filtered = current.filterNot { it.startsWith("$claimId|") }
        if (filtered.size != current.size) {
            authPreferences.setBlockedChannels(filtered)
        }
        bgScope.launch { runCatching { writeToSharedPreference() } }
    }

    override suspend fun toggle(claimId: String, name: String): Boolean {
        return if (isBlocked(claimId)) {
            unblock(claimId); false
        } else {
            block(claimId, name); true
        }
    }

    override suspend fun syncFromServer(): Result<Int> = runCatching {
        val resp = sdkProxyApi.preferenceGet(
            JsonRpcRequest(method = "preference_get", params = PreferenceGetParams(key = "shared")),
        ).unwrap()
        val shared = (resp["shared"] as? JsonObject) ?: return@runCatching 0
        val value = (shared["value"] as? JsonObject) ?: return@runCatching 0
        val urls = (value["blocked"] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: return@runCatching 0
        val parsed = urls.mapNotNull { parseLbryUrl(it) }
        val entries = parsed.map { "${it.claimId}|${it.name}" }
        authPreferences.setBlockedChannels(entries)
        entries.size
    }

    private suspend fun writeToSharedPreference() {
        val list = blocked.first()
        val urls = list.map { "lbry://${it.name}#${it.claimId}" }
        val existing = runCatching {
            sdkProxyApi.preferenceGet(
                JsonRpcRequest(method = "preference_get", params = PreferenceGetParams("shared")),
            ).unwrap()
        }.getOrNull()
        val sharedExisting = (existing?.get("shared") as? JsonObject)
        val valueExisting = (sharedExisting?.get("value") as? JsonObject)
        val newValue = buildJsonObject {
            valueExisting?.forEach { (k, v) ->
                if (k != "blocked") put(k, v)
            }
            put("blocked", buildJsonArray { urls.forEach { add(JsonPrimitive(it)) } })
        }
        val newShared = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("version", JsonPrimitive("0.1"))
            put("value", newValue)
        }
        sdkProxyApi.preferenceSet(
            JsonRpcRequest(method = "preference_set", params = PreferenceSetParams(key = "shared", value = newShared.toString())),
        )
    }
}
