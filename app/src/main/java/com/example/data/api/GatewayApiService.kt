package com.example.data.api

import com.example.data.model.ConsentRequest
import com.example.data.model.HealthResponse
import com.example.data.model.HistoryResponse
import com.example.data.model.PaymentEventPayload
import com.example.data.model.PaymentEventResponse
import com.example.data.model.StatsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface GatewayApiService {

    @GET("v1/health")
    suspend fun checkHealth(
        @Header("Authorization") authHeader: String
    ): Response<HealthResponse>

    @POST("v1/consent")
    suspend fun recordConsent(
        @Header("Authorization") authHeader: String,
        @Body request: ConsentRequest
    ): Response<Map<String, Any>>

    @POST("v1/payments")
    suspend fun submitPayment(
        @Header("Authorization") authHeader: String,
        @Body payload: PaymentEventPayload
    ): Response<PaymentEventResponse>

    @GET("v1/stats")
    suspend fun getStats(
        @Header("Authorization") authHeader: String
    ): Response<StatsResponse>

    @GET("v1/payments")
    suspend fun getPaymentsHistory(
        @Header("Authorization") authHeader: String,
        @Query("status") status: String? = null,
        @Query("provider") provider: String? = null,
        @Query("q") search: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<HistoryResponse>
}
