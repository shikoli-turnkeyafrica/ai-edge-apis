package com.google.sample.fcdemo.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.sample.fcdemo.navigation.MedicalFormNavHost
import com.google.sample.fcdemo.ui.theme.FunctionCallingDemoTheme
import com.google.sample.fcdemo.viewmodel.FormViewModel
import com.google.sample.fcdemo.viewmodel.MpesaViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionsToRequest = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS
    )

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Check if all permissions were granted
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Log.i("MainActivity", "All required permissions granted.")
                startApp()
            } else {
                Log.e("MainActivity", "One or more permissions were denied. App cannot function.")
                // Close the app or show an error screen, as it cannot work without permissions.
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNotGranted.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsNotGranted.toTypedArray())
        } else {
            // All permissions are already granted
            Log.i("MainActivity", "All permissions already granted, starting app.")
            startApp()
        }
    }

    private fun startApp() {
        val mpesaViewModel = ViewModelProvider(this)[MpesaViewModel::class.java]

        setContent {
            FunctionCallingDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Show the new CashFlow M-PESA tracker screen
                    com.google.sample.fcdemo.ui.screens.MpesaTrackerScreen(viewModel = mpesaViewModel)
                }
            }
        }
    }
}