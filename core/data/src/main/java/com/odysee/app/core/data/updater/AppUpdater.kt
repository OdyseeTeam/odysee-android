package com.odysee.app.core.data.updater

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UpdateInfo(
    val displayVersion: String,
    val installedVersion: String,
    val apkUrl: String,
)

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class UpToDate(val installedVersion: String) : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState()
    data class ReadyToInstall(val info: UpdateInfo, val apkPath: String) : UpdateState()
    data class Failed(val message: String) : UpdateState()
}

interface AppUpdater {
    val state: StateFlow<UpdateState>
    val isSupported: Boolean
    suspend fun checkForUpdates(silent: Boolean = false): UpdateState
    suspend fun downloadAndInstall()
    fun dismiss()
}

class NoOpAppUpdater : AppUpdater {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    override val state: StateFlow<UpdateState> = _state.asStateFlow()
    override val isSupported: Boolean = false
    override suspend fun checkForUpdates(silent: Boolean): UpdateState = UpdateState.Idle
    override suspend fun downloadAndInstall() = Unit
    override fun dismiss() = Unit
}
