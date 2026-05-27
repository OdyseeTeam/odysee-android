package com.odysee.app.feature.library

import kotlinx.serialization.Serializable

@Serializable
data object WatchHistoryRoute

@Serializable
data object PlaylistsRoute

@Serializable
data object WatchLaterRoute

@Serializable
data object FavoritesRoute

@Serializable
data class PlaylistDetailRoute(
    val playlistId: String,
    val autoplay: Boolean = false,
    val shuffle: Boolean = false,
)
