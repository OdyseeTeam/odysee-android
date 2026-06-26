package com.odysee.app.core.model

/**
 * Strongly typed view of a `lbry://...` URI.
 *
 * The full LBRY URI grammar is:
 *   `[lbry://][@channel(#|:)<claimId>][/stream-name(#|:)<claimId>][?query]`
 *
 * Examples we encounter in real payloads:
 * - `lbry://@channel#abc…`            channel claim
 * - `lbry://video-name#abc…`           content claim
 * - `lbry://@channel:abc…`             legacy `:` separator (claim-search form)
 * - `lbry://@channel#abc/video#xyz`    combined form: stream within a channel
 * - `lbry://@channel#abc?src=foo`      with query string
 *
 * Use [parse] for forgiving input parsing (returns null on malformed input).
 * [claimIdOf] is the drop-in for the old `extractClaimId` helpers — it
 * returns the **stream**'s claim ID for combined URIs and the primary
 * claim ID otherwise (i.e. "the claim ID of the thing the URL refers to").
 *
 * Modeled on `Odysee/Utils/LbryUri.swift` in the iOS app. Sequence (`:N`)
 * and bid-position (`$N`) modifiers are recognised by the grammar but not
 * preserved: we don't currently encounter them in stored URIs.
 */
data class LbryUri(
    /** Primary segment name. Includes the leading `@` for channel claims. */
    val name: String,
    /** Primary segment claim ID (8..40 hex chars). */
    val claimId: String,
    /** Stream segment name (without leading `@`) for combined URIs. */
    val streamName: String? = null,
    /** Stream segment claim ID for combined URIs. */
    val streamClaimId: String? = null,
    /** Query string content (without the leading `?`). */
    val queryString: String? = null,
) {
    /**
     * True only for pure-channel references like `lbry://@chan#abc` with no
     * `/stream` component. A combined `@chan#abc/video#xyz` URI refers to
     * the video, not the channel — `isChannel` is false there.
     */
    val isChannel: Boolean
        get() = name.startsWith("@") && streamName == null

    /** Channel handle without the `@`, or null for content-only URIs. */
    val channelHandle: String?
        get() = name.takeIf { it.startsWith("@") }?.removePrefix("@")

    /**
     * The claim ID this URL semantically refers to:
     *  - combined `@chan#abc/video#xyz` → the stream's claim id (`xyz`)
     *  - plain `@chan#abc` or `video#abc` → the primary claim id
     *
     * This is what code that wants "what's the claim of this URL" should
     * use — including the legacy `extractClaimId` helpers, which only ever
     * cared about whatever the URL "pointed at."
     */
    val effectiveClaimId: String
        get() = streamClaimId ?: claimId

    /** Canonical `lbry://...#...` form. Always uses `#` regardless of input separator. */
    val canonical: String
        get() = buildString {
            append("lbry://")
            append(name).append('#').append(claimId)
            if (streamName != null && streamClaimId != null) {
                append('/').append(streamName).append('#').append(streamClaimId)
            }
        }

    override fun toString(): String = canonical

    companion object {
        /**
         * Forgiving parse. Returns null for malformed input. Accepts:
         *  - canonical `lbry://...#...` form
         *  - legacy `:` separator
         *  - bare `name#claimId` (no scheme)
         *  - `https://odysee.com/...`, `open.lbry.com/...`, `lbry.tv/...` URLs
         *  - combined `@chan#abc/video#xyz` form (extracts both segments)
         *  - URLs with trailing query strings
         */
        fun parse(input: String?): LbryUri? {
            if (input.isNullOrBlank()) return null
            // Drop leading scheme + host prefixes. We accept the web forms
            // so deep-linked URLs are parseable without callers having to
            // pre-strip them.
            var s = input.trim().removePrefix("lbry://")
            for (host in WEB_PREFIXES) s = s.removePrefix(host)
            if (s.isEmpty()) return null

            // Isolate the query string.
            var query: String? = null
            val qIdx = s.indexOf('?')
            if (qIdx >= 0) {
                query = s.substring(qIdx + 1).takeIf { it.isNotEmpty() }
                s = s.substring(0, qIdx)
            }

            // Split on the first `/` for the combined channel/stream form.
            val slashIdx = s.indexOf('/')
            val primaryStr: String
            val secondaryStr: String?
            if (slashIdx >= 0) {
                primaryStr = s.substring(0, slashIdx)
                secondaryStr = s.substring(slashIdx + 1).takeIf { it.isNotEmpty() }
            } else {
                primaryStr = s
                secondaryStr = null
            }

            val primary = parseSegment(primaryStr) ?: return null
            val secondary = secondaryStr?.let { parseSegment(it) }

            // Auto-restore `@` when both primary and secondary are present
            // but the primary doesn't have it — mirrors the iOS heuristic
            // for URLs that came through `URL(string:)` which strips `@`.
            var name = primary.first
            if (secondary != null && !name.startsWith("@")) {
                name = "@$name"
            }

            return LbryUri(
                name = name,
                claimId = primary.second,
                streamName = secondary?.first,
                streamClaimId = secondary?.second,
                queryString = query,
            )
        }

        /**
         * Pulls just the claim ID of whatever the URL refers to. For
         * combined channel/stream URIs this returns the **stream** claim
         * ID (the video), matching what the legacy `extractClaimId`
         * helpers returned and what callers actually want.
         */
        fun claimIdOf(input: String?): String? = parse(input)?.effectiveClaimId

        /** Parse one `name(#|:)claimId` segment. Returns null if malformed. */
        private fun parseSegment(s: String): Pair<String, String>? {
            val sepIdx = s.indexOfAny(SEPARATORS)
            if (sepIdx <= 0) return null
            val name = s.substring(0, sepIdx)
            if (name.isBlank()) return null
            // Strip any trailing path (defensive: parse() already split on `/`,
            // but a `?` could survive in pathological input).
            val tail = s.substring(sepIdx + 1).takeWhile { it != '/' && it != '?' }
            // Real LBRY claim IDs are 8..40 hex chars. Sequence (`:N`) and
            // bid-position (`$N`) modifiers won't pass this check — we
            // don't carry them through but recognising the grammar
            // prevents accidental misparses.
            if (tail.length !in 8..40 || !tail.all { it.isLetterOrDigit() }) return null
            return name to tail
        }

        private val SEPARATORS = charArrayOf('#', ':')
        private val WEB_PREFIXES = listOf("odysee.com/", "open.lbry.com/", "lbry.tv/")
    }
}
