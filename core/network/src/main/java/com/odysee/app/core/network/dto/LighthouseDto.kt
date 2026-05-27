package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LighthouseHit(
    val name: String,
    val claimId: String,
    @SerialName("title_url_score") val titleUrlScore: Double? = null,
    val title: String? = null,
)
