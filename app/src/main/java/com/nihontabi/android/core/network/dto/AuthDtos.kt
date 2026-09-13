package com.nihontabi.android.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
)

@Serializable
data class AuthResponse(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
)
