package com.odysee.app.updater

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import com.odysee.app.BuildConfig
import com.odysee.app.core.data.updater.AppUpdater
import com.odysee.app.core.data.updater.UpdateInfo
import com.odysee.app.core.data.updater.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAppUpdater @Inject constructor(
    private val application: Application,
    private val okHttpClient: OkHttpClient,
) : AppUpdater {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    override val state: StateFlow<UpdateState> = _state.asStateFlow()
    override val isSupported: Boolean = true

    override suspend fun checkForUpdates(silent: Boolean): UpdateState = withContext(Dispatchers.IO) {
        _state.value = UpdateState.Checking
        val installed = BuildConfig.VERSION_NAME
        val result = runCatching {
            val req = Request.Builder().url(RELEASE_MANIFEST_URL).get().build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val obj = json.parseToJsonElement(body) as? JsonObject
                    ?: error("Invalid response")
                obj["versionName"]?.jsonPrimitive?.content
                    ?: error("Missing versionName")
            }
        }
        val state = result.fold(
            onSuccess = { remoteVersion ->
                if (isNewer(remoteVersion, installed)) {
                    UpdateState.Available(
                        UpdateInfo(
                            displayVersion = remoteVersion,
                            installedVersion = installed,
                            apkUrl = APK_URL_TEMPLATE.format(remoteVersion),
                        ),
                    )
                } else {
                    UpdateState.UpToDate(installed)
                }
            },
            onFailure = { err ->
                UpdateState.Failed(err.message ?: "Update check failed")
            },
        )
        _state.value = state
        state
    }

    override suspend fun downloadAndInstall() = withContext(Dispatchers.IO) {
        val current = _state.value
        val info = when (current) {
            is UpdateState.Available -> current.info
            is UpdateState.ReadyToInstall -> {
                launchInstaller(File(current.apkPath))
                return@withContext
            }
            else -> {
                _state.value = UpdateState.Failed("No update available")
                return@withContext
            }
        }
        _state.value = UpdateState.Downloading(info, progress = 0f)
        val cacheDir = File(application.cacheDir, "updates").apply { mkdirs() }
        val outFile = File(cacheDir, "odysee-${info.displayVersion}.apk")
        outFile.delete()
        val downloadResult = runCatching {
            val req = Request.Builder().url(info.apkUrl).get().build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body ?: error("Empty body")
                val total = body.contentLength().coerceAtLeast(1L)
                body.byteStream().use { input ->
                    outFile.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var copied = 0L
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            out.write(buf, 0, read)
                            copied += read
                            _state.value = UpdateState.Downloading(
                                info,
                                progress = (copied.toFloat() / total.toFloat()).coerceIn(0f, 1f),
                            )
                        }
                    }
                }
            }
        }
        downloadResult
            .onSuccess {
                _state.value = UpdateState.ReadyToInstall(info, outFile.absolutePath)
                launchInstaller(outFile)
            }
            .onFailure { err ->
                _state.value = UpdateState.Failed(err.message ?: "Download failed")
            }
    }

    private fun launchInstaller(apkFile: File) {
        val authority = "${application.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(application, authority, apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        application.startActivity(intent)
    }

    override fun dismiss() {
        _state.value = UpdateState.Idle
    }

    companion object {
        private const val RELEASE_MANIFEST_URL = "https://apk.odysee.tv/release_native.json"
        private const val APK_URL_TEMPLATE = "https://apk.odysee.tv/apk/odysee-%s.apk"

        internal fun isNewer(remote: String, installed: String): Boolean {
            val r = parseSemver(remote)
            val i = parseSemver(installed)
            val len = maxOf(r.size, i.size)
            for (idx in 0 until len) {
                val ri = r.getOrElse(idx) { 0 }
                val ii = i.getOrElse(idx) { 0 }
                if (ri != ii) return ri > ii
            }
            return false
        }

        private fun parseSemver(value: String): List<Int> =
            value.substringBefore('-').substringBefore('+')
                .split('.')
                .map { it.toIntOrNull() ?: 0 }
    }
}
