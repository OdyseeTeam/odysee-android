package com.odysee.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.odysee.app.core.data.auth.AuthRepository
import com.odysee.app.notifications.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class OdyseeApplication : Application(), Configuration.Provider {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationScheduler: NotificationScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            runCatching { authRepository.ensureBootstrap() }
        }
        notificationScheduler.observePreferencesAndApply()
    }
}
