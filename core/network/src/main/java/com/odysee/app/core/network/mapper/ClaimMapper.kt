package com.odysee.app.core.network.mapper

import com.odysee.app.core.model.Channel
import com.odysee.app.core.model.ChannelRef
import com.odysee.app.core.model.Claim
import com.odysee.app.core.model.Paywall
import com.odysee.app.core.network.dto.ClaimDto

fun ClaimDto.toDomain(): Claim? {
    val id = claimId?.takeIf { it.isNotBlank() } ?: return null
    val n = name?.takeIf { it.isNotBlank() } ?: return null
    val duration = value?.video?.duration ?: value?.audio?.duration
    val release = value?.releaseTime?.toLongOrNull() ?: meta?.creationTimestamp
    val tagSet = value?.tags ?: emptyList()
    val paywall = resolvePaywall(value?.fee, tagSet)
    val membersOnly = tagSet.any { it.equals("c:members-only", ignoreCase = true) }
    return Claim(
        claimId = id,
        name = n,
        title = value?.title ?: n,
        description = value?.description?.takeIf { it.isNotBlank() },
        permanentUrl = permanentUrl ?: "lbry://$n#$id",
        canonicalUrl = canonicalUrl,
        thumbnailUrl = value?.thumbnail?.url?.takeIf { it.isNotBlank() },
        durationSeconds = duration,
        releaseTime = release,
        signingChannel = signingChannel?.toChannelRef(),
        videoWidth = value?.video?.width,
        videoHeight = value?.video?.height,
        hasSource = value?.video != null || value?.audio != null || value?.source != null,
        mediaType = value?.source?.mediaType?.takeIf { it.isNotBlank() },
        paywall = paywall,
        isPurchased = purchaseReceipt != null,
        isMembersOnly = membersOnly,
    )
}

private fun resolvePaywall(
    fee: com.odysee.app.core.network.dto.FeeDto?,
    tags: List<String>,
): Paywall {
    // Fiat purchase / rental tags take precedence (web encodes them this way).
    tags.firstOrNull { it.startsWith("c:rental:", ignoreCase = true) }?.let { tag ->
        val parts = tag.removePrefix("c:rental:").split(':')
        val amount = parts.getOrNull(0)?.toDoubleOrNull()
        val secs = parts.getOrNull(1)?.toLongOrNull()
        if (amount != null && amount > 0 && secs != null && secs > 0) {
            return Paywall.FiatRental(usd = amount, expirySeconds = secs)
        }
    }
    tags.firstOrNull { it.startsWith("c:purchase:", ignoreCase = true) }?.let { tag ->
        val amount = tag.removePrefix("c:purchase:").toDoubleOrNull()
        if (amount != null && amount > 0) return Paywall.FiatPurchase(usd = amount)
    }
    // SDK LBC fee.
    val feeAmount = fee?.amount?.toDoubleOrNull()
    val feeCurrency = fee?.currency
    if (feeAmount != null && feeAmount > 0 && feeCurrency.equals("LBC", ignoreCase = true)) {
        return Paywall.Lbc(amount = feeAmount)
    }
    return Paywall.Free
}

private fun ClaimDto.toChannelRef(): ChannelRef? {
    val id = claimId?.takeIf { it.isNotBlank() } ?: return null
    val n = name?.takeIf { it.isNotBlank() } ?: return null
    return ChannelRef(
        claimId = id,
        name = n,
        title = value?.title,
        thumbnailUrl = value?.thumbnail?.url?.takeIf { it.isNotBlank() },
    )
}

fun ClaimDto.toChannel(): Channel? {
    val id = claimId?.takeIf { it.isNotBlank() } ?: return null
    val n = name?.takeIf { it.isNotBlank() } ?: return null
    val amount = this.amount?.toDoubleOrNull() ?: 0.0
    val support = meta?.supportAmount?.toDoubleOrNull() ?: 0.0
    return Channel(
        claimId = id,
        name = n,
        title = value?.title?.takeIf { it.isNotBlank() },
        description = value?.description?.takeIf { it.isNotBlank() },
        thumbnailUrl = value?.thumbnail?.url?.takeIf { it.isNotBlank() },
        coverUrl = value?.cover?.url?.takeIf { it.isNotBlank() },
        permanentUrl = permanentUrl ?: "lbry://$n#$id",
        canonicalUrl = canonicalUrl,
        tags = (value?.tags ?: emptyList()).filterNot { it.startsWith("c:", ignoreCase = true) },
        languages = value?.languages ?: emptyList(),
        email = value?.email?.takeIf { it.isNotBlank() },
        websiteUrl = value?.websiteUrl?.takeIf { it.isNotBlank() },
        stakedAmount = amount + support,
        claimsInChannel = meta?.claimsInChannel,
        creationTimestamp = meta?.creationTimestamp,
        modifiedAt = timestamp,
        featuredUris = value?.featured ?: emptyList(),
    )
}
