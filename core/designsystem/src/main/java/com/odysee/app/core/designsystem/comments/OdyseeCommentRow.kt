package com.odysee.app.core.designsystem.comments

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.odysee.app.core.designsystem.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OdyseeCommentRow(
    comment: OdyseeComment,
    actions: OdyseeCommentActions,
    canReply: Boolean = false,
    replyOpen: Boolean = false,
    onReplyOpenChange: (Boolean) -> Unit = {},
    onLongPress: () -> Unit = {},
    avatarSize: Int = 32,
) {
    var draft by remember(comment.id) { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current
    val isHyperchat = comment.supportAmount > 0.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
            )
            .then(
                if (isHyperchat) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                else Modifier,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        OdyseeChannelAvatar(
            avatarUrl = comment.authorAvatarUrl,
            initial = comment.authorInitial,
            size = avatarSize,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (comment.isPinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    val pinLabel = comment.pinnedByName?.let { name ->
                        val handle = if (name.startsWith("@")) name else "@$name"
                        "Pinned by $handle"
                    } ?: "Pinned"
                    Text(
                        text = pinLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                if (comment.isCreator) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = comment.authorDisplayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                } else {
                    Text(
                        text = comment.authorDisplayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                if (comment.isGlobalMod) {
                    Spacer(Modifier.width(4.dp))
                    Image(
                        painter = painterResource(id = R.drawable.badge_admin),
                        contentDescription = "Admin",
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (comment.isModerator) {
                    Spacer(Modifier.width(4.dp))
                    Image(
                        painter = painterResource(id = R.drawable.badge_mod),
                        contentDescription = "Moderator",
                        modifier = Modifier.size(18.dp),
                    )
                }
                when (comment.membership) {
                    OdyseeMembershipTier.Premium -> {
                        Spacer(Modifier.width(4.dp))
                        Image(
                            painter = painterResource(id = R.drawable.badge_premium),
                            contentDescription = "Premium",
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    OdyseeMembershipTier.PremiumPlus -> {
                        Spacer(Modifier.width(4.dp))
                        Image(
                            painter = painterResource(id = R.drawable.badge_premium_plus),
                            contentDescription = "Premium+",
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    OdyseeMembershipTier.None -> Unit
                }
                comment.creatorMembership?.takeIf { it.isNotBlank() }?.let { tier ->
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFE7500))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = tier,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
                if (comment.ageLabel.isNotBlank()) {
                    Text(
                        text = " • ${comment.ageLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (comment.isEdited) {
                    Text(
                        text = " (edited)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isHyperchat) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "${"%.0f".format(comment.supportAmount)} LBC",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.size(2.dp))
            RichCommentBody(body = comment.body)
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CommentReactionButton(
                    iconRes = if (comment.myReaction == OdyseeReaction.Like)
                        R.drawable.ic_reaction_fire_active
                    else R.drawable.ic_reaction_fire,
                    count = comment.likes,
                    active = comment.myReaction == OdyseeReaction.Like,
                    onClick = actions.onLike,
                )
                Spacer(Modifier.width(12.dp))
                CommentReactionButton(
                    iconRes = if (comment.myReaction == OdyseeReaction.Dislike)
                        R.drawable.ic_reaction_slime_active
                    else R.drawable.ic_reaction_slime,
                    count = comment.dislikes,
                    active = comment.myReaction == OdyseeReaction.Dislike,
                    onClick = actions.onDislike,
                )
                if (comment.creatorLiked) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(22.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_creator_like_heart),
                            contentDescription = "Creator loved this",
                            modifier = Modifier.size(22.dp),
                        )
                        if (!comment.creatorAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = comment.creatorAvatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .align(Alignment.Center),
                            )
                        }
                    }
                }
                if (canReply && actions.onReply != null) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { onReplyOpenChange(!replyOpen) },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = if (replyOpen) "Cancel" else "Reply",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            if (canReply && replyOpen && actions.onReply != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a reply...") },
                        maxLines = 4,
                    )
                    Spacer(Modifier.size(8.dp))
                    TextButton(
                        enabled = draft.isNotBlank(),
                        onClick = {
                            actions.onReply.invoke(draft.trim())
                            draft = ""
                            onReplyOpenChange(false)
                        },
                    ) { Text("Post") }
                }
            }
        }
    }
}

@Composable
private fun CommentTag(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CommentReactionButton(
    iconRes: Int,
    count: Int,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            colorFilter = if (active) null else ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = formatCount(count.toLong()),
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatCount(n: Long): String = when {
    n < 1000L -> n.toString()
    n < 10_000L -> "%.1fK".format(n / 1000.0).removeSuffix(".0K") + (if (n % 1000 == 0L) "K" else "")
    n < 1_000_000L -> "${n / 1000}K"
    else -> "%.1fM".format(n / 1_000_000.0)
}

@Composable
fun OdyseeChannelAvatar(avatarUrl: String?, initial: Char, size: Int) {
    val tint = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(size.dp)
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
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun OdyseeCommentActionsSheet(
    comment: OdyseeComment,
    isClaimOwner: Boolean,
    canReply: Boolean,
    actions: OdyseeCommentActions,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                if (!comment.authorChannelId.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = actions.onVisitChannel)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OdyseeChannelAvatar(
                            avatarUrl = comment.authorAvatarUrl,
                            initial = comment.authorInitial,
                            size = 28,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Visit ${comment.authorDisplayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
                if (canReply && actions.onReply != null) {
                    SheetRow(icon = Icons.AutoMirrored.Outlined.Reply, label = "Reply") {
                        actions.onReply.invoke("")
                    }
                }
                SheetRow(icon = Icons.Outlined.ContentCopy, label = "Copy text", onClick = actions.onCopyText)
                SheetRow(icon = Icons.Outlined.Link, label = "Copy link", onClick = actions.onCopyLink)
                if (isClaimOwner) {
                    SheetRow(
                        icon = Icons.Outlined.PushPin,
                        label = if (comment.isPinned) "Unpin" else "Pin",
                        onClick = actions.onPinToggle,
                    )
                }
                if (comment.isMine) {
                    SheetRow(icon = Icons.Outlined.Edit, label = "Edit", onClick = actions.onEdit)
                }
                if (comment.isMine || isClaimOwner) {
                    SheetRow(
                        icon = Icons.Outlined.Delete,
                        label = "Delete",
                        danger = true,
                        onClick = actions.onDelete,
                    )
                }
                if (!comment.isMine && !comment.authorChannelId.isNullOrBlank()) {
                    SheetRow(icon = Icons.Outlined.Block, label = "Block", onClick = actions.onBlock)
                }
                if (isClaimOwner && !comment.isMine) {
                    SheetRow(
                        icon = Icons.Outlined.AdminPanelSettings,
                        label = if (comment.isModerator) "Remove as moderator" else "Add as moderator",
                    ) {
                        if (comment.isModerator) actions.onRemoveModerator() else actions.onAddModerator()
                    }
                }
                if (!comment.isMine) {
                    SheetRow(icon = Icons.Outlined.Flag, label = "Report", onClick = actions.onReport)
                }
            }
        }
    }
}

@Composable
private fun SheetRow(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (danger) Color(0xFFE2202D) else MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
