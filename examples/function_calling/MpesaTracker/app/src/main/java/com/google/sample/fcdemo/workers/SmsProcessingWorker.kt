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
import com.google.sample.fcdemo.data.MpesaDatabase
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

    override suspend fun doWork(): Result {
        val smsText = inputData.getString(SMS_TEXT_KEY) ?: return Result.failure()
        
        return try {
            val startTime = System.currentTimeMillis()
            Log.i(TAG, "WorkManager: SMS processing started at $startTime")
            
            // Update progress: Initializing
            setProgress(workDataOf(PROGRESS_KEY to "Initializing model..."))
            
            val generativeModel = createGenerativeModel()
            val chatSession = generativeModel.startChat()
            
            Log.d(TAG, "WorkManager: Chat session created")
            setProgress(workDataOf(PROGRESS_KEY to "Processing SMS..."))
            
            // Process SMS with AI model function calling
            try {
                Log.d(TAG, "WorkManager: Sending SMS to model: $smsText")
                
                val response = withTimeout(120000L) { // 2 minutes timeout
                    chatSession.sendMessage(smsText)
                }
                
                val duration = System.currentTimeMillis() - startTime
                Log.i(TAG, "WorkManager: Model inference completed in ${duration}ms")
                
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
                        parseAndSaveTransaction(parts, smsText)
                        setProgress(workDataOf(PROGRESS_KEY to "Transaction saved"))
                        Log.i(TAG, "WorkManager: SMS processing completed successfully with AI function calling")
                        Result.success()
                    } else {
                        Log.e(TAG, "WorkManager: No content parts in model response")
                        Result.failure()
                    }
                } else {
                    Log.e(TAG, "WorkManager: No candidates in model response")
                    Result.failure()
                }
                
            } catch (functionCallException: com.google.ai.edge.localagents.fc.FunctionCallException) {
                Log.e(TAG, "WorkManager: FunctionCallException occurred")
                Log.e(TAG, "WorkManager: Exception message: ${functionCallException.message}")
                Log.e(TAG, "WorkManager: Exception cause: ${functionCallException.cause}")
                functionCallException.printStackTrace()
                
                // Let's still try to get the raw response to understand what happened
                Log.e(TAG, "WorkManager: Function calling validation failed - this suggests the model output doesn't match expected function call format")
                Result.failure()
                
            } catch (e: Exception) {
                Log.e(TAG, "WorkManager: Unexpected error during AI processing: ${e.message}")
                Log.e(TAG, "WorkManager: Exception type: ${e.javaClass.simpleName}")
                e.printStackTrace()
                Result.failure()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager: SMS processing failed: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun parseAndSaveTransaction(parts: List<Part?>, rawMessage: String) {
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
                    
                    if (transactionId.isBlank()) {
                        Log.w(TAG, "WorkManager: Transaction ID is blank, skipping")
                        return
                    }
                    
                    if (transactionDao.exists(transactionId)) {
                        Log.w(TAG, "WorkManager: Transaction $transactionId already exists, skipping")
                        return
                    }

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
                    .setText("You are an assistant that extracts structured transaction data from M-PESA SMS messages. You MUST respond with a function call to 'parse_mpesa_sms' with the extracted data. Always use function calls, never respond with plain text. Parse the SMS and call the parse_mpesa_sms function with the transaction details.")
            )
            .build()

        return GenerativeModel(
            llmInferenceBackend,
            systemInstruction,
            listOf(MpesaTools.mpesaSmsTool).toMutableList()
        )
    }


} 