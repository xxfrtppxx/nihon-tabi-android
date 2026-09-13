package com.nihontabi.android.core.network

import com.nihontabi.android.core.network.dto.RefreshRequest
import com.nihontabi.android.di.ApiBaseUrl
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On a 401, calls `/auth/refresh` once and retries the original request with
 * the new access token — the OkHttp equivalent of nihon-tabi-web's
 * `tryRefresh()` in lib/api.ts. Deliberately uses its own bare [OkHttpClient]
 * (no [AuthInterceptor]/authenticator attached) to avoid a circular
 * dependency on the app's authenticated client.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val json: Json,
    @ApiBaseUrl private val baseUrl: String,
) : Authenticator {

    private val refreshClient = OkHttpClient.Builder().build()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once for this call chain — give up rather than loop.
        if (responseCount(response) >= 2) return null

        val refreshToken = tokenStore.currentRefreshToken() ?: return null
        val failedAccessToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(this) {
            // Another thread may have refreshed already while we waited for the lock.
            val current = tokenStore.currentAccessToken()
            if (current != null && current != failedAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $current")
                    .build()
            }

            val newAccessToken = runCatching { callRefresh(refreshToken) }.getOrNull()
                ?: run {
                    tokenStore.clear()
                    return null
                }
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
        }
    }

    private fun callRefresh(refreshToken: String): String? {
        val body = json.encodeToString(RefreshRequest(refreshToken))
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/auth/refresh")
            .post(body)
            .build()
        refreshClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val text = resp.body?.string() ?: return null
            val parsed = json.decodeFromString(
                com.nihontabi.android.core.network.dto.AuthResponse.serializer(),
                text,
            )
            tokenStore.setTokens(AuthTokens(parsed.accessToken, parsed.refreshToken))
            return parsed.accessToken
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
