package com.odysee.app.core.data.preferences

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

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
    // Tags can arrive in any of three shapes from the web's stored payloads:
    //  - missing / null     → treat as empty
    //  - ["foo", "bar"]     → plain strings
    //  - [{"name": "foo"}]  → tag objects (older format the web still tolerates;
    //                         see odysee-frontend/.../actions/collections.ts:135)
    // The custom serializer normalizes all three into a List<String>, so any
    // collection that previously failed to parse (and silently disappeared
    // from the playlists screen) now loads correctly.
    @Serializable(with = LooseTagListSerializer::class)
    val tags: List<String> = emptyList(),
)

@Serializable
data class CollectionThumbnail(val url: String? = null)

/**
 * Reads a tag list that may be: absent, null, ["string", ...], or
 * [{"name":"string"}, ...]. Writes back as a plain string list.
 */
object LooseTagListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("LooseTagList")

    override fun deserialize(decoder: Decoder): List<String> {
        val input = decoder as? JsonDecoder
            ?: error("LooseTagListSerializer requires kotlinx-serialization JSON")
        return when (val element = input.decodeJsonElement()) {
            is JsonNull -> emptyList()
            is JsonArray -> element.mapNotNull { item ->
                when (item) {
                    is JsonPrimitive -> item.contentOrNull
                    is JsonObject -> item["name"]?.jsonPrimitive?.contentOrNull
                    else -> null
                }
            }
            else -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        val output = encoder as? JsonEncoder
            ?: error("LooseTagListSerializer requires kotlinx-serialization JSON")
        output.encodeJsonElement(
            buildJsonArray { value.forEach { add(JsonPrimitive(it)) } },
        )
    }
}

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
    // The web persists every CLIENT_SYNC_KEY under this single slice — a
    // flat object whose keys are the setting name strings from
    // odysee-frontend/ui/constants/settings.ts (e.g. "show_mature",
    // "hide_reposts", "autoplay_next"). Values are heterogeneous (bool,
    // string, number) so we keep it as a raw JsonObject and let the
    // ClientSettingsRepository handle per-key decoding.
    const val SETTINGS = "settings"

    val StringList = ListSerializer(String.serializer())
    val FollowingList = ListSerializer(FollowingEntry.serializer())
    val CollectionMap = MapSerializer(String.serializer(), CollectionDoc.serializer())
    val BoolMap = MapSerializer(String.serializer(), Boolean.serializer())
}
