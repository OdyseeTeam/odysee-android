package com.odysee.app.telemetry

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.odysee.app.core.common.telemetry.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashlyticsCrashReporter @Inject constructor() : CrashReporter {
    private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

    override fun setUserId(id: String?) {
        crashlytics.setUserId(id.orEmpty())
    }

    override fun setKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun recordNonFatal(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }
}
