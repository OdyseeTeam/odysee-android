package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomepageEnvelopeDto(
    val status: String,
    val data: Map<String, HomepageLangDto?> = emptyMap(),
)

@Serializable
data class HomepageLangDto(
    val categories: Map<String, HomepageCategoryDto> = emptyMap(),
)

@Serializable
data class HomepageCategoryDto(
    val name: String,
    val label: String,
    val icon: String? = null,
    val description: String? = null,
    val image: String? = null,
    val sortOrder: Int? = null,
    val channelIds: List<String> = emptyList(),
    val channelLimit: String? = null,
    val daysOfContent: Int? = null,
    val pageSize: Int? = null,
    @SerialName("exclude_shorts") val excludeShorts: Boolean? = null,
)
