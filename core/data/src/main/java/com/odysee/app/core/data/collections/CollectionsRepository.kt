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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

data class CollectionEntry(
    val claimId: String,
    val permanentUrl: String,
    val title: String,
    val channelName: String,
    val channelClaimId: String?,
    val thumbnailUrl: String?,
    val addedAt: Long,
)

interface LocalCollectionRepository {
    val items: Flow<List<CollectionEntry>>
    suspend fun add(entry: CollectionEntry)
    suspend fun remove(claimId: String)
    suspend fun isIn(claimId: String): Boolean
    suspend fun toggle(entry: CollectionEntry): Boolean
    suspend fun syncFromServer(): Result<Int>
}

private const val SEP = ""

private fun encode(e: CollectionEntry): String = listOf(
    e.claimId,
    e.permanentUrl,
    e.title,
    e.channelName,
    e.channelClaimId.orEmpty(),
    e.thumbnailUrl.orEmpty(),
    e.addedAt.toString(),
).joinToString(SEP)

private fun decode(raw: String): CollectionEntry? {
    val parts = raw.split(SEP)
    if (parts.size < 7) return null
    return CollectionEntry(
        claimId = parts[0],
        permanentUrl = parts[1],
        title = parts[2],
        channelName = parts[3],
        channelClaimId = parts[4].takeIf { it.isNotBlank() },
        thumbnailUrl = parts[5].takeIf { it.isNotBlank() },
        addedAt = parts[6].toLongOrNull() ?: 0L,
    )
}

