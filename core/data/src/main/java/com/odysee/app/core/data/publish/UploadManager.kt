package com.odysee.app.core.data.publish

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

enum class UploadStatus { Idle, Running, Completed, Failed }

data class UploadJob(
    val id: String,
    val title: String,
    val totalBytes: Long,
    val uploadedBytes: Long = 0L,
    val status: UploadStatus = UploadStatus.Running,
    val errorMessage: String? = null,
    val resultTxId: String? = null,
)

@Singleton
class UploadManager @Inject constructor() {

    private val _job = MutableStateFlow<UploadJob?>(null)
    val job: StateFlow<UploadJob?> = _job.asStateFlow()

    fun start(id: String, title: String, totalBytes: Long) {
        _job.value = UploadJob(id = id, title = title, totalBytes = totalBytes)
    }

    fun progress(id: String, uploaded: Long, total: Long) {
        _job.update { current ->
            if (current?.id != id) current
            else current.copy(uploadedBytes = uploaded, totalBytes = total)
        }
    }

    fun completed(id: String, txId: String) {
        _job.update { current ->
            if (current?.id != id) current
            else current.copy(status = UploadStatus.Completed, resultTxId = txId)
        }
    }

    fun failed(id: String, message: String) {
        _job.update { current ->
            if (current?.id != id) current
            else current.copy(status = UploadStatus.Failed, errorMessage = message)
        }
    }

    fun clear() {
        _job.value = null
    }
}
