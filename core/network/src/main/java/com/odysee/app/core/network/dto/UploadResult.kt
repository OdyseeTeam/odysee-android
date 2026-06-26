package com.odysee.app.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Shape of the TUS-style upload server's terminal JSON response across
 * file uploads, livestream stub publishes, and post publishes. The body
 * is a single line of JSON sitting at the bottom of the multipart
 * response; consumers parse it via [parseLenient] to get a typed view.
 *
 * Typical successful body:
 *   `{"type":"success","message":"https://odysee.com/@chan#abc/video#xyz"}`
 * Typical failure body:
 *   `{"type":"error","message":"streaming-only claims must be live"}`
 */
@Serializable
data class UploadResult(
    val type: String? = null,
    val message: String? = null,
) {
    val isSuccess: Boolean get() = type == "success"

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true; isLenient = true }

        /** Tolerant of garbage prefixes/suffixes around the JSON body. */
        fun parseLenient(text: String): UploadResult? {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return null
            // Servers sometimes wrap the JSON in newlines / extra envelope
            // chatter; grab the first '{...}' object we can find.
            val start = trimmed.indexOf('{')
            val end = trimmed.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            return runCatching {
                JSON.decodeFromString(serializer(), trimmed.substring(start, end + 1))
            }.getOrNull()
        }
    }
}
