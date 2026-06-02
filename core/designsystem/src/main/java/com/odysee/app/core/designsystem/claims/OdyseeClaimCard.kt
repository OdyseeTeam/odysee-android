package com.odysee.app.core.designsystem.claims

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Generic claim "card" used in the home feed, channel videos tab, search
 * results, related items, etc. Switches between vertical (portrait) and
 * horizontal (landscape) layouts automatically.
 */
data class OdyseeClaimCardModel(
    val claimId: String,
    val title: String,
    val channelName: String,
    val channelClaimId: String? = null,
    val channelTitle: String? = null,
    val channelAvatarUrl: String? = null,
    val channelInitial: Char = 'O',
    val thumbnailUrl: String? = null,
    val durationLabel: String = "",
    val ageLabel: String = "",
    val viewCount: Long? = null,
    val isLivestream: Boolean = false,
    val isUpcoming: Boolean = false,
    val isShort: Boolean = false,
    val thumbnailTintIndex: Int = 0,
    val channelAvatarTintIndex: Int = 0,
    val paywall: ClaimCardPaywall = ClaimCardPaywall.None,
    val isPurchased: Boolean = false,
    val isMembersOnly: Boolean = false,
)

sealed class ClaimCardPaywall {
    data object None : ClaimCardPaywall()
    data class Lbc(val amount: Double) : ClaimCardPaywall()
    data class Usd(val amount: Double) : ClaimCardPaywall()
    data class Rental(val amount: Double) : ClaimCardPaywall()
}

val OdyseeThumbnailPalette: List<Color> = listOf(
    Color(0xFF7B2942),
    Color(0xFF2C5364),
    Color(0xFF4A4E69),
    Color(0xFF3D405B),
    Color(0xFF52489C),
    Color(0xFF2A4858),
)

val OdyseeAvatarPalette: List<Color> = listOf(
    Color(0xFFE50054),
    Color(0xFF2EC4B6),
    Color(0xFFFF9F1C),
    Color(0xFF5E60CE),
    Color(0xFF06D6A0),
    Color(0xFFEF476F),
)

fun formatViewCount(n: Long): String = when {
    n >= 1_000_000 -> "${(n / 100_000).let { it / 10.0 }}M"
    n >= 10_000 -> "${n / 1_000}K"
    n >= 1_000 -> "${(n / 100).let { it / 10.0 }}K"
    else -> n.toString()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OdyseeClaimCard(
    claim: OdyseeClaimCardModel,
    onClick: () -> Unit,
    onChannelClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    showChannelAvatar: Boolean = true,
    landscapeThumbnailWidthFraction: Float = 0.42f,
    forceColumnLayout: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    val landscape = !forceColumnLayout && LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val rowModifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = onClick,
            onLongClick = {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onLongPress()
            },
        )

    if (landscape) {
        Row(
            modifier = rowModifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(landscapeThumbnailWidthFraction)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OdyseeThumbnailPalette[claim.thumbnailTintIndex % OdyseeThumbnailPalette.size]),
            ) { ThumbnailOverlay(claim) }
            Spacer(Modifier.width(12.dp))
            ClaimMeta(
                claim = claim,
                onChannelClick = onChannelClick,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Column(modifier = rowModifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(OdyseeThumbnailPalette[claim.thumbnailTintIndex % OdyseeThumbnailPalette.size]),
            ) { ThumbnailOverlay(claim) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (showChannelAvatar) {
                    Box(modifier = Modifier.clickable(onClick = onChannelClick)) {
                        ChannelAvatar(
                            avatarUrl = claim.channelAvatarUrl,
                            initial = claim.channelInitial,
                            tint = OdyseeAvatarPalette[claim.channelAvatarTintIndex % OdyseeAvatarPalette.size],
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }
                ClaimMeta(
                    claim = claim,
                    onChannelClick = onChannelClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.ThumbnailOverlay(claim: OdyseeClaimCardModel) {
    if (!claim.thumbnailUrl.isNullOrBlank()) {
        AsyncImage(
            model = claim.thumbnailUrl,
            contentDescription = claim.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
    PaywallBadge(claim = claim)
    when {
        claim.isLivestream -> {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE2202D))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "LIVE",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        claim.isUpcoming -> {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "UPCOMING",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        claim.durationLabel.isNotEmpty() -> {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = claim.durationLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.PaywallBadge(claim: OdyseeClaimCardModel) {
    val purchased = claim.isPurchased
    val (label, rentalLabel) = when (val pw = claim.paywall) {
        is ClaimCardPaywall.Lbc -> formatLbcAmount(pw.amount) to null
        is ClaimCardPaywall.Usd -> "$${"%.2f".format(pw.amount)}" to null
        is ClaimCardPaywall.Rental -> "$${"%.2f".format(pw.amount)}" to "Rent"
        ClaimCardPaywall.None -> if (claim.isMembersOnly) "Members" to null else return
    }
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (purchased) Color(0xCC1AC04F)
                else if (claim.isMembersOnly) Color(0xCC5E60CE)
                else Color(0xCCE2202D),
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when {
                purchased -> Icons.Outlined.CheckCircle
                claim.isMembersOnly -> Icons.Outlined.Star
                else -> Icons.Outlined.Lock
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.size(4.dp))
        if (rentalLabel != null) {
            Text(
                text = "$rentalLabel $label",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Text(
                text = if (purchased) "Purchased" else label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun formatLbcAmount(amount: Double): String {
    val formatted = if (amount == amount.toLong().toDouble()) "${amount.toLong()}"
    else "%.2f".format(amount).trimEnd('0').trimEnd('.')
    return "$formatted LBC"
}

@Composable
private fun ClaimMeta(
    claim: OdyseeClaimCardModel,
    onChannelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = claim.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(4.dp))
        val subtitle = buildString {
            if (claim.channelName.isNotEmpty()) append(claim.channelName)
            claim.viewCount?.let {
                if (isNotEmpty()) append(" • ")
                append(formatViewCount(it))
                append(" views")
            }
            if (claim.ageLabel.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(claim.ageLabel)
            }
        }
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onChannelClick),
            )
        }
    }
}

@Composable
private fun ChannelAvatar(avatarUrl: String?, initial: Char, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(tint),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = initial.toString(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
