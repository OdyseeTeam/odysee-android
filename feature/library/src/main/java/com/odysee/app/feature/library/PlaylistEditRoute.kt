package com.odysee.app.feature.library

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistEditRoute(
    /** Existing playlist id when editing; null when creating a new one. */
    val id: String? = null,
)
