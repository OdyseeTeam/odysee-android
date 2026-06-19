package com.odysee.app.core.data.player

import androidx.media3.common.C
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * Retry policy shared by both the main player and the shorts player.
 *
 * - 429 / 5xx → exponential backoff up to ~2 minutes (the player CDN returns
 *   429 while a freshly published video is still transcoding).
 * - 4xx other than 429 → fail immediately (404 / 410 / 403 mean the content
 *   is gone or forbidden; retrying forever produces the "video buffers
 *   forever" UX users were reporting).
 * - I/O errors (timeouts, DNS, etc.) → six retries with default backoff,
 *   then surface as an error.
 */
fun odyseeLoadErrorHandlingPolicy(): LoadErrorHandlingPolicy =
    object : DefaultLoadErrorHandlingPolicy() {
        override fun getRetryDelayMsFor(
            loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo,
        ): Long {
            val ex = loadErrorInfo.exception
            val errorCount = loadErrorInfo.errorCount
            if (ex is HttpDataSource.InvalidResponseCodeException) {
                val code = ex.responseCode
                return when {
                    code == 429 || code in 500..599 -> {
                        if (errorCount > 24) C.TIME_UNSET
                        else {
                            val base = 2000L
                            val backoff =
                                base * (1L shl (errorCount.coerceAtMost(4) - 1).coerceAtLeast(0))
                            backoff.coerceAtMost(15_000L)
                        }
                    }
                    code in 400..499 -> C.TIME_UNSET
                    else -> super.getRetryDelayMsFor(loadErrorInfo)
                }
            }
            if (errorCount > 6) return C.TIME_UNSET
            return super.getRetryDelayMsFor(loadErrorInfo)
        }

        override fun getMinimumLoadableRetryCount(dataType: Int): Int = Int.MAX_VALUE
    }
