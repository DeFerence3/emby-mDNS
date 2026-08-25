package com.deference.mdns

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class MdnsForegroundService : Service() {

    private lateinit var broadcaster: MdnsBroadcaster
    private var foregroundStarted = false

    override fun onCreate() {
        super.onCreate()

        broadcaster = MdnsBroadcaster(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopAdvertising()
                START_NOT_STICKY
            }

            else -> {
                startAdvertising()
                START_STICKY
            }
        }
    }

    private fun startAdvertising() {
        /*
         * startForegroundService() requires the service to promote itself
         * to the foreground promptly.
         */
        if (!foregroundStarted) {
            val foregroundServiceType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    0
                }

            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                createNotification(),
                foregroundServiceType
            )

            foregroundStarted = true
        }

        broadcaster.registerNginxService()
        Log.i(TAG, "mDNS foreground service started")
    }

    private fun stopAdvertising() {
        broadcaster.stopBroadcasting()

        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }

        stopSelf()
        Log.i(TAG, "mDNS foreground service stopped")
    }

    override fun onDestroy() {
        // Safe to call again because stopBroadcasting() is idempotent.
        broadcaster.stopBroadcasting()
        foregroundStarted = false

        Log.i(TAG, "mDNS foreground service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopServiceIntent = Intent(
            this,
            MdnsForegroundService::class.java
        ).apply {
            action = ACTION_STOP
        }

        val stopServicePendingIntent = PendingIntent.getService(
            this,
            1,
            stopServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            /*
             * Replace this with a dedicated monochrome notification icon
             * when available.
             */
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("mDNS broadcasting")
            .setContentText("Advertising home-emby on port 8096")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Stop",
                stopServicePendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager =
            getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "mDNS broadcasting",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows when the mDNS broadcaster is running"
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "MdnsService"
        private const val CHANNEL_ID = "mdns_broadcast_channel"
        private const val NOTIFICATION_ID = 8096

        private const val ACTION_START = "com.deference.mdns.action.START_MDNS"

        private const val ACTION_STOP = "com.deference.mdns.action.STOP_MDNS"

        fun start(context: Context) {
            val intent = Intent(
                context,
                MdnsForegroundService::class.java
            ).apply {
                action = ACTION_START
            }

            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(
                context,
                MdnsForegroundService::class.java
            ).apply {
                action = ACTION_STOP
            }

            /*
             * This sends the stop command to the already-running service.
             * It is called from either the visible activity or notification.
             */
            context.startService(intent)
        }
    }
}