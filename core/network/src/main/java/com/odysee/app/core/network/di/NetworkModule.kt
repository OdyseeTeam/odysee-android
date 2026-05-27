package com.odysee.app.core.network.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.odysee.app.core.network.CommentronApi
import com.odysee.app.core.network.LbryioApi
import com.odysee.app.core.network.LbryioAuthInterceptor
import com.odysee.app.core.network.LighthouseApi
import com.odysee.app.core.network.LivestreamApi
import com.odysee.app.core.network.OdyseeContentApi
import com.odysee.app.core.network.SdkProxyApi
import com.odysee.app.core.network.SdkProxyAuthInterceptor
import com.odysee.app.core.network.WatchmanApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val SDK_PROXY_BASE = "https://api.na-backend.odysee.com/"
    private const val ODYSEE_BASE = "https://odysee.com/"
    private const val COMMENTRON_BASE = "https://comments.odysee.tv/"
    private const val LBRYIO_BASE = "https://api.odysee.com/"
    private const val LIGHTHOUSE_BASE = "https://lighthouse.odysee.tv/"
    private const val LIVESTREAM_BASE = "https://api.odysee.live/"
    private const val WATCHMAN_BASE = "https://watchman.na-backend.odysee.com/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideSdkProxyApi(
        json: Json,
        client: OkHttpClient,
        sdkAuthInterceptor: SdkProxyAuthInterceptor,
    ): SdkProxyApi {
        val contentType = "application/json".toMediaType()
        val authedClient = client.newBuilder().addInterceptor(sdkAuthInterceptor).build()
        return Retrofit.Builder()
            .baseUrl(SDK_PROXY_BASE)
            .client(authedClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(SdkProxyApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOdyseeContentApi(json: Json, client: OkHttpClient): OdyseeContentApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(ODYSEE_BASE)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OdyseeContentApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCommentronApi(json: Json, client: OkHttpClient): CommentronApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(COMMENTRON_BASE)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(CommentronApi::class.java)
    }

    @Provides
    @Singleton
    fun provideLighthouseApi(json: Json, client: OkHttpClient): LighthouseApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(LIGHTHOUSE_BASE)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(LighthouseApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWatchmanApi(json: Json, client: OkHttpClient): WatchmanApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(WATCHMAN_BASE)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(WatchmanApi::class.java)
    }

    @Provides
    @Singleton
    fun provideLivestreamApi(json: Json, client: OkHttpClient): LivestreamApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(LIVESTREAM_BASE)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(LivestreamApi::class.java)
    }

    @Provides
    @Singleton
    fun provideLbryioApi(
        json: Json,
        client: OkHttpClient,
        authInterceptor: LbryioAuthInterceptor,
    ): LbryioApi {
        val contentType = "application/json".toMediaType()
        val authedClient = client.newBuilder()
            .addInterceptor(authInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl(LBRYIO_BASE)
            .client(authedClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(LbryioApi::class.java)
    }
}
