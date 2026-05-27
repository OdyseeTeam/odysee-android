package com.odysee.app.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface LivestreamApi {
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("livestream/all")
    suspend fun all(@Body body: okhttp3.RequestBody = "".toRequestBody()): LivestreamAllResponse
}

@Serializable
data class LivestreamAllResponse(
    val data: List<LivestreamEntry> = emptyList(),
)

@Serializable
data class LivestreamEntry(
    @SerialName("ChannelClaimID") val channelClaimId: String? = null,
    @SerialName("Live") val live: Boolean? = null,
    @SerialName("ViewerCount") val viewerCount: Int? = null,
    @SerialName("VideoURL") val videoUrl: String? = null,
    @SerialName("VideoURLLLHLS") val videoUrlLowLatency: String? = null,
    @SerialName("ActiveClaim") val activeClaim: LivestreamActiveClaim? = null,
)

@Serializable
data class LivestreamActiveClaim(
    @SerialName("ClaimID") val claimId: String? = null,
    @SerialName("ClaimURI") val claimUri: String? = null,
)
