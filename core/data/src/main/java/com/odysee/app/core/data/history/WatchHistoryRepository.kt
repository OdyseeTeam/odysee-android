package com.odysee.app.core.data.history

import com.odysee.app.core.datastore.AuthPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
}

private const val MAX_ENTRIES = 200
private const val SEPARATOR = ""

@Singleton
class WatchHistoryRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
) : WatchHistoryRepository {

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
}
