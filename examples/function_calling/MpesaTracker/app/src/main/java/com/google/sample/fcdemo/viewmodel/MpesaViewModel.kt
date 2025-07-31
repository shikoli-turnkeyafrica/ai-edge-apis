package com.google.sample.fcdemo.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
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
} 