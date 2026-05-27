package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelListParams(
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 99,
    val resolve: Boolean = true,
)

@Serializable
data class ChannelListResult(
    val items: List<ClaimDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int? = null,
)
