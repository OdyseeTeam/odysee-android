package com.odysee.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.odysee.app.MainActivity
import com.odysee.app.core.data.notifications.NotificationItem
import com.odysee.app.core.data.notifications.NotificationsRepository
import com.odysee.app.core.datastore.NotificationPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OdyseeNotificationPoller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationsRepository: NotificationsRepository,
    private val prefs: NotificationPreferences,
) {

    suspend fun pollOnce() {
        ensureChannel()
        val items = runCatching { notificationsRepository.list() }.getOrNull().orEmpty()
        if (items.isEmpty()) return
        val maxId = items.maxOf { it.id }
        val lastSeen = prefs.lastSeenNotificationId.first()
        if (maxId <= lastSeen) {
            prefs.setLastSeenNotificationId(maxId)
            return
        }
        val fresh = items
            .filter { it.id > lastSeen && !it.isSeen }
            .sortedBy { it.id }
            .take(MAX_NOTIFICATIONS_PER_POLL)
        if (fresh.isEmpty()) {
            prefs.setLastSeenNotificationId(maxId)
            return
        }
        if (!hasPostPermission()) {
            prefs.setLastSeenNotificationId(maxId)
            return
        }
        val manager = NotificationManagerCompat.from(context)
        fresh.forEach { item ->
            manager.notify(item.id.toInt(), buildNotification(item))
        }
        prefs.setLastSeenNotificationId(maxId)
    }

    private fun buildNotification(item: NotificationItem): android.app.Notification {
        val tap = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
            item.target?.let { putExtra(EXTRA_NOTIFICATION_TARGET, it) }
        }
        val pending = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            tap,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val largeIcon = item.channelThumbnail?.let { loadBitmap(it) }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.odysee.app.R.drawable.ic_stat_odysee)
            .setColor(ODYSEE_ACCENT)
            .setContentTitle(item.title.ifBlank { "Odysee" })
            .setContentText(item.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.text))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (largeIcon != null) builder.setLargeIcon(largeIcon)
        return builder.build()
    }

    private fun loadBitmap(url: String): android.graphics.Bitmap? = runCatching {
        URL(url).openStream().use { BitmapFactory.decodeStream(it) }
    }.getOrNull()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Odysee notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "New comments, replies, uploads from channels you follow"
        }
        manager.createNotificationChannel(channel)
    }

    private fun hasPostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "odysee_notifications"
        const val EXTRA_OPEN_NOTIFICATIONS = "com.odysee.app.OPEN_NOTIFICATIONS"
        const val EXTRA_NOTIFICATION_TARGET = "com.odysee.app.NOTIFICATION_TARGET"
        const val ODYSEE_ACCENT: Int = 0xFFE50054.toInt()
        private const val MAX_NOTIFICATIONS_PER_POLL = 5
    }
}
