package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncHashParams(val placeholder: String? = null)

@Serializable
data class SyncApplyParams(
    val password: String = "",
    val data: String? = null,
    val blocking: Boolean = true,
)

@Serializable
data class SyncApplyResult(
    val hash: String? = null,
    val data: String? = null,
)
