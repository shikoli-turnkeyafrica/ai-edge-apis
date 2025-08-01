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
     * Allocate M-PESA transactions using the correct flow:
     * 💰 RECEIVED → Suspense Account (income pool for future allocation)
     * 💸 SENT → Target Envelope (direct spending allocation)
     */
    suspend fun allocateTransaction(
        transactionId: String,
        category: String,
        amount: Double,
        direction: String,
        counterparty: String = ""
    ): EnvelopeAllocationResult {
        
        try {
            Log.d(TAG, "📱 M-PESA allocation: $transactionId, category: $category, amount: $amount, direction: $direction")
            
            return when (direction.lowercase()) {
                "received" -> {
                    // 💰 RECEIVED money goes to Suspense Account (income pool)
                    handleReceivedMoney(amount, counterparty)
                }
                "sent" -> {
                    // 💸 SENT money goes directly to target envelope
                    handleSpentMoney(category, amount, counterparty)
                }
                else -> {
                    Log.w(TAG, "⚠️ Unknown direction: $direction, defaulting to spending")
                    handleSpentMoney(category, amount, counterparty)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in M-PESA allocation: ${e.message}", e)
            return EnvelopeAllocationResult(
                success = false,
                message = "M-PESA allocation failed: ${e.message}",
                warningLevel = WarningLevel.ERROR
            )
        }
    }
    
    /**
     * Handle received money: Add to Suspense Account
     */
    private suspend fun handleReceivedMoney(
        amount: Double,
        counterparty: String
    ): EnvelopeAllocationResult {
        val suspenseAccount = envelopeDao.getEnvelopeById("suspense")
        if (suspenseAccount == null) {
            Log.e(TAG, "❌ Suspense Account not found!")
            return EnvelopeAllocationResult(
                success = false,
                message = "Suspense Account missing - system error",
                warningLevel = WarningLevel.ERROR
            )
        }
        
        // Add income to Suspense Account
        envelopeDao.updateEnvelopeBalance("suspense", amount)
        Log.i(TAG, "💰 Income KSh${amount} added to Suspense Account")
        
        return EnvelopeAllocationResult(
            success = true,
            envelopeId = "suspense",
            envelopeName = "Suspense Account",
            message = "💰 Income KSh${amount} received from $counterparty → Suspense Account",
            warningLevel = WarningLevel.NONE
        )
    }
    
    /**
     * Handle spent money: Direct allocation to target envelope (or Miscellaneous if no envelope)
     */
    private suspend fun handleSpentMoney(
        category: String,
        amount: Double,
        counterparty: String
    ): EnvelopeAllocationResult {
        // Find target envelope for this category
        val targetEnvelope = findEnvelopeForCategory(category)
        
        if (targetEnvelope != null && targetEnvelope.envelopeId != "suspense") {
            // Direct allocation to target envelope
            envelopeDao.updateEnvelopeBalance(targetEnvelope.envelopeId, -amount)
            Log.i(TAG, "💸 Spending KSh${amount} allocated to ${targetEnvelope.displayName}")
            
            // Get updated envelope for status
            val updatedEnvelope = envelopeDao.getEnvelopeById(targetEnvelope.envelopeId)
            if (updatedEnvelope != null) {
                val warningLevel = when {
                    updatedEnvelope.isOverBudget -> WarningLevel.CRITICAL
                    updatedEnvelope.isApproachingLimit -> WarningLevel.WARNING  
                    updatedEnvelope.budgetUsagePercentage > 0.75 -> WarningLevel.CAUTION
                    else -> WarningLevel.NONE
                }
                
                val message = generateSpendingMessage(updatedEnvelope, amount, counterparty)
                
                return EnvelopeAllocationResult(
                    success = true,
                    envelopeId = targetEnvelope.envelopeId,
                    envelopeName = targetEnvelope.displayName,
                    previousBalance = targetEnvelope.currentBalanceKes,
                    newBalance = updatedEnvelope.currentBalanceKes,
                    budgetUsagePercentage = updatedEnvelope.budgetUsagePercentage,
                    message = message,
                    warningLevel = warningLevel
                )
            }
        } else {
            // No envelope found - allocate to Miscellaneous envelope (create if needed)
            return handleMiscellaneousSpending(category, amount, counterparty)
        }
        
        return EnvelopeAllocationResult(
            success = false,
            message = "Spending allocation failed"
        )
    }
    
    /**
     * Handle spending that doesn't match any envelope - goes to Miscellaneous
     */
    private suspend fun handleMiscellaneousSpending(
        category: String,
        amount: Double,
        counterparty: String
    ): EnvelopeAllocationResult {
        // Find or create Miscellaneous envelope
        var miscEnvelope = envelopeDao.getEnvelopeById("miscellaneous")
        
        if (miscEnvelope == null) {
            // Create Miscellaneous envelope if it doesn't exist
            val currentTime = System.currentTimeMillis()
            miscEnvelope = EnvelopeEntity(
                envelopeId = "miscellaneous",
                displayName = "Miscellaneous",
                description = "Uncategorized spending - needs manual allocation",
                budgetAmountKes = 5000.0,  // Default budget
                icon = "📦",
                color = "#9E9E9E",  // Gray color
                sortOrder = 99  // Last in list
            )
            envelopeDao.insertEnvelope(miscEnvelope)
            Log.i(TAG, "📦 Created Miscellaneous envelope")
        }
        
        // Allocate to Miscellaneous envelope
        envelopeDao.updateEnvelopeBalance("miscellaneous", -amount)
        Log.w(TAG, "📦 Uncategorized spending KSh${amount} → Miscellaneous (category: $category)")
        
        val updatedMisc = envelopeDao.getEnvelopeById("miscellaneous")
        if (updatedMisc != null) {
            return EnvelopeAllocationResult(
                success = true,
                envelopeId = "miscellaneous",
                envelopeName = "Miscellaneous",
                newBalance = updatedMisc.currentBalanceKes,
                message = "📦 Uncategorized: KSh${amount} → Miscellaneous (category: $category)",
                warningLevel = WarningLevel.INFO
            )
        }
        
        return EnvelopeAllocationResult(
            success = false,
            message = "Failed to allocate to Miscellaneous envelope"
        )
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
     * Generate user-friendly spending message for direct envelope allocation
     */
    private fun generateSpendingMessage(
        envelope: EnvelopeEntity, 
        amount: Double,
        counterparty: String
    ): String {
        val usagePercent = (envelope.budgetUsagePercentage * 100).toInt()
        
        return when {
            envelope.isOverBudget -> "⚠️ Over budget! ${envelope.displayName}: KSh$amount spent to $counterparty. Budget exceeded by KSh${envelope.spentAmountKes - envelope.budgetAmountKes}"
            envelope.isApproachingLimit -> "🟡 Approaching limit: ${envelope.displayName}: KSh$amount spent to $counterparty ($usagePercent% used)"
            usagePercent > 75 -> "🟠 High usage: ${envelope.displayName}: KSh$amount spent to $counterparty ($usagePercent% used)"
            else -> "✅ ${envelope.displayName}: KSh$amount spent to $counterparty ($usagePercent% used)"
        }
    }
    
    /**
     * Generate user-friendly allocation message for suspense account flow (legacy)
     */
    private fun generateSuspenseAllocationMessage(
        envelope: EnvelopeEntity, 
        amount: Double,
        counterparty: String
    ): String {
        val usagePercent = (envelope.budgetUsagePercentage * 100).toInt()
        
        return when {
            envelope.isOverBudget -> "⚠️ Over budget! Suspense → ${envelope.displayName}: KSh$amount allocated. Budget exceeded by KSh${envelope.spentAmountKes - envelope.budgetAmountKes}"
            envelope.isApproachingLimit -> "🟡 Approaching limit: Suspense → ${envelope.displayName}: KSh$amount allocated ($usagePercent% used)"
            usagePercent > 75 -> "🟠 High usage: Suspense → ${envelope.displayName}: KSh$amount allocated ($usagePercent% used)"
            else -> "✅ Suspense → ${envelope.displayName}: KSh$amount allocated ($usagePercent% used)"
        }
    }
    
    /**
     * Generate user-friendly allocation message (legacy method for compatibility)
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
     * Get the current Suspense Account balance for UI display
     */
    suspend fun getSuspenseAccountBalance(): Double {
        val suspenseAccount = envelopeDao.getEnvelopeById("suspense")
        return suspenseAccount?.currentBalanceKes ?: 0.0
    }
    
    /**
     * Get Suspense Account info for UI display
     */
    suspend fun getSuspenseAccountInfo(): EnvelopeEntity? {
        return envelopeDao.getEnvelopeById("suspense")
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