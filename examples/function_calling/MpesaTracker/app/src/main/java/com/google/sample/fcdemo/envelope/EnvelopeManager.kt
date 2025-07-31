package com.google.sample.fcdemo.envelope

import android.content.Context
import android.util.Log
import com.google.sample.fcdemo.data.EnvelopeDao
import com.google.sample.fcdemo.data.EnvelopeEntity
import com.google.sample.fcdemo.data.MpesaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Envelope Manager - Core allocation engine for virtual envelope budgeting
 * Handles transaction allocation, balance updates, and envelope management
 */
class EnvelopeManager private constructor(context: Context) {
    
    companion object {
        private const val TAG = "EnvelopeManager"
        
        @Volatile
        private var INSTANCE: EnvelopeManager? = null
        
        fun getInstance(context: Context): EnvelopeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EnvelopeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val envelopeDao: EnvelopeDao = MpesaDatabase.getDatabase(context).envelopeDao()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Allocate a transaction to the appropriate envelope
     * This is the core method called from SMS processing
     */
    suspend fun allocateTransaction(
        transactionId: String,
        category: String,
        amount: Double,
        direction: String,
        counterparty: String = ""
    ): EnvelopeAllocationResult {
        
        try {
            Log.d(TAG, "🎯 Allocating transaction: $transactionId, category: $category, amount: $amount, direction: $direction")
            
            // Find appropriate envelope for this category
            val envelope = findEnvelopeForCategory(category)
            
            if (envelope == null) {
                Log.w(TAG, "⚠️ No envelope found for category: $category - using default allocation")
                return EnvelopeAllocationResult(
                    success = false,
                    envelopeId = null,
                    message = "No envelope configured for category: $category",
                    warningLevel = WarningLevel.INFO
                )
            }
            
            // Calculate amount change based on direction
            val amountChange = when (direction.lowercase()) {
                "sent" -> -amount  // Spending - deduct from envelope
                "received" -> amount // Income - add to envelope (rare, but possible for refunds)
                else -> -amount // Default to spending
            }
            
            // Update envelope balance
            envelopeDao.updateEnvelopeBalance(envelope.envelopeId, amountChange)
            
            // Get updated envelope to check status
            val updatedEnvelope = envelopeDao.getEnvelopeById(envelope.envelopeId)
            
            if (updatedEnvelope != null) {
                Log.i(TAG, "✅ Allocated ${if (amountChange < 0) "expense" else "income"} of KSh${amount} to ${envelope.displayName}")
                Log.d(TAG, "📊 ${envelope.displayName}: Balance: KSh${updatedEnvelope.currentBalanceKes}, Spent: KSh${updatedEnvelope.spentAmountKes}/${updatedEnvelope.budgetAmountKes}")
                
                // Check for warnings
                val warningLevel = when {
                    updatedEnvelope.isOverBudget -> WarningLevel.CRITICAL
                    updatedEnvelope.isApproachingLimit -> WarningLevel.WARNING  
                    updatedEnvelope.budgetUsagePercentage > 0.75 -> WarningLevel.CAUTION
                    else -> WarningLevel.NONE
                }
                
                val message = generateAllocationMessage(updatedEnvelope, amountChange, counterparty)
                
                return EnvelopeAllocationResult(
                    success = true,
                    envelopeId = envelope.envelopeId,
                    envelopeName = envelope.displayName,
                    previousBalance = envelope.currentBalanceKes,
                    newBalance = updatedEnvelope.currentBalanceKes,
                    budgetUsagePercentage = updatedEnvelope.budgetUsagePercentage,
                    message = message,
                    warningLevel = warningLevel
                )
            }
            
            return EnvelopeAllocationResult(
                success = false,
                envelopeId = envelope.envelopeId,
                message = "Failed to update envelope balance"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error allocating transaction to envelope: ${e.message}", e)
            return EnvelopeAllocationResult(
                success = false,
                message = "Envelope allocation failed: ${e.message}",
                warningLevel = WarningLevel.ERROR
            )
        }
    }
    
    /**
     * Find the appropriate envelope for a transaction category
     */
    private suspend fun findEnvelopeForCategory(category: String): EnvelopeEntity? {
        return envelopeDao.getEnvelopeForCategory(category) ?: run {
            // Fallback: try to find by partial matching
            when (category.lowercase()) {
                "groceries", "food" -> envelopeDao.getEnvelopeById("groceries")
                "transport", "travel", "matatu", "boda" -> envelopeDao.getEnvelopeById("transport")
                "bills", "utilities", "electricity", "water" -> envelopeDao.getEnvelopeById("bills")
                "entertainment", "dining", "movie", "social" -> envelopeDao.getEnvelopeById("entertainment")
                "shopping", "clothes", "personal" -> envelopeDao.getEnvelopeById("shopping")
                "health", "medical", "doctor", "medicine" -> envelopeDao.getEnvelopeById("health")
                "education", "school", "fees" -> envelopeDao.getEnvelopeById("education")
                "business", "investment" -> envelopeDao.getEnvelopeById("savings")
                else -> {
                    Log.d(TAG, "🔍 No specific envelope found for category: $category, using savings as fallback")
                    envelopeDao.getEnvelopeById("savings")
                }
            }
        }
    }
    
    /**
     * Generate user-friendly allocation message
     */
    private fun generateAllocationMessage(
        envelope: EnvelopeEntity, 
        amountChange: Double,
        counterparty: String
    ): String {
        val action = if (amountChange < 0) "spent" else "received"
        val amount = kotlin.math.abs(amountChange)
        val usagePercent = (envelope.budgetUsagePercentage * 100).toInt()
        
        return when {
            envelope.isOverBudget -> "⚠️ Over budget! ${envelope.displayName}: KSh$amount $action. Budget exceeded by KSh${envelope.spentAmountKes - envelope.budgetAmountKes}"
            envelope.isApproachingLimit -> "🟡 Approaching limit: ${envelope.displayName}: KSh$amount $action ($usagePercent% used)"
            usagePercent > 75 -> "🟠 High usage: ${envelope.displayName}: KSh$amount $action ($usagePercent% used)"
            else -> "✅ ${envelope.displayName}: KSh$amount $action ($usagePercent% used)"
        }
    }
    
    /**
     * Get all envelopes with their current status
     */
    suspend fun getAllEnvelopes(): List<EnvelopeEntity> {
        return envelopeDao.getAllEnvelopesList()
    }
    
    /**
     * Get envelopes that need attention (approaching limit or over budget)
     */
    suspend fun getEnvelopesNeedingAttention(): List<EnvelopeEntity> {
        val approaching = envelopeDao.getEnvelopesApproachingLimit()
        val overBudget = envelopeDao.getOverBudgetEnvelopes()
        return (approaching + overBudget).distinctBy { it.envelopeId }
    }
    
    /**
     * Transfer money between envelopes
     */
    suspend fun transferBetweenEnvelopes(
        fromEnvelopeId: String,
        toEnvelopeId: String,
        amount: Double
    ): EnvelopeTransferResult {
        return try {
            val fromEnvelope = envelopeDao.getEnvelopeById(fromEnvelopeId)
            val toEnvelope = envelopeDao.getEnvelopeById(toEnvelopeId)
            
            if (fromEnvelope == null || toEnvelope == null) {
                return EnvelopeTransferResult(
                    success = false,
                    message = "One or both envelopes not found"
                )
            }
            
            if (fromEnvelope.currentBalanceKes < amount) {
                return EnvelopeTransferResult(
                    success = false,
                    message = "Insufficient balance in ${fromEnvelope.displayName}"
                )
            }
            
            envelopeDao.transferBetweenEnvelopes(fromEnvelopeId, toEnvelopeId, amount)
            
            Log.i(TAG, "💸 Transferred KSh$amount from ${fromEnvelope.displayName} to ${toEnvelope.displayName}")
            
            EnvelopeTransferResult(
                success = true,
                message = "Transferred KSh$amount from ${fromEnvelope.displayName} to ${toEnvelope.displayName}"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Transfer failed: ${e.message}", e)
            EnvelopeTransferResult(
                success = false,
                message = "Transfer failed: ${e.message}"
            )
        }
    }
    
    /**
     * Initialize default envelopes if none exist
     */
    suspend fun initializeDefaultEnvelopesIfNeeded(): Boolean {
        return try {
            val count = envelopeDao.getActiveEnvelopeCount()
            if (count == 0) {
                Log.i(TAG, "🎯 No envelopes found - initializing defaults")
                val defaultEnvelopes = com.google.sample.fcdemo.data.DefaultEnvelopes.getDefaultEnvelopes()
                envelopeDao.insertEnvelopes(defaultEnvelopes)
                Log.i(TAG, "✅ Initialized ${defaultEnvelopes.size} default envelopes")
                true
            } else {
                Log.d(TAG, "📋 Found $count active envelopes - no initialization needed")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize default envelopes: ${e.message}", e)
            false
        }
    }
}

/**
 * Result of envelope allocation operation
 */
data class EnvelopeAllocationResult(
    val success: Boolean,
    val envelopeId: String? = null,
    val envelopeName: String? = null,
    val previousBalance: Double = 0.0,
    val newBalance: Double = 0.0,
    val budgetUsagePercentage: Double = 0.0,
    val message: String,
    val warningLevel: WarningLevel = WarningLevel.NONE
)

/**
 * Result of envelope transfer operation
 */
data class EnvelopeTransferResult(
    val success: Boolean,
    val message: String
)

/**
 * Warning levels for envelope status
 */
enum class WarningLevel {
    NONE,        // All good
    INFO,        // Informational
    CAUTION,     // 75%+ usage
    WARNING,     // 90%+ usage  
    CRITICAL,    // Over budget
    ERROR        // System error
}