package com.odysee.app.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface WatchmanApi {
    @POST("reports/playback")
    suspend fun report(@Body body: WatchmanReportDto)
}

@Serializable
data class WatchmanReportDto(
    val rebuf_count: Int,
    val rebuf_duration: Long,
    val url: String,
    val device: String,
    val duration: Long,
    val protocol: String,
    val player: String,
    val user_id: String,
    val position: Long,
    val rel_position: Int,
    val bitrate: Long?,
    val bandwidth: Long? = null,
    val preview: Boolean? = null,
)
