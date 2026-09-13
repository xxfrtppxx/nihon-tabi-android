package com.nihontabi.android.core.network

import com.nihontabi.android.core.network.dto.AuthResponse
import com.nihontabi.android.core.network.dto.LoginRequest
import com.nihontabi.android.core.network.dto.RefreshRequest
import com.nihontabi.android.core.network.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout()
}
