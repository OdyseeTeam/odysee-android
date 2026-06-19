package com.odysee.app.core.designsystem.claims

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.odysee.app.core.designsystem.i18n.tr

@Composable
fun OdyseeClaimMenuSheet(
    target: OdyseeClaimMenuTarget,
    actions: OdyseeClaimMenuActions,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = target.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                MenuRow(Icons.Outlined.PictureInPicture, tr("Play in background")) {
                    actions.onPlayBackground(); onDismiss()
                }
                MenuRow(Icons.Outlined.PictureInPicture, tr("Play in pop-up player")) {
                    actions.onPlayPip(); onDismiss()
                }
                MenuRow(Icons.Outlined.Bookmark, tr("Save to Watch Later")) {
                    actions.onSaveWatchLater(); onDismiss()
                }
                MenuRow(Icons.Outlined.Favorite, tr("Add to Favorites")) {
                    actions.onSaveFavorite(); onDismiss()
                }
                MenuRow(Icons.Outlined.PlaylistAdd, tr("Add to playlist")) {
                    actions.onAddToPlaylist(); onDismiss()
                }
                actions.onRemoveFromPlaylist?.let { remove ->
                    MenuRow(Icons.Outlined.PlaylistRemove, tr("Remove from this playlist")) {
                        remove(); onDismiss()
                    }
                }
                actions.onRepost?.let { repost ->
                    MenuRow(Icons.Outlined.Repeat, tr("Repost")) {
                        repost(); onDismiss()
                    }
                }
                actions.onEdit?.let { edit ->
                    MenuRow(Icons.Outlined.Edit, tr("Edit")) {
                        edit(); onDismiss()
                    }
                }
                MenuRow(Icons.Outlined.Share, tr("Share")) {
                    actions.onShare(); onDismiss()
                }
                MenuRow(Icons.Outlined.ContentCopy, tr("Copy link")) {
                    actions.onCopyLink(); onDismiss()
                }
                actions.onGoToChannel?.let { go ->
                    target.channelName?.let { name ->
                        MenuRow(Icons.Outlined.PersonOutline, "Go to $name") {
                            go(); onDismiss()
                        }
                    }
                }
                actions.onBlockChannel?.let { block ->
                    MenuRow(Icons.Outlined.Block, tr("Block channel")) {
                        block(); onDismiss()
                    }
                }
                MenuRow(Icons.Outlined.Flag, tr("Report content")) {
                    actions.onReport(); onDismiss()
                }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text = com.odysee.app.core.designsystem.i18n.tr(label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
