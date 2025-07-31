package com.google.sample.fcdemo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Envelope Entity for Virtual Envelope Budgeting System
 * Each envelope represents a spending category with budget and balance tracking
 */
@Entity(tableName = "envelopes")
data class EnvelopeEntity(
    @PrimaryKey
    val envelopeId: String,                    // Unique identifier (e.g., "rent", "groceries")
    val displayName: String,                   // User-friendly name (e.g., "Rent & Housing")
    val description: String = "",              // Optional description
    val budgetAmountKes: Double = 0.0,        // Monthly budget allocation in KES
    val currentBalanceKes: Double = 0.0,      // Current available balance in KES  
    val spentAmountKes: Double = 0.0,         // Amount spent this period in KES
    val icon: String = "💰",                  // Emoji icon for UI display
    val color: String = "#4CAF50",            // Hex color for UI theming
    val isActive: Boolean = true,             // Whether envelope is currently in use
    val sortOrder: Int = 0,                   // Display order in UI
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    // Budget period tracking
    val budgetPeriodStart: Long = System.currentTimeMillis(),  // Start of current budget period
    val budgetPeriodEnd: Long = 0L,           // End of current budget period (0 = no end)
    val isOverspent: Boolean = false,         // Flag when spent > budget
    val allowOverspend: Boolean = true        // Whether to allow spending beyond budget
) {
    /**
     * Calculate remaining budget amount
     */
    val remainingBudgetKes: Double
        get() = budgetAmountKes - spentAmountKes
    
    /**
     * Calculate budget usage percentage (0.0 to 1.0+)
     */
    val budgetUsagePercentage: Double
        get() = if (budgetAmountKes > 0) spentAmountKes / budgetAmountKes else 0.0
    
    /**
     * Check if envelope is approaching budget limit (90% threshold)
     */
    val isApproachingLimit: Boolean
        get() = budgetUsagePercentage >= 0.9
    
    /**
     * Check if envelope has exceeded budget
     */
    val isOverBudget: Boolean
        get() = spentAmountKes > budgetAmountKes && budgetAmountKes > 0
    
    /**
     * Get status for UI display
     */
    val status: EnvelopeStatus
        get() = when {
            !isActive -> EnvelopeStatus.INACTIVE
            isOverBudget -> EnvelopeStatus.OVER_BUDGET
            isApproachingLimit -> EnvelopeStatus.APPROACHING_LIMIT
            budgetUsagePercentage > 0.5 -> EnvelopeStatus.MODERATE_USAGE
            else -> EnvelopeStatus.HEALTHY
        }
}

/**
 * Envelope status enumeration for UI display
 */
enum class EnvelopeStatus {
    HEALTHY,           // Under 50% usage
    MODERATE_USAGE,    // 50-89% usage  
    APPROACHING_LIMIT, // 90-99% usage
    OVER_BUDGET,       // 100%+ usage
    INACTIVE           // Envelope disabled
}

/**
 * Default envelope categories for initial setup
 */
object DefaultEnvelopes {
    val SUSPENSE = "suspense"        // Special: Central hub for all M-PESA flows
    val RENT = "rent"
    val GROCERIES = "groceries" 
    val TRANSPORT = "transport"
    val ENTERTAINMENT = "entertainment"
    val BILLS = "bills"
    val SAVINGS = "savings"
    val SHOPPING = "shopping"
    val HEALTH = "health"
    
    /**
     * Get default envelope configurations for first-time setup
     */
    fun getDefaultEnvelopes(): List<EnvelopeEntity> {
        return listOf(
            // SUSPENSE ACCOUNT: Special envelope that receives all M-PESA transactions first
            EnvelopeEntity(
                envelopeId = SUSPENSE,
                displayName = "Suspense Account",
                description = "Central hub - all M-PESA money flows through here before allocation",
                budgetAmountKes = 0.0,  // No budget limit for suspense account
                icon = "⚖️",
                color = "#607D8B",  // Neutral gray color
                sortOrder = 0,      // Always first in list
                allowOverspend = true  // Suspense can go negative (temporary state)
            ),
            EnvelopeEntity(
                envelopeId = RENT,
                displayName = "Rent & Housing",
                description = "Rent, utilities, and housing expenses",
                budgetAmountKes = 15000.0,
                icon = "🏠",
                color = "#FF5722",
                sortOrder = 1
            ),
            EnvelopeEntity(
                envelopeId = GROCERIES,
                displayName = "Groceries & Food",
                description = "Food shopping and household supplies",
                budgetAmountKes = 8000.0,
                icon = "🛒",
                color = "#4CAF50",
                sortOrder = 2
            ),
            EnvelopeEntity(
                envelopeId = TRANSPORT,
                displayName = "Transport",
                description = "Matatu, boda boda, fuel, and travel",
                budgetAmountKes = 3000.0,
                icon = "🚌",
                color = "#2196F3",
                sortOrder = 3
            ),
            EnvelopeEntity(
                envelopeId = BILLS,
                displayName = "Bills & Utilities",
                description = "Electricity, water, internet, phone",
                budgetAmountKes = 5000.0,
                icon = "📱",
                color = "#FF9800",
                sortOrder = 4
            ),
            EnvelopeEntity(
                envelopeId = ENTERTAINMENT,
                displayName = "Entertainment",
                description = "Movies, dining out, social activities",
                budgetAmountKes = 2000.0,
                icon = "🎬",
                color = "#9C27B0",
                sortOrder = 5
            ),
            EnvelopeEntity(
                envelopeId = SHOPPING,
                displayName = "Shopping",
                description = "Clothes, personal items, non-essentials",
                budgetAmountKes = 3000.0,
                icon = "🛍️",
                color = "#E91E63",
                sortOrder = 6
            ),
            EnvelopeEntity(
                envelopeId = SAVINGS,
                displayName = "Savings",
                description = "Emergency fund and long-term savings",
                budgetAmountKes = 5000.0,
                icon = "💰",
                color = "#4CAF50",
                sortOrder = 7
            ),
            EnvelopeEntity(
                envelopeId = HEALTH,
                displayName = "Health & Medical",
                description = "Doctor visits, medicine, healthcare",
                budgetAmountKes = 2000.0,
                icon = "🏥",
                color = "#F44336",
                sortOrder = 8
            )
        )
    }
}