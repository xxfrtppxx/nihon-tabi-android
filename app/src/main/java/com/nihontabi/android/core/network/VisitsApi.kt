package com.nihontabi.android.core.network

import com.nihontabi.android.core.network.dto.CreateVisitRequest
import com.nihontabi.android.core.network.dto.UpdateVisitRequest
import com.nihontabi.android.core.network.dto.VisitDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface VisitsApi {
    @GET("visits")
    suspend fun list(): List<VisitDto>

    @POST("visits")
    suspend fun create(@Body body: CreateVisitRequest): VisitDto

    @PATCH("visits/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdateVisitRequest): VisitDto

    @DELETE("visits/{id}")
    suspend fun remove(@Path("id") id: String)
}
