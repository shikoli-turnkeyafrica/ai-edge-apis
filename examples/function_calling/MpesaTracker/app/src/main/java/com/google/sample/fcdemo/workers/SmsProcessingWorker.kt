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
            
            // Process SMS with timeout
            val response = withTimeout(120000L) { // 2 minutes timeout
                chatSession.sendMessage(smsText)
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "WorkManager: Model inference completed in ${duration}ms")
            
            // Parse and save transaction
            response.getCandidates(0).content.partsList?.let { parts ->
                parseAndSaveTransaction(parts, smsText)
                setProgress(workDataOf(PROGRESS_KEY to "Transaction saved"))
                Log.i(TAG, "WorkManager: SMS processing completed successfully")
                Result.success()
            } ?: run {
                Log.e(TAG, "WorkManager: No response from model")
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
        
        parts.firstOrNull()?.functionCall?.args?.fieldsMap?.let { args ->
            val transactionId = args["transaction_id"]?.stringValue ?: ""
            
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
                direction = args["direction"]?.stringValue ?: "unknown",
                amountKes = args["amount_kes"]?.stringValue?.toDoubleOrNull() ?: 0.0,
                counterparty = args["counterparty"]?.stringValue ?: "Unknown",
                dateTime = args["date_time"]?.stringValue ?: "Unknown Time",
                rawMessage = rawMessage
            )
            
            transactionDao.insert(transaction)
            Log.i(TAG, "WorkManager: Successfully saved transaction: $transactionId")
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
                    .setText("You are an assistant that extracts structured transaction data from M-PESA SMS messages.")
            )
            .build()

        return GenerativeModel(
            llmInferenceBackend,
            systemInstruction,
            listOf(MpesaTools.mpesaSmsTool).toMutableList()
        )
    }
} 