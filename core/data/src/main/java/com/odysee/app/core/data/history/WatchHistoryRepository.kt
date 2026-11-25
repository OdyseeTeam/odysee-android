package com.odysee.app.core.data.history

import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.datastore.AuthPreferences
import com.odysee.app.core.network.LbryioApi
import com.odysee.app.core.network.dto.ViewHistoryItemDto
import com.odysee.app.core.network.dto.ViewHistoryPageDto
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class WatchHistoryEntry(
    val claimId: String,
    val permanentUrl: String,
    val title: String,
    val channelName: String,
    val channelClaimId: String?,
    val thumbnailUrl: String?,
    val watchedAt: Long,
)

interface WatchHistoryRepository {
    val history: Flow<List<WatchHistoryEntry>>
    suspend fun add(entry: WatchHistoryEntry)
    suspend fun remove(claimId: String)
    suspend fun clear()
    suspend fun syncFromServer(): Result<Int>
}

private const val MAX_ENTRIES = 200
private const val SEPARATOR = ""

@Singleton
class WatchHistoryRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val lbryioApi: LbryioApi,
    private val contentRepository: Lazy<ContentRepository>,
) : WatchHistoryRepository {

    private val syncMutex = Mutex()

    override val history: Flow<List<WatchHistoryEntry>> = authPreferences.watchHistory.map { raw ->
        raw.mapNotNull { encoded -> decode(encoded) }
    }

    override suspend fun add(entry: WatchHistoryEntry) {
        val current = authPreferences.watchHistory.first()
        val withoutClaim = current.filterNot { it.startsWith("${entry.claimId}$SEPARATOR") }
        val next = listOf(encode(entry)) + withoutClaim
        authPreferences.setWatchHistory(next.take(MAX_ENTRIES))
    }

    override suspend fun remove(claimId: String) {
        val current = authPreferences.watchHistory.first()
        val filtered = current.filterNot { it.startsWith("$claimId$SEPARATOR") }
        if (filtered.size != current.size) {
            authPreferences.setWatchHistory(filtered)
        }
    }

    override suspend fun clear() {
        authPreferences.setWatchHistory(emptyList())
    }

    override suspend fun syncFromServer(): Result<Int> = runCatching {
        syncMutex.withLock { syncFromServerLocked() }
    }

    private suspend fun syncFromServerLocked(): Int {
        val remote = mutableListOf<Pair<String, Long>>()
        var page = 1
        var hasMore = true
        while (hasMore && page <= 10) {
            val env = lbryioApi.userViewHistory(page = page, pageSize = 100)
            if (!env.success) {
                val err = env.error.orEmpty()
                if (err.contains("not found", ignoreCase = true)) return 0
                error(err.ifBlank { "view_history failed" })
            }
            val (items, more) = parseViewHistoryPage(env.data)
            if (items.isEmpty()) {
                hasMore = false
            } else {
                for (item in items) {
                    val id = item.claimId.takeIf { it.isNotBlank() } ?: continue
                    val at = parseUpdatedAt(item.updatedAt)
                    remote += id to at
                }
                hasMore = more
                page += 1
            }
        }
        if (remote.isEmpty()) return 0

        val byId = linkedMapOf<String, Long>()
        for ((id, at) in remote) {
            val prev = byId[id]
            if (prev == null || at > prev) byId[id] = at
        }
        val claimIds = byId.keys.toList()
        val claims = contentRepository.get().getClaimsByIds(claimIds)
        val byClaimId = claims.associateBy { it.claimId }

        val localById = authPreferences.watchHistory.first()
            .mapNotNull { decode(it) }
            .associateBy { it.claimId }

        val merged = claimIds.mapNotNull { id ->
            val claim = byClaimId[id]
            val watchedAt = byId[id] ?: return@mapNotNull null
            val existing = localById[id]
            if (claim != null) {
                WatchHistoryEntry(
                    claimId = id,
                    permanentUrl = claim.permanentUrl,
                    title = claim.title,
                    channelName = claim.signingChannel?.name.orEmpty(),
                    channelClaimId = claim.signingChannel?.claimId,
                    thumbnailUrl = claim.thumbnailUrl,
                    watchedAt = maxOf(watchedAt, existing?.watchedAt ?: 0L),
                )
            } else {
                existing?.copy(watchedAt = maxOf(watchedAt, existing.watchedAt))
            }
        }.sortedByDescending { it.watchedAt }

        val remoteIds = claimIds.toSet()
        val localOnly = localById.values.filter { it.claimId !in remoteIds }
        val next = (merged + localOnly)
            .sortedByDescending { it.watchedAt }
            .take(MAX_ENTRIES)
        authPreferences.setWatchHistory(next.map { encode(it) })
        android.util.Log.i(TAG, "syncFromServer: remote=${claimIds.size} stored=${next.size}")
        return next.size
    }

    private fun parseViewHistoryPage(raw: JsonElement?): Pair<List<ViewHistoryItemDto>, Boolean> {
        if (raw == null) return emptyList<ViewHistoryItemDto>() to false
        return when (raw) {
            is JsonArray -> {
                val items = runCatching {
                    json.decodeFromJsonElement<List<ViewHistoryItemDto>>(raw)
                }.getOrDefault(emptyList())
                items to false
            }
            is JsonObject -> {
                val page = runCatching {
                    json.decodeFromJsonElement<ViewHistoryPageDto>(raw)
                }.getOrNull()
                (page?.items.orEmpty()) to (page?.hasMore == true)
            }
            else -> emptyList<ViewHistoryItemDto>() to false
        }
    }

    private fun parseUpdatedAt(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        raw.toLongOrNull()?.let { epoch ->
            return if (epoch < 10_000_000_000L) epoch * 1000L else epoch
        }
        return runCatching { Instant.parse(raw).toEpochMilli() }.getOrDefault(0L)
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun encode(e: WatchHistoryEntry): String = listOf(
        e.claimId,
        e.permanentUrl,
        e.title,
        e.channelName,
        e.channelClaimId.orEmpty(),
        e.thumbnailUrl.orEmpty(),
        e.watchedAt.toString(),
    ).joinToString(SEPARATOR)

    private fun decode(raw: String): WatchHistoryEntry? {
        val parts = raw.split(SEPARATOR)
        if (parts.size < 7) return null
        return WatchHistoryEntry(
            claimId = parts[0],
            permanentUrl = parts[1],
            title = parts[2],
            channelName = parts[3],
            channelClaimId = parts[4].takeIf { it.isNotBlank() },
            thumbnailUrl = parts[5].takeIf { it.isNotBlank() },
            watchedAt = parts[6].toLongOrNull() ?: 0L,
        )
    }

    private companion object {
        const val TAG = "WatchHistory"
    }
}
