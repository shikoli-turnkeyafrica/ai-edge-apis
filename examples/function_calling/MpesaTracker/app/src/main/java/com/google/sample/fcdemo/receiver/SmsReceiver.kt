package com.google.sample.fcdemo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.sample.fcdemo.workers.SmsProcessingWorker

private const val TAG = "SmsReceiver"

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val fullMessage = StringBuilder()
            var sender: String? = null

            for (sms in messages) {
                fullMessage.append(sms.messageBody)
                if (sender == null) {
                    sender = sms.originatingAddress
                }
            }

            val finalMessage = fullMessage.toString()
            // We now have the complete message body in one string.
            Log.d(TAG, "Full SMS Received from $sender: $finalMessage")

            // Only process messages from MPESA
            if (sender == "MPESA") {
                Log.d(TAG, "Processing M-PESA SMS message with WorkManager")
                
                // Create work request with SMS data
                val inputData = Data.Builder()
                    .putString(SmsProcessingWorker.SMS_TEXT_KEY, finalMessage)
                    .build()
                
                val workRequest = OneTimeWorkRequestBuilder<SmsProcessingWorker>()
                    .setInputData(inputData)
                    .addTag("mpesa_sms_processing")
                    .build()
                
                // Enqueue work - this will survive app restarts
                WorkManager.getInstance(context).enqueue(workRequest)
                
                Log.i(TAG, "SMS processing work enqueued with ID: ${workRequest.id}")
            } else {
                Log.d(TAG, "Ignoring SMS from non-MPESA sender: $sender")
            }
        }
    }
} 