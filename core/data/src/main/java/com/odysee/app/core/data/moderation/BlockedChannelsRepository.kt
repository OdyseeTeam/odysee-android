package com.odysee.app.core.data.moderation

import com.odysee.app.core.data.preferences.SharedPrefSlices
import com.odysee.app.core.data.preferences.SharedPreferencesStore
import com.odysee.app.core.data.subscriptions.parseLbryUrl
import com.odysee.app.core.datastore.AuthPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class BlockedChannel(val claimId: String, val name: String)

interface BlockedChannelsRepository {
    val blocked: Flow<List<BlockedChannel>>
    suspend fun isBlocked(claimId: String): Boolean
    suspend fun block(claimId: String, name: String)
    suspend fun unblock(claimId: String)
    suspend fun toggle(claimId: String, name: String): Boolean
    suspend fun syncFromServer(): Result<Int>
}

@Singleton
class BlockedChannelsRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val sharedStore: SharedPreferencesStore,
) : BlockedChannelsRepository {

    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val blocked: Flow<List<BlockedChannel>> = authPreferences.blockedChannels.map { raw ->
        raw.mapNotNull { entry ->
            val parts = entry.split('|', limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) BlockedChannel(parts[0], parts[1]) else null
        }
    }

    override suspend fun isBlocked(claimId: String): Boolean =
        blocked.first().any { it.claimId == claimId }

    override suspend fun block(claimId: String, name: String) {
        val current = authPreferences.blockedChannels.first().toMutableList()
        if (current.none { it.startsWith("$claimId|") }) {
            current.add("$claimId|$name")
            authPreferences.setBlockedChannels(current)
        }
        bgScope.launch { runCatching { push() } }
    }

    override suspend fun unblock(claimId: String) {
        val current = authPreferences.blockedChannels.first()
        val filtered = current.filterNot { it.startsWith("$claimId|") }
        if (filtered.size != current.size) {
            authPreferences.setBlockedChannels(filtered)
        }
        bgScope.launch { runCatching { push() } }
    }

    override suspend fun toggle(claimId: String, name: String): Boolean {
        return if (isBlocked(claimId)) {
            unblock(claimId); false
        } else {
            block(claimId, name); true
        }
    }

    override suspend fun syncFromServer(): Result<Int> = runCatching {
        val urls = sharedStore.readSlice(SharedPrefSlices.BLOCKED, SharedPrefSlices.StringList)
            ?: return@runCatching 0
        val parsed = urls.mapNotNull { parseLbryUrl(it) }
        val entries = parsed.map { "${it.claimId}|${it.name}" }
        authPreferences.setBlockedChannels(entries)
        entries.size
    }

    private suspend fun push() {
        val list = blocked.first()
        val urls = list.map { "lbry://${it.name}#${it.claimId}" }
        sharedStore.writeSlice(
            key = SharedPrefSlices.BLOCKED,
            serializer = SharedPrefSlices.StringList,
            value = urls,
        )
    }
}
