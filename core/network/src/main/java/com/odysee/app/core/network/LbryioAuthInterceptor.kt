package com.odysee.app.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the current Lbryio auth token in memory so the interceptor can read it
 * synchronously. AuthRepository keeps this in sync with DataStore.
 */
@Singleton
class LbryioAuthHolder @Inject constructor() {
    private val ref = AtomicReference<String?>(null)
    fun set(token: String?) { ref.set(token) }
    fun get(): String? = ref.get()
}

/**
 * Adds `auth_token=<token>` to api.odysee.com requests:
 * - GET: appended as query param
 * - POST form-urlencoded: appended to the form body
 */
@Singleton
class LbryioAuthInterceptor @Inject constructor(
    private val holder: LbryioAuthHolder,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val host = original.url.host
        if (host != "api.odysee.com") return chain.proceed(original)
        val token = holder.get() ?: return chain.proceed(original)

        return when {
            original.method.equals("GET", ignoreCase = true) -> {
                val urlWithToken = original.url.newBuilder()
                    .setQueryParameter("auth_token", token)
                    .build()
                chain.proceed(original.newBuilder().url(urlWithToken).build())
            }
            else -> {
                val mediaType = original.body?.contentType()
                val isForm = mediaType?.toString()?.startsWith("application/x-www-form-urlencoded") == true
                if (isForm) {
                    val buffer = okio.Buffer().also { original.body?.writeTo(it) }
                    val existing = buffer.readUtf8()
                    val updated = if (existing.isEmpty()) {
                        "auth_token=$token"
                    } else {
                        "$existing&auth_token=$token"
                    }
                    val newBody = updated.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
                    chain.proceed(original.newBuilder().method(original.method, newBody).build())
                } else {
                    val urlWithToken = original.url.newBuilder()
                        .setQueryParameter("auth_token", token)
                        .build()
                    chain.proceed(original.newBuilder().url(urlWithToken).build())
                }
            }
        }
    }
}

@Suppress("unused")
private fun keepReference(): okhttp3.HttpUrl? = "".toHttpUrlOrNull()
