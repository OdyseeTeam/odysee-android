package com.odysee.app.core.designsystem.i18n

import androidx.compose.runtime.compositionLocalOf

/**
 * The active English→translated-text map for the current UI locale. The host
 * app provides this from its `TranslationStore`; designsystem only reads it.
 *
 * Kept around for backwards compatibility — callers should prefer [tr] which
 * pulls from the same snapshot the data layer uses.
 *
 * Empty map means English (no translation needed).
 */
val LocalTranslations = compositionLocalOf<Map<String, String>> { emptyMap() }

/**
 * Translate an English source string into the current locale. Unknown strings
 * fall back to the source, matching the web's behavior.
 *
 * Intentionally NOT `@Composable` — works in both Composable functions and
 * arbitrary callbacks (onClick lambdas, coroutine launches, ViewModel-style
 * callbacks, error handlers). Internally reads the [Translations] snapshot
 * which the data-layer `TranslationStore` keeps in sync with the active
 * locale.
 *
 * Trade-off: language changes don't recompose existing UI automatically;
 * the user must back out and re-enter the screen. That matches what the web
 * does (a hard refresh on language change), and avoids forcing every caller
 * to be `@Composable`.
 */
fun tr(source: String): String =
    com.odysee.app.core.model.i18n.Translations.tr(source)

/**
 * Like [tr] but performs `%s`-style substitution on the translated template.
 */
fun trFormat(source: String, vararg args: Any?): String {
    val template = tr(source)
    return runCatching { String.format(template, *args) }.getOrDefault(template)
}
