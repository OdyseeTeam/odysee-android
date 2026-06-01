package com.odysee.app.core.common.telemetry

interface CrashReporter {
    fun setUserId(id: String?)
    fun setKey(key: String, value: String)
    fun log(message: String)
    fun recordNonFatal(throwable: Throwable)
}

object NoopCrashReporter : CrashReporter {
    override fun setUserId(id: String?) = Unit
    override fun setKey(key: String, value: String) = Unit
    override fun log(message: String) = Unit
    override fun recordNonFatal(throwable: Throwable) = Unit
}
