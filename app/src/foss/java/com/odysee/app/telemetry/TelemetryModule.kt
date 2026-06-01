package com.odysee.app.telemetry

import com.odysee.app.core.common.telemetry.CrashReporter
import com.odysee.app.core.common.telemetry.NoopCrashReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TelemetryModule {
    @Provides
    @Singleton
    fun provideCrashReporter(): CrashReporter = NoopCrashReporter
}
