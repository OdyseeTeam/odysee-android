package com.odysee.app.core.model.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Process-wide accessor for translated UI strings. Works in any context —
 * Composables, ViewModels, coroutine callbacks, services. Backed by a
 * Compose [androidx.compose.runtime.mutableStateOf] so that Composables
 * which read `tr(...)` are auto-recomposed when [TranslationStore] updates
 * the snapshot on a language change.
 */
object Translations {
    private var snapshotState by mutableStateOf<Map<String, String>>(emptyMap())

    /**
     * Replaced by `TranslationStore` whenever the active locale's catalog
     * is loaded. Writing this from outside the data layer is a bug.
     */
    var snapshot: Map<String, String>
        get() = snapshotState
        set(value) { snapshotState = value }

    fun tr(source: String): String {
        val map = snapshotState
        return map[source] ?: map[fold(source)] ?: source
    }

    private fun fold(s: String): String =
        s.trim().removeSuffix("…").removeSuffix("...").trimEnd().lowercase()
}
