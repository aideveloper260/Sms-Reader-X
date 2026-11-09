package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Normalized payment event matching the master prompt specification.
 * Secure financial decimal amount string (e.g., "500.00").
 */
@JsonClass(generateAdapter = true)
data class PaymentEventPayload(
    @Json(name = "amount")
    val amount: String,

    @Json(name = "provider")
    val provider: String, // "bkash", "nagad", "rocket"

    @Json(name = "transaction_id")
    val transactionId: String,

    @Json(name = "reference")
    val reference: String = "",

    @Json(name = "receiver_number")
    val receiverNumber: String = "",

    @Json(name = "currency")
    val currency: String = "BDT",

    @Json(name = "received_at")
    val receivedAt: String, // ISO-8601 string, e.g. 2026-11-09T10:30:00Z

    @Json(name = "sim_slot")
    val simSlot: Int = 1,

    @Json(name = "device_id")
    val deviceId: String,

    @Json(name = "merchant_id")
    val merchantId: String,

    @Json(name = "event_id")
    val eventId: String
)

@JsonClass(generateAdapter = true)
data class PaymentEventResponse(
    @Json(name = "status")
    val status: String, // APPROVED, UNMATCHED, PENDING_REVIEW, DUPLICATE, REJECTED

    @Json(name = "duplicate")
    val duplicate: Boolean = false,

    @Json(name = "reason")
    val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @Json(name = "ok")
    val ok: Boolean,

    @Json(name = "service")
    val service: String? = null,

    @Json(name = "merchant_id")
    val merchantId: String? = null,

    @Json(name = "server_time")
    val serverTime: String? = null
)

@JsonClass(generateAdapter = true)
data class ConsentRequest(
    @Json(name = "merchant_id")
    val merchantId: String,

    @Json(name = "device_id")
    val deviceId: String,

    @Json(name = "consent_version")
    val consentVersion: String = "1.0",

    @Json(name = "accepted_at")
    val acceptedAt: String
)

@JsonClass(generateAdapter = true)
data class StatsResponse(
    @Json(name = "total")
    val total: Int = 0,

    @Json(name = "today_amount")
    val todayAmount: String = "0.00",

    @Json(name = "by_status")
    val byStatus: Map<String, Int> = emptyMap(),

    @Json(name = "by_provider")
    val byProvider: Map<String, ProviderStat> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class ProviderStat(
    @Json(name = "count")
    val count: Int = 0,

    @Json(name = "amount")
    val amount: String = "0.00"
)

@JsonClass(generateAdapter = true)
data class HistoryItem(
    @Json(name = "provider")
    val provider: String,

    @Json(name = "transaction_id")
    val transactionId: String,

    @Json(name = "amount")
    val amount: String,

    @Json(name = "currency")
    val currency: String = "BDT",

    @Json(name = "status")
    val status: String,

    @Json(name = "reason")
    val reason: String? = null,

    @Json(name = "received_at")
    val receivedAt: String,

    @Json(name = "telegram_user_id")
    val telegramUserId: Long? = null,

    @Json(name = "telegram_username")
    val telegramUsername: String? = null,

    @Json(name = "telegram_full_name")
    val telegramFullName: String? = null
)

@JsonClass(generateAdapter = true)
data class HistoryResponse(
    @Json(name = "items")
    val items: List<HistoryItem> = emptyList(),

    @Json(name = "count")
    val count: Int = 0
)
