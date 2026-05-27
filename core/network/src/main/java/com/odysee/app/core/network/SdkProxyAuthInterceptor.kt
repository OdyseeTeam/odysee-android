package com.odysee.app.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds `X-Lbry-Auth-Token: <token>` to api.na-backend.odysee.com requests when
 * the user has an auth token. Required for wallet-bound SDK calls
 * (channel_list, wallet_balance, etc.). Anonymous reads (claim_search, get,
 * resolve) work without it.
 */
@Singleton
class SdkProxyAuthInterceptor @Inject constructor(
    private val holder: LbryioAuthHolder,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.url.host != "api.na-backend.odysee.com") return chain.proceed(original)
        val token = holder.get() ?: return chain.proceed(original)
        val authed = original.newBuilder()
            .header("X-Lbry-Auth-Token", token)
            .build()
        return chain.proceed(authed)
    }
}
