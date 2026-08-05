package com.odysee.app.core.data.collections

import com.odysee.app.core.data.preferences.CollectionDoc
import com.odysee.app.core.data.preferences.CollectionThumbnail
import com.odysee.app.core.data.preferences.SharedPrefSlices
import com.odysee.app.core.data.preferences.SharedPreferencesStore
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

data class Playlist(
    val id: String,
    val name: String,
    val itemUrls: List<String>,
    val updatedAt: Long,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val isPublic: Boolean = false,
    val autoPublish: Boolean = false,
    val hasUnpublishedChanges: Boolean = false,
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
    val playlists: Flow<List<Playlist>>
    suspend fun syncFromServer(): Result<Int>
    suspend fun upsertLocalPlaylist(draft: PlaylistDraft): String
    suspend fun deleteLocalPlaylist(id: String)
    suspend fun getLocalPlaylist(id: String): Playlist?
    suspend fun addItem(playlistId: String, permanentUrl: String)
    suspend fun removeItem(playlistId: String, permanentUrl: String)
    suspend fun setAutoPublish(playlistId: String, enabled: Boolean)
    suspend fun reorderItems(playlistId: String, orderedUrls: List<String>)
    suspend fun publishChanges(playlistId: String): Result<Unit>
}

@Singleton
class PlaylistsRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val sdkProxyApi: SdkProxyApi,
    private val sharedStore: SharedPreferencesStore,
    private val authRepository: com.odysee.app.core.data.auth.AuthRepository,
) : PlaylistsRepository {

    private fun signedIn(): Boolean =
        authRepository.state.value is com.odysee.app.core.data.auth.AuthState.SignedIn

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Serializes any operation that reads or writes the server's
    // shared-preferences blob. Without this, a push triggered by upsertLocal
    // can race the sync triggered by PlaylistsViewModel.init and the sync
    // overwrites the local cache with a server snapshot that doesn't yet
    // contain the just-created playlist.
    private val syncMutex = Mutex()

    private fun pushToServerAsync() {
        bgScope.launch { runCatching { writePlaylistsToSharedPreference() } }
    }

    private suspend fun writePlaylistsToSharedPreference() = syncMutex.withLock {
        val all = decode(authPreferences.customPlaylists.first())
        val current = all
            // isPublic playlists belong to collection_list, NOT unpublishedCollections.
            // Including them here pollutes the bucket with malformed entries that
            // confuse the web and iOS clients.
            .filterNot { it.isPublic }
        val edited = all.filter { it.isPublic && it.hasUnpublishedChanges }

        // Preserve unpublished entries that aren't playlists (web stores other
        // claim types in the same bucket). Preserve them verbatim as raw JSON
        // so we don't lose any future keys we don't know about.
        val existingValue = sharedStore.readValueObject()
        val existingUnpublishedRaw = existingValue?.get(SharedPrefSlices.UNPUBLISHED_COLLECTIONS) as? JsonObject

        val mergedUnpublished = buildJsonObject {
            existingUnpublishedRaw?.forEach { (id, entry) ->
                val obj = entry as? JsonObject ?: return@forEach
                val type = obj["type"]?.jsonPrimitive?.contentOrNull
                // Drop any existing playlist entry that we manage — we'll re-add
                // it below. Anything else stays verbatim.
                if (type != "playlist") put(id, entry)
            }
            current.forEach { p ->
                put(p.id, buildPlaylistDocJson(p))
            }
        }

        // Merge autoPublishById — keep keys for playlists we don't manage,
        // overwrite ours, drop entries that turned off auto-publish.
        val existingAuto = sharedStore.readSlice(
            SharedPrefSlices.AUTO_PUBLISH_BY_ID,
            SharedPrefSlices.BoolMap,
        ).orEmpty()
        val knownIds = current.map { it.id }.toSet()
        val mergedAuto = existingAuto.filterKeys { it !in knownIds }.toMutableMap()
        current.forEach { p -> if (p.autoPublish) mergedAuto[p.id] = true }

        val existingEditedRaw = existingValue?.get(SharedPrefSlices.EDITED_COLLECTIONS) as? JsonObject
        val mergedEdited = buildJsonObject {
            existingEditedRaw?.forEach { (id, entry) ->
                val obj = entry as? JsonObject ?: return@forEach
                val type = obj["type"]?.jsonPrimitive?.contentOrNull
                if (type != "playlist") put(id, entry)
            }
            edited.forEach { p ->
                put(p.id, buildPlaylistDocJson(p))
            }
        }

        sharedStore.patch(
            mapOf(
                SharedPrefSlices.UNPUBLISHED_COLLECTIONS to mergedUnpublished,
                SharedPrefSlices.EDITED_COLLECTIONS to mergedEdited,
                SharedPrefSlices.AUTO_PUBLISH_BY_ID to
                    json.encodeToJsonElement(SharedPrefSlices.BoolMap, mergedAuto),
            ),
        )
    }

    private fun buildPlaylistDocJson(p: Playlist): JsonObject = buildJsonObject {
        put("id", JsonPrimitive(p.id))
        put("name", JsonPrimitive(p.name))
        put("type", JsonPrimitive("playlist"))
        // Always emit an items array, even when empty — web and iOS rely on
        // the key being present.
        put("items", buildJsonArray { p.itemUrls.forEach { add(JsonPrimitive(it)) } })
        put("updatedAt", JsonPrimitive(p.updatedAt / 1000L))
        p.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { url ->
            put("thumbnail", buildJsonObject { put("url", JsonPrimitive(url)) })
        }
        p.description?.takeIf { it.isNotBlank() }?.let { put("description", JsonPrimitive(it)) }
        if (p.tags.isNotEmpty()) {
            put("tags", buildJsonArray { p.tags.forEach { add(JsonPrimitive(it)) } })
        }
    }

    override val playlists: Flow<List<Playlist>> =
        authPreferences.customPlaylists.map { raw -> decode(raw) }

    override suspend fun syncFromServer(): Result<Int> = runCatching {
        syncMutex.withLock { syncFromServerLocked() }
    }

    private suspend fun syncFromServerLocked(): Int {
        val collections = mutableMapOf<String, Playlist>()

        // 1. Private / unpublished / edited collections from shared preferences.
        val autoPublishMap = sharedStore.readSlice(
            SharedPrefSlices.AUTO_PUBLISH_BY_ID,
            SharedPrefSlices.BoolMap,
        ).orEmpty()
        val editedIds = mutableSetOf<String>()
        listOf(
            SharedPrefSlices.UNPUBLISHED_COLLECTIONS,
            SharedPrefSlices.EDITED_COLLECTIONS,
        ).forEach { key ->
            val isEdited = key == SharedPrefSlices.EDITED_COLLECTIONS
            val group = sharedStore.readSlice(key, SharedPrefSlices.CollectionMap) ?: return@forEach
            for ((id, coll) in group) {
                if (coll.type != "playlist") continue
                val name = coll.name ?: continue
                if (isEdited) editedIds += id
                collections[id] = Playlist(
                    id = id,
                    name = name,
                    itemUrls = coll.items,
                    updatedAt = (coll.updatedAt ?: 0L) * 1000L,
                    thumbnailUrl = coll.thumbnail?.url?.takeIf { it.isNotBlank() },
                    description = coll.description?.takeIf { it.isNotBlank() },
                    tags = coll.tags,
                    isPublic = isEdited,
                    hasUnpublishedChanges = isEdited,
                )
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
                    // c.timestamp is in seconds; normalize to ms so comparison
                    // with unpublished entries (also normalized to ms) is sane.
                    val updatedAt = (c.timestamp ?: 0L) * 1000L
                    val edited = collections[c.claimId]?.takeIf { c.claimId in editedIds }
                    if (edited != null) {
                        val editedItemIds = edited.itemUrls.mapNotNull(::extractClaimId)
                        if (editedItemIds == urls) {
                            editedIds -= c.claimId
                        } else {
                            collections[c.claimId] = edited.copy(
                                thumbnailUrl = edited.thumbnailUrl
                                    ?: c.value?.thumbnail?.url?.takeIf { it.isNotBlank() },
                                description = edited.description
                                    ?: c.value?.description?.takeIf { it.isNotBlank() },
                            )
                            return@forEach
                        }
                    }
                    collections[c.claimId] = Playlist(
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

        // Resolve every item claim referenced by published playlists so we can
        // store the full permanent_url (with the human-readable name) instead
        // of the short `lbry://?#claimId` form. While we're at it, pick up
        // each playlist's first-item thumbnail so the playlist tile has a
        // cover image. One batched claim_search per 50 IDs.
        val publishedClaimIds = summaries
            .filter { it.isPublic }
            .flatMap { it.itemUrls.mapNotNull(::extractClaimId) }
            .distinct()
        val resolved = if (publishedClaimIds.isEmpty()) emptyMap<String, Pair<String, String?>>()
        else publishedClaimIds.chunked(50).flatMap { chunk ->
            runCatching {
                sdkProxyApi.claimSearch(
                    JsonRpcRequest(
                        method = "claim_search",
                        params = com.odysee.app.core.network.dto.ClaimSearchParams(
                            claimType = listOf("stream"),
                            claimIds = chunk,
                            pageSize = chunk.size.coerceAtMost(50),
                            noTotals = true,
                            notTags = null,
                            hasSource = null,
                            streamTypes = null,
                        ),
                    ),
                ).unwrap().items.mapNotNull { c ->
                    val cid = c.claimId ?: return@mapNotNull null
                    val purl = c.permanentUrl?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val thumb = c.value?.thumbnail?.url?.takeIf { it.isNotBlank() }
                    cid to (purl to thumb)
                }
            }.getOrNull().orEmpty()
        }.toMap()

        val enriched = summaries.map { s ->
            val rewrittenItems = if (s.isPublic) {
                s.itemUrls.map { url ->
                    val cid = extractClaimId(url) ?: return@map url
                    resolved[cid]?.first ?: url
                }
            } else s.itemUrls
            val firstCid = extractClaimId(rewrittenItems.firstOrNull())
            val thumb = firstCid?.let { resolved[it]?.second }
            val withThumb = if (s.thumbnailUrl.isNullOrBlank() && thumb != null) s.copy(thumbnailUrl = thumb) else s
            if (rewrittenItems !== s.itemUrls) withThumb.copy(itemUrls = rewrittenItems) else withThumb
        }

        // Preserve any local-only playlists that exist in the current cache but
        // haven't shown up in the server response yet (likely a write that
        // hasn't been picked up by the server, or a write we issued that's
        // still propagating).
        val localCache = decode(authPreferences.customPlaylists.first())
        val seenIds = enriched.mapTo(mutableSetOf()) { it.id }
        val pending = localCache.filter { p -> !p.isPublic && p.id !in seenIds && p.id.startsWith("local-") }
        val merged = (enriched + pending).sortedByDescending { it.updatedAt }

        val encoded = encode(merged)
        authPreferences.setCustomPlaylists(encoded)
        return merged.size
    }

    override suspend fun upsertLocalPlaylist(draft: PlaylistDraft): String {
        if (!signedIn()) return draft.id ?: ""
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
                Playlist(
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
        if (!signedIn()) return
        val current = decode(authPreferences.customPlaylists.first())
        val updated = current.filterNot { it.id == id }
        authPreferences.setCustomPlaylists(encode(updated))
        pushToServerAsync()
    }

    override suspend fun getLocalPlaylist(id: String): Playlist? {
        return decode(authPreferences.customPlaylists.first()).firstOrNull { it.id == id }
    }

    override suspend fun addItem(playlistId: String, permanentUrl: String) {
        if (!signedIn()) return
        val url = permanentUrl.trim().ifBlank { return }
        val current = decode(authPreferences.customPlaylists.first()).toMutableList()
        val idx = current.indexOfFirst { it.id == playlistId }
        if (idx < 0) return
        val ex = current[idx]
        if (ex.itemUrls.contains(url)) return
        current[idx] = ex.copy(
            itemUrls = ex.itemUrls + url,
            updatedAt = System.currentTimeMillis(),
            hasUnpublishedChanges = ex.hasUnpublishedChanges || ex.isPublic,
        )
        authPreferences.setCustomPlaylists(encode(current))
        pushToServerAsync()
    }

    override suspend fun removeItem(playlistId: String, permanentUrl: String) {
        if (!signedIn()) return
        val url = permanentUrl.trim().ifBlank { return }
        val current = decode(authPreferences.customPlaylists.first()).toMutableList()
        val idx = current.indexOfFirst { it.id == playlistId }
        if (idx < 0) return
        val ex = current[idx]
        if (!ex.itemUrls.contains(url)) return
        current[idx] = ex.copy(
            itemUrls = ex.itemUrls - url,
            updatedAt = System.currentTimeMillis(),
            hasUnpublishedChanges = ex.hasUnpublishedChanges || ex.isPublic,
        )
        authPreferences.setCustomPlaylists(encode(current))
        pushToServerAsync()
    }

    override suspend fun setAutoPublish(playlistId: String, enabled: Boolean) {
        if (!signedIn()) return
        val current = decode(authPreferences.customPlaylists.first()).toMutableList()
        val idx = current.indexOfFirst { it.id == playlistId }
        if (idx < 0) return
        val ex = current[idx]
        if (ex.autoPublish == enabled) return
        current[idx] = ex.copy(autoPublish = enabled, updatedAt = System.currentTimeMillis())
        authPreferences.setCustomPlaylists(encode(current))
        pushToServerAsync()
    }

    override suspend fun reorderItems(playlistId: String, orderedUrls: List<String>) {
        if (!signedIn()) return
        val current = decode(authPreferences.customPlaylists.first()).toMutableList()
        val idx = current.indexOfFirst { it.id == playlistId }
        if (idx < 0) return
        val ex = current[idx]
        // Only accept a permutation of the existing items — never drop or add via reorder.
        if (orderedUrls.toSet() != ex.itemUrls.toSet()) return
        if (orderedUrls == ex.itemUrls) return
        current[idx] = ex.copy(itemUrls = orderedUrls, updatedAt = System.currentTimeMillis(), hasUnpublishedChanges = ex.hasUnpublishedChanges || ex.isPublic)
        authPreferences.setCustomPlaylists(encode(current))
        pushToServerAsync()
    }

    override suspend fun publishChanges(playlistId: String): Result<Unit> = runCatching {
        val p = decode(authPreferences.customPlaylists.first()).firstOrNull { it.id == playlistId }
            ?: error("Playlist not found")
        val claimIds = p.itemUrls.mapNotNull(::extractClaimId)
        sdkProxyApi.collectionUpdate(
            JsonRpcRequest(
                method = "collection_update",
                params = com.odysee.app.core.network.dto.CollectionUpdateParams(
                    claimId = playlistId,
                    claims = claimIds,
                ),
            ),
        ).unwrap()
        val current = decode(authPreferences.customPlaylists.first()).toMutableList()
        val idx = current.indexOfFirst { it.id == playlistId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(hasUnpublishedChanges = false)
            authPreferences.setCustomPlaylists(encode(current))
        }
        pushToServerAsync()
    }

    private fun extractClaimId(url: String?): String? =
        com.odysee.app.core.model.LbryUri.claimIdOf(url)

    private fun extractItemUrl(raw: String?): String? {
        val s = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        // Heal a historical Android bug: a previous build wrote object
        // toString() output of the form
        //   `OdyseeCollection.Item(url=lbry://..., itemOrder=N)`
        // into the items array. Contaminated accounts still have these
        // strings on the server. Extract the real `url=` value when we
        // see one; otherwise pass the string through.
        val match = MALFORMED_ITEM.find(s)
        return match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() } ?: s
    }

    private fun decode(raw: String?): List<Playlist> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(StoredPlaylist.serializer()),
                raw,
            ).map { it.toPlaylist(::extractItemUrl) }
        }.getOrDefault(emptyList())
    }

    private fun encode(list: List<Playlist>): String =
        json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(StoredPlaylist.serializer()),
            list.map { StoredPlaylist.fromPlaylist(it) },
        )

    private companion object {
        private val MALFORMED_ITEM = Regex(
            """OdyseeCollection\.Item\s*\(\s*url\s*=\s*([^,)\s]+).*\)""",
            RegexOption.IGNORE_CASE,
        )
    }
}

