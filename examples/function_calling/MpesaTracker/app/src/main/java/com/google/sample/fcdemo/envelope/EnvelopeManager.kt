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
     * Allocate a transaction using the two-step Suspense Account flow
     * STEP 1: All M-PESA money flows through Suspense Account first
     * STEP 2: Money gets allocated from Suspense to the target envelope
     */
    suspend fun allocateTransaction(
        transactionId: String,
        category: String,
        amount: Double,
        direction: String,
        counterparty: String = ""
    ): EnvelopeAllocationResult {
        
        try {
            Log.d(TAG, "⚖️ Starting two-step allocation: $transactionId, category: $category, amount: $amount, direction: $direction")
            
            // STEP 1: Add all M-PESA transactions to Suspense Account first
            val suspenseAccount = envelopeDao.getEnvelopeById("suspense")
            if (suspenseAccount == null) {
                Log.e(TAG, "❌ Suspense Account not found! This should never happen.")
                return EnvelopeAllocationResult(
                    success = false,
                    message = "Suspense Account missing - system error",
                    warningLevel = WarningLevel.ERROR
                )
            }
            
            // Calculate the raw M-PESA amount (positive for received, negative for sent)
            val mpesaAmount = when (direction.lowercase()) {
                "received" -> amount   // Money coming in
                "sent" -> -amount     // Money going out
                else -> -amount       // Default to outgoing
            }
            
            // Add to Suspense Account
            envelopeDao.updateEnvelopeBalance("suspense", mpesaAmount)
            Log.i(TAG, "⚖️ Step 1: Added KSh${mpesaAmount} to Suspense Account")
            
            // STEP 2: Allocate from Suspense to target envelope (only for spending)
            if (direction.lowercase() == "sent") {
                // Find target envelope for spending allocation
                val targetEnvelope = findEnvelopeForCategory(category)
                
                if (targetEnvelope != null && targetEnvelope.envelopeId != "suspense") {
                    // Transfer from Suspense to target envelope
                    envelopeDao.transferBetweenEnvelopes("suspense", targetEnvelope.envelopeId, amount)
                    Log.i(TAG, "💸 Step 2: Allocated KSh${amount} from Suspense to ${targetEnvelope.displayName}")
                    
                    // Get updated target envelope for status
                    val updatedTarget = envelopeDao.getEnvelopeById(targetEnvelope.envelopeId)
                    if (updatedTarget != null) {
                        val warningLevel = when {
                            updatedTarget.isOverBudget -> WarningLevel.CRITICAL
                            updatedTarget.isApproachingLimit -> WarningLevel.WARNING  
                            updatedTarget.budgetUsagePercentage > 0.75 -> WarningLevel.CAUTION
                            else -> WarningLevel.NONE
                        }
                        
                        val message = generateSuspenseAllocationMessage(updatedTarget, amount, counterparty)
                        
                        return EnvelopeAllocationResult(
                            success = true,
                            envelopeId = targetEnvelope.envelopeId,
                            envelopeName = targetEnvelope.displayName,
                            previousBalance = targetEnvelope.currentBalanceKes,
                            newBalance = updatedTarget.currentBalanceKes,
                            budgetUsagePercentage = updatedTarget.budgetUsagePercentage,
                            message = message,
                            warningLevel = warningLevel
                        )
                    }
                } else {
                    Log.w(TAG, "⚠️ No target envelope found for category: $category - money stays in Suspense")
                    return EnvelopeAllocationResult(
                        success = true,
                        envelopeId = "suspense",
                        envelopeName = "Suspense Account",
                        message = "⚖️ Transaction added to Suspense Account (category: $category)",
                        warningLevel = WarningLevel.INFO
                    )
                }
            } else {
                // For received money, just stay in Suspense Account
                Log.i(TAG, "💰 Income KSh${amount} added to Suspense Account")
                return EnvelopeAllocationResult(
                    success = true,
                    envelopeId = "suspense",
                    envelopeName = "Suspense Account",
                    message = "💰 Income KSh${amount} added to Suspense Account",
                    warningLevel = WarningLevel.NONE
                )
            }
            
            return EnvelopeAllocationResult(
                success = false,
                message = "Allocation process incomplete"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in suspense allocation: ${e.message}", e)
            return EnvelopeAllocationResult(
                success = false,
                message = "Suspense allocation failed: ${e.message}",
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
     * Generate user-friendly allocation message for suspense account flow
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