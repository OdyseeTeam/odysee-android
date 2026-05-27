package com.odysee.app.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.odysee.app.R
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.publish.PublishParams
import com.odysee.app.core.data.publish.UploadManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class UploadForegroundService : Service() {

    @Inject lateinit var contentRepository: ContentRepository
    @Inject lateinit var uploadManager: UploadManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var workerJob: Job? = null
    private var lastNotifiedPercent = -1
    private lateinit var notificationManager: NotificationManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            workerJob?.cancel()
            stopForegroundCompat()
            return START_NOT_STICKY
        }
        val jobId = intent?.getStringExtra(EXTRA_JOB_ID) ?: run {
            stopSelf(); return START_NOT_STICKY
        }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: run {
            stopSelf(); return START_NOT_STICKY
        }
        val authToken = intent.getStringExtra(EXTRA_AUTH_TOKEN) ?: run {
            stopSelf(); return START_NOT_STICKY
        }
        val paramsJson = intent.getStringExtra(EXTRA_PARAMS_JSON) ?: run {
            stopSelf(); return START_NOT_STICKY
        }
        val params = try {
            Json.decodeFromString(PublishParams.serializer(), paramsJson)
        } catch (t: Throwable) {
            stopSelf(); return START_NOT_STICKY
        }

        val file = File(filePath)
        val total = max(1L, file.length())
        uploadManager.start(id = jobId, title = title.ifBlank { params.title }, totalBytes = total)
        startForeground(NOTIFICATION_ID, buildNotification(title, uploaded = 0L, total = total))

        workerJob = scope.launch {
            val result = runCatching {
                contentRepository.publishStream(
                    file = file,
                    authToken = authToken,
                    params = params,
                    onProgress = { uploaded, totalBytes ->
                        uploadManager.progress(jobId, uploaded, totalBytes)
                        updateNotification(title.ifBlank { params.title }, uploaded, totalBytes)
                    },
                )
            }
            result
                .onSuccess { tx ->
                    uploadManager.completed(jobId, tx)
                    postFinalNotification(title.ifBlank { params.title }, success = true)
                }
                .onFailure { err ->
                    uploadManager.failed(jobId, err.message ?: "Upload failed")
                    postFinalNotification(title.ifBlank { params.title }, success = false)
                }
            runCatching { file.delete() }
            stopForegroundCompat()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        workerJob?.cancel()
        super.onDestroy()
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun ensureChannel() {
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Uploads",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows upload progress"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, uploaded: Long, total: Long): Notification {
        val percent = if (total > 0) ((uploaded.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100) else 0
        val openIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = PendingIntent.getService(
            this, 1,
            Intent(this, UploadForegroundService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_odysee)
            .setColor(0xFFFF4F84.toInt())
            .setContentTitle("Uploading: ${title.ifBlank { "video" }}")
            .setContentText("$percent% • ${humanBytes(uploaded)} / ${humanBytes(total)}")
            .setProgress(100, percent, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Cancel", cancelIntent)
            .build()
    }

    private fun updateNotification(title: String, uploaded: Long, total: Long) {
        val percent = if (total > 0) ((uploaded.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100) else 0
        if (percent == lastNotifiedPercent) return
        lastNotifiedPercent = percent
        notificationManager.notify(NOTIFICATION_ID, buildNotification(title, uploaded, total))
    }

    private fun postFinalNotification(title: String, success: Boolean) {
        val openIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this, 2, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_odysee)
            .setColor(0xFFFF4F84.toInt())
            .setContentTitle(if (success) "Upload complete" else "Upload failed")
            .setContentText(title.ifBlank { "video" })
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 1, notif)
    }

    private fun humanBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.2f GB".format(gb)
    }

    companion object {
        private const val CHANNEL_ID = "odysee_uploads"
        private const val NOTIFICATION_ID = 4711
        private const val ACTION_CANCEL = "com.odysee.app.upload.ACTION_CANCEL"
        private const val EXTRA_JOB_ID = "job_id"
        private const val EXTRA_FILE_PATH = "file_path"
        private const val EXTRA_AUTH_TOKEN = "auth_token"
        private const val EXTRA_PARAMS_JSON = "params_json"
        private const val EXTRA_TITLE = "title"

        fun start(
            context: Context,
            jobId: String,
            title: String,
            filePath: String,
            authToken: String,
            params: PublishParams,
        ) {
            val intent = Intent(context, UploadForegroundService::class.java).apply {
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_FILE_PATH, filePath)
                putExtra(EXTRA_AUTH_TOKEN, authToken)
                putExtra(EXTRA_PARAMS_JSON, Json.encodeToString(PublishParams.serializer(), params))
            }
            context.startForegroundService(intent)
        }
    }
}
