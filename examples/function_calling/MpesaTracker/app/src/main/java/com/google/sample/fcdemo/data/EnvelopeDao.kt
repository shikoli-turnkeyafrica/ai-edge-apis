package com.google.sample.fcdemo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Envelope operations
 * Provides database queries for virtual envelope budgeting system
 */
@Dao
interface EnvelopeDao {
    
    /**
     * Get all envelopes ordered by sort order
     */
    @Query("SELECT * FROM envelopes WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun getAllEnvelopes(): Flow<List<EnvelopeEntity>>
    
    /**
     * Get all envelopes as list (for one-time queries)
     */
    @Query("SELECT * FROM envelopes WHERE isActive = 1 ORDER BY sortOrder ASC")
    suspend fun getAllEnvelopesList(): List<EnvelopeEntity>
    
    /**
     * Get envelope by ID
     */
    @Query("SELECT * FROM envelopes WHERE envelopeId = :envelopeId LIMIT 1")
    suspend fun getEnvelopeById(envelopeId: String): EnvelopeEntity?
    
    /**
     * Update envelope by ID (for budget changes, balance updates)
     */
    @Update
    suspend fun updateEnvelope(envelope: EnvelopeEntity)
    
    /**
     * Update envelope balance and spent amount
     * This is the key method for transaction allocation
     */
    @Query("""
        UPDATE envelopes 
        SET currentBalanceKes = currentBalanceKes + :amountChange,
            spentAmountKes = CASE 
                WHEN :amountChange < 0 THEN spentAmountKes + ABS(:amountChange)
                ELSE spentAmountKes 
            END,
            isOverspent = CASE
                WHEN (spentAmountKes + CASE WHEN :amountChange < 0 THEN ABS(:amountChange) ELSE 0 END) > budgetAmountKes 
                THEN 1 
                ELSE 0 
            END,
            lastUpdated = :timestamp
        WHERE envelopeId = :envelopeId
    """)
    suspend fun updateEnvelopeBalance(
        envelopeId: String, 
        amountChange: Double,  // Negative for spending, positive for income/refunds
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Reset all envelope spent amounts (for new budget period)
     */
    @Query("""
        UPDATE envelopes 
        SET spentAmountKes = 0.0,
            isOverspent = 0,
            budgetPeriodStart = :newPeriodStart,
            lastUpdated = :timestamp
        WHERE isActive = 1
    """)
    suspend fun resetEnvelopeSpending(
        newPeriodStart: Long = System.currentTimeMillis(),
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Transfer money between envelopes
     */
    @Transaction
    suspend fun transferBetweenEnvelopes(
        fromEnvelopeId: String,
        toEnvelopeId: String, 
        amount: Double
    ) {
        // Deduct from source envelope (this will increase spent amount)
        updateEnvelopeBalance(fromEnvelopeId, -amount)
        // Add to destination envelope (this adds to balance without affecting spent)
        updateEnvelopeBalance(toEnvelopeId, amount)
    }
    
    /**
     * Insert new envelope
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnvelope(envelope: EnvelopeEntity)
    
    /**
     * Insert multiple envelopes (for initial setup)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnvelopes(envelopes: List<EnvelopeEntity>)
    
    /**
     * Delete envelope (sets inactive instead of hard delete)
     */
    @Query("UPDATE envelopes SET isActive = 0, lastUpdated = :timestamp WHERE envelopeId = :envelopeId")
    suspend fun deactivateEnvelope(envelopeId: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Get envelopes that are approaching their budget limit (90%+)
     */
    @Query("""
        SELECT * FROM envelopes 
        WHERE isActive = 1 
        AND budgetAmountKes > 0
        AND (spentAmountKes / budgetAmountKes) >= 0.9
        ORDER BY (spentAmountKes / budgetAmountKes) DESC
    """)
    suspend fun getEnvelopesApproachingLimit(): List<EnvelopeEntity>
    
    /**
     * Get envelopes that are over budget
     */
    @Query("""
        SELECT * FROM envelopes 
        WHERE isActive = 1 
        AND spentAmountKes > budgetAmountKes 
        AND budgetAmountKes > 0
        ORDER BY (spentAmountKes - budgetAmountKes) DESC
    """)
    suspend fun getOverBudgetEnvelopes(): List<EnvelopeEntity>
    
    /**
     * Get envelope for a specific transaction category
     * This maps transaction categories to envelope IDs
     */
    @Query("""
        SELECT * FROM envelopes 
        WHERE envelopeId = CASE :category
            WHEN 'groceries' THEN 'groceries'
            WHEN 'transport' THEN 'transport'
            WHEN 'bills' THEN 'bills'
            WHEN 'entertainment' THEN 'entertainment'
            WHEN 'shopping' THEN 'shopping'
            WHEN 'health' THEN 'health'
            WHEN 'education' THEN 'education'
            WHEN 'business' THEN 'savings'
            WHEN 'personal' THEN 'shopping'
            ELSE 'other'
        END
        AND isActive = 1
        LIMIT 1
    """)
    suspend fun getEnvelopeForCategory(category: String): EnvelopeEntity?
    
    /**
     * Get total budget across all active envelopes
     */
    @Query("SELECT SUM(budgetAmountKes) FROM envelopes WHERE isActive = 1")
    suspend fun getTotalBudget(): Double?
    
    /**
     * Get total spent across all active envelopes
     */
    @Query("SELECT SUM(spentAmountKes) FROM envelopes WHERE isActive = 1")
    suspend fun getTotalSpent(): Double?
    
    /**
     * Check if envelopes are initialized (for first-run detection)
     */
    @Query("SELECT COUNT(*) FROM envelopes WHERE isActive = 1")
    suspend fun getActiveEnvelopeCount(): Int
}