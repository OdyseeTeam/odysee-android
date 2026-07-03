package com.odysee.app.core.data.subscriptions

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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

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
    // Serializes sync and push. Without this, a push triggered by subscribe()
    // can race with the initial syncFromServer (kicked off by AuthRepository
    // on sign-in) and overwrite the user's existing follows on the server.
    private val syncMutex = Mutex()
    private val hasSynced = AtomicBoolean(false)

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

    // Pull the server's current list into the local cache once before any
    // mutation, so a sub/unsub never writes back a list that's missing
    // entries we never loaded.
    private suspend fun ensureSynced() {
        if (hasSynced.get()) return
        syncFromServerInternal()
    }

    override suspend fun subscribe(claimId: String, name: String) {
        if (!ensureSignedIn()) return
        ensureSynced()
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
        ensureSynced()
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
        syncFromServerInternal()
    }

    private suspend fun syncFromServerInternal(): Int = syncMutex.withLock {
        // Read BOTH server slices and union their URIs. Historically we only
        // read `subscriptions`, so any account whose server state only
        // populated `following` (older web sessions, iOS, etc.) would sync
        // as empty and the next subscribe() would push a fresh 1-entry list
        // that overwrites all the user's existing follows on the server.
        //
        // `following` is preferred as the source of truth — it's the richer
        // shape ({uri, notificationsDisabled}) and the web always writes it.
        val followingEntries = sharedStore.readSlice(
            SharedPrefSlices.FOLLOWING,
            SharedPrefSlices.FollowingList,
        ).orEmpty()
        val subscriptionUrls = sharedStore.readSlice(
            SharedPrefSlices.SUBSCRIPTIONS,
            SharedPrefSlices.StringList,
        ).orEmpty()
        if (followingEntries.isEmpty() && subscriptionUrls.isEmpty()) {
            hasSynced.set(true)
            return@withLock 0
        }
        // Merge — following wins on ties (same URI in both) so we don't
        // duplicate. Preserves any URI that appears in only one slice.
        val urls = LinkedHashSet<String>().apply {
            addAll(followingEntries.map { it.uri })
            addAll(subscriptionUrls)
        }
        val parsed = urls.mapNotNull { parseLbryUrl(it) }
        val serverEntries = parsed.map { "${it.claimId}|${it.name}" }
        val serverIds = parsed.map { it.claimId }.toSet()
        // Preserve any local-only subs whose claimId isn't on the server.
        val localOnly = authPreferences.subscriptions.first().filterNot { entry ->
            entry.substringBefore('|') in serverIds
        }
        authPreferences.setSubscriptions(serverEntries + localOnly)
        hasSynced.set(true)
        parsed.size
    }

    private suspend fun push() = syncMutex.withLock {
        val subs = subscriptions.first()
        val urls = subs.map { "lbry://${it.name}#${it.claimId}" }

        // Preserve per-URI notificationsDisabled from whatever's on the
        // server. We don't surface a per-channel mute UI in the app, so if
        // we naively emitted `notificationsDisabled: false` for everything
        // we'd nuke mutes the user set on web.
        val existingMutes: Map<String, Boolean> = runCatching {
            sharedStore.readSlice(SharedPrefSlices.FOLLOWING, SharedPrefSlices.FollowingList)
        }.getOrNull().orEmpty().associate { it.uri to it.notificationsDisabled }

        val subscriptionsJson = buildJsonArray { urls.forEach { add(JsonPrimitive(it)) } }
        // Build following manually so notificationsDisabled is always emitted,
        // even when false. kotlinx with encodeDefaults=false would drop it.
        val followingJson = buildJsonArray {
            urls.forEach { uri ->
                add(
                    buildJsonObject {
                        put("uri", JsonPrimitive(uri))
                        put(
                            "notificationsDisabled",
                            JsonPrimitive(existingMutes[uri] ?: false),
                        )
                    },
                )
            }
        }
        sharedStore.patch(
            mapOf(
                SharedPrefSlices.SUBSCRIPTIONS to subscriptionsJson,
                SharedPrefSlices.FOLLOWING to followingJson,
            ),
        )
    }
}

/**
 * Parse a `lbry://@channel#claimid` (or `:claimid`) URL into a Subscription.
 * Backed by [com.odysee.app.core.model.LbryUri] so URI handling stays
 * consistent across the codebase.
 */
internal fun parseLbryUrl(url: String): Subscription? {
    val parsed = com.odysee.app.core.model.LbryUri.parse(url) ?: return null
    // Subscriptions are by definition channels — content claims wouldn't be
    // in the subscriptions slice.
    if (!parsed.isChannel) return null
    return Subscription(claimId = parsed.claimId, name = parsed.name)
}
