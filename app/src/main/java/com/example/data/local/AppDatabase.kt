package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocalPaymentEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun paymentEventDao(): PaymentEventDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_reader_x.db"
                ).build().also { instance = it }
            }
        }
    }
}
