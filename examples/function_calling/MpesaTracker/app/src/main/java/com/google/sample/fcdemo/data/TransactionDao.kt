package com.google.sample.fcdemo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllGrouped(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getById(transactionId: String): TransactionEntity?

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE transactionId = :transactionId)")
    suspend fun exists(transactionId: String): Boolean

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @androidx.paging.ExperimentalPagingApi
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun pagingSource(): androidx.paging.PagingSource<Int, TransactionEntity>
} 