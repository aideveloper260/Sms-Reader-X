package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: LocalPaymentEvent): Long

    @Update
    suspend fun updateEvent(event: LocalPaymentEvent)

    @Query("SELECT * FROM local_payment_events ORDER BY createdAt DESC")
    fun getAllEvents(): Flow<List<LocalPaymentEvent>>

    @Query("SELECT * FROM local_payment_events WHERE syncStatus IN ('QUEUED', 'FAILED_RETRY') ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingEvents(limit: Int = 20): List<LocalPaymentEvent>

    @Query("SELECT * FROM local_payment_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getEventById(eventId: String): LocalPaymentEvent?

    @Query("SELECT * FROM local_payment_events WHERE provider = :provider AND transactionId = :trxId LIMIT 1")
    suspend fun getEventByTrx(provider: String, trxId: String): LocalPaymentEvent?

    @Query("SELECT COUNT(*) FROM local_payment_events")
    fun getTotalEventCount(): Flow<Int>

    @Query("DELETE FROM local_payment_events WHERE syncStatus LIKE 'SENT_%'")
    suspend fun clearDeliveredEvents()
}
