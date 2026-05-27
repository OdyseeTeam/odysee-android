package com.odysee.app.core.model

data class Claim(
    val claimId: String,
    val name: String,
    val title: String,
    val description: String?,
    val permanentUrl: String,
    val canonicalUrl: String?,
    val thumbnailUrl: String?,
    val durationSeconds: Long?,
    val releaseTime: Long?,
    val signingChannel: ChannelRef?,
    val videoWidth: Int? = null,
    val videoHeight: Int? = null,
    val hasSource: Boolean = true,
    val mediaType: String? = null,
    val paywall: Paywall = Paywall.Free,
    val isPurchased: Boolean = false,
    val isMembersOnly: Boolean = false,
) {
    val isUpcoming: Boolean
        get() {
            val release = releaseTime ?: return false
            return release > System.currentTimeMillis() / 1000
        }
    val isLivestream: Boolean
        get() = !hasSource && !isUpcoming
    /** Matches the web's Shorts criteria: aspect ratio ≤ 0.8 and duration ≤ 180s. */
    val isShort: Boolean
        get() {
            val w = videoWidth ?: return false
            val h = videoHeight ?: return false
            if (h <= 0) return false
            val aspect = w.toFloat() / h.toFloat()
            val dur = durationSeconds ?: return false
            return aspect <= 0.8f && dur <= 180L
        }
}

sealed class Paywall {
    data object Free : Paywall()
    /** Pure SDK LBC fee on the claim (fee_amount + fee_currency=LBC). */
    data class Lbc(val amount: Double) : Paywall()
    /** Fiat one-time purchase via Stripe — encoded by web as `c:purchase:<usd>` tag. */
    data class FiatPurchase(val usd: Double) : Paywall()
    /**
     * Fiat rental via Stripe — encoded by web as `c:rental:<usd>:<seconds>` tag. The
     * purchase is single-use and only valid for the [expirySeconds] window.
     */
    data class FiatRental(val usd: Double, val expirySeconds: Long) : Paywall()
}

data class ChannelRef(
    val claimId: String,
    val name: String,
    val title: String?,
    val thumbnailUrl: String?,
)
