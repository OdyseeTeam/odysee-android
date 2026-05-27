package com.odysee.app.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.odysee.app.core.designsystem.comments.RichCommentBody
import com.odysee.app.core.model.Claim
import com.odysee.app.core.model.Comment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnCommentsScreen(
    onBack: () -> Unit,
    onOpenContent: (Claim, commentId: String) -> Unit,
    viewModel: OwnCommentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    val activeChannel = state.channels.firstOrNull { it.claimId == state.activeChannelClaimId }
        ?: state.channels.firstOrNull()

    var menuTarget by remember { mutableStateOf<Comment?>(null) }
    var editTarget by remember { mutableStateOf<Comment?>(null) }
    var deleteTarget by remember { mutableStateOf<Comment?>(null) }
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Your comments")
                        if (state.totalItems > 0) {
                            Text(
                                text = if (state.totalItems == 1) "1 comment" else "${state.totalItems} comments",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            if (state.channels.size > 1) {
                ChannelSelector(
                    channels = state.channels,
                    activeClaimId = state.activeChannelClaimId,
                    onSelect = viewModel::selectChannel,
                )
            }
            when {
                state.channels.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Create a channel to view your comments",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                state.comments.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No comments yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.comments, key = { it.commentId }) { comment ->
                        val claim = comment.claimId?.let { state.claimsById[it] }
                        CommentCard(
                            comment = comment,
                            claim = claim,
                            authorTitle = activeChannel?.title,
                            authorHandle = activeChannel?.name,
                            authorAvatarUrl = activeChannel?.thumbnailUrl,
                            onClick = {
                                if (claim != null) onOpenContent(claim, comment.commentId)
                            },
                            onLongPress = { menuTarget = comment },
                        )
                    }
                    if (state.isLoadingMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        state.error?.let { err ->
            Surface(
                color = Color(0x33E2202D),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = err,
                    color = Color(0xFFE2202D),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }

    menuTarget?.let { target ->
        val claim = target.claimId?.let { state.claimsById[it] }
        CommentActionsDialog(
            comment = target,
            claim = claim,
            onDismiss = { menuTarget = null },
            onOpenContent = {
                menuTarget = null
                if (claim != null) onOpenContent(claim, target.commentId)
            },
            onCopyText = {
                clipboard.setText(AnnotatedString(target.body))
                menuTarget = null
            },
            onCopyLink = {
                val link = claim?.canonicalUrl
                    ?: claim?.let { "https://odysee.com/${it.name}:${it.claimId}" }
                if (link != null) {
                    val full = "$link?lc=${target.commentId}"
                    clipboard.setText(AnnotatedString(full))
                }
                menuTarget = null
            },
            onEdit = {
                editTarget = target
                menuTarget = null
            },
            onDelete = {
                deleteTarget = target
                menuTarget = null
            },
        )
    }

    editTarget?.let { target ->
        CommentEditDialog(
            initialText = target.body,
            onCancel = { editTarget = null },
            onConfirm = { newBody ->
                viewModel.editComment(target.commentId, newBody)
                editTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete comment?") },
            text = { Text("This will permanently remove your comment.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteComment(target.commentId)
                        deleteTarget = null
                    },
                ) { Text("Delete", color = Color(0xFFE2202D)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CommentActionsDialog(
    comment: Comment,
    claim: Claim?,
    onDismiss: () -> Unit,
    onOpenContent: () -> Unit,
    onCopyText: () -> Unit,
    onCopyLink: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                if (claim != null) {
                    ActionRow(icon = Icons.Outlined.OpenInNew, label = "Open content", onClick = onOpenContent)
                }
                ActionRow(icon = Icons.Outlined.ContentCopy, label = "Copy text", onClick = onCopyText)
                if (claim != null) {
                    ActionRow(icon = Icons.Outlined.Link, label = "Copy link", onClick = onCopyLink)
                }
                ActionRow(icon = Icons.Outlined.Edit, label = "Edit", onClick = onEdit)
                ActionRow(
                    icon = Icons.Outlined.Delete,
                    label = "Delete",
                    color = Color(0xFFE2202D),
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(16.dp))
        Text(label, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CommentEditDialog(
    initialText: String,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Edit comment") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank() && text.trim() != initialText.trim(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}

@Composable
private fun ChannelSelector(
    channels: List<OwnCommentsChannel>,
    activeClaimId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val active = channels.firstOrNull { it.claimId == activeClaimId } ?: channels.firstOrNull() ?: return
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clickable { expanded = true },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChannelAvatar(thumbnailUrl = active.thumbnailUrl, fallback = active.displayName, size = 28.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = active.displayName,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            channels.forEach { ch ->
                DropdownMenuItem(
                    text = { Text(ch.displayName) },
                    onClick = {
                        onSelect(ch.claimId)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentCard(
    comment: Comment,
    claim: Claim?,
    authorTitle: String?,
    authorHandle: String?,
    authorAvatarUrl: String?,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (claim != null) onClick() },
                onLongClick = onLongPress,
            ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Target content row (thumb + title)
            if (claim != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val thumb = claim.thumbnailUrl
                    Box(
                        modifier = Modifier
                            .size(width = 88.dp, height = 50.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        if (!thumb.isNullOrBlank()) {
                            AsyncImage(
                                model = thumb,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = claim.title.ifBlank { "(no title)" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                        )
                        claim.signingChannel?.let { ch ->
                            Text(
                                text = ch.title?.takeIf { it.isNotBlank() } ?: ch.name.removePrefix("@"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Content or channel was deleted.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            // Author + time row
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelAvatar(
                    thumbnailUrl = authorAvatarUrl,
                    fallback = authorTitle?.takeIf { it.isNotBlank() }
                        ?: authorHandle?.removePrefix("@") ?: "O",
                    size = 32.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = authorTitle?.takeIf { it.isNotBlank() }
                            ?: authorHandle?.removePrefix("@").orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = formatTimeAgo(comment.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            // Body — rich content (markdown + emotes + stickers)
            RichCommentBody(body = comment.body)
        }
    }
}

@Composable
private fun ChannelAvatar(
    thumbnailUrl: String?,
    fallback: String,
    size: androidx.compose.ui.unit.Dp,
) {
    val initial = (fallback.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar()
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Text(
                text = initial.toString(),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private fun formatTimeAgo(timestampSeconds: Long): String {
    if (timestampSeconds <= 0) return ""
    val nowSec = System.currentTimeMillis() / 1000
    val diff = nowSec - timestampSeconds
    return when {
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86_400 -> "${diff / 3600}h ago"
        diff < 30L * 86_400 -> "${diff / 86_400}d ago"
        diff < 365L * 86_400 -> "${diff / (30L * 86_400)}mo ago"
        else -> "${diff / (365L * 86_400)}y ago"
    }
}
