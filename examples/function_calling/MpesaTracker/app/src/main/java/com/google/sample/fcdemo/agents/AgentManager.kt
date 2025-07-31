package com.google.sample.fcdemo.agents

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * EdgeFinance AI Agent Manager
 * Orchestrates the collaboration between FinanceIQ and SpendWise agents
 * Provides real-time status updates for UI showcase
 */
class AgentManager private constructor() {
    
    companion object {
        private const val TAG = "AgentManager"
        
        @Volatile
        private var INSTANCE: AgentManager? = null
        
        fun getInstance(): AgentManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AgentManager().also { INSTANCE = it }
            }
        }
    }
    
    // Agent States
    private val _financeIQState = MutableStateFlow(
        AgentState(
            type = AgentType.FINANCE_IQ,
            status = AgentStatus.IDLE,
            message = ""  // Empty to allow dynamic rotation
        )
    )
    val financeIQState: StateFlow<AgentState> = _financeIQState.asStateFlow()
    
    private val _spendWiseState = MutableStateFlow(
        AgentState(
            type = AgentType.SPEND_WISE,
            status = AgentStatus.IDLE,
            message = ""  // Empty to allow dynamic rotation
        )
    )
    val spendWiseState: StateFlow<AgentState> = _spendWiseState.asStateFlow()
    
    // Chat Messages
    private val _chatMessages = MutableStateFlow<List<AgentChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<AgentChatMessage>> = _chatMessages.asStateFlow()
    
    // Function Calls
    private val _functionCalls = MutableStateFlow<List<FunctionCall>>(emptyList())
    val functionCalls: StateFlow<List<FunctionCall>> = _functionCalls.asStateFlow()
    
    // Processing Timeline
    private val _timeline = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val timeline: StateFlow<List<TimelineEvent>> = _timeline.asStateFlow()
    
    // Current Session
    private val _currentSession = MutableStateFlow<CollaborationSession?>(null)
    val currentSession: StateFlow<CollaborationSession?> = _currentSession.asStateFlow()
    
    // Edge AI Stats
    private val _edgeAIStats = MutableStateFlow(EdgeAIStats())
    val edgeAIStats: StateFlow<EdgeAIStats> = _edgeAIStats.asStateFlow()
    
    /**
     * Start a new agent collaboration session
     */
    fun startSession(smsContent: String): String {
        val sessionId = UUID.randomUUID().toString()
        
        val newSession = CollaborationSession(
            sessionId = sessionId,
            startTime = System.currentTimeMillis(),
            rawInput = smsContent  // Add raw SMS for theater view
        )
        
        _currentSession.value = newSession
        
        // Clear previous data
        _chatMessages.value = emptyList()
        _functionCalls.value = emptyList()
        _timeline.value = emptyList()
        
        // Add initial timeline event
        addTimelineEvent(TimelineEvent(ProcessingStage.SMS_RECEIVED, details = "SMS: ${smsContent.take(50)}..."))
        
        // Initial agent states
        updateFinanceIQState(
            status = AgentStatus.INITIALIZING,
            message = "Analyzing M-PESA message...",
            progress = 0.1f
        )
        
        addChatMessage(AgentType.FINANCE_IQ, "📱 New M-PESA message received! Let me analyze it...")
        
        Log.d(TAG, "Started new agent collaboration session: $sessionId")
        return sessionId
    }
    
    /**
     * Update FinanceIQ agent state
     */
    fun updateFinanceIQState(
        status: AgentStatus,
        message: String = "",
        progress: Float = _financeIQState.value.progress,
        confidence: Float? = null,
        processingTimeMs: Long = 0L
    ) {
        val currentState = _financeIQState.value
        val newState = currentState.copy(
            status = status,
            message = message,
            progress = progress,
            confidence = confidence,
            processingTimeMs = currentState.processingTimeMs + processingTimeMs,
            lastUpdated = System.currentTimeMillis()
        )
        
        _financeIQState.value = newState
        Log.d(TAG, "FinanceIQ: $status - $message")
    }
    
    /**
     * Update SpendWise agent state
     */
    fun updateSpendWiseState(
        status: AgentStatus,
        message: String = "",
        progress: Float = _spendWiseState.value.progress,
        confidence: Float? = null,
        processingTimeMs: Long = 0L
    ) {
        val currentState = _spendWiseState.value
        val newState = currentState.copy(
            status = status,
            message = message,
            progress = progress,
            confidence = confidence,
            processingTimeMs = currentState.processingTimeMs + processingTimeMs,
            lastUpdated = System.currentTimeMillis()
        )
        
        _spendWiseState.value = newState
        Log.d(TAG, "SpendWise: $status - $message")
    }
    
    /**
     * Add agent chat message
     */
    fun addChatMessage(agentType: AgentType, message: String, isThinking: Boolean = false) {
        val chatMessage = AgentChatMessage(agentType, message, isThinking = isThinking)
        _chatMessages.value = _chatMessages.value + chatMessage
        Log.d(TAG, "Chat - ${agentType.name}: $message")
    }
    
    /**
     * Update session with structured data for theater view
     */
    fun updateSessionData(structuredData: Map<String, Any>) {
        val currentSession = _currentSession.value
        if (currentSession != null) {
            val updatedSession = currentSession.copy(
                structuredData = structuredData
            )
            _currentSession.value = updatedSession
            Log.d(TAG, "Updated session with structured data: ${structuredData.keys}")
        }
    }
    
    /**
     * Record function call
     */
    fun recordFunctionCall(
        agentType: AgentType,
        functionName: String,
        inputs: Map<String, Any>,
        outputs: Map<String, Any>? = null,
        success: Boolean = false,
        executionTimeMs: Long = 0L,
        confidence: Float? = null
    ) {
        val functionCall = FunctionCall(
            agentType = agentType,
            functionName = functionName,
            inputs = inputs,
            outputs = outputs ?: emptyMap(),
            success = success,
            executionTimeMs = executionTimeMs,
            confidence = confidence
        )
        
        _functionCalls.value = _functionCalls.value + functionCall
        Log.d(TAG, "Function Call - ${agentType.name}: $functionName ($executionTimeMs ms)")
    }
    
    /**
     * Add timeline event
     */
    fun addTimelineEvent(event: TimelineEvent) {
        _timeline.value = _timeline.value + event
        Log.d(TAG, "Timeline: ${event.stage} - ${event.details}")
    }
    
    /**
     * Complete current session
     */
    fun completeSession(success: Boolean, transactionId: String? = null) {
        val currentSession = _currentSession.value ?: return
        
        val completedSession = currentSession.copy(
            endTime = System.currentTimeMillis(),
            success = success,
            transactionId = transactionId,
            agents = listOf(_financeIQState.value, _spendWiseState.value),
            functionCalls = _functionCalls.value,
            chatMessages = _chatMessages.value,
            timeline = _timeline.value
        )
        
        _currentSession.value = completedSession
        
        // Update stats
        updateEdgeAIStats(completedSession)
        
        // Reset agents to idle (empty message allows dynamic rotation)
        updateFinanceIQState(AgentStatus.IDLE, "", 0f)
        updateSpendWiseState(AgentStatus.IDLE, "", 0f)
        
        addTimelineEvent(TimelineEvent(ProcessingStage.COMPLETE, details = "Session completed successfully"))
        
        Log.d(TAG, "Completed session: ${currentSession.sessionId} - Success: $success")
    }
    
    /**
     * Update Edge AI statistics
     */
    private fun updateEdgeAIStats(completedSession: CollaborationSession) {
        val current = _edgeAIStats.value
        val duration = (completedSession.endTime ?: 0L) - completedSession.startTime
        
        val newStats = current.copy(
            totalSessions = current.totalSessions + 1,
            successfulSessions = if (completedSession.success) current.successfulSessions + 1 else current.successfulSessions,
            totalFunctionCalls = current.totalFunctionCalls + completedSession.functionCalls.size,
            averageSessionDuration = ((current.averageSessionDuration * current.totalSessions) + duration) / (current.totalSessions + 1)
        )
        
        _edgeAIStats.value = newStats
    }
    
    /**
     * Get agent emoji representation
     */
    fun getAgentEmoji(agentType: AgentType): String {
        return when (agentType) {
            AgentType.FINANCE_IQ -> "💎"
            AgentType.SPEND_WISE -> "🧠"
        }
    }
    
    /**
     * Get agent display name
     */
    fun getAgentName(agentType: AgentType): String {
        return when (agentType) {
            AgentType.FINANCE_IQ -> "FinanceIQ"
            AgentType.SPEND_WISE -> "SpendWise"
        }
    }
    
    /**
     * Get agent specialty description
     */
    fun getAgentSpecialty(agentType: AgentType): String {
        return when (agentType) {
            AgentType.FINANCE_IQ -> "Transaction Extraction Specialist"
            AgentType.SPEND_WISE -> "Financial Categorization Expert"
        }
    }
}