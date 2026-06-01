package com.odysee.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.odysee.app.core.common.telemetry.CrashReporter
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
    @Inject lateinit var crashReporter: CrashReporter

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        crashReporter.setKey("flavor", BuildConfig.FLAVOR)
        crashReporter.setKey("version_name", BuildConfig.VERSION_NAME)
        crashReporter.setKey("version_code", BuildConfig.VERSION_CODE.toString())
        scope.launch {
            runCatching { authRepository.ensureBootstrap() }
        }
        notificationScheduler.observePreferencesAndApply()
    }
}
