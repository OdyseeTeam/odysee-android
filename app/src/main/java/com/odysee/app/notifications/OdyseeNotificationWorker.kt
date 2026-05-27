package com.odysee.app.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OdyseeNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val poller: OdyseeNotificationPoller,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching { poller.pollOnce() }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }
}
