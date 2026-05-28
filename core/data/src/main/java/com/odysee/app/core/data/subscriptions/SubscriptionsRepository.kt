package com.odysee.app.core.data.subscriptions

import com.odysee.app.core.data.preferences.FollowingEntry
import com.odysee.app.core.data.preferences.SharedPrefSlices
import com.odysee.app.core.data.preferences.SharedPreferencesStore
import com.odysee.app.core.datastore.AuthPreferences
import com.odysee.app.core.network.LbryioApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class Subscription(val claimId: String, val name: String)

interface SubscriptionsRepository {
    val subscriptions: Flow<List<Subscription>>
    suspend fun isSubscribed(claimId: String): Boolean
    suspend fun subscribe(claimId: String, name: String)
    suspend fun unsubscribe(claimId: String)
    suspend fun toggle(claimId: String, name: String): Boolean
    suspend fun syncFromServer(): Result<Int>
}

@Singleton
class SubscriptionsRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val sharedStore: SharedPreferencesStore,
    private val lbryioApi: LbryioApi,
    private val authRepository: com.odysee.app.core.data.auth.AuthRepository,
) : SubscriptionsRepository {

    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { encodeDefaults = false }

    override val subscriptions: Flow<List<Subscription>> = authPreferences.subscriptions.map { raw ->
        raw.mapNotNull { entry ->
            val parts = entry.split('|', limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) Subscription(parts[0], parts[1]) else null
        }
    }

    override suspend fun isSubscribed(claimId: String): Boolean =
        subscriptions.first().any { it.claimId == claimId }

    private fun ensureSignedIn(): Boolean =
        authRepository.state.value is com.odysee.app.core.data.auth.AuthState.SignedIn

    override suspend fun subscribe(claimId: String, name: String) {
        if (!ensureSignedIn()) return
        val current = authPreferences.subscriptions.first().toMutableList()
        if (current.none { it.startsWith("$claimId|") }) {
            current.add("$claimId|$name")
            authPreferences.setSubscriptions(current)
        }
        bgScope.launch {
            runCatching { lbryioApi.subscriptionNew(claimId = claimId, channelName = name) }
            runCatching { push() }
        }
    }

    override suspend fun unsubscribe(claimId: String) {
        if (!ensureSignedIn()) return
        val current = authPreferences.subscriptions.first()
        val filtered = current.filterNot { it.startsWith("$claimId|") }
        if (filtered.size != current.size) {
            authPreferences.setSubscriptions(filtered)
        }
        bgScope.launch {
            runCatching { lbryioApi.subscriptionDelete(claimId = claimId) }
            runCatching { push() }
        }
    }

    override suspend fun toggle(claimId: String, name: String): Boolean {
        return if (isSubscribed(claimId)) {
            unsubscribe(claimId); false
        } else {
            subscribe(claimId, name); true
        }
    }

    override suspend fun syncFromServer(): Result<Int> = runCatching {
        val urls = sharedStore.readSlice(SharedPrefSlices.SUBSCRIPTIONS, SharedPrefSlices.StringList)
            ?: return@runCatching 0
        val parsed = urls.mapNotNull { parseLbryUrl(it) }
        val serverEntries = parsed.map { "${it.claimId}|${it.name}" }
        val serverIds = parsed.map { it.claimId }.toSet()
        // Preserve any local-only subs whose claimId isn't on the server.
        val localOnly = authPreferences.subscriptions.first().filterNot { entry ->
            entry.substringBefore('|') in serverIds
        }
        authPreferences.setSubscriptions(serverEntries + localOnly)
        parsed.size
    }

    private suspend fun push() {
        val subs = subscriptions.first()
        val urls = subs.map { "lbry://${it.name}#${it.claimId}" }
        val following = urls.map { FollowingEntry(uri = it, notificationsDisabled = false) }
        // subscriptions + following are written together: web keeps them in sync.
        sharedStore.patch(
            mapOf(
                SharedPrefSlices.SUBSCRIPTIONS to
                    json.encodeToJsonElement(SharedPrefSlices.StringList, urls),
                SharedPrefSlices.FOLLOWING to
                    json.encodeToJsonElement(SharedPrefSlices.FollowingList, following),
            ),
        )
    }
}

/**
 * Parses a Lbry URL like "lbry://@channel#claimid" or "lbry://@channel:claimid" → Subscription.
 */
internal fun parseLbryUrl(url: String): Subscription? {
    val stripped = url.removePrefix("lbry://").trim()
    if (stripped.isBlank() || !stripped.startsWith("@")) return null
    val sepIdx = stripped.indexOfAny(charArrayOf('#', ':'))
    return if (sepIdx > 0) {
        val name = stripped.substring(0, sepIdx)
        val claimId = stripped.substring(sepIdx + 1).takeWhile { it != '/' }
        if (claimId.isBlank()) null else Subscription(claimId = claimId, name = name)
    } else null
}
