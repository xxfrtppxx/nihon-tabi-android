package com.nihontabi.android.core.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AuthTokens(val accessToken: String, val refreshToken: String)

/**
 * Encrypted on-device store for the JWT access/refresh token pair — the
 * Android equivalent of nihon-tabi-web/lib/token-store.ts, but persisted
 * (EncryptedSharedPreferences) rather than in-memory + localStorage, since
 * there's no browser session boundary on mobile.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "nihon_tabi_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _tokens = MutableStateFlow(readTokens())

    /** Null once logged out — the NavHost's start destination follows this. */
    val tokens: StateFlow<AuthTokens?> = _tokens.asStateFlow()

    private fun readTokens(): AuthTokens? {
        val access = prefs.getString(KEY_ACCESS, null)
        val refresh = prefs.getString(KEY_REFRESH, null)
        return if (access != null && refresh != null) AuthTokens(access, refresh) else null
    }

    fun setTokens(tokens: AuthTokens) {
        prefs.edit()
            .putString(KEY_ACCESS, tokens.accessToken)
            .putString(KEY_REFRESH, tokens.refreshToken)
            .apply()
        _tokens.value = tokens
    }

    /** Updates only the access token — used after a silent refresh. */
    fun setAccessToken(accessToken: String) {
        val current = _tokens.value ?: return
        val updated = current.copy(accessToken = accessToken)
        prefs.edit().putString(KEY_ACCESS, accessToken).apply()
        _tokens.value = updated
    }

    fun clear() {
        prefs.edit().clear().apply()
        _tokens.value = null
    }

    fun currentAccessToken(): String? = _tokens.value?.accessToken
    fun currentRefreshToken(): String? = _tokens.value?.refreshToken

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}
