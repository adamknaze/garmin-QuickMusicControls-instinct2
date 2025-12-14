package com.adamk.garminqmccompanion

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.widget.Toast
import android.provider.Settings
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if all necessary permissions were granted
        val granted = permissions.entries.all { it.value }
        if (granted) {
            checkAndRequestNotificationAccess()
        } else {
            Toast.makeText(this, "Bluetooth permissions are required for device communication.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestBluetoothPermissions()
    }

    override fun onResume() {
        super.onResume()
        // Check permission status whenever the app returns to the foreground
        if (isNotificationAccessEnabled()) {
            startCompanionService()
            finish()
        } else {
            Toast.makeText(this, "Please grant Notification Access to control music.", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestBluetoothPermissions() {
        // Bluetooth permissions are only runtime checks on API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ (API 31)
            val permissionsToRequest = mutableListOf<String>()

            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (permissionsToRequest.isNotEmpty()) {
                // Request permissions using the launcher defined above
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else {
                // Permissions are already granted, move to the next check
                checkAndRequestNotificationAccess()
            }
        } else {
            // For older devices, the permissions are granted at install time, proceed directly.
            checkAndRequestNotificationAccess()
        }
    }

    // Check if the user has granted the required Notification Access
    private fun isNotificationAccessEnabled(): Boolean {
        // This is the standard way to check if our service component is enabled
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = ComponentName(this, MyNotificationListener::class.java).flattenToString()

        return enabledListeners?.contains(componentName) == true
    }

    private fun checkAndRequestNotificationAccess() {
        if (!isNotificationAccessEnabled()) {
            // Open the Notification Access Settings screen
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        } else {
            // All permissions are granted, start the bridge service
            startCompanionService()
            finish()
        }
    }

    // Start the background service using startForegroundService
    private fun startCompanionService() {
        val serviceIntent = Intent(this, MusicControlService::class.java)
        // Start the service as a Foreground Service (required for API 26+)
        startForegroundService(serviceIntent)
        Toast.makeText(this, "Garmin QMC Companion Service started.", Toast.LENGTH_SHORT).show()
    }
}
