package com.odysee.app.notifications

import android.app.NotificationManager
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.flow.first
import com.google.firebase.messaging.RemoteMessage
import com.odysee.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OdyseeFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var poller: OdyseeNotificationPoller
    @Inject lateinit var notificationPrefs: com.odysee.app.core.datastore.NotificationPreferences

    override fun onNewToken(token: String) {
        // The Cordova app stored this in localStorage without shipping anywhere;
        // FCM project-wide topic delivery does the actual routing. No backend
        // endpoint to register against. Persist locally for future use.
        runCatching {
            val prefs = getSharedPreferences("odysee_fcm", MODE_PRIVATE)
            prefs.edit().putString("fcm_token", token).apply()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Honour the master switch.
        val enabled = kotlinx.coroutines.runBlocking {
            runCatching { notificationPrefs.notificationsEnabled.first() }.getOrDefault(true)
        }
        if (!enabled) return
        // Ensure the channel exists. The simplest path is to delegate to the
        // poller which now has everything new and will post a notification for
        // the latest message just delivered.
        val title = message.notification?.title ?: message.data["title"] ?: "Odysee"
        val body = message.notification?.body ?: message.data["body"] ?: return
        val link = message.data["target"]

        val tap = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(OdyseeNotificationPoller.EXTRA_OPEN_NOTIFICATIONS, true)
            link?.let { putExtra(OdyseeNotificationPoller.EXTRA_NOTIFICATION_TARGET, it) }
        }
        val pending = android.app.PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            tap,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val manager = getSystemService(NotificationManager::class.java)
        if (manager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            manager.getNotificationChannel(OdyseeNotificationPoller.CHANNEL_ID) == null
        ) {
            manager.createNotificationChannel(
                android.app.NotificationChannel(
                    OdyseeNotificationPoller.CHANNEL_ID,
                    "Odysee notifications",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val notification = NotificationCompat.Builder(this, OdyseeNotificationPoller.CHANNEL_ID)
            .setSmallIcon(com.odysee.app.R.drawable.ic_stat_odysee)
            .setColor(OdyseeNotificationPoller.ODYSEE_ACCENT)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }
}
