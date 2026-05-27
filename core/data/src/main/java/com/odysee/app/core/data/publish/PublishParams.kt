package com.odysee.app.core.data.publish

import kotlinx.serialization.Serializable

@Serializable
enum class PublishVisibility { Public, Unlisted, Scheduled, Private }

@Serializable
data class PublishParams(
    val name: String,
    val title: String,
    val description: String? = null,
    val bid: String,
    val channelId: String? = null,
    val thumbnailUrl: String? = null,
    val tags: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val license: String? = null,
    val licenseUrl: String? = null,
    val locations: List<String> = emptyList(),
    val releaseTime: Long? = null,
    // System flags emitted as `c:*` tags by the publish layer. Matches web
    // `PAYLOAD.tags.visibility/.fiatPaywall/.membershipRestrictions`.
    val visibility: PublishVisibility = PublishVisibility.Public,
    val scheduledShow: Boolean = true,
    val membersOnly: Boolean = false,
    val feeAmountLbc: String? = null,
    val fiatPurchaseUsd: String? = null,
    val fiatRentalUsd: String? = null,
    val fiatRentalSeconds: Long? = null,
)