/**
 * On-disk serialization of [Playlist] for the DataStore-backed local cache.
 *
 * Kept as a separate type so the public domain `Playlist` doesn't have to
 * leak `@Serializable` annotations or care about historical field-name
 * quirks. The wire format we PUSH to the server is built independently in
 * `buildPlaylistDocJson` because the server-side schema uses different
 * field names (e.g. seconds-since-epoch `updatedAt`, nested `thumbnail`
 * object). This type is local-only.
 *
 * Backward-compat quirks:
 *  - `isPublic` / `autoPublish` used to be stored as the STRINGS `"true"`
 *    / `"false"` (see legacy encoder). [LooseBoolSerializer] accepts both
 *    forms on read and emits a real boolean on write so old caches still
 *    decode after this refactor.
 *  - `items` historically may contain Android-emitted `OdyseeCollection.Item(...)`
 *    strings; the repository's `extractItemUrl` strips them at the
 *    domain-mapping boundary instead of here, so the wire form stays
 *    transparent.
 */
@kotlinx.serialization.Serializable
private data class StoredPlaylist(
    val id: String,
    val name: String,
    val items: List<String> = emptyList(),
    val updatedAt: Long = 0L,
    val thumbnail: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    @kotlinx.serialization.Serializable(with = LooseBoolSerializer::class)
    val isPublic: Boolean = false,
    @kotlinx.serialization.Serializable(with = LooseBoolSerializer::class)
    val autoPublish: Boolean = false,
    @kotlinx.serialization.Serializable(with = LooseBoolSerializer::class)
    val hasUnpublishedChanges: Boolean = false,
) {
    fun toPlaylist(healItemUrl: (String?) -> String?): Playlist = Playlist(
        id = id,
        name = name,
        itemUrls = items.mapNotNull(healItemUrl),
        updatedAt = updatedAt,
        thumbnailUrl = thumbnail?.takeIf { it.isNotBlank() },
        description = description?.takeIf { it.isNotBlank() },
        tags = tags,
        isPublic = isPublic,
        autoPublish = autoPublish,
        hasUnpublishedChanges = hasUnpublishedChanges,
    )

    companion object {
        fun fromPlaylist(p: Playlist): StoredPlaylist = StoredPlaylist(
            id = p.id,
            name = p.name,
            items = p.itemUrls,
            updatedAt = p.updatedAt,
            thumbnail = p.thumbnailUrl?.takeIf { it.isNotBlank() },
            description = p.description?.takeIf { it.isNotBlank() },
            tags = p.tags,
            isPublic = p.isPublic,
            autoPublish = p.autoPublish,
            hasUnpublishedChanges = p.hasUnpublishedChanges,
        )
    }
}

/** Reads either a JSON boolean or the legacy `"true"`/`"false"` string. */
private object LooseBoolSerializer : kotlinx.serialization.KSerializer<Boolean> {
    override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor =
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
            "LooseBool",
            kotlinx.serialization.descriptors.PrimitiveKind.BOOLEAN,
        )

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Boolean {
        val jd = decoder as? kotlinx.serialization.json.JsonDecoder
            ?: return decoder.decodeBoolean()
        val element = jd.decodeJsonElement() as? kotlinx.serialization.json.JsonPrimitive
            ?: return false
        return looseBool(element)
    }

    private fun looseBool(p: kotlinx.serialization.json.JsonPrimitive): Boolean {
        val b: Boolean? = p.booleanOrNull
        if (b != null) return b
        return p.contentOrNull?.equals("true", ignoreCase = true) == true
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }
}
