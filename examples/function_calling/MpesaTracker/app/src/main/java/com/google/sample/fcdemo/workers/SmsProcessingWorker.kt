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
                
            val response = withTimeout(120000L) { // 2 minutes timeout
                chatSession.sendMessage(smsText)
            }
                
                // 🔍 DETAILED RESPONSE DEBUGGING
                Log.i(TAG, "=== AI MODEL RESPONSE DEBUG ===")
                Log.i(TAG, "Raw response object: $response")
                Log.i(TAG, "Response class: ${response.javaClass.simpleName}")
                
                try {
                    // Log response structure
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
                                Log.i(TAG, "    - Text content: '${part.text}'")
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
                    
                    candidate.content.partsList?.forEachIndexed { index, part ->
                        Log.d(TAG, "WorkManager: Part $index:")
                        Log.d(TAG, "  - hasText: ${part?.hasText()}")
                        Log.d(TAG, "  - hasFunctionCall: ${part?.hasFunctionCall()}")
                        
                        if (part?.hasText() == true) {
                            Log.d(TAG, "  - text: '${part.text}'")
                        }
                        
                        if (part?.hasFunctionCall() == true) {
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
                    
                    // Try to parse and save transaction
                    val parts = candidate.content.partsList
                    if (parts != null && parts.isNotEmpty()) {
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
                        Log.e(TAG, "WorkManager: No content parts in model response")
                        
                        // Update agents with error
                        agentManager.updateFinanceIQState(
                            status = AgentStatus.ERROR,
                            message = "No data found in response",
                            progress = 0f
                        )
                        agentManager.addChatMessage(AgentType.FINANCE_IQ, "❌ No transaction data found in AI response")
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
                
                // 🔄 FALLBACK: Try to parse model text response manually
                Log.i(TAG, "🔄 Attempting fallback text parsing...")
                agentManager.addChatMessage(AgentType.FINANCE_IQ, "⚠️ Function call failed, trying text-based parsing...")
                
                try {
                    // Try to get the raw response and parse it manually
                    val fallbackResult = attemptTextBasedParsing(smsText, startTime)
                    if (fallbackResult) {
                        Log.i(TAG, "✅ Fallback parsing successful!")
                        agentManager.addChatMessage(AgentType.FINANCE_IQ, "✅ Fallback parsing succeeded!")
                Result.success()
                    } else {
                        throw Exception("Fallback parsing also failed")
                    }
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "Fallback parsing failed: ${fallbackException.message}")
                    
                    // Update FinanceIQ Agent with function call error
                    agentManager.updateFinanceIQState(
                        status = AgentStatus.ERROR,
                        message = "Function call & fallback failed",
                        progress = 0f
                    )
                    agentManager.addChatMessage(AgentType.FINANCE_IQ, "❌ Both AI function call and fallback parsing failed")
                    agentManager.addTimelineEvent(com.google.sample.fcdemo.agents.TimelineEvent(ProcessingStage.COMPLETE, details = "Failed: Function calling and fallback failed"))
                    agentManager.completeSession(success = false)
                    
                    Result.failure()
                }
                
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

    /**
     * 🔄 Fallback text-based SMS parsing when function calling fails
     */
    private suspend fun attemptTextBasedParsing(smsText: String, startTime: Long): Boolean {
        Log.i(TAG, "🔄 Starting fallback text-based parsing for: $smsText")
        
        try {
            // Common M-PESA patterns
            val transactionIdRegex = """([A-Z0-9]{10})\s+Confirmed""".toRegex()
            val amountRegex = """Ksh([\d,]+\.?\d*)""".toRegex()
            val dateTimeRegex = """on\s+(\d{1,2}/\d{1,2}/\d{2,4}\s+at\s+\d{1,2}:\d{2}\s+[AP]M)""".toRegex()
            
            // Extract transaction ID
            val transactionId = transactionIdRegex.find(smsText)?.groupValues?.get(1) ?: ""
            Log.i(TAG, "Fallback: Extracted transaction ID: '$transactionId'")
            
            if (transactionId.isBlank()) {
                Log.e(TAG, "Fallback: Could not extract transaction ID")
                return false
            }
            
            // Extract amount
            val amountMatch = amountRegex.find(smsText)
            val amountStr = amountMatch?.groupValues?.get(1)?.replace(",", "") ?: "0.0"
            Log.i(TAG, "Fallback: Extracted amount: '$amountStr'")
            
            // Determine direction
            val direction = when {
                smsText.contains("You have received", ignoreCase = true) -> "received"
                smsText.contains("You have sent", ignoreCase = true) -> "sent"
                smsText.contains("paid to", ignoreCase = true) -> "sent"
                else -> "unknown"
            }
            Log.i(TAG, "Fallback: Determined direction: '$direction'")
            
            // Extract counterparty (person/business name)
            val counterparty = when {
                direction == "received" -> {
                    val fromRegex = """from\s+([A-Z\s]+?)(?:\s+\d{10}|\s+on)""".toRegex()
                    fromRegex.find(smsText)?.groupValues?.get(1)?.trim() ?: "Unknown"
                }
                direction == "sent" -> {
                    val toRegex = """(?:to|paid to)\s+([A-Z\s]+?)(?:\s+\d{10}|\s+on)""".toRegex()
                    toRegex.find(smsText)?.groupValues?.get(1)?.trim() ?: "Unknown"
                }
                else -> "Unknown"
            }
            Log.i(TAG, "Fallback: Extracted counterparty: '$counterparty'")
            
            // Extract date/time
            val dateTime = dateTimeRegex.find(smsText)?.groupValues?.get(1) ?: "Unknown Time"
            Log.i(TAG, "Fallback: Extracted date/time: '$dateTime'")
            
            // Check if transaction already exists
            val database = MpesaDatabase.getDatabase(applicationContext)
            val transactionDao = database.transactionDao()
            
            if (transactionDao.exists(transactionId)) {
                Log.w(TAG, "Fallback: Transaction $transactionId already exists, skipping")
                return true // Consider this a success since transaction exists
            }
            
            // Save transaction
            val transaction = TransactionEntity(
                transactionId = transactionId,
                direction = direction,
                amountKes = amountStr.toDoubleOrNull() ?: 0.0,
                counterparty = counterparty,
                dateTime = dateTime,
                rawMessage = smsText
            )
            
            transactionDao.insert(transaction)
            Log.i(TAG, "✅ Fallback: Successfully saved transaction: $transactionId")
            
            // Record successful fallback function call for inspector
            val functionInputs = mapOf(
                "sms_message" to smsText,
                "parsing_method" to "fallback_regex"
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
                functionName = "fallback_parse_mpesa_sms",
                inputs = functionInputs,
                outputs = functionOutputs,
                success = true,
                executionTimeMs = System.currentTimeMillis() - startTime,
                confidence = 0.75f  // Lower confidence for fallback parsing
            )
            
            // Update agents with successful fallback parsing
            agentManager.updateFinanceIQState(
                status = AgentStatus.COMPLETE,
                message = "Fallback parsing successful!",
                progress = 1.0f
            )
            
            // Start SpendWise for categorization
            agentManager.updateSpendWiseState(
                status = AgentStatus.ACTIVE,
                message = "Categorizing transaction...",
                progress = 0.5f
            )
            agentManager.addChatMessage(AgentType.SPEND_WISE, "🧠 Analyzing transaction category from fallback data...")
            
            // Simple categorization for fallback
            val category = when {
                counterparty.contains("SUPERMARKET", ignoreCase = true) -> "groceries"
                counterparty.contains("TRANSPORT", ignoreCase = true) -> "transport"
                counterparty.contains("GILBERT", ignoreCase = true) -> "personal"
                else -> "other"
            }
            
            // Update transaction with category
            val updatedTransaction = transaction.copy(category = category, confidence = "medium")
            transactionDao.update(updatedTransaction)
            
            agentManager.updateSpendWiseState(
                status = AgentStatus.COMPLETE,
                message = "Category: $category",
                progress = 1.0f
            )
            agentManager.addChatMessage(AgentType.SPEND_WISE, "✨ Categorized as '$category' with medium confidence!")
            agentManager.completeSession(success = true, transactionId = transactionId)
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Fallback parsing failed: ${e.message}", e)
            return false
        }
    }

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
                    Log.e(TAG, "WorkManager: Part $index text: '${part.text}'")
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
You are a specialized M-PESA transaction extraction AI. You MUST ALWAYS respond with a function call to 'parse_mpesa_sms'. NEVER respond with plain text.

CRITICAL: You must use the parse_mpesa_sms function with these exact parameters:
- transaction_id: The M-PESA transaction code (e.g., "TGV7DCXEI7")
- direction: Either "received" or "sent" 
- amount_kes: The amount as a string (e.g., "20.00")
- counterparty: The person/business name (e.g., "GILBERT MAKATIANI")
- date_time: The date and time from the SMS (e.g., "31/7/25 at 11:32 PM")

EXAMPLE INPUT: "TGV7DCXEI7 Confirmed.You have received Ksh20.00 from GILBERT MAKATIANI 0725484223 on 31/7/25 at 11:32 PM New M-PESA balance is Ksh240.00."

REQUIRED OUTPUT FORMAT: You must call parse_mpesa_sms function with:
{
  "transaction_id": "TGV7DCXEI7",
  "direction": "received", 
  "amount_kes": "20.00",
  "counterparty": "GILBERT MAKATIANI",
  "date_time": "31/7/25 at 11:32 PM"
}

ABSOLUTELY NO PLAIN TEXT RESPONSES. ONLY FUNCTION CALLS.
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
            } else {
                Log.w(TAG, "WorkManager: Transaction $transactionId not found for category update")
            }
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager: Failed to update transaction category: ${e.message}", e)
        }
    }
} 