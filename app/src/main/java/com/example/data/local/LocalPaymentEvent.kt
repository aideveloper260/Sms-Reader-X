package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_payment_events",
    indices = [
        Index(value = ["eventId"], unique = true),
        Index(value = ["provider", "transactionId"]),
        Index(value = ["syncStatus"])
    ]
)
data class LocalPaymentEvent(
    @PrimaryKey
    val eventId: String,
    val provider: String,              // "bkash", "nagad", "rocket"
    val transactionId: String,
    val amount: String,                // "500.00"
    val currency: String = "BDT",
    val receiverNumber: String,
    val reference: String,
    val simSlot: Int,
    val receivedAt: String,            // ISO timestamp
    val createdAt: Long = System.currentTimeMillis(),

    // Sync state
    val syncStatus: String,            // QUEUED, SENDING, SENT_APPROVED, SENT_UNMATCHED, SENT_REVIEW, SENT_DUPLICATE, FAILED_RETRY
    val retryCount: Int = 0,
    val lastError: String? = null,
    val backendReason: String? = null,
    val matchedDepositId: Long? = null
)
