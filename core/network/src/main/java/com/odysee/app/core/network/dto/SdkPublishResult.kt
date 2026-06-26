package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shape of the SDK's `outputs` envelope returned by claim-mutating
 * methods (`stream_create`, `stream_update`, `stream_repost`,
 * `channel_create`, etc.).
 *
 * Servers wrap it as `{ "result": { "outputs": [...] } }` at the
 * JSON-RPC layer; the JSON-RPC unwrap leaves us with this inner object.
 *
 * Only `claimId` is universally interesting to callers; everything else
 * is captured loosely as JSON for the rare places that look deeper.
 */
@Serializable
data class SdkPublishResult(
    val outputs: List<SdkPublishOutput> = emptyList(),
    /** Total fee paid for the transaction, in LBC. */
    @SerialName("total_fee") val totalFee: String? = null,
    /** Transaction ID of the underlying wallet send. */
    val txid: String? = null,
) {
    /** Convenience: claim ID of the first output, the one callers want 99% of the time. */
    val claimId: String? get() = outputs.firstOrNull()?.claimId
}

@Serializable
data class SdkPublishOutput(
    @SerialName("claim_id") val claimId: String? = null,
    val name: String? = null,
    @SerialName("permanent_url") val permanentUrl: String? = null,
    @SerialName("canonical_url") val canonicalUrl: String? = null,
)
