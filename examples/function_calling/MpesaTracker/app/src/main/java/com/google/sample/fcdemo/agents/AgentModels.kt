package com.google.sample.fcdemo.agents

/**
 * EdgeFinance AI Agents - Core Data Models
 * Showcasing autonomous AI agents working together at the edge
 */

// Agent Types
enum class AgentType {
    FINANCE_IQ,      // 💎 Transaction extraction specialist
    SPEND_WISE       // 🧠 Categorization specialist
}

// Agent Status States
enum class AgentStatus {
    IDLE,           // Agent is waiting
    INITIALIZING,   // Agent is starting up
    ACTIVE,         // Agent is processing
    THINKING,       // Agent is analyzing (for UI animation)
    COMPLETE,       // Agent finished successfully
    ERROR,          // Agent encountered an error
    HANDOFF         // Agent is passing data to next agent
}

// Processing Stages
enum class ProcessingStage {
    SMS_RECEIVED,           // Initial SMS received
    FINANCE_IQ_ACTIVATED,   // FinanceIQ agent starts
    EXTRACTING_DATA,        // Parsing transaction details
    DATA_EXTRACTED,         // Extraction complete
    SPEND_WISE_ACTIVATED,   // SpendWise agent starts
    CATEGORIZING,           // Analyzing for category
    CATEGORIZED,            // Categorization complete
    TRANSACTION_SAVED,      // Final save to database
    COMPLETE               // All processing finished
}

// Agent State Data Class
data class AgentState(
    val type: AgentType,
    val status: AgentStatus,
    val progress: Float = 0f,            // 0.0 to 1.0
    val message: String = "",            // Current activity message
    val confidence: Float? = null,       // Agent confidence in result
    val processingTimeMs: Long = 0L,     // Time spent processing
    val lastUpdated: Long = System.currentTimeMillis()
)

// Function Call Record
data class FunctionCall(
    val agentType: AgentType,
    val functionName: String,
    val inputs: Map<String, Any>,
    val outputs: Map<String, Any>? = null,
    val success: Boolean = false,
    val executionTimeMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float? = null
)

// Agent Chat Message
data class AgentChatMessage(
    val agentType: AgentType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isThinking: Boolean = false      // For typing indicator animation
)

// Processing Timeline Event
data class TimelineEvent(
    val stage: ProcessingStage,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0L,             // Duration for this stage
    val agentType: AgentType? = null,    // Which agent handled this stage
    val details: String = ""             // Additional details
)

// Agent Collaboration Session
data class CollaborationSession(
    val sessionId: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val agents: List<AgentState> = emptyList(),
    val functionCalls: List<FunctionCall> = emptyList(),
    val chatMessages: List<AgentChatMessage> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList(),
    val success: Boolean = false,
    val transactionId: String? = null,
    // Processing Theater data
    val rawInput: String? = null,           // Raw SMS text for FinanceIQ view
    val structuredData: Map<String, Any> = emptyMap()  // Extracted data for SpendWise view
)

// Agent Performance Metrics
data class AgentMetrics(
    val agentType: AgentType,
    val totalFunctionCalls: Int = 0,
    val successfulCalls: Int = 0,
    val averageProcessingTimeMs: Long = 0L,
    val averageConfidence: Float = 0f,
    val lastActiveTime: Long? = null
)

// System-wide Edge AI Stats
data class EdgeAIStats(
    val totalSessions: Int = 0,
    val successfulSessions: Int = 0,
    val totalFunctionCalls: Int = 0,
    val averageSessionDuration: Long = 0L,
    val agentMetrics: Map<AgentType, AgentMetrics> = emptyMap(),
    val uptime: Long = 0L
)