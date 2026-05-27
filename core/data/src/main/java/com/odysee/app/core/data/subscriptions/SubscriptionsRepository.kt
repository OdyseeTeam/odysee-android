package com.odysee.app.core.data.subscriptions

import com.odysee.app.core.datastore.AuthPreferences
import com.odysee.app.core.network.LbryioApi
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
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

data class Subscription(val claimId: String, val name: String)

interface SubscriptionsRepository {
    val subscriptions: Flow<List<Subscription>>
    suspend fun isSubscribed(claimId: String): Boolean
    suspend fun subscribe(claimId: String, name: String)
    suspend fun unsubscribe(claimId: String)
    suspend fun toggle(claimId: String, name: String): Boolean
    suspend fun syncFromServer(): Result<Int>
}

@Singleton
class SubscriptionsRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val sdkProxyApi: SdkProxyApi,
    private val lbryioApi: LbryioApi,
) : SubscriptionsRepository {

    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val subscriptions: Flow<List<Subscription>> = authPreferences.subscriptions.map { raw ->
        raw.mapNotNull { entry ->
            val parts = entry.split('|', limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) Subscription(parts[0], parts[1]) else null
        }
    }

    override suspend fun isSubscribed(claimId: String): Boolean =
        subscriptions.first().any { it.claimId == claimId }

    override suspend fun subscribe(claimId: String, name: String) {
        val current = authPreferences.subscriptions.first().toMutableList()
        if (current.none { it.startsWith("$claimId|") }) {
            current.add("$claimId|$name")
            authPreferences.setSubscriptions(current)
        }
        bgScope.launch {
            runCatching {
                lbryioApi.subscriptionNew(claimId = claimId, channelName = name)
            }
            runCatching { writeSubscriptionsToSharedPreference() }
        }
    }

    override suspend fun unsubscribe(claimId: String) {
        val current = authPreferences.subscriptions.first()
        val filtered = current.filterNot { it.startsWith("$claimId|") }
        if (filtered.size != current.size) {
            authPreferences.setSubscriptions(filtered)
        }
        bgScope.launch {
            runCatching { lbryioApi.subscriptionDelete(claimId = claimId) }
            runCatching { writeSubscriptionsToSharedPreference() }
        }
    }

    private suspend fun writeSubscriptionsToSharedPreference() {
        val subs = subscriptions.first()
        val urls = subs.map { "lbry://${it.name}#${it.claimId}" }
        // Read existing shared blob so we don't wipe other prefs
        val existing = runCatching {
            sdkProxyApi.preferenceGet(
                JsonRpcRequest(method = "preference_get", params = PreferenceGetParams("shared")),
            ).unwrap()
        }.getOrNull()
        val sharedExisting = (existing?.get("shared") as? JsonObject)
        val valueExisting = (sharedExisting?.get("value") as? JsonObject)

        val newValue = buildJsonObject {
            valueExisting?.forEach { (k, v) ->
                if (k != "subscriptions" && k != "following") put(k, v)
            }
            put("subscriptions", buildJsonArray { urls.forEach { add(JsonPrimitive(it)) } })
            put(
                "following",
                buildJsonArray {
                    urls.forEach { url ->
                        add(
                            buildJsonObject {
                                put("uri", JsonPrimitive(url))
                                put("notificationsDisabled", JsonPrimitive(false))
                            },
                        )
                    }
                },
            )
        }
        val newShared = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("version", JsonPrimitive("0.1"))
            put("value", newValue)
        }
        val payload = buildJsonObject {
            put("shared", newShared)
        }
        sdkProxyApi.preferenceSet(
            JsonRpcRequest(method = "preference_set", params = PreferenceSetParams(key = "shared", value = payload["shared"].toString())),
        )
    }

    override suspend fun toggle(claimId: String, name: String): Boolean {
        return if (isSubscribed(claimId)) {
            unsubscribe(claimId)
            false
        } else {
            subscribe(claimId, name)
            true
        }
    }

    override suspend fun syncFromServer(): Result<Int> = runCatching {
        val resp = sdkProxyApi.preferenceGet(
            JsonRpcRequest(method = "preference_get", params = PreferenceGetParams(key = "shared")),
        ).unwrap()
        val shared = (resp["shared"] as? JsonObject) ?: return@runCatching 0
        val value = (shared["value"] as? JsonObject) ?: return@runCatching 0
        val urls = (value["subscriptions"] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: return@runCatching 0
        val parsed = urls.mapNotNull { parseLbryUrl(it) }
        // Merge: keep any local-only subs, server is source of truth for follows
        val current = authPreferences.subscriptions.first()
        val serverEntries = parsed.map { "${it.claimId}|${it.name}" }
        // Replace with server set (server is canonical), preserving local-only ones whose claimIds aren't in server set
        val serverIds = parsed.map { it.claimId }.toSet()
        val localOnly = current.filterNot { entry ->
            val id = entry.substringBefore('|')
            id in serverIds
        }
        authPreferences.setSubscriptions(serverEntries + localOnly)
        parsed.size
    }
}

/**
 * Parses a Lbry URL like "lbry://@channel#claimid" or "lbry://@channel:claimid" → Subscription.
 */
internal fun parseLbryUrl(url: String): Subscription? {
    val stripped = url.removePrefix("lbry://").trim()
    if (stripped.isBlank() || !stripped.startsWith("@")) return null
    val sepIdx = stripped.indexOfAny(charArrayOf('#', ':'))
    return if (sepIdx > 0) {
        val name = stripped.substring(0, sepIdx)
        val claimId = stripped.substring(sepIdx + 1).takeWhile { it != '/' }
        if (claimId.isBlank()) null else Subscription(claimId = claimId, name = name)
    } else null
}
