package com.nihontabi.android.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Serializable
private data class NestErrorBody(
    val message: String? = null,
    val error: String? = null,
    val statusCode: Int? = null,
)

/**
 * Extracts NestJS's `{ message, error, statusCode }` error body into a
 * user-facing string — the Android equivalent of `ApiError` in
 * `nihon-tabi-web/lib/api.ts`.
 */
fun Throwable.toUserMessage(json: Json): String {
    if (this is HttpException) {
        val body = response()?.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            runCatching { json.decodeFromString(NestErrorBody.serializer(), body) }
                .getOrNull()?.message?.let { return it }
        }
        return message()?.takeIf { it.isNotBlank() } ?: "Request failed"
    }
    return localizedMessage ?: "Something went wrong. Please try again."
}
