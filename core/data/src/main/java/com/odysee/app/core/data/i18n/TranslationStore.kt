package com.odysee.app.core.data.i18n

import android.content.Context
import com.odysee.app.core.datastore.AuthPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authPreferences: AuthPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cache = mutableMapOf<String, Map<String, String>>()

    private val _locale = MutableStateFlow(systemLocaleTag())
    val locale: StateFlow<String> = _locale.asStateFlow()

    private fun systemLocaleTag(): String {
        // Read straight from the system Configuration — Locale.getDefault() can
        // be overridden by other code (per-app language settings, library
        // initialisation, etc.) so it's not reliable for "what is the user's
        // device language."
        val sysLocales = android.content.res.Resources.getSystem().configuration.locales
        val locale = if (!sysLocales.isEmpty) sysLocales[0] else java.util.Locale.ENGLISH
        return locale.toLanguageTag()
    }

    private val _messages = MutableStateFlow<Map<String, String>>(emptyMap())
    val messages: StateFlow<Map<String, String>> = _messages.asStateFlow()

    init {
        scope.launch {
            authPreferences.languageOverride.collect { stored ->
                val target = stored?.takeIf { it.isNotBlank() } ?: systemLocaleTag()
                _locale.value = target
                val loaded = loadAssetMessages(target)
                _messages.value = loaded
                com.odysee.app.core.model.i18n.Translations.snapshot = loaded
            }
        }
    }

    suspend fun setLanguage(tag: String?) {
        authPreferences.setLanguageOverride(tag)
    }

    private suspend fun loadAssetMessages(tag: String): Map<String, String> = withContext(Dispatchers.IO) {
        if (tag.equals("en", ignoreCase = true)) return@withContext emptyMap()
        cache[tag]?.let { return@withContext it }
        val file = tagToAssetFile(tag) ?: return@withContext emptyMap()
        val parsed = runCatching {
            context.assets.open("i18n/$file").use { input ->
                val raw = input.readBytes().decodeToString()
                val obj = json.parseToJsonElement(raw) as? JsonObject ?: return@runCatching emptyMap()
                // Build the primary map (exact source → translated) plus a
                // case-and-punctuation-folded index so slight differences
                // between our Kotlin source strings and the web's catalog
                // still resolve. "Add a tag…" matches the web's "Add a tag",
                // "discussion" matches "Discussion", etc.
                val primary = LinkedHashMap<String, String>(obj.size * 2)
                obj.forEach { (k, v) -> primary[k] = v.jsonPrimitive.content }
                obj.forEach { (k, v) ->
                    val folded = fold(k)
                    if (folded != k && folded !in primary) {
                        primary[folded] = v.jsonPrimitive.content
                    }
                }
                primary
            }
        }.getOrNull().orEmpty()
        cache[tag] = parsed
        parsed
    }

    fun translate(source: String): String {
        val map = _messages.value
        return map[source] ?: map[fold(source)] ?: source
    }

    /**
     * Normalize a source string so trivial differences don't block a
     * translation hit. Strips trailing ellipsis / punctuation / whitespace
     * and lower-cases — the web catalog mixes Title Case and sentence case
     * for the same UI concepts, so we want both to resolve.
     */
    private fun fold(s: String): String =
        s.trim().removeSuffix("…").removeSuffix("...").trimEnd().lowercase()

    companion object {
        // Maps an Android language tag (e.g. "zh-CN", "pt-BR") to a filename
        // we shipped under assets/i18n/. Returns null when we don't have a
        // file for that locale, in which case the UI falls back to English.
        fun tagToAssetFile(tag: String): String? {
            val normalized = tag.replace('_', '-')
            // Exact match first.
            ASSETS_EXACT[normalized]?.let { return it }
            // Try language-only.
            val lang = normalized.substringBefore('-').lowercase()
            return ASSETS_LANG_ONLY[lang]
        }

        // Region-specific assets that should win when the tag matches exactly.
        private val ASSETS_EXACT = mapOf(
            "pt-BR" to "pt-BR.json",
            "zh-Hans" to "zh-Hans.json",
            "zh-Hant" to "zh-Hant.json",
            "zh-CN" to "zh-Hans.json",
            "zh-SG" to "zh-Hans.json",
            "zh-TW" to "zh-Hant.json",
            "zh-HK" to "zh-Hant.json",
            "zh-MO" to "zh-Hant.json",
        )

        // Language-only fallback (e.g. "de", "fr").
        private val ASSETS_LANG_ONLY: Map<String, String> = listOf(
            "af", "ar", "bg", "bn", "ca", "cs", "cy", "da", "de", "el",
            "eo", "es", "et", "fa", "fi", "fr", "gu", "he", "hi", "hr",
            "hu", "id", "it", "ja", "jv", "kn", "ko", "lt", "lv", "ml",
            "mr", "ms", "ne", "nl", "no", "pa", "pl", "pt", "ro", "ru",
            "sk", "sl", "sr", "sv", "ta", "th", "tl", "tr", "uk", "ur", "vi",
        ).associateWith { "$it.json" }

        /**
         * The languages we ship. Used by the settings picker.
         *
         * Order: "System default" + English first, then Latin-script
         * languages alphabetized in their native form, then the
         * non-Latin-script blocks grouped together at the bottom
         * (Cyrillic, Greek, Hebrew, Arabic/Persian/Urdu, Indic, CJK,
         * Thai). This keeps scripts the user can't visually scan from
         * being interleaved among ones they can.
         */
        val SUPPORTED: List<Pair<String, String>> = listOf(
            "system" to "System default",
            "en" to "English",
            // --- Latin-script (alphabetised by endonym) ---
            "af" to "Afrikaans",
            "id" to "Bahasa Indonesia",
            "ms" to "Bahasa Melayu",
            "jv" to "Basa Jawa",
            "ca" to "Català",
            "cy" to "Cymraeg",
            "cs" to "Čeština",
            "da" to "Dansk",
            "de" to "Deutsch",
            "et" to "Eesti",
            "eo" to "Esperanto",
            "es" to "Español",
            "fr" to "Français",
            "hr" to "Hrvatski",
            "it" to "Italiano",
            "lv" to "Latviešu",
            "lt" to "Lietuvių",
            "hu" to "Magyar",
            "nl" to "Nederlands",
            "no" to "Norsk",
            "pl" to "Polski",
            "pt" to "Português",
            "pt-BR" to "Português (Brasil)",
            "ro" to "Română",
            "sk" to "Slovenčina",
            "sl" to "Slovenščina",
            "fi" to "Suomi",
            "sv" to "Svenska",
            "vi" to "Tiếng Việt",
            "tl" to "Tagalog",
            "tr" to "Türkçe",
            // --- Cyrillic ---
            "bg" to "Български",
            "ru" to "Русский",
            "sr" to "Српски",
            "uk" to "Українська",
            // --- Greek ---
            "el" to "Ελληνικά",
            // --- Hebrew ---
            "he" to "עברית",
            // --- Arabic / Persian / Urdu ---
            "ar" to "العربية",
            "fa" to "فارسی",
            "ur" to "اردو",
            // --- Indic ---
            "hi" to "हिन्दी",
            "mr" to "मराठी",
            "ne" to "नेपाली",
            "bn" to "বাংলা",
            "pa" to "ਪੰਜਾਬੀ",
            "gu" to "ગુજરાતી",
            "ta" to "தமிழ்",
            "kn" to "ಕನ್ನಡ",
            "ml" to "മലയാളം",
            // --- CJK ---
            "zh-Hans" to "简体中文",
            "zh-Hant" to "繁體中文",
            "ja" to "日本語",
            "ko" to "한국어",
            // --- Thai ---
            "th" to "ไทย",
        )
    }
}
