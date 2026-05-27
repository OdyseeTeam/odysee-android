package com.odysee.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.odysee.app.core.datastore.FlavorConfig
import com.odysee.app.core.datastore.NotificationDeliveryMode
import com.odysee.app.core.datastore.NotificationPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: NotificationPreferences,
    private val flavorConfig: FlavorConfig,
) {
    private val scope = CoroutineScope(SupervisorJob())

    fun observePreferencesAndApply() {
        scope.launch {
            combine(prefs.deliveryMode, prefs.pollIntervalMinutes) { mode, interval ->
                val effective = mode ?: flavorConfig.defaultDeliveryMode
                effective to interval
            }
                .distinctUntilChanged()
                .collect { (mode, interval) ->
                    when (mode) {
                        NotificationDeliveryMode.Push -> if (flavorConfig.pushSupported) cancel()
                        else schedule(interval)
                        NotificationDeliveryMode.Poll -> schedule(interval)
                    }
                }
        }
    }

    fun schedule(intervalMinutes: Int) {
        val request = PeriodicWorkRequestBuilder<OdyseeNotificationWorker>(
            intervalMinutes.toLong().coerceAtLeast(15L),
            TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "odysee_notification_poll"
    }
}
