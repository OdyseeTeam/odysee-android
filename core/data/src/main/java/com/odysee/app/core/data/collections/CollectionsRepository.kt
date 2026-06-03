package com.odysee.app.core.data.collections

import com.odysee.app.core.data.preferences.CollectionDoc
import com.odysee.app.core.data.preferences.SharedPrefSlices
import com.odysee.app.core.data.preferences.SharedPreferencesStore
import com.odysee.app.core.datastore.AuthPreferences
import com.odysee.app.core.network.SdkProxyApi
import com.odysee.app.core.network.jsonrpc.JsonRpcRequest
import com.odysee.app.core.network.jsonrpc.unwrap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    private val sharedStore: SharedPreferencesStore,
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
        val builtins = sharedStore.readSlice(
            SharedPrefSlices.BUILTIN_COLLECTIONS,
            SharedPrefSlices.CollectionMap,
        ) ?: return@runCatching 0
        val coll = builtins[collectionId] ?: return@runCatching 0
        val serverUpdatedAt = coll.updatedAt ?: 0L
        val urls = coll.items
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
                response.unwrap().items.mapNotNull { dto -> dto.claimId?.let { it to dto } }.toMap()
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
        // Merge into the existing builtinCollections map, preserving entries
        // for other built-in collections (e.g. favorites when writing watchlater).
        val existing = sharedStore.readSlice(
            SharedPrefSlices.BUILTIN_COLLECTIONS,
            SharedPrefSlices.CollectionMap,
        ).orEmpty()
        val merged = existing.toMutableMap()
        merged[collectionId] = CollectionDoc(
            id = collectionId,
            name = collectionName,
            type = "playlist",
            items = urls,
            updatedAt = System.currentTimeMillis() / 1000L,
        )
        sharedStore.writeSlice(
            key = SharedPrefSlices.BUILTIN_COLLECTIONS,
            serializer = SharedPrefSlices.CollectionMap,
            value = merged,
        )
    }
}

interface WatchLaterRepository : LocalCollectionRepository
interface FavoritesRepository : LocalCollectionRepository

@Singleton
class WatchLaterRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    sdkProxyApi: SdkProxyApi,
    sharedStore: SharedPreferencesStore,
) : BaseLocalCollectionRepository(
    rawFlow = authPreferences.watchLater,
    setter = { authPreferences.setWatchLater(it) },
    sdkProxyApi = sdkProxyApi,
    sharedStore = sharedStore,
    collectionId = "watchlater",
    collectionName = "Watch Later",
), WatchLaterRepository

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    sdkProxyApi: SdkProxyApi,
    sharedStore: SharedPreferencesStore,
) : BaseLocalCollectionRepository(
    rawFlow = authPreferences.favorites,
    setter = { authPreferences.setFavorites(it) },
    sdkProxyApi = sdkProxyApi,
    sharedStore = sharedStore,
    collectionId = "favorites",
    collectionName = "Favorites",
), FavoritesRepository
