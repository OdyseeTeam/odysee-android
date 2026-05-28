package com.odysee.app.core.data.preferences

import com.odysee.app.core.network.SdkProxyApi
import com.odysee.app.core.network.dto.PreferenceGetParams
import com.odysee.app.core.network.dto.PreferenceSetParams
import com.odysee.app.core.network.jsonrpc.JsonRpcRequest
import com.odysee.app.core.network.jsonrpc.unwrap
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

    /** Fetch the inner `value` object of the shared envelope. */
    suspend fun readValueObject(): JsonObject? = runCatching {
        val resp = sdkProxyApi.preferenceGet(
            JsonRpcRequest(
                method = "preference_get",
                params = PreferenceGetParams("shared"),
            ),
        ).unwrap()
        val shared = resp["shared"] as? JsonObject ?: return@runCatching null
        shared["value"] as? JsonObject
    }.getOrNull()

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
    }

    /** Convenience: encode [value] via kotlinx.serialization and patch one key. */
    suspend fun <T> writeSlice(key: String, serializer: SerializationStrategy<T>, value: T) {
        patch(mapOf(key to json.encodeToJsonElement(serializer, value)))
    }
}
