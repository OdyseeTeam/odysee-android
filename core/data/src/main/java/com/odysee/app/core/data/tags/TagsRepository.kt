package com.odysee.app.core.data.tags

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

interface TagsRepository {
    val tags: Flow<List<String>>
    suspend fun isFollowed(tag: String): Boolean
    suspend fun follow(tag: String)
    suspend fun unfollow(tag: String)
    suspend fun toggle(tag: String): Boolean
    suspend fun syncFromServer(): Result<Int>
}

@Singleton
class TagsRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val sdkProxyApi: SdkProxyApi,
) : TagsRepository {

    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val tags: Flow<List<String>> = authPreferences.followedTags

    override suspend fun isFollowed(tag: String): Boolean =
        authPreferences.followedTags.first().contains(tag.lowercase())

    override suspend fun follow(tag: String) {
        val key = tag.lowercase()
        val current = authPreferences.followedTags.first()
        if (key !in current) {
            authPreferences.setFollowedTags(current + key)
            bgScope.launch { runCatching { writeToSharedPreference() } }
        }
    }

    override suspend fun unfollow(tag: String) {
        val key = tag.lowercase()
        val current = authPreferences.followedTags.first()
        if (key in current) {
            authPreferences.setFollowedTags(current - key)
            bgScope.launch { runCatching { writeToSharedPreference() } }
        }
    }

    override suspend fun toggle(tag: String): Boolean {
        return if (isFollowed(tag)) {
            unfollow(tag); false
        } else {
            follow(tag); true
        }
    }

    override suspend fun syncFromServer(): Result<Int> = runCatching {
        val resp = sdkProxyApi.preferenceGet(
            JsonRpcRequest(method = "preference_get", params = PreferenceGetParams("shared")),
        ).unwrap()
        val shared = (resp["shared"] as? JsonObject) ?: return@runCatching 0
        val value = (shared["value"] as? JsonObject) ?: return@runCatching 0
        val tags = (value["tags"] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull?.lowercase() } ?: return@runCatching 0
        authPreferences.setFollowedTags(tags)
        tags.size
    }

    private suspend fun writeToSharedPreference() {
        val list = tags.first()
        val existing = runCatching {
            sdkProxyApi.preferenceGet(
                JsonRpcRequest(method = "preference_get", params = PreferenceGetParams("shared")),
            ).unwrap()
        }.getOrNull()
        val sharedExisting = (existing?.get("shared") as? JsonObject)
        val valueExisting = (sharedExisting?.get("value") as? JsonObject)
        val newValue = buildJsonObject {
            valueExisting?.forEach { (k, v) -> if (k != "tags") put(k, v) }
            put("tags", buildJsonArray { list.forEach { add(JsonPrimitive(it)) } })
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
