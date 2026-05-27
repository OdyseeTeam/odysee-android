package com.odysee.app.core.network

import com.odysee.app.core.network.dto.HomepageEnvelopeDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OdyseeContentApi {
    @GET("\$/api/content/v2/get")
    suspend fun getHomepage(@Query("hp") lang: String = "en"): HomepageEnvelopeDto
}
