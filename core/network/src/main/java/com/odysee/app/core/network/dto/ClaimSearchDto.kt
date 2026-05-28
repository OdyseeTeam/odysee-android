package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClaimSearchParams(
    @SerialName("claim_type") val claimType: List<String> = listOf("stream"),
    @SerialName("stream_types") val streamTypes: List<String>? = listOf("video"),
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    @SerialName("order_by") val orderBy: List<String> = listOf("release_time"),
    @SerialName("no_totals") val noTotals: Boolean = true,
    @SerialName("has_source") val hasSource: Boolean? = true,
    @SerialName("not_tags") val notTags: List<String>? = listOf("mature"),
    @SerialName("channel_ids") val channelIds: List<String>? = null,
    @SerialName("claim_ids") val claimIds: List<String>? = null,
    @SerialName("limit_claims_per_channel") val limitClaimsPerChannel: Int? = null,
    @SerialName("release_time") val releaseTime: String? = null,
    val duration: String? = null,
    @SerialName("content_aspect_ratio") val contentAspectRatio: String? = null,
    @SerialName("any_languages") val anyLanguages: List<String>? = null,
    val name: String? = null,
)

@Serializable
data class ClaimSearchResult(
    val items: List<ClaimDto> = emptyList(),
    val page: Int? = null,
    @SerialName("page_size") val pageSize: Int? = null,
)

@Serializable
data class ClaimDto(
    @SerialName("claim_id") val claimId: String,
    val name: String,
    val amount: String? = null,
    @SerialName("permanent_url") val permanentUrl: String? = null,
    @SerialName("canonical_url") val canonicalUrl: String? = null,
    @SerialName("short_url") val shortUrl: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("value_type") val valueType: String? = null,
    val value: ClaimValueDto? = null,
    val meta: ClaimMetaDto? = null,
    @SerialName("signing_channel") val signingChannel: ClaimDto? = null,
    val timestamp: Long? = null,
    @SerialName("purchase_receipt") val purchaseReceipt: PurchaseReceiptDto? = null,
)

@Serializable
data class PurchaseReceiptDto(
    @SerialName("claim_id") val claimId: String? = null,
    val amount: String? = null,
    val txid: String? = null,
)

@Serializable
data class ClaimValueDto(
    val title: String? = null,
    val description: String? = null,
    val thumbnail: ThumbnailDto? = null,
    val cover: ThumbnailDto? = null,
    val video: VideoStreamDto? = null,
    val audio: AudioStreamDto? = null,
    val source: ClaimSourceDto? = null,
    val claims: List<String>? = null,
    val featured: List<String>? = null,
    @SerialName("stream_type") val streamType: String? = null,
    @SerialName("release_time") val releaseTime: String? = null,
    val tags: List<String>? = null,
    val languages: List<String>? = null,
    val email: String? = null,
    @SerialName("website_url") val websiteUrl: String? = null,
    val fee: FeeDto? = null,
)

@Serializable
data class FeeDto(
    val amount: String? = null,
    val currency: String? = null,
    val address: String? = null,
)

@Serializable
data class ClaimSourceDto(
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("size") val size: String? = null,
    @SerialName("name") val name: String? = null,
)

@Serializable
data class ThumbnailDto(
    val url: String? = null,
)

@Serializable
data class VideoStreamDto(
    val duration: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class AudioStreamDto(
    val duration: Long? = null,
)

@Serializable
data class ClaimMetaDto(
    @SerialName("creation_timestamp") val creationTimestamp: Long? = null,
    @SerialName("effective_amount") val effectiveAmount: String? = null,
    @SerialName("support_amount") val supportAmount: String? = null,
    @SerialName("claims_in_channel") val claimsInChannel: Long? = null,
    @SerialName("trending_mixed") val trendingMixed: Double? = null,
    @SerialName("trending_group") val trendingGroup: Int? = null,
)
