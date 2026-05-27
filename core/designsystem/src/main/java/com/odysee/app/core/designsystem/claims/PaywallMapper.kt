package com.odysee.app.core.designsystem.claims

import com.odysee.app.core.model.Paywall

fun Paywall.toCardPaywall(): ClaimCardPaywall = when (this) {
    Paywall.Free -> ClaimCardPaywall.None
    is Paywall.Lbc -> ClaimCardPaywall.Lbc(amount)
    is Paywall.FiatPurchase -> ClaimCardPaywall.Usd(usd)
    is Paywall.FiatRental -> ClaimCardPaywall.Rental(usd)
}

@JvmName("paywallToCard")
fun toCardPaywall(paywall: Paywall): ClaimCardPaywall = paywall.toCardPaywall()
