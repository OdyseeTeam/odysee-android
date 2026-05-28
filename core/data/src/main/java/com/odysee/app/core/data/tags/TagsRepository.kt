package com.odysee.app.core.data.tags

import com.odysee.app.core.data.preferences.SharedPrefSlices
import com.odysee.app.core.data.preferences.SharedPreferencesStore
import com.odysee.app.core.datastore.AuthPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val sharedStore: SharedPreferencesStore,
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
            bgScope.launch { runCatching { push() } }
        }
    }

    override suspend fun unfollow(tag: String) {
        val key = tag.lowercase()
        val current = authPreferences.followedTags.first()
        if (key in current) {
            authPreferences.setFollowedTags(current - key)
            bgScope.launch { runCatching { push() } }
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
        val tags = sharedStore.readSlice(SharedPrefSlices.TAGS, SharedPrefSlices.StringList)
            ?.map { it.lowercase() }
            ?: return@runCatching 0
        authPreferences.setFollowedTags(tags)
        tags.size
    }

    private suspend fun push() {
        sharedStore.writeSlice(
            key = SharedPrefSlices.TAGS,
            serializer = SharedPrefSlices.StringList,
            value = tags.first(),
        )
    }
}