abstract class BaseLocalCollectionRepository(
    private val rawFlow: Flow<List<String>>,
    private val setter: suspend (List<String>) -> Unit,
    private val sdkProxyApi: SdkProxyApi,
    private val collectionId: String,
    private val collectionName: String,
) : LocalCollectionRepository {

    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val items: Flow<List<CollectionEntry>> = rawFlow.map { raw -> raw.mapNotNull { decode(it) } }

    override suspend fun add(entry: CollectionEntry) {
        val current = rawFlow.first()
        val without = current.filterNot { it.startsWith("${entry.claimId}$SEP") }
        setter(listOf(encode(entry)) + without)
        bgScope.launch { runCatching { writeCollectionToSharedPreference() } }
    }

    override suspend fun remove(claimId: String) {
        val current = rawFlow.first()
        val next = current.filterNot { it.startsWith("$claimId$SEP") }
        if (next.size != current.size) {
            setter(next)
            bgScope.launch { runCatching { writeCollectionToSharedPreference() } }
        }
    }

    override suspend fun isIn(claimId: String): Boolean = items.first().any { it.claimId == claimId }

    override suspend fun toggle(entry: CollectionEntry): Boolean {
        return if (isIn(entry.claimId)) {
            remove(entry.claimId); false
        } else {
            add(entry); true
        }
    }

    override suspend fun syncFromServer(): Result<Int> = runCatching {
        val resp = sdkProxyApi.preferenceGet(
            JsonRpcRequest(method = "preference_get", params = PreferenceGetParams("shared")),
        ).unwrap()
        val shared = (resp["shared"] as? JsonObject) ?: return@runCatching 0
        val value = (shared["value"] as? JsonObject) ?: return@runCatching 0
        val builtins = (value["builtinCollections"] as? JsonObject) ?: return@runCatching 0
        val coll = (builtins[collectionId] as? JsonObject) ?: return@runCatching 0
        val serverUpdatedAt = (coll["updatedAt"]?.jsonPrimitive?.longOrNull) ?: 0L
        val urls = (coll["items"] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: return@runCatching 0
        if (urls.isEmpty()) {
            setter(emptyList())
            return@runCatching 0
        }
        val localEntries = items.first()
        val localByClaim = localEntries.associateBy { it.claimId }
        val claimIds = urls.mapNotNull { extractClaimIdFromLbryUrl(it) }.distinct()

        // Bulk-fetch claim metadata.
        val resolvedById: Map<String, com.odysee.app.core.network.dto.ClaimDto> =
            if (claimIds.isEmpty()) emptyMap() else runCatching {
                val response = sdkProxyApi.claimSearch(
                    JsonRpcRequest(
                        method = "claim_search",
                        params = com.odysee.app.core.network.dto.ClaimSearchParams(
                            claimIds = claimIds,
                            pageSize = claimIds.size.coerceIn(1, 50),
                            orderBy = listOf("name"),
                            streamTypes = null,
                            hasSource = null,
                            notTags = null,
                        ),
                    ),
                )
                response.unwrap().items.associateBy { it.claimId }
            }.getOrDefault(emptyMap())

        val merged = urls.mapNotNull { url ->
            val cid = extractClaimIdFromLbryUrl(url) ?: return@mapNotNull null
            val existing = localByClaim[cid]
            val claim = resolvedById[cid]
            val title = claim?.value?.title ?: existing?.title ?: cid
            val channelName = claim?.signingChannel?.name ?: existing?.channelName ?: ""
            val channelClaimId = claim?.signingChannel?.claimId ?: existing?.channelClaimId
            val thumbnailUrl = claim?.value?.thumbnail?.url ?: existing?.thumbnailUrl
            val permanentUrl = claim?.permanentUrl ?: existing?.permanentUrl ?: url
            CollectionEntry(
                claimId = cid,
                permanentUrl = permanentUrl,
                title = title,
                channelName = channelName,
                channelClaimId = channelClaimId,
                thumbnailUrl = thumbnailUrl,
                addedAt = existing?.addedAt ?: (serverUpdatedAt * 1000L),
            )
        }
        setter(merged.map { encode(it) })
        merged.size
    }

    private fun extractClaimIdFromLbryUrl(url: String): String? {
        val stripped = url.removePrefix("lbry://").trim()
        val sepIdx = stripped.lastIndexOfAny(charArrayOf('#', ':'))
        if (sepIdx <= 0) return null
        val tail = stripped.substring(sepIdx + 1).takeWhile { it != '/' && it != '?' }
        return tail.takeIf { id -> id.length in 8..40 && id.all { it.isLetterOrDigit() } }
    }

    private suspend fun writeCollectionToSharedPreference() {
        val list = items.first()
        val urls = list.map { e ->
            e.permanentUrl.takeIf { it.startsWith("lbry://") } ?: "lbry://${e.title}#${e.claimId}"
        }
        val existing = runCatching {
            sdkProxyApi.preferenceGet(
                JsonRpcRequest(method = "preference_get", params = PreferenceGetParams("shared")),
            ).unwrap()
        }.getOrNull()
        val sharedExisting = (existing?.get("shared") as? JsonObject)
        val valueExisting = (sharedExisting?.get("value") as? JsonObject)
        val builtinsExisting = (valueExisting?.get("builtinCollections") as? JsonObject)
        val newCollection = buildJsonObject {
            put("id", JsonPrimitive(collectionId))
            put("name", JsonPrimitive(collectionName))
            put("type", JsonPrimitive("playlist"))
            put("items", buildJsonArray { urls.forEach { add(JsonPrimitive(it)) } })
            put("updatedAt", JsonPrimitive(System.currentTimeMillis() / 1000L))
        }
        val newBuiltins = buildJsonObject {
            builtinsExisting?.forEach { (k, v) -> if (k != collectionId) put(k, v) }
            put(collectionId, newCollection)
        }
        val newValue = buildJsonObject {
            valueExisting?.forEach { (k, v) -> if (k != "builtinCollections") put(k, v) }
            put("builtinCollections", newBuiltins)
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

interface WatchLaterRepository : LocalCollectionRepository
interface FavoritesRepository : LocalCollectionRepository

@Singleton
class WatchLaterRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    sdkProxyApi: SdkProxyApi,
) : BaseLocalCollectionRepository(
    rawFlow = authPreferences.watchLater,
    setter = { authPreferences.setWatchLater(it) },
    sdkProxyApi = sdkProxyApi,
    collectionId = "watchlater",
    collectionName = "Watch Later",
), WatchLaterRepository

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    sdkProxyApi: SdkProxyApi,
) : BaseLocalCollectionRepository(
    rawFlow = authPreferences.favorites,
    setter = { authPreferences.setFavorites(it) },
    sdkProxyApi = sdkProxyApi,
    collectionId = "favorites",
    collectionName = "Favorites",
), FavoritesRepository
