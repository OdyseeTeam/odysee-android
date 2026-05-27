package com.odysee.app.core.data.collections

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

data class PlaylistSummary(
    val id: String,
    val name: String,
    val itemUrls: List<String>,
    val updatedAt: Long,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val isPublic: Boolean = false,
    val autoPublish: Boolean = false,
)

data class PlaylistDraft(
    val id: String? = null,
    val name: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val tags: List<String> = emptyList(),
    val isPublic: Boolean = false,
)

interface PlaylistsRepository {
    val playlists: Flow<List<PlaylistSummary>>
    suspend fun syncFromServer(): Result<Int>
    suspend fun upsertLocalPlaylist(draft: PlaylistDraft): String
    suspend fun deleteLocalPlaylist(id: String)
    suspend fun getLocalPlaylist(id: String): PlaylistSummary?
    suspend fun addItem(playlistId: String, permanentUrl: String)
    suspend fun removeItem(playlistId: String, permanentUrl: String)
    suspend fun setAutoPublish(playlistId: String, enabled: Boolean)
}

@Singleton
class PlaylistsRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val sdkProxyApi: SdkProxyApi,
) : PlaylistsRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun pushToServerAsync() {
        bgScope.launch { runCatching { writePlaylistsToSharedPreference() } }
    }

    private suspend fun writePlaylistsToSharedPreference() {
        val current = decode(authPreferences.customPlaylists.first())
        val existing = runCatching {
            sdkProxyApi.preferenceGet(
                JsonRpcRequest(method = "preference_get", params = PreferenceGetParams("shared")),
            ).unwrap()
        }.getOrNull()
        val sharedExisting = (existing?.get("shared") as? JsonObject)
        val valueExisting = (sharedExisting?.get("value") as? JsonObject)
        val unpublishedExisting = (valueExisting?.get("unpublishedCollections") as? JsonObject)
        val autoPublishExisting = (valueExisting?.get("autoPublishById") as? JsonObject)

        // Preserve any non-playlist or published entries we don't own.
        val newUnpublished = buildJsonObject {
            unpublishedExisting?.forEach { (k, v) ->
                val obj = v as? JsonObject
                val type = obj?.get("type")?.jsonPrimitive?.contentOrNull
                // Drop only our playlist entries; keep everything else.
                if (type != "playlist") put(k, v)
            }
            current.forEach { p ->
                put(p.id, buildJsonObject {
                    put("id", JsonPrimitive(p.id))
                    put("name", JsonPrimitive(p.name))
                    put("type", JsonPrimitive("playlist"))
                    put("items", buildJsonArray { p.itemUrls.forEach { add(JsonPrimitive(it)) } })
                    put("updatedAt", JsonPrimitive(p.updatedAt / 1000L))
                    p.thumbnailUrl?.takeIf { it.isNotBlank() }?.let {
                        put("thumbnail", buildJsonObject { put("url", JsonPrimitive(it)) })
                    }
                    p.description?.takeIf { it.isNotBlank() }?.let {
                        put("description", JsonPrimitive(it))
                    }
                    if (p.tags.isNotEmpty()) {
                        put("tags", buildJsonArray { p.tags.forEach { add(JsonPrimitive(it)) } })
                    }
                    if (p.isPublic) put("isPublic", JsonPrimitive(true))
                })
            }
        }

        // Merge autoPublishById — keep any pre-existing keys we don't know about,
        // overwrite ours.
        val knownIds = current.map { it.id }.toSet()
        val newAutoPublish = buildJsonObject {
            autoPublishExisting?.forEach { (k, v) -> if (k !in knownIds) put(k, v) }
            current.forEach { p ->
                if (p.autoPublish) put(p.id, JsonPrimitive(true))
            }
        }

        val newValue = buildJsonObject {
            valueExisting?.forEach { (k, v) ->
                if (k != "unpublishedCollections" && k != "autoPublishById") put(k, v)
            }
            put("unpublishedCollections", newUnpublished)
            put("autoPublishById", newAutoPublish)
        }
        val newShared = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("version", JsonPrimitive("0.1"))
            put("value", newValue)
        }
        sdkProxyApi.preferenceSet(
            JsonRpcRequest(
                method = "preference_set",
                params = PreferenceSetParams(key = "shared", value = newShared.toString()),
            ),
        )
    }

    override val playlists: Flow<List<PlaylistSummary>> =
        authPreferences.customPlaylists.map { raw -> decode(raw) }

    override suspend fun syncFromServer(): Result<Int> = runCatching {
        val collections = mutableMapOf<String, PlaylistSummary>()
        var autoPublishMap: Map<String, Boolean> = emptyMap()

        // 1. Private / unpublished / edited collections from shared preferences.
        val resp = runCatching {
            sdkProxyApi.preferenceGet(
                JsonRpcRequest(method = "preference_get", params = PreferenceGetParams("shared")),
            ).unwrap()
        }.getOrNull()
        if (resp != null) {
            val shared = resp["shared"] as? JsonObject
            val value = shared?.get("value") as? JsonObject
            autoPublishMap = (value?.get("autoPublishById") as? JsonObject)
                ?.mapValues { (_, v) ->
                    v.jsonPrimitive.contentOrNull?.toBoolean() ?: false
                } ?: emptyMap()
            if (value != null) {
                listOf("unpublishedCollections", "editedCollections").forEach { key ->
                    val group = value[key] as? JsonObject ?: return@forEach
                    for ((id, raw) in group) {
                        val coll = raw as? JsonObject ?: continue
                        if ((coll["type"]?.jsonPrimitive?.contentOrNull) != "playlist") continue
                        val name = coll["name"]?.jsonPrimitive?.contentOrNull ?: continue
                        val itemUrls = (coll["items"] as? JsonArray)
                            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                        val updatedAt = (coll["updatedAt"]?.jsonPrimitive?.longOrNull)?.let { it * 1000L } ?: 0L
                        val thumb = (coll["thumbnail"] as? JsonObject)
                            ?.get("url")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                            ?: coll["thumbnail"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                        val desc = coll["description"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                        val tags = (coll["tags"] as? JsonArray)
                            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                        val isPublic = coll["isPublic"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
                        collections[id] = PlaylistSummary(
                            id = id,
                            name = name,
                            itemUrls = itemUrls,
                            updatedAt = updatedAt,
                            thumbnailUrl = thumb,
                            description = desc,
                            tags = tags,
                            isPublic = isPublic,
                        )
                    }
                }
            }
        }

        // 2. Published collections via collection_list (paginated).
        runCatching {
            var page = 1
            do {
                val list = sdkProxyApi.collectionList(
                    JsonRpcRequest(
                        method = "collection_list",
                        params = com.odysee.app.core.network.dto.CollectionListParams(
                            resolve = true,
                            page = page,
                            pageSize = 50,
                        ),
                    ),
                ).unwrap()
                list.items.forEach { c ->
                    val name = c.value?.title ?: c.name ?: return@forEach
                    val urls = c.value?.claims ?: emptyList()
                    val updatedAt = c.timestamp ?: 0L
                    collections[c.claimId] = PlaylistSummary(
                        id = c.claimId,
                        name = name,
                        itemUrls = urls.map { id -> "lbry://?#$id" },
                        updatedAt = updatedAt,
                        thumbnailUrl = c.value?.thumbnail?.url?.takeIf { it.isNotBlank() },
                        description = c.value?.description?.takeIf { it.isNotBlank() },
                        tags = c.value?.tags ?: emptyList(),
                        isPublic = true,
                    )
                }
                val total = list.totalPages ?: 1
                page++
                if (page > total) break
            } while (true)
        }

        val summaries = collections.values
            .filter { it.id != "watchlater" && it.id != "favorites" }
            .map { s -> if (autoPublishMap[s.id] == true) s.copy(autoPublish = true) else s }
            .sortedByDescending { it.updatedAt }

        // Enrich with the first-item thumbnail for each playlist that has at
        // least one entry — one batched claim_search call covers them all.
        val firstClaimIds = summaries.mapNotNull { extractClaimId(it.itemUrls.firstOrNull()) }
        val thumbnailByClaimId = if (firstClaimIds.isEmpty()) emptyMap()
        else runCatching {
            sdkProxyApi.claimSearch(
                JsonRpcRequest(
                    method = "claim_search",
                    params = com.odysee.app.core.network.dto.ClaimSearchParams(
                        claimType = listOf("stream"),
                        claimIds = firstClaimIds.distinct(),
                        pageSize = firstClaimIds.size.coerceAtMost(50),
                        noTotals = true,
                        notTags = null,
                        hasSource = null,
                        streamTypes = null,
                    ),
                ),
            ).unwrap().items.associate { c ->
                c.claimId to (c.value?.thumbnail?.url?.takeIf { it.isNotBlank() })
            }
        }.getOrNull().orEmpty()

        val enriched = summaries.map { s ->
            val cid = extractClaimId(s.itemUrls.firstOrNull())
            val thumb = cid?.let { thumbnailByClaimId[it] }
            if (thumb != null) s.copy(thumbnailUrl = thumb) else s
        }

        val encoded = encode(enriched)
        authPreferences.setCustomPlaylists(encoded)
        enriched.size
    }

    override suspend fun upsertLocalPlaylist(draft: PlaylistDraft): String {
        val current = decode(authPreferences.customPlaylists.first()).toMutableList()
        val now = System.currentTimeMillis()
        val targetId = draft.id ?: ("local-" + java.util.UUID.randomUUID().toString().take(12))
        val existingIdx = current.indexOfFirst { it.id == targetId }
        val cleanName = draft.name.trim().ifBlank { "Untitled playlist" }
        val cleanDescription = draft.description?.trim()?.takeIf { it.isNotBlank() }
        val cleanThumb = draft.thumbnailUrl?.trim()?.takeIf { it.isNotBlank() }
        if (existingIdx >= 0) {
            val ex = current[existingIdx]
            current[existingIdx] = ex.copy(
                name = cleanName,
                description = cleanDescription,
                thumbnailUrl = cleanThumb ?: ex.thumbnailUrl,
                tags = draft.tags,
                isPublic = draft.isPublic,
                updatedAt = now,
            )
        } else {
            current.add(
                0,
                PlaylistSummary(
                    id = targetId,
                    name = cleanName,
                    itemUrls = emptyList(),
                    updatedAt = now,
                    thumbnailUrl = cleanThumb,
                    description = cleanDescription,
                    tags = draft.tags,
                    isPublic = draft.isPublic,
                ),
            )
        }
        authPreferences.setCustomPlaylists(encode(current))
        pushToServerAsync()
        return targetId
    }

    override suspend fun deleteLocalPlaylist(id: String) {
        val current = decode(authPreferences.customPlaylists.first())
        val updated = current.filterNot { it.id == id }
        authPreferences.setCustomPlaylists(encode(updated))
        pushToServerAsync()
    }

    override suspend fun getLocalPlaylist(id: String): PlaylistSummary? {
        return decode(authPreferences.customPlaylists.first()).firstOrNull { it.id == id }
    }

    override suspend fun addItem(playlistId: String, permanentUrl: String) {
        val url = permanentUrl.trim().ifBlank { return }
        val current = decode(authPreferences.customPlaylists.first()).toMutableList()
        val idx = current.indexOfFirst { it.id == playlistId }
        if (idx < 0) return
        val ex = current[idx]
        if (ex.itemUrls.contains(url)) return
        current[idx] = ex.copy(
            itemUrls = ex.itemUrls + url,
            updatedAt = System.currentTimeMillis(),
        )
        authPreferences.setCustomPlaylists(encode(current))
        pushToServerAsync()
    }

    override suspend fun removeItem(playlistId: String, permanentUrl: String) {
        val url = permanentUrl.trim().ifBlank { return }
        val current = decode(authPreferences.customPlaylists.first()).toMutableList()
        val idx = current.indexOfFirst { it.id == playlistId }
        if (idx < 0) return
        val ex = current[idx]
        if (!ex.itemUrls.contains(url)) return
        current[idx] = ex.copy(
            itemUrls = ex.itemUrls - url,
            updatedAt = System.currentTimeMillis(),
        )
        authPreferences.setCustomPlaylists(encode(current))
        pushToServerAsync()
    }

    override suspend fun setAutoPublish(playlistId: String, enabled: Boolean) {
        val current = decode(authPreferences.customPlaylists.first()).toMutableList()
        val idx = current.indexOfFirst { it.id == playlistId }
        if (idx < 0) return
        val ex = current[idx]
        if (ex.autoPublish == enabled) return
        current[idx] = ex.copy(autoPublish = enabled, updatedAt = System.currentTimeMillis())
        authPreferences.setCustomPlaylists(encode(current))
        pushToServerAsync()
    }

    private fun extractClaimId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val hash = url.lastIndexOf('#')
        if (hash < 0 || hash >= url.length - 1) return null
        return url.substring(hash + 1).takeWhile { it.isLetterOrDigit() }.takeIf { it.isNotBlank() }
    }

    private fun decode(raw: String?): List<PlaylistSummary> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
            arr.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val urls = (obj["items"] as? JsonArray)
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                val updatedAt = obj["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L
                val thumb = obj["thumbnail"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                val desc = obj["description"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                val tags = (obj["tags"] as? JsonArray)
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                val isPublic = obj["isPublic"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
                val autoPublish = obj["autoPublish"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
                PlaylistSummary(
                    id = id,
                    name = name,
                    itemUrls = urls,
                    updatedAt = updatedAt,
                    thumbnailUrl = thumb,
                    description = desc,
                    tags = tags,
                    isPublic = isPublic,
                    autoPublish = autoPublish,
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encode(list: List<PlaylistSummary>): String {
        val arr = list.map { p ->
            JsonObject(
                buildMap {
                    put("id", JsonPrimitive(p.id))
                    put("name", JsonPrimitive(p.name))
                    put("items", JsonArray(p.itemUrls.map { JsonPrimitive(it) }))
                    put("updatedAt", JsonPrimitive(p.updatedAt))
                    if (!p.thumbnailUrl.isNullOrBlank()) {
                        put("thumbnail", JsonPrimitive(p.thumbnailUrl))
                    }
                    p.description?.takeIf { it.isNotBlank() }?.let {
                        put("description", JsonPrimitive(it))
                    }
                    if (p.tags.isNotEmpty()) {
                        put("tags", JsonArray(p.tags.map { JsonPrimitive(it) }))
                    }
                    if (p.isPublic) {
                        put("isPublic", JsonPrimitive("true"))
                    }
                    if (p.autoPublish) {
                        put("autoPublish", JsonPrimitive("true"))
                    }
                },
            )
        }
        return JsonArray(arr).toString()
    }
}
