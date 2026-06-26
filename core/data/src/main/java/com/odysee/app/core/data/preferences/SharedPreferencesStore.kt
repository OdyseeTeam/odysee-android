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

/**
 * The Odysee SDK persists the user's per-account preferences as a single
 * "shared" key on `preference_get`/`preference_set`. The payload is an
 * envelope `{ type, version, value }` where `value` is an object whose
 * top-level keys are namespaces ("subscriptions", "blocked", "tags",
 * "unpublishedCollections", …).
 *
 * Each consumer used to inline the get/set round-trip plus the envelope
 * wrapping plus the "preserve other keys" merge. This class is the single
 * owner of that protocol; consumers just deal in typed slices.
 */
@Singleton
class SharedPreferencesStore @Inject constructor(
    private val sdkProxyApi: SdkProxyApi,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    // Cache the shared-prefs envelope so a sync burst (subscriptions +
    // blocked + tags + playlists + settings + watchLater + favorites all
    // calling readSlice() in parallel on login) shares ONE network round
    // trip instead of firing seven identical preference_get requests.
    //
    // - In-flight requests are deduped via [inFlight]: concurrent callers
    //   await the same Deferred.
    // - Completed reads cache for [cacheTtlMs]; subsequent reads within
    //   that window skip the network entirely.
    // - Any call to [patch] invalidates the cache so the next read sees
    //   the just-written changes.
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cacheMutex = Mutex()
    private var cachedValue: JsonObject? = null
    private var cachedAt: Long = 0L
    private var inFlight: kotlinx.coroutines.Deferred<JsonObject?>? = null
    private val cacheTtlMs: Long = 30_000L

    /** Fetch the inner `value` object of the shared envelope. */
    suspend fun readValueObject(forceRefresh: Boolean = false): JsonObject? {
        val deferred = cacheMutex.withLock {
            val now = System.currentTimeMillis()
            if (!forceRefresh && cachedValue != null && (now - cachedAt) < cacheTtlMs) {
                return cachedValue
            }
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
                // Settle the cache once the request completes.
                cacheScope.launch {
                    val result = runCatching { d.await() }.getOrNull()
                    cacheMutex.withLock {
                        cachedValue = result
                        cachedAt = System.currentTimeMillis()
                        // Only clear inFlight if it's still pointing at our
                        // deferred — concurrent invalidations might have
                        // replaced it already.
                        if (inFlight === d) inFlight = null
                    }
                }
            }
        }
        return deferred.await()
    }

    /** Drop the cache so the next read goes to the network. */
    private suspend fun invalidate() = cacheMutex.withLock {
        cachedValue = null
        cachedAt = 0L
    }

    /**
     * Read a typed slice — pulls one top-level key out of `value` and decodes
     * it through kotlinx.serialization. Returns null if the key is missing or
     * the slice fails to decode.
     */
    suspend fun <T> readSlice(key: String, deserializer: DeserializationStrategy<T>): T? {
        val value = readValueObject() ?: return null
        val element = value[key] ?: return null
        return runCatching { json.decodeFromJsonElement(deserializer, element) }.getOrNull()
    }

    /**
     * Update top-level keys inside `value`. Any keys present in the existing
     * blob but absent from [overrides] are preserved verbatim. Wraps the
     * result in the envelope and POSTs `preference_set` for "shared".
     */
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
        // The server now holds a newer state than our cache; force the next
        // reader to fetch fresh. We could optimistically prime the cache
        // with `newValue` but the round-trip is harmless and avoids
        // dropping in-flight server-side merges of other clients.
        invalidate()
    }

    /** Convenience: encode [value] via kotlinx.serialization and patch one key. */
    suspend fun <T> writeSlice(key: String, serializer: SerializationStrategy<T>, value: T) {
        patch(mapOf(key to json.encodeToJsonElement(serializer, value)))
    }
}
