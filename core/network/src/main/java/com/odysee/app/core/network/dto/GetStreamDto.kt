package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetStreamParams(
    val uri: String,
    @SerialName("save_file") val saveFile: Boolean = false,
)

@Serializable
data class GetStreamResult(
    @SerialName("streaming_url") val streamingUrl: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
)
