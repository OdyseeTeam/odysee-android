package com.odysee.app.core.data.preferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Typed views over individual slices of the SDK `shared` preferences payload.
 *
 * Each top-level key has a known shape on the web frontend; mirroring those
 * here means consumers don't need to reach into untyped JSON each time.
 *
 * Web reference: `ui/redux/actions/sync.ts` / `ui/util/sync-utils.ts` for the
 * canonical names.
 */

/** "shared.value.following" — channel subscription entries with per-channel notification settings. */
@Serializable
data class FollowingEntry(
    val uri: String,
    val notificationsDisabled: Boolean = false,
)

/** "shared.value.unpublishedCollections" entry — a private / local playlist. */
@Serializable
data class CollectionDoc(
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,
    val items: List<String> = emptyList(),
    val updatedAt: Long? = null,
    val thumbnail: CollectionThumbnail? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val isPublic: Boolean? = null,
)

@Serializable
data class CollectionThumbnail(val url: String? = null)

// ---------- Convenience serializers for the slice keys we read/write ----------

object SharedPrefSlices {
    const val SUBSCRIPTIONS = "subscriptions"
    const val FOLLOWING = "following"
    const val BLOCKED = "blocked"
    const val TAGS = "tags"
    const val UNPUBLISHED_COLLECTIONS = "unpublishedCollections"
    const val EDITED_COLLECTIONS = "editedCollections"
    const val AUTO_PUBLISH_BY_ID = "autoPublishById"
    const val BUILTIN_COLLECTIONS = "builtinCollections"

    val StringList = ListSerializer(String.serializer())
    val FollowingList = ListSerializer(FollowingEntry.serializer())
    val CollectionMap = MapSerializer(String.serializer(), CollectionDoc.serializer())
    val BoolMap = MapSerializer(String.serializer(), Boolean.serializer())
}
