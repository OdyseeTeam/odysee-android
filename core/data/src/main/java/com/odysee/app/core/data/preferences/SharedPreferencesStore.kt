package com.odysee.app.core.data.preferences

import com.odysee.app.core.network.SdkProxyApi
import com.odysee.app.core.network.dto.PreferenceGetParams
import com.odysee.app.core.network.dto.PreferenceSetParams
import com.odysee.app.core.network.jsonrpc.JsonRpcRequest
import com.odysee.app.core.network.jsonrpc.unwrap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** Owns the SDK `shared` preference_get/set envelope and typed slice access. */
@Singleton
class SharedPreferencesStore @Inject constructor(
    private val sdkProxyApi: SdkProxyApi,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cacheMutex = Mutex()
    private var cachedValue: JsonObject? = null
    private var cachedAt: Long = 0L
    private var inFlight: kotlinx.coroutines.Deferred<JsonObject?>? = null
    private var cacheGeneration: Long = 0L
    private val cacheTtlMs: Long = 30_000L

    suspend fun readValueObject(forceRefresh: Boolean = false): JsonObject? {
        val deferred = cacheMutex.withLock {
            val now = System.currentTimeMillis()
            if (!forceRefresh && cachedValue != null && (now - cachedAt) < cacheTtlMs) {
                return cachedValue
            }
            val generation = cacheGeneration
            inFlight ?: cacheScope.async {
                runCatching {
                    val resp = sdkProxyApi.preferenceGet(
                        JsonRpcRequest(
                            method = "preference_get",
                            params = PreferenceGetParams("shared"),
                        ),
                    ).unwrap()
                    val shared = resp["shared"] as? JsonObject ?: return@runCatching null
                    shared["value"] as? JsonObject
                }.getOrNull()
            }.also { d ->
                inFlight = d
                cacheScope.launch {
                    val result = runCatching { d.await() }.getOrNull()
                    cacheMutex.withLock {
                        if (generation != cacheGeneration) {
                            if (inFlight === d) inFlight = null
                            return@withLock
                        }
                        cachedValue = result
                        cachedAt = System.currentTimeMillis()
                        if (inFlight === d) inFlight = null
                    }
                }
            }
        }
        return deferred.await()
    }

    suspend fun invalidateCache() = cacheMutex.withLock {
        cachedValue = null
        cachedAt = 0L
        inFlight = null
        cacheGeneration++
    }

    private suspend fun invalidate() = invalidateCache()

    suspend fun <T> readSlice(key: String, deserializer: DeserializationStrategy<T>): T? {
        val value = readValueObject() ?: return null
        val element = value[key] ?: return null
        return runCatching { json.decodeFromJsonElement(deserializer, element) }.getOrNull()
    }

    suspend fun patch(overrides: Map<String, JsonElement>) {
        if (overrides.isEmpty()) return
        val existing = readValueObject()
        val newValue = buildJsonObject {
            existing?.forEach { (k, v) -> if (k !in overrides) put(k, v) }
            overrides.forEach { (k, v) -> put(k, v) }
        }
        val envelope = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("version", JsonPrimitive("0.1"))
            put("value", newValue)
        }
        sdkProxyApi.preferenceSet(
            JsonRpcRequest(
                method = "preference_set",
                params = PreferenceSetParams(
                    key = "shared",
                    value = envelope.toString(),
                ),
            ),
        )
        invalidate()
    }

    suspend fun <T> writeSlice(key: String, serializer: SerializationStrategy<T>, value: T) {
        patch(mapOf(key to json.encodeToJsonElement(serializer, value)))
    }
}
