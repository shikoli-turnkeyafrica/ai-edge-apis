package com.google.sample.fcdemo.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.sample.fcdemo.agents.AgentManager
import com.google.sample.fcdemo.agents.AgentState
import com.google.sample.fcdemo.agents.AgentChatMessage
import com.google.sample.fcdemo.agents.FunctionCall
import com.google.sample.fcdemo.agents.TimelineEvent
import com.google.sample.fcdemo.agents.CollaborationSession
import com.google.sample.fcdemo.agents.EdgeAIStats
import com.google.sample.fcdemo.data.MpesaDatabase
import com.google.sample.fcdemo.data.TransactionDao
import com.google.sample.fcdemo.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

@androidx.paging.ExperimentalPagingApi
class MpesaViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao: TransactionDao
    private val workManager = WorkManager.getInstance(application)
    
    // EdgeFinance AI Agents Manager
    private val agentManager = AgentManager.getInstance()

    // Database transactions flow
    val transactions: Flow<List<TransactionEntity>>

    // Paging flow
    val pagedTransactions: kotlinx.coroutines.flow.Flow<PagingData<TransactionEntity>>

    // Grouped transactions by month
    val groupedTransactions: Flow<Map<String, List<TransactionEntity>>>

    // Processing state based on WorkManager
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Work progress
    private val _workProgress = MutableStateFlow("")
    val workProgress: StateFlow<String> = _workProgress.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // 🤖 EDGEFINANCE AI AGENTS - Real-time Agent State Flows
    // ═══════════════════════════════════════════════════════════════════════════════
    
    // Agent States
    val financeIQState: StateFlow<AgentState> = agentManager.financeIQState
    val spendWiseState: StateFlow<AgentState> = agentManager.spendWiseState
    
    // Agent Chat Messages
    val agentChatMessages: StateFlow<List<AgentChatMessage>> = agentManager.chatMessages
    
    // Function Call History
    val functionCalls: StateFlow<List<FunctionCall>> = agentManager.functionCalls
    
    // Processing Timeline
    val processingTimeline: StateFlow<List<TimelineEvent>> = agentManager.timeline
    
    // Current Collaboration Session
    val currentSession: StateFlow<CollaborationSession?> = agentManager.currentSession
    
    // EdgeAI Performance Stats
    val edgeAIStats: StateFlow<EdgeAIStats> = agentManager.edgeAIStats

    init {
        transactionDao = MpesaDatabase.getDatabase(application).transactionDao()
        transactions = transactionDao.getAll()
        pagedTransactions = Pager(PagingConfig(pageSize = 20)) { transactionDao.pagingSource() }
            .flow
            .cachedIn(viewModelScope)
            
        // Group transactions by month
        groupedTransactions = transactionDao.getAllGrouped().map { transactions ->
            transactions.groupBy { transaction ->
                // Convert timestamp to month-year format
                val date = Date(transaction.timestamp)
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
            }
        }
        
        // Debug: Log what's in the database on startup
        viewModelScope.launch(Dispatchers.IO) {
            val existingTransactions = transactionDao.getAllSync()
            Log.i("MpesaViewModel", "Database initialized with ${existingTransactions.size} existing transactions")
            existingTransactions.forEach { transaction ->
                Log.d("MpesaViewModel", "Existing transaction: ${transaction.transactionId} - ${transaction.direction} KSh${transaction.amountKes}")
            }
        }
        
        // Monitor WorkManager for SMS processing jobs
        monitorWorkManager()
    }
    
    private fun monitorWorkManager() {
        viewModelScope.launch {
            workManager.getWorkInfosByTagLiveData("mpesa_sms_processing").observeForever { workInfos ->
                val activeWork = workInfos.find { it.state == WorkInfo.State.RUNNING }
                _isProcessing.value = activeWork != null
                
                activeWork?.progress?.getString("progress")?.let { progress ->
                    _workProgress.value = progress
                    Log.d("MpesaViewModel", "Work progress: $progress")
                }
                
                // Log work status
                workInfos.forEach { workInfo ->
                    Log.d("MpesaViewModel", "Work ${workInfo.id}: ${workInfo.state}")
                }
            }
        }
    }
    
    // Debug method to check database contents
    fun debugDatabaseContents() {
        viewModelScope.launch(Dispatchers.IO) {
            val allTransactions = transactionDao.getAllSync()
            Log.i("MpesaViewModel", "=== DATABASE DEBUG ===")
            Log.i("MpesaViewModel", "Total transactions in database: ${allTransactions.size}")
            allTransactions.forEach { transaction ->
                Log.i("MpesaViewModel", "Transaction: ${transaction.transactionId} | ${transaction.direction} | KSh${transaction.amountKes}")
            }
            Log.i("MpesaViewModel", "=== END DEBUG ===")
        }
    }
    
    // Method to check active WorkManager jobs
    fun debugWorkManagerStatus() {
        viewModelScope.launch {
            val workInfos = workManager.getWorkInfosByTag("mpesa_sms_processing").get()
            Log.i("MpesaViewModel", "=== WORKMANAGER DEBUG ===")
            Log.i("MpesaViewModel", "Total SMS processing jobs: ${workInfos.size}")
            workInfos.forEach { workInfo ->
                Log.i("MpesaViewModel", "Job ${workInfo.id}: ${workInfo.state} - ${workInfo.progress}")
            }
            Log.i("MpesaViewModel", "=== END WORKMANAGER DEBUG ===")
        }
    }
    
    // Debug method to manually test SMS processing
    fun debugProcessTestSms() {
        val testSms = "TGV2D1J8P6 Confirmed.You have received Ksh20.00 from GILBERT  MAKATIANI 0725484223 on 31/7/25 at 9:41 PM  New M-PESA balance is Ksh120.00. Earn interest daily on Ziidi MMF,Dial *334#"
        
        Log.i("MpesaViewModel", "=== MANUAL SMS TEST ===")
        Log.i("MpesaViewModel", "Testing SMS: $testSms")
        
        // Queue SMS processing work
        val inputData = androidx.work.workDataOf("sms_text" to testSms)
        val smsWork = androidx.work.OneTimeWorkRequestBuilder<com.google.sample.fcdemo.workers.SmsProcessingWorker>()
            .setInputData(inputData)
            .addTag("mpesa_sms_processing")
            .addTag("manual_test")
            .build()
        
        workManager.enqueue(smsWork)
        Log.i("MpesaViewModel", "Manual SMS processing work enqueued with ID: ${smsWork.id}")
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // 🤖 EDGEFINANCE AI AGENTS - Control Methods
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Start agent demonstration with sample SMS
     */
    fun startAgentDemo() {
        val demoSms = "TGV9D69SIP Confirmed.You have received Ksh20.00 from GILBERT MAKATIANI 0725484223 on 31/7/25 at 10:14 PM New M-PESA balance is Ksh180.00. Earn interest daily on Ziidi MMF,Dial *334#"
        
        Log.i("MpesaViewModel", "🎭 Starting EdgeFinance AI Agents Demo")
        
        // Start agent collaboration session
        val sessionId = agentManager.startSession(demoSms)
        
        // Queue actual SMS processing work with agent integration
        val inputData = androidx.work.workDataOf(
            "sms_text" to demoSms,
            "agent_session_id" to sessionId
        )
        
        val smsWork = androidx.work.OneTimeWorkRequestBuilder<com.google.sample.fcdemo.workers.SmsProcessingWorker>()
            .setInputData(inputData)
            .addTag("mpesa_sms_processing")
            .addTag("agent_demo")
            .build()
        
        workManager.enqueue(smsWork)
        Log.i("MpesaViewModel", "🚀 Agent demo work enqueued: ${smsWork.id}")
    }
    
    /**
     * Get agent display information
     */
    fun getAgentInfo(agentType: com.google.sample.fcdemo.agents.AgentType): Triple<String, String, String> {
        return Triple(
            agentManager.getAgentEmoji(agentType),
            agentManager.getAgentName(agentType),
            agentManager.getAgentSpecialty(agentType)
        )
    }
    
    /**
     * Clear agent chat history
     */
    fun clearAgentChat() {
        Log.i("MpesaViewModel", "🧹 Clearing agent chat history")
        // This would require adding a clear method to AgentManager
        // For now, starting a new session will clear the chat
    }
    
    /**
     * Get formatted edge AI stats for display
     */
    fun getFormattedEdgeAIStats(): String {
        val stats = edgeAIStats.value
        return """
            🚀 EdgeFinance AI Performance:
            ├ Total Sessions: ${stats.totalSessions}
            ├ Success Rate: ${if (stats.totalSessions > 0) "%.1f%%".format((stats.successfulSessions.toFloat() / stats.totalSessions) * 100) else "N/A"}
            ├ Function Calls: ${stats.totalFunctionCalls}
            ├ Avg Duration: ${if (stats.averageSessionDuration > 0) "${stats.averageSessionDuration}ms" else "N/A"}
            └ Processing: 100% On-Device
        """.trimIndent()
    }
    
    /**
     * Debug method to show current agent states
     */
    fun debugAgentStates() {
        val financeIQ = financeIQState.value
        val spendWise = spendWiseState.value
        
        Log.i("MpesaViewModel", "=== AGENT STATES DEBUG ===")
        Log.i("MpesaViewModel", "💎 FinanceIQ: ${financeIQ.status} - ${financeIQ.message}")
        Log.i("MpesaViewModel", "   Progress: ${financeIQ.progress}, Confidence: ${financeIQ.confidence}")
        Log.i("MpesaViewModel", "🧠 SpendWise: ${spendWise.status} - ${spendWise.message}")
        Log.i("MpesaViewModel", "   Progress: ${spendWise.progress}, Confidence: ${spendWise.confidence}")
        Log.i("MpesaViewModel", "Chat Messages: ${agentChatMessages.value.size}")
        Log.i("MpesaViewModel", "Function Calls: ${functionCalls.value.size}")
        Log.i("MpesaViewModel", "Timeline Events: ${processingTimeline.value.size}")
        Log.i("MpesaViewModel", "=== END AGENT DEBUG ===")
    }
} 