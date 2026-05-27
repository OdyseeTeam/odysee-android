package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelUpdateParams(
    @SerialName("claim_id") val claimId: String,
    val title: String? = null,
    val description: String? = null,
    val thumbnail_url: String? = null,
    val cover_url: String? = null,
    val website_url: String? = null,
    val email: String? = null,
    val tags: List<String>? = null,
    val languages: List<String>? = null,
    val locations: List<String>? = null,
    val featured: List<String>? = null,
    val clear_tags: Boolean? = null,
    val clear_languages: Boolean? = null,
    val clear_locations: Boolean? = null,
    val clear_featured: Boolean? = null,
    @SerialName("replace") val replace: Boolean? = null,
    val blocking: Boolean = true,
)

@Serializable
data class ChannelUpdateResult(
    val txid: String? = null,
    val outputs: List<ClaimDto> = emptyList(),
)

@Serializable
data class ChannelCreateParams(
    val name: String,
    val bid: String,
    val title: String? = null,
    val description: String? = null,
    val thumbnail_url: String? = null,
    val cover_url: String? = null,
    val website_url: String? = null,
    val email: String? = null,
    val tags: List<String>? = null,
    val languages: List<String>? = null,
    val blocking: Boolean = true,
)
