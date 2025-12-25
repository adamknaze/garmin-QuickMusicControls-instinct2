package com.adamk.garminqmccompanion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    private val TAG = "QMCCompanionBootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        // We only want to act on the BOOT_COMPLETED action
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed. Attempting to start service.")

            // Create the Intent for your MusicControlService
            val serviceIntent = Intent(context, MusicControlService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26+ requires startForegroundService()
                context.startForegroundService(serviceIntent)
            } else {
                // Older APIs use startService()
                context.startService(serviceIntent)
            }
        }
    }
}