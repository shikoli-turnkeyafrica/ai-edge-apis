package com.google.sample.fcdemo.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.ai.edge.localagents.core.proto.Content
import com.google.ai.edge.localagents.core.proto.Part
import com.google.ai.edge.localagents.fc.ChatSession
import com.google.ai.edge.localagents.fc.GenerativeModel
import com.google.ai.edge.localagents.fc.HammerFormatter
import com.google.ai.edge.localagents.fc.LlmInferenceBackend
import com.google.ai.edge.localagents.fc.ModelFormatterOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.sample.fcdemo.agents.AgentManager
import com.google.sample.fcdemo.agents.AgentStatus
import com.google.sample.fcdemo.agents.AgentType
import com.google.sample.fcdemo.agents.ProcessingStage
import com.google.sample.fcdemo.data.MpesaDatabase
import com.google.sample.fcdemo.data.TransactionDao
import com.google.sample.fcdemo.data.TransactionEntity
import com.google.sample.fcdemo.envelope.EnvelopeManager
import com.google.sample.fcdemo.functioncalling.MpesaTools
import kotlinx.coroutines.withTimeout

class SmsProcessingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val SMS_TEXT_KEY = "sms_text"
        const val PROGRESS_KEY = "progress"
        const val TAG = "SmsProcessingWorker"
    }
    
    // EdgeFinance AI Agents Manager
    private val agentManager = AgentManager.getInstance()

    override suspend fun doWork(): Result {
        val smsText = inputData.getString(SMS_TEXT_KEY) ?: return Result.failure()
        
        return try {
            val startTime = System.currentTimeMillis()
            Log.i(TAG, "WorkManager: SMS processing started at $startTime")
            
            // 🤖 START AGENT COLLABORATION SESSION
            val agentSessionId = agentManager.startSession(smsText)
            Log.i(TAG, "🤖 EdgeFinance AI Agents session started: $agentSessionId")
            
            // Update FinanceIQ Agent: Starting Up
            agentManager.updateFinanceIQState(
                status = AgentStatus.INITIALIZING,
                message = "Starting AI model...",
                progress = 0.1f
            )
            agentManager.addChatMessage(AgentType.FINANCE_IQ, "📱 New M-PESA SMS received! Let me extract the transaction details...")
            agentManager.addTimelineEvent(com.google.sample.fcdemo.agents.TimelineEvent(ProcessingStage.FINANCE_IQ_ACTIVATED, details = "FinanceIQ agent activated"))
            
            // Update progress: Initializing
            setProgress(workDataOf(PROGRESS_KEY to "Initializing model..."))
            
            val generativeModel = createGenerativeModel()
            val chatSession = generativeModel.startChat()
            
            Log.d(TAG, "WorkManager: Chat session created")
            
            // Update FinanceIQ Agent: Active Processing
            agentManager.updateFinanceIQState(
                status = AgentStatus.ACTIVE,
                message = "Analyzing M-PESA message...",
                progress = 0.3f
            )
            agentManager.addChatMessage(AgentType.FINANCE_IQ, "🔍 Analyzing SMS structure and extracting transaction details...")
            agentManager.addTimelineEvent(com.google.sample.fcdemo.agents.TimelineEvent(ProcessingStage.EXTRACTING_DATA, details = "Parsing SMS with AI model"))
            
            setProgress(workDataOf(PROGRESS_KEY to "Processing SMS..."))
            
            // Process SMS with AI model function calling
            try {
                Log.d(TAG, "WorkManager: Sending SMS to model: $smsText")
                agentManager.addChatMessage(AgentType.FINANCE_IQ, "🔍 Sending SMS to AI model for analysis...")
                
            // 🔄 RETRY MECHANISM: Try up to 2 times if AI doesn't follow function-call-only requirement
            var response = withTimeout(120000L) { // 2 minutes timeout
                chatSession.sendMessage(smsText)
            }
            
            // Check if first attempt violated function-call-only requirement
            var retryAttempted = false
            if (response.candidatesCount > 0) {
                val candidate = response.getCandidates(0)
                val parts = candidate.content.partsList
                var hasTextOnly = false
                
                parts?.forEach { part ->
                    if (part?.hasText() == true && part.hasFunctionCall() == false) {
                        hasTextOnly = true
                    }
                }
                
                if (hasTextOnly && !retryAttempted) {
                    Log.w(TAG, "🔄 AI returned text instead of function call - retrying once...")
                    agentManager.addChatMessage(AgentType.FINANCE_IQ, "🔄 Retrying - AI must use function calls only...")
                    
                    retryAttempted = true
                    response = withTimeout(120000L) {
                        chatSession.sendMessage("FUNCTION CALL REQUIRED: $smsText")
                    }
                }
            }
                
                // 🔍 SAFE RESPONSE DEBUGGING (avoid protobuf toString crashes)
                Log.i(TAG, "=== AI MODEL RESPONSE DEBUG ===")
                Log.i(TAG, "Response class: ${response.javaClass.simpleName}")
                
                try {
                    // Log response structure safely
                    Log.i(TAG, "Response candidates count: ${response.candidatesCount}")
                    
                    for (candidateIndex in 0 until response.candidatesCount) {
                        val candidate = response.getCandidates(candidateIndex)
                        Log.i(TAG, "Candidate $candidateIndex:")
                        Log.i(TAG, "  - Content parts count: ${candidate.content.partsCount}")
                        
                        for (partIndex in 0 until candidate.content.partsCount) {
                            val part = candidate.content.getPartsList()[partIndex]
                            Log.i(TAG, "  - Part $partIndex:")
                            Log.i(TAG, "    - Has text: ${part.hasText()}")
                            Log.i(TAG, "    - Has function call: ${part.hasFunctionCall()}")
                            
                            if (part.hasText()) {
                                // Safely truncate text content to prevent log overflow
                                val textContent = part.text
                                val truncatedText = if (textContent.length > 200) {
                                    textContent.take(200) + "... [truncated]"
                                } else textContent
                                Log.i(TAG, "    - Text content: '$truncatedText'")
                            }
                            
                            if (part.hasFunctionCall()) {
                                val funcCall = part.functionCall
                                Log.i(TAG, "    - Function name: '${funcCall.name}'")
                                Log.i(TAG, "    - Args count: ${funcCall.args.fieldsCount}")
                                funcCall.args.fieldsMap.forEach { (key, value) ->
                                    Log.i(TAG, "      - $key: '${value.stringValue}' (${value.kindCase})")
                                }
                            }
                        }
                    }
                } catch (debugException: Exception) {
                    Log.e(TAG, "Error during response debugging: ${debugException.message}")
                    debugException.printStackTrace()
                }
                Log.i(TAG, "=== END RESPONSE DEBUG ===")
                
                agentManager.addChatMessage(AgentType.FINANCE_IQ, "🤖 Received AI model response! Analyzing structure...")
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "WorkManager: Model inference completed in ${duration}ms")
            
                // Update FinanceIQ Agent: Thinking/Processing Complete
                agentManager.updateFinanceIQState(
                    status = AgentStatus.THINKING,
                    message = "Processing AI response...",
                    progress = 0.7f,
                    processingTimeMs = duration
                )
                agentManager.addChatMessage(AgentType.FINANCE_IQ, "✅ Successfully extracted transaction data! Processing function call...")
                
                // Update progress: AI inference complete
                setProgress(workDataOf(PROGRESS_KEY to "🤖 AI inference complete..."))
                
                // Detailed logging of the model response
                Log.d(TAG, "WorkManager: Model response candidates: ${response.candidatesCount}")
                
                if (response.candidatesCount > 0) {
                    val candidate = response.getCandidates(0)
                    Log.d(TAG, "WorkManager: Candidate content parts: ${candidate.content.partsCount}")
                    
                    // 🔍 ENHANCED VALIDATION: Check for actual function calls
                    var hasFunctionCalls = false
                    var hasTextContent = false
                    
                    candidate.content.partsList?.forEachIndexed { index, part ->
                        Log.d(TAG, "WorkManager: Part $index:")
                        Log.d(TAG, "  - hasText: ${part?.hasText()}")
                        Log.d(TAG, "  - hasFunctionCall: ${part?.hasFunctionCall()}")
                        
                        if (part?.hasText() == true) {
                            hasTextContent = true
                            // 🔍 SAFE TEXT LOGGING: Truncate large text to prevent crashes
                            val textContent = part.text
                            val truncatedText = if (textContent.length > 100) {
                                textContent.take(100) + "... [truncated]"
                            } else textContent
                            Log.d(TAG, "  - text: '$truncatedText'")
                            // 🚨 CRITICAL: If AI returns text instead of function call, this is a failure
                            Log.w(TAG, "  ⚠️ AI returned TEXT instead of FUNCTION CALL - this violates system instructions!")
                        }
                        
                        if (part?.hasFunctionCall() == true) {
                            hasFunctionCalls = true
                            val funcCall = part.functionCall
                            Log.d(TAG, "  - function name: '${funcCall.name}'")
                            Log.d(TAG, "  - function args count: ${funcCall.args.fieldsCount}")
                            Log.d(TAG, "  - function args: ${funcCall.args.fieldsMap}")
                            
                            // Log each argument in detail
                            funcCall.args.fieldsMap.forEach { (key, value) ->
                                Log.d(TAG, "    - $key: '${value.stringValue}' (kind: ${value.kindCase})")
                            }
                        }
                    }
                    
                    // 🔍 ENHANCED DEBUGGING: Log AI behavior analysis
                    Log.i(TAG, "=== AI BEHAVIOR ANALYSIS ===")
                    Log.i(TAG, "Has function calls: $hasFunctionCalls")
                    Log.i(TAG, "Has text content: $hasTextContent")
                    Log.i(TAG, "Parts count: ${candidate.content.partsCount}")
                    
                    if (hasTextContent && !hasFunctionCalls) {
                        Log.e(TAG, "🚨 CRITICAL: AI returned plain text - system instructions violated!")
                        agentManager.addChatMessage(AgentType.FINANCE_IQ, "⚠️ AI model violated function-call-only requirement")
                    }
                    
                    // Try to parse and save transaction
                    val parts = candidate.content.partsList
                    if (parts != null && parts.isNotEmpty() && hasFunctionCalls) {
                        // Update FinanceIQ Agent: Data Extracted Successfully
                        agentManager.updateFinanceIQState(
                            status = AgentStatus.COMPLETE,
                            message = "Transaction data extracted!",
                            progress = 1.0f
                        )
                        agentManager.addChatMessage(AgentType.FINANCE_IQ, "🎉 Perfect! Found transaction details. Passing to SpendWise for categorization...")
                        agentManager.addTimelineEvent(com.google.sample.fcdemo.agents.TimelineEvent(ProcessingStage.DATA_EXTRACTED, details = "FinanceIQ extracted transaction details"))
                        
                        // Update progress: Extracting data
                        setProgress(workDataOf(PROGRESS_KEY to "📊 Extracting transaction data..."))
                        
                        // Start SpendWise Agent for Categorization
                        agentManager.updateSpendWiseState(
                            status = AgentStatus.INITIALIZING,
                            message = "Analyzing transaction category...",
                            progress = 0.2f
                        )
                        agentManager.addChatMessage(AgentType.SPEND_WISE, "🧠 Received transaction data from FinanceIQ! Analyzing spending category...")
                        agentManager.addTimelineEvent(com.google.sample.fcdemo.agents.TimelineEvent(ProcessingStage.SPEND_WISE_ACTIVATED, details = "SpendWise agent activated for categorization"))
                        
                        parseAndSaveTransaction(parts, smsText, startTime)
                        
                        // Complete All Agents Successfully
                        agentManager.updateSpendWiseState(
                            status = AgentStatus.COMPLETE,
                            message = "Categorization complete!",
                            progress = 1.0f
                        )
                        agentManager.addChatMessage(AgentType.SPEND_WISE, "✨ Transaction categorized successfully! All done!")
                        agentManager.addTimelineEvent(com.google.sample.fcdemo.agents.TimelineEvent(ProcessingStage.COMPLETE, details = "All agents completed successfully"))
                        
                        // Complete the agent session
                        agentManager.completeSession(success = true, transactionId = "extracted")
                        
                        // Update progress: Complete
                        setProgress(workDataOf(PROGRESS_KEY to "✅ Transaction saved!"))
                        Log.i(TAG, "WorkManager: SMS processing completed successfully with AI function calling")
                Result.success()
                    } else {
                        // 🔍 ENHANCED ERROR REPORTING: More specific error messages
                        val errorReason = when {
                            parts == null -> "Content parts list is null"
                            parts.isEmpty() -> "Content parts list is empty"
                            !hasFunctionCalls && hasTextContent -> "AI returned text instead of function call"
                            !hasFunctionCalls && !hasTextContent -> "No function calls or content found"
                            else -> "Unknown parsing error"
                        }
                        
                        Log.e(TAG, "WorkManager: Failed to extract transaction data - $errorReason")
                        
                        // Update agents with detailed error
                        agentManager.updateFinanceIQState(
                            status = AgentStatus.ERROR,
                            message = "AI model error: $errorReason",
                            progress = 0f
                        )
                        agentManager.addChatMessage(AgentType.FINANCE_IQ, "❌ $errorReason - Check system instructions")
                        agentManager.completeSession(success = false)
                        
                        Result.failure()
                    }
                } else {
                    Log.e(TAG, "WorkManager: No candidates in model response")
                    
                    // Update FinanceIQ Agent with no candidates error
                    agentManager.updateFinanceIQState(
                        status = AgentStatus.ERROR,
                        message = "No AI response candidates",
                        progress = 0f
                    )
                    agentManager.addChatMessage(AgentType.FINANCE_IQ, "❌ AI model didn't generate any response candidates")
                    agentManager.completeSession(success = false)
                    
                    Result.failure()
                }
                
                        } catch (functionCallException: com.google.ai.edge.localagents.fc.FunctionCallException) {
                Log.e(TAG, "WorkManager: FunctionCallException occurred")
                Log.e(TAG, "WorkManager: Exception message: ${functionCallException.message}")
                Log.e(TAG, "WorkManager: Exception cause: ${functionCallException.cause}")
                functionCallException.printStackTrace()
                
                // 🚫 NO FALLBACK: AI function calling must work
                Log.e(TAG, "❌ AI function calling failed - this needs to be fixed, not bypassed")
                
                // Update FinanceIQ Agent with function call error
                agentManager.updateFinanceIQState(
                    status = AgentStatus.ERROR,
                    message = "AI function call failed",
                    progress = 0f
                )
                agentManager.addChatMessage(AgentType.FINANCE_IQ, "❌ AI function calling failed - system needs attention")
                agentManager.addTimelineEvent(com.google.sample.fcdemo.agents.TimelineEvent(ProcessingStage.COMPLETE, details = "Failed: AI function calling error"))
                agentManager.completeSession(success = false)
                
                Result.failure()
                
            } catch (e: Exception) {
                Log.e(TAG, "WorkManager: Unexpected error during AI processing: ${e.message}")
                Log.e(TAG, "WorkManager: Exception type: ${e.javaClass.simpleName}")
                e.printStackTrace()
                
                // Update FinanceIQ Agent with general error
                agentManager.updateFinanceIQState(
                    status = AgentStatus.ERROR,
                    message = "Processing error: ${e.message}",
                    progress = 0f
                )
                agentManager.addChatMessage(AgentType.FINANCE_IQ, "❌ Unexpected error during SMS processing: ${e.javaClass.simpleName}")
                agentManager.completeSession(success = false)
                
                Result.failure()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager: SMS processing failed: ${e.message}", e)
            
            // Update agents with outer exception error
            agentManager.updateFinanceIQState(
                status = AgentStatus.ERROR,
                message = "SMS processing failed: ${e.message}",
                progress = 0f
            )
            agentManager.addChatMessage(AgentType.FINANCE_IQ, "❌ Critical error during SMS processing setup")
            agentManager.completeSession(success = false)
            
            Result.failure()
        }
    }

    // 🚫 FALLBACK REMOVED: AI function calling must work reliably
    // No more fallback methods - if AI function calling fails, we fix the AI system instead

    private suspend fun parseAndSaveTransaction(parts: List<Part?>, rawMessage: String, startTime: Long) {
        val database = MpesaDatabase.getDatabase(applicationContext)
        val transactionDao = database.transactionDao()
        
        Log.d(TAG, "WorkManager: Parsing transaction from ${parts.size} parts")
        
        // Look for function calls in all parts
        var functionCallFound = false
        parts.forEachIndexed { index, part ->
            Log.d(TAG, "WorkManager: Examining part $index")
            if (part?.hasFunctionCall() == true) {
                functionCallFound = true
                val funcCall = part.functionCall
                Log.d(TAG, "WorkManager: Found function call: ${funcCall.name}")
                
                if (funcCall.name == "parse_mpesa_sms") {
                    val args = funcCall.args.fieldsMap
                    Log.d(TAG, "WorkManager: Function args: $args")
                    
            val transactionId = args["transaction_id"]?.stringValue ?: ""
                    val direction = args["direction"]?.stringValue ?: "unknown"
                    val amountStr = args["amount_kes"]?.stringValue ?: "0.0"
                    val counterparty = args["counterparty"]?.stringValue ?: "Unknown"
                    val dateTime = args["date_time"]?.stringValue ?: "Unknown Time"
                    
                    Log.d(TAG, "WorkManager: Extracted data:")
                    Log.d(TAG, "  - transaction_id: '$transactionId'")
                    Log.d(TAG, "  - direction: '$direction'")
                    Log.d(TAG, "  - amount_kes: '$amountStr'")
                    Log.d(TAG, "  - counterparty: '$counterparty'")
                    Log.d(TAG, "  - date_time: '$dateTime'")
                    
                    // 🔍 Record Function Call for Inspector
                    val functionInputs = mapOf(
                        "sms_message" to rawMessage,
                        "message_length" to rawMessage.length.toString()
                    )
                    val functionOutputs = mapOf(
                        "transaction_id" to transactionId,
                        "direction" to direction,
                        "amount_kes" to amountStr,
                        "counterparty" to counterparty,
                        "date_time" to dateTime
                    )
                    
                    agentManager.recordFunctionCall(
                        agentType = AgentType.FINANCE_IQ,
                        functionName = "parse_mpesa_sms",
                        inputs = functionInputs,
                        outputs = functionOutputs,
                        success = true,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        confidence = 0.95f  // High confidence for successful extraction
                    )
                    
                    // 🎯 PHASE 3: FinanceIQ Envelope Intelligence
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    
                    // Get Suspense Account balance for context
                    val envelopeManager = com.google.sample.fcdemo.envelope.EnvelopeManager.getInstance(applicationContext)
                    val suspenseBalance = try {
                        envelopeManager.getSuspenseAccountBalance()
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not get suspense balance: ${e.message}")
                        0.0
                    }
                    
                    // FinanceIQ: Smart transaction analysis with envelope context
                    agentManager.financeIQAnalyzeTransaction(
                        transactionId = transactionId,
                        amount = amount,
                        direction = direction,
                        counterparty = counterparty,
                        suspenseBalance = suspenseBalance
                    )
                    
                    // FinanceIQ: Suggest envelope allocation
                    agentManager.financeIQSuggestAllocation(
                        direction = direction,
                        amount = amount,
                        counterparty = counterparty
                    )
            
            if (transactionId.isBlank()) {
                Log.w(TAG, "WorkManager: Transaction ID is blank, skipping")
                return
            }
            
            if (transactionDao.exists(transactionId)) {
                Log.w(TAG, "WorkManager: Transaction $transactionId already exists, skipping")
                return
            }

                    // Update progress: Saving to database
                    setProgress(workDataOf(PROGRESS_KEY to "💾 Saving to database..."))

            val transaction = TransactionEntity(
                transactionId = transactionId,
                        direction = direction,
                        amountKes = amountStr.toDoubleOrNull() ?: 0.0,
                        counterparty = counterparty,
                        dateTime = dateTime,
                rawMessage = rawMessage
            )
            
            // Update theater with structured data
            val structuredData = mapOf(
                "transaction_id" to transactionId,
                "direction" to direction,
                "amount_kes" to amountStr,
                "counterparty" to counterparty,
                "date_time" to dateTime
            )
            agentManager.updateSessionData(structuredData)
            
            transactionDao.insert(transaction)
            Log.i(TAG, "WorkManager: Successfully saved transaction: $transactionId")
                    
                    // Chain function call: Auto-categorize the transaction
                    categorizeParsedTransaction(transactionId, counterparty, direction, amountStr.toDoubleOrNull() ?: 0.0, transactionDao)
                    return
                } else {
                    Log.w(TAG, "WorkManager: Unexpected function call name: ${funcCall.name}")
                }
            }
        }
        
        if (!functionCallFound) {
            Log.e(TAG, "WorkManager: No function calls found in response parts!")
            parts.forEachIndexed { index, part ->
                if (part?.hasText() == true) {
                    // 🔍 SAFE ERROR LOGGING: Truncate large text to prevent crashes
                    val textContent = part.text
                    val truncatedText = if (textContent.length > 150) {
                        textContent.take(150) + "... [truncated]"
                    } else textContent
                    Log.e(TAG, "WorkManager: Part $index text: '$truncatedText'")
                }
            }
        }
    }

    private fun createGenerativeModel(): GenerativeModel {
        val formatter = HammerFormatter(
            ModelFormatterOptions.builder().setAddPromptTemplate(true).build()
        )

        val llmInferenceOptions = LlmInferenceOptions.builder()
            .setModelPath("/data/local/tmp/hammer2.1_1.5b_q8_ekv4096.task")
            .setMaxTokens(2048)
            .apply { setPreferredBackend(LlmInference.Backend.GPU) }
            .build()

        val llmInference = LlmInference.createFromOptions(applicationContext, llmInferenceOptions)
        val llmInferenceBackend = LlmInferenceBackend(llmInference, formatter)

        val systemInstruction = Content.newBuilder()
            .setRole("system")
            .addParts(
                Part.newBuilder()
                    .setText("""
🔴 CRITICAL FUNCTION CALLING REQUIREMENT 🔴
You are a specialized M-PESA transaction extraction AI. 

MANDATORY BEHAVIOR:
- You MUST ALWAYS respond with ONLY a function call to 'parse_mpesa_sms'
- You MUST NEVER respond with plain text, explanations, or any other content
- Every response MUST be a valid function call with ALL required parameters
- If you cannot extract a parameter, use reasonable defaults but STILL call the function

REQUIRED PARAMETERS (ALL MANDATORY):
- transaction_id: M-PESA code (e.g., "TGV7DCXEI7") 
- direction: EXACTLY "received" OR "sent" (never "unknown")
- amount_kes: Amount as string without "Ksh" (e.g., "20.00")
- counterparty: Person/business name (e.g., "GILBERT MAKATIANI")
- date_time: Date/time from SMS (e.g., "31/7/25 at 11:32 PM")

PARSING RULES:
- For "received": look for "You have received", "from [NAME]"
- For "sent": look for "You have sent", "paid to [NAME]", "sent to [NAME]"
- Remove commas from amounts: "1,500.00" → "1500.00"
- Keep counterparty names uppercase: "GILBERT MAKATIANI"
- Extract exact date/time format from SMS

EXAMPLE PROCESSING:
INPUT: "TGV7DCXEI7 Confirmed.You have received Ksh20.00 from GILBERT MAKATIANI 0725484223 on 31/7/25 at 11:32 PM New M-PESA balance is Ksh240.00."

REQUIRED FUNCTION CALL:
parse_mpesa_sms({
  "transaction_id": "TGV7DCXEI7",
  "direction": "received", 
  "amount_kes": "20.00",
  "counterparty": "GILBERT MAKATIANI",
  "date_time": "31/7/25 at 11:32 PM"
})

🚫 FORBIDDEN: Plain text, explanations, "I cannot", error messages, anything except function calls
✅ REQUIRED: Only valid parse_mpesa_sms function calls with all 5 parameters

FAILURE TO FOLLOW = SYSTEM ERROR. ALWAYS USE FUNCTION CALLS.
                    """.trimIndent())
            )
            .build()

        return GenerativeModel(
            llmInferenceBackend,
            systemInstruction,
            listOf(MpesaTools.mpesaSmsTool).toMutableList()
        )
    }

    private fun createCategorizationModel(): GenerativeModel {
        val formatter = HammerFormatter(
            ModelFormatterOptions.builder().setAddPromptTemplate(true).build()
        )

        val llmInferenceOptions = LlmInferenceOptions.builder()
            .setModelPath("/data/local/tmp/hammer2.1_1.5b_q8_ekv4096.task")
            .setMaxTokens(2048)
            .apply { setPreferredBackend(LlmInference.Backend.GPU) }
            .build()

        val llmInference = LlmInference.createFromOptions(applicationContext, llmInferenceOptions)
        val llmInferenceBackend = LlmInferenceBackend(llmInference, formatter)

        val categorizationSystemInstruction = Content.newBuilder()
            .setRole("system")
            .addParts(
                Part.newBuilder()
                    .setText("You are an assistant that categorizes M-PESA transactions. You MUST respond with a function call to 'categorize_transaction' with the category and confidence level. Always use function calls, never respond with plain text.")
            )
            .build()

        return GenerativeModel(
            llmInferenceBackend,
            categorizationSystemInstruction,
            listOf(MpesaTools.categorizationTool).toMutableList()
        )
    }

    private suspend fun categorizeParsedTransaction(
        transactionId: String,
        counterparty: String,
        direction: String,
        amount: Double,
        transactionDao: TransactionDao
    ) {
        try {
            // Update progress: Categorizing
            setProgress(workDataOf(PROGRESS_KEY to "🏷️ Auto-categorizing..."))
            
            Log.d(TAG, "WorkManager: Starting auto-categorization for transaction: $transactionId")
            
            // Create a separate AI model for categorization
            val categorizationModel = createCategorizationModel()
            val categorizationChat = categorizationModel.startChat()
            
            // Create categorization prompt
            val categorizationPrompt = """
                Categorize this M-PESA transaction:
                - Transaction ID: $transactionId
                - Counterparty: $counterparty
                - Direction: $direction
                - Amount: KSh$amount
                
                Based on the counterparty name, determine the most appropriate category and confidence level.
            """.trimIndent()
            
            Log.d(TAG, "WorkManager: Sending categorization request: $categorizationPrompt")
            
            val categorizationResponse = withTimeout(60000L) { // 1 minute timeout
                categorizationChat.sendMessage(categorizationPrompt)
            }
            
            Log.d(TAG, "WorkManager: Categorization inference completed")
            
            // Process categorization response
            if (categorizationResponse.candidatesCount > 0) {
                val candidate = categorizationResponse.getCandidates(0)
                candidate.content.partsList?.forEach { part ->
                    if (part?.hasFunctionCall() == true && part.functionCall.name == "categorize_transaction") {
                        val args = part.functionCall.args.fieldsMap
                        val category = args["category"]?.stringValue ?: "other"
                        val confidence = args["confidence"]?.stringValue ?: "low"
                        
                        Log.d(TAG, "WorkManager: Auto-categorization result - Category: $category, Confidence: $confidence")
                        updateTransactionCategory(transactionId, category, confidence, transactionDao)
                        return
                    }
                }
            }
            
            Log.w(TAG, "WorkManager: No categorization function call found, using default category")
            updateTransactionCategory(transactionId, "other", "low", transactionDao)
            
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager: Categorization failed: ${e.message}", e)
            // Fallback to default category
            updateTransactionCategory(transactionId, "other", "low", transactionDao)
        }
    }

    private suspend fun updateTransactionCategory(
        transactionId: String,
        category: String,
        confidence: String,
        transactionDao: TransactionDao
    ) {
        try {
            // Get the existing transaction
            val existingTransaction = transactionDao.getById(transactionId)
            if (existingTransaction != null) {
                // Create updated transaction with category
                val updatedTransaction = existingTransaction.copy(
                    category = category,
                    confidence = confidence
                )
                
                transactionDao.update(updatedTransaction)
                Log.i(TAG, "WorkManager: Updated transaction $transactionId with category: $category ($confidence confidence)")
                
                // 🎯 ENVELOPE ALLOCATION: Allocate transaction to appropriate envelope
                try {
                    setProgress(workDataOf(PROGRESS_KEY to "💰 Allocating to envelope..."))
                    
                    val envelopeManager = EnvelopeManager.getInstance(applicationContext)
                    val allocationResult = envelopeManager.allocateTransaction(
                        transactionId = transactionId,
                        category = category,
                        amount = existingTransaction.amountKes,
                        direction = existingTransaction.direction,
                        counterparty = existingTransaction.counterparty
                    )
                    
                    if (allocationResult.success) {
                        Log.i(TAG, "💰 Envelope allocation successful: ${allocationResult.message}")
                        
                        // 🔧 SUSPENSE BALANCE FIX: Trigger UI refresh after allocation
                        // Post message to main thread to refresh suspense balance
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            try {
                                // Trigger ViewModel refresh through WorkManager result data
                                setProgressAsync(workDataOf(
                                    PROGRESS_KEY to "✅ Allocation complete",
                                    "suspense_refresh" to "true"
                                ))
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to trigger suspense balance refresh: ${e.message}")
                            }
                        }
                        
                        // 🎯 PHASE 3: SpendWise Envelope Intelligence - Smart Budget Coaching
                        val warningLevelStr = when (allocationResult.warningLevel) {
                            com.google.sample.fcdemo.envelope.WarningLevel.CRITICAL -> "critical"
                            com.google.sample.fcdemo.envelope.WarningLevel.WARNING -> "warning"
                            com.google.sample.fcdemo.envelope.WarningLevel.CAUTION -> "caution"
                            com.google.sample.fcdemo.envelope.WarningLevel.ERROR -> "error"
                            else -> "none"
                        }
                        
                        // SpendWise: Smart budget coaching
                        agentManager.spendWiseBudgetCoaching(
                            envelopeName = allocationResult.envelopeName,
                            budgetUsage = allocationResult.budgetUsagePercentage ?: 0.0,
                            warningLevel = warningLevelStr,
                            amount = existingTransaction.amountKes
                        )
                        
                        // SpendWise: Smart envelope recommendation
                        agentManager.spendWiseRecommendEnvelope(
                            category = category,
                            amount = existingTransaction.amountKes,
                            availableEnvelopes = listOf("groceries", "transport", "bills", "entertainment", "health", "savings")
                        )
                        
                        // Legacy chat message (keeping for compatibility)
                        agentManager.addChatMessage(
                            AgentType.SPEND_WISE, 
                            "💰 Smart allocation complete: ${allocationResult.envelopeName} (${((allocationResult.budgetUsagePercentage ?: 0.0) * 100).toInt()}% budget used)"
                        )
                        
                    } else {
                        Log.w(TAG, "⚠️ Envelope allocation failed: ${allocationResult.message}")
                        agentManager.addChatMessage(
                            AgentType.SPEND_WISE,
                            "⚠️ Could not allocate to envelope: ${allocationResult.message}"
                        )
                    }
                    
                } catch (envelopeError: Exception) {
                    Log.e(TAG, "❌ Envelope allocation error: ${envelopeError.message}", envelopeError)
                    agentManager.addChatMessage(
                        AgentType.SPEND_WISE,
                        "❌ Envelope system error: ${envelopeError.message}"
                    )
                }
                
            } else {
                Log.w(TAG, "WorkManager: Transaction $transactionId not found for category update")
            }
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager: Failed to update transaction category: ${e.message}", e)
        }
    }
} 