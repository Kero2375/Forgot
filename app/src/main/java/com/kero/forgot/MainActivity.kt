package com.kero.forgot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.kero.forgot.ui.ForgotApp
import com.kero.forgot.ui.theme.ForgotTheme

class MainActivity : ComponentActivity() {

    private var requestedDestination by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled gracefully */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        requestedDestination = intent?.getStringExtra(EXTRA_DESTINATION)

        val container = (application as ForgotApplication).container

        setContent {
            ForgotTheme {
                ForgotApp(
                    container = container,
                    requestedDestination = requestedDestination,
                    onDestinationConsumed = { requestedDestination = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_DESTINATION)?.let { destination ->
            requestedDestination = destination
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "extra_destination"
        const val DESTINATION_HOME = "home"
        const val DESTINATION_REMINDERS = "reminders"
    }
}
