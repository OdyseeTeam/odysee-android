package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `sync/get`. `changed = true` means the server has a different
 * (typically newer) wallet blob and `data` carries it. `changed = false` means
 * our local SDK wallet is in sync with the server.
 */
@Serializable
data class SyncGetResponse(
    val hash: String? = null,
    val data: String? = null,
    val changed: Boolean = false,
)

@Serializable
data class SyncSetResponse(
    val hash: String? = null,
)
