package com.nihontabi.android.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Attaches `Authorization: Bearer <accessToken>` to every request that has one. */
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenStore.currentAccessToken()
        val authed = if (token != null) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            request
        }
        return chain.proceed(authed)
    }
}
