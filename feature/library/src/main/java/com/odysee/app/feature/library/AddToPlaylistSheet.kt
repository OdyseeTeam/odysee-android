package com.odysee.app.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

@Composable
fun AddToPlaylistSheet(
    title: String,
    permanentUrl: String,
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
    quickTarget: QuickTargetClaim? = null,
    viewModel: AddToPlaylistViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val watchLaterIds by viewModel.watchLaterIds.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf("") }
    val cleanUrl = permanentUrl.trim()

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
                    text = "Save \"$title\" to…",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                quickTarget?.let { q ->
                    val inLater = watchLaterIds.contains(q.claimId)
                    val inFav = favoriteIds.contains(q.claimId)
                    QuickToggleRow(
                        label = if (inLater) "Saved to Watch Later" else "Watch Later",
                        checked = inLater,
                        iconChecked = Icons.Filled.Bookmark,
                        iconUnchecked = Icons.Outlined.BookmarkBorder,
                        onClick = { viewModel.toggleWatchLater(q) },
                    )
                    QuickToggleRow(
                        label = if (inFav) "Favorited" else "Favorites",
                        checked = inFav,
                        iconChecked = Icons.Filled.Favorite,
                        iconUnchecked = Icons.Outlined.FavoriteBorder,
                        onClick = { viewModel.toggleFavorite(q) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }

                if (playlists.isEmpty()) {
                    Text(
                        text = "You don't have any playlists yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(playlists, key = { it.id }) { p ->
                            val alreadyIn = p.itemUrls.contains(cleanUrl)
                            PlaylistToggleRow(
                                name = p.name,
                                count = p.itemUrls.size,
                                thumbnailUrl = p.thumbnailUrl,
                                checked = alreadyIn,
                                onClick = { viewModel.toggle(p.id, cleanUrl, alreadyIn) },
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }

                if (creating) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        TextField(
                            value = draftName,
                            onValueChange = { draftName = it },
                            placeholder = {
                                Text("New playlist name", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { creating = false; draftName = "" }) { Text("Cancel") }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (draftName.isNotBlank()) {
                                        viewModel.createAndAdd(draftName.trim(), cleanUrl)
                                        creating = false
                                        draftName = ""
                                        onDismiss()
                                    }
                                },
                                enabled = draftName.isNotBlank(),
                            ) { Text("Create & add", color = Color.White, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { creating = true }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlaylistAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "Create new playlist",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onCreateNew()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Spacer(Modifier.size(20.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "Open full editor…",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun QuickToggleRow(
    label: String,
    checked: Boolean,
    iconChecked: androidx.compose.ui.graphics.vector.ImageVector,
    iconUnchecked: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (checked) iconChecked else iconUnchecked,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (checked) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "In",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PlaylistToggleRow(
    name: String,
    count: Int,
    thumbnailUrl: String?,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).aspectRatio(16f / 9f),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$count ${if (count == 1) "video" else "videos"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (checked) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "In playlist",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
