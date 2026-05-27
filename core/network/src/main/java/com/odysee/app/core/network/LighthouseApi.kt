package com.odysee.app.core.network

import com.odysee.app.core.network.dto.LighthouseHit
import retrofit2.http.GET
import retrofit2.http.Query

interface LighthouseApi {
    @GET("search")
    suspend fun search(
        @Query("s") query: String,
        @Query("size") size: Int = 20,
        @Query("from") from: Int = 0,
        @Query("nsfw") nsfw: Boolean = false,
        @Query("claimType") claimType: String? = null,
        @Query("mediaType") mediaType: String? = null,
    ): List<LighthouseHit>
}
