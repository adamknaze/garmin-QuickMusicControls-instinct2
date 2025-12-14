package com.adamk.garminqmccompanion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.IBinder
import android.util.Log
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.os.Build
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.IQApp

private const val APP_UUID = "2c63f055-206c-4768-83d2-19a8cd81a2fe"
private const val TAG = "MusicControlService"
private const val CHANNEL_ID = "GarminQMCChannel"
private const val NOTIFICATION_ID = 1

class MusicControlService : Service() {
    private lateinit var connectIQ: ConnectIQ
    private var iqDevice: IQDevice? = null

    private var isSdkReady = false
    private var mediaController: MediaController? = null

    // ------------------------------------------------------------------------
    // Notification Helper
    // ------------------------------------------------------------------------

    private fun createNotificationChannel() {
        val name = "Garmin QMC Companion"
        val descriptionText = "Service running to relay music control commands from your Garmin watch."
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun getForegroundNotification(): Notification {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE // Required for modern Android
        )

        // Building the persistent notification
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Garmin QMC Companion Active")
            .setContentText("Listening for commands from your watch.")
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.ic_media_play) // Replace with a proper icon later
            .setTicker("Service running...")
            .build()
    }

    // ------------------------------------------------------------------------
    // Service Lifecycle
    // ------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")

        val notification = getForegroundNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        connectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS)
        connectIQ.initialize(this, true, connectIQListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mediaController = getActiveMediaController(this)

        if (isSdkReady && iqDevice == null) {
            loadDevices()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        if (iqDevice != null && isSdkReady) {
            try {
                connectIQ.unregisterForApplicationEvents(iqDevice, IQApp(APP_UUID))
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering events: ${e.message}")
            }
        }
        stopForeground(STOP_FOREGROUND_DETACH)
        Log.d(TAG, "Service Destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // ------------------------------------------------------------------------
    // Garmin Connect IQ Logic
    // ------------------------------------------------------------------------

    private val connectIQListener = object : ConnectIQ.ConnectIQListener {
        override fun onInitializeError(errStatus: ConnectIQ.IQSdkErrorStatus) {
            Log.e(TAG, "ConnectIQ initialization error: ${errStatus.name}")
            isSdkReady = false
        }

        override fun onSdkReady() {
            Log.d(TAG, "ConnectIQ SDK ready")
            isSdkReady = true
            loadDevices()
        }

        override fun onSdkShutDown() {
            Log.w(TAG, "ConnectIQ SDK shutdown")
            isSdkReady = false
            iqDevice = null
        }
    }

    private fun loadDevices() {
        try {
            // Get all paired Garmin devices
            val devices = connectIQ.knownDevices
            if (devices.isNullOrEmpty()) {
                Log.w(TAG, "No known Garmin devices found.")
                return
            }

            for (device in devices) {
                checkAndRegisterApp(device)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading devices", e)
        }
    }

    private fun checkAndRegisterApp(device: IQDevice) {
        connectIQ.getApplicationInfo(APP_UUID, device, object : ConnectIQ.IQApplicationInfoListener {
            override fun onApplicationInfoReceived(app: IQApp?) {
                if (app != null && app.status == IQApp.IQAppStatus.INSTALLED) {
                    Log.i(TAG, "Found installed app on device: ${device.friendlyName}")

                    // We found the watch with the app! Register for messages.
                    iqDevice = device
                    registerForMessages(device, app)
                } else {
                    Log.d(TAG, "App not installed on device: ${device.friendlyName}")
                }
            }

            override fun onApplicationNotInstalled(applicationId: String?) {
                Log.d(TAG, "App ID $applicationId not found on device ${device.friendlyName}")
            }
        })
    }

    private fun registerForMessages(device: IQDevice, app: IQApp) {
        try {
            connectIQ.registerForAppEvents(device, app, object : ConnectIQ.IQApplicationEventListener {
                override fun onMessageReceived(
                    device: IQDevice?,
                    app: IQApp?,
                    messageData: MutableList<Any>?,
                    status: ConnectIQ.IQMessageStatus?
                ) {
                    if (status == ConnectIQ.IQMessageStatus.SUCCESS && messageData != null && messageData.isNotEmpty()) {
                        val data = messageData[0] // ConnectIQ sends a List, our dict is the first item
                        if (data is Map<*, *>) {
                            val command = data["COMMAND"] as? String
                            if (command != null) {
                                Log.i(TAG, "Received command: $command")
                                handleMusicCommand(command)
                            }
                        }
                    }
                }
            })
            Log.i(TAG, "Successfully registered for events on ${device.friendlyName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register for events", e)
        }
    }

    // ------------------------------------------------------------------------
    // Music Controls Logic
    // ------------------------------------------------------------------------

    private fun getActiveMediaController(context: Context): MediaController? {
        val mediaSessionManager =
            context.getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager

        val controllers = mediaSessionManager.getActiveSessions(
            ComponentName(context, MyNotificationListener::class.java)
        )

        fun isControllable(controller: MediaController): Boolean {
            val actions = controller.playbackState?.actions ?: 0L
            return actions and PlaybackState.ACTION_PLAY != 0L ||
                    actions and PlaybackState.ACTION_PAUSE != 0L
        }

        // Prefer a controller that is currently playing or buffering
        return controllers.firstOrNull { controller ->
            val state = controller.playbackState?.state
            isControllable(controller) &&
                    (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING)
        }
            // Fallback: any controllable session
            ?: controllers.firstOrNull { isControllable(it) }
    }

    private fun isLikelyPodcast(controller: MediaController): Boolean {
        val state = controller.playbackState ?: return false
        val metadata = controller.metadata

        val actions = state.actions
        val hasSeek = actions and PlaybackState.ACTION_SEEK_TO != 0L
//        val hasSkipNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L

        // Duration heuristic (15+ minutes)
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val longForm = durationMs > 15 * 60 * 1000

        // Spotify-specific hint (not required, but helpful)
//        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""

        return hasSeek && longForm
    }

    private fun jumpForward(controller: MediaController, seconds: Long = 15) {
        val state = controller.playbackState ?: return
        val current = state.position
        val newPosition = (current + seconds * 1000).coerceAtLeast(0)

        controller.transportControls.seekTo(newPosition)
    }

    private fun togglePlayPause(controller : MediaController) {

        when (controller.playbackState?.state) {
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING -> {
                controller.transportControls.pause()
            }

            PlaybackState.STATE_PAUSED,
            PlaybackState.STATE_STOPPED,
            PlaybackState.STATE_NONE -> {
                controller.transportControls.play()
            }

            else -> {
                // Defensive fallback
                controller.transportControls.play()
            }
        }
    }

    private fun handleMusicCommand(command: String) {
        var controller = mediaController

        // Invalidate dead controller
        if (controller?.playbackState == null ||
            controller.playbackState?.state == PlaybackState.STATE_NONE
        ) {
            mediaController = null
            controller = null
        }

        if (controller == null) {
            controller = getActiveMediaController(this)
            mediaController = controller
        }

        if (controller == null) {
            Log.w(TAG, "No active MediaController found")
            return
        }

        when (command) {
            "PLAY_PAUSE" -> togglePlayPause(controller)
            "NEXT_TRACK" -> {
                if (isLikelyPodcast(controller)) {
                    jumpForward(controller, seconds = 15)
                } else {
                    controller.transportControls.skipToNext()
                }
            }
            "PREV_TRACK" -> {
                if (isLikelyPodcast(controller)) {
                    jumpForward(controller, seconds = -15)
                } else {
                    controller.transportControls.skipToPrevious()
                }
            }
            "VOLUME_UP" -> controller.adjustVolume(android.media.AudioManager.ADJUST_RAISE, 0)
            "VOLUME_DOWN" -> controller.adjustVolume(android.media.AudioManager.ADJUST_LOWER, 0)
            else -> Log.w(TAG, "Unknown command received: $command")
        }
    }
}