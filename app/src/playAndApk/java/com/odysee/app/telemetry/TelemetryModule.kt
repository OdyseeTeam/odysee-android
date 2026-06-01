package com.odysee.app.telemetry

import com.odysee.app.core.common.telemetry.CrashReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TelemetryModule {
    @Binds
    abstract fun bindCrashReporter(impl: CrashlyticsCrashReporter): CrashReporter
}
