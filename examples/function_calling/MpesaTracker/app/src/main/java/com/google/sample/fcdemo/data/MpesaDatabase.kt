package com.google.sample.fcdemo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, EnvelopeEntity::class], 
    version = 4, 
    exportSchema = false
)
abstract class MpesaDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun envelopeDao(): EnvelopeDao

    companion object {
        @Volatile
        private var INSTANCE: MpesaDatabase? = null
        
        /**
         * Migration from version 3 to 4: Add envelope table
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create envelopes table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS envelopes (
                        envelopeId TEXT NOT NULL PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        budgetAmountKes REAL NOT NULL DEFAULT 0.0,
                        currentBalanceKes REAL NOT NULL DEFAULT 0.0,
                        spentAmountKes REAL NOT NULL DEFAULT 0.0,
                        icon TEXT NOT NULL DEFAULT '💰',
                        color TEXT NOT NULL DEFAULT '#4CAF50',
                        isActive INTEGER NOT NULL DEFAULT 1,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        budgetPeriodStart INTEGER NOT NULL,
                        budgetPeriodEnd INTEGER NOT NULL DEFAULT 0,
                        isOverspent INTEGER NOT NULL DEFAULT 0,
                        allowOverspend INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                
                // Insert default envelopes
                val currentTime = System.currentTimeMillis()
                database.execSQL("""
                    INSERT INTO envelopes (envelopeId, displayName, description, budgetAmountKes, icon, color, sortOrder, createdAt, lastUpdated, budgetPeriodStart) VALUES
                    ('rent', 'Rent & Housing', 'Rent, utilities, and housing expenses', 15000.0, '🏠', '#FF5722', 1, $currentTime, $currentTime, $currentTime),
                    ('groceries', 'Groceries & Food', 'Food shopping and household supplies', 8000.0, '🛒', '#4CAF50', 2, $currentTime, $currentTime, $currentTime),
                    ('transport', 'Transport', 'Matatu, boda boda, fuel, and travel', 3000.0, '🚌', '#2196F3', 3, $currentTime, $currentTime, $currentTime),
                    ('bills', 'Bills & Utilities', 'Electricity, water, internet, phone', 5000.0, '📱', '#FF9800', 4, $currentTime, $currentTime, $currentTime),
                    ('entertainment', 'Entertainment', 'Movies, dining out, social activities', 2000.0, '🎬', '#9C27B0', 5, $currentTime, $currentTime, $currentTime),
                    ('shopping', 'Shopping', 'Clothes, personal items, non-essentials', 3000.0, '🛍️', '#E91E63', 6, $currentTime, $currentTime, $currentTime),
                    ('savings', 'Savings', 'Emergency fund and long-term savings', 5000.0, '💰', '#4CAF50', 7, $currentTime, $currentTime, $currentTime),
                    ('health', 'Health & Medical', 'Doctor visits, medicine, healthcare', 2000.0, '🏥', '#F44336', 8, $currentTime, $currentTime, $currentTime)
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): MpesaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MpesaDatabase::class.java,
                    "mpesa_database"
                )
                .addMigrations(MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 