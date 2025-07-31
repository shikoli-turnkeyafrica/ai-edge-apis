package com.google.sample.fcdemo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val transactionId: String,
    val direction: String,
    val amountKes: Double,
    val feeKes: Double = 0.0,
    val counterparty: String = "unknown",
    val channel: String = "unknown",
    val dateTime: String = "unknown",
    val balanceAfter: Double? = null,
    val rawMessage: String,
    val timestamp: Long = System.currentTimeMillis()
) 