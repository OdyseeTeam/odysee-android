package com.odysee.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PreferenceGetParams(val key: String = "shared")

@Serializable
data class PreferenceSetParams(
    val key: String,
    val value: String,
)
