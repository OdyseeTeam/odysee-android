package com.odysee.app.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncDisabled
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.odysee.app.core.data.collections.CollectionEntry
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchLaterScreen(
    viewModel: WatchLaterViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onWatchPlaylist: (PlaylistWatchTarget) -> Unit,
) {
    val entries by viewModel.items.collectAsStateWithLifecycle()
    CollectionListContent(
        title = "Watch later",
        playlistId = "watchlater",
        entries = entries,
        emptyTitle = "No videos saved",
        emptyMessage = "Tap the bookmark icon on a video to save it for later.",
        onBack = onBack,
        onWatchPlaylist = onWatchPlaylist,
        onRemove = viewModel::remove,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onWatchPlaylist: (PlaylistWatchTarget) -> Unit,
) {
    val entries by viewModel.items.collectAsStateWithLifecycle()
    CollectionListContent(
        title = "Favorites",
        playlistId = "favorites",
        entries = entries,
        emptyTitle = "No favorites yet",
        emptyMessage = "Mark videos as favorite to find them here.",
        onBack = onBack,
        onWatchPlaylist = onWatchPlaylist,
        onRemove = viewModel::remove,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionListContent(
    title: String,
    playlistId: String,
    entries: List<CollectionEntry>,
    emptyTitle: String,
    emptyMessage: String,
    onBack: () -> Unit,
    onWatchPlaylist: (PlaylistWatchTarget) -> Unit,
    onRemove: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardMgr = context.getSystemService(android.content.ClipboardManager::class.java)
    var claimMenuTarget by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<CollectionEntry?>(null)
    }
    var addToPlaylistTarget by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<CollectionEntry?>(null)
    }
    fun buildPlaylistTarget(startIndex: Int): PlaylistWatchTarget = PlaylistWatchTarget(
        playlistId = playlistId,
        playlistName = title,
        startIndex = startIndex.coerceIn(0, (entries.size - 1).coerceAtLeast(0)),
        items = entries.map {
            PlaylistWatchItem(
                claimId = it.claimId,
                permanentUrl = it.permanentUrl,
                title = it.title,
                channelName = it.channelName,
                channelClaimId = it.channelClaimId,
                channelAvatarUrl = null,
                thumbnailUrl = it.thumbnailUrl,
            )
        },
    )
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = emptyTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                item(key = "__header__") {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            text = "${entries.size} ${if (entries.size == 1) "video" else "videos"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        androidx.compose.material3.Button(
                            onClick = { if (entries.isNotEmpty()) onWatchPlaylist(buildPlaylistTarget(0)) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Play all",
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                items(entries.size, key = { entries[it].claimId }) { idx ->
                    val entry = entries[idx]
                    EntryRow(
                        entry = entry,
                        onClick = { onWatchPlaylist(buildPlaylistTarget(idx)) },
                        onRemove = { onRemove(entry.claimId) },
                        onLongPress = { claimMenuTarget = entry },
                    )
                }
            }
        }
    }

    claimMenuTarget?.let { target ->
        com.odysee.app.core.designsystem.claims.OdyseeClaimMenuSheet(
            target = com.odysee.app.core.designsystem.claims.OdyseeClaimMenuTarget(
                claimId = target.claimId,
                name = target.title,
                title = target.title,
                permanentUrl = target.permanentUrl,
                channelClaimId = target.channelClaimId,
                channelName = target.channelName,
            ),
            actions = com.odysee.app.core.designsystem.claims.OdyseeClaimMenuActions(
                onAddToPlaylist = { addToPlaylistTarget = target },
                onShare = {
                    val stripped = target.permanentUrl.removePrefix("lbry://")
                    val url = "https://odysee.com/$stripped"
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, target.title)
                        putExtra(android.content.Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
                },
                onCopyLink = {
                    val stripped = target.permanentUrl.removePrefix("lbry://")
                    clipboardMgr?.setPrimaryClip(
                        android.content.ClipData.newPlainText("Video link", "https://odysee.com/$stripped"),
                    )
                },
                onReport = {
                    val link = "https://odysee.com/\$/report-content?claimId=${target.claimId}"
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link)),
                        )
                    }
                },
            ),
            onDismiss = { claimMenuTarget = null },
        )
    }

    addToPlaylistTarget?.let { target ->
        AddToPlaylistSheet(
            title = target.title,
            permanentUrl = target.permanentUrl,
            onDismiss = { addToPlaylistTarget = null },
            onCreateNew = { addToPlaylistTarget = null },
            quickTarget = QuickTargetClaim(
                claimId = target.claimId,
                permanentUrl = target.permanentUrl,
                title = target.title,
                channelName = target.channelName,
                channelClaimId = target.channelClaimId,
                thumbnailUrl = target.thumbnailUrl,
            ),
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    entry: CollectionEntry,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onLongPress: () -> Unit = {},
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onLongPress()
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!entry.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = entry.thumbnailUrl,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.channelName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenPlaylist: (String) -> Unit = {},
    onPlayPlaylist: (String) -> Unit = {},
    onShufflePlaylist: (String) -> Unit = {},
    onPublishPlaylist: (String) -> Unit = {},
    onAddPlaylist: () -> Unit = {},
    onEditPlaylist: (String) -> Unit = {},
) {
    BackHandler(onBack = onBack)
    val items by viewModel.playlists.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }
    var menuTarget by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<com.odysee.app.core.data.collections.PlaylistSummary?>(null)
    }
    var deleteTarget by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<com.odysee.app.core.data.collections.PlaylistSummary?>(null)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Playlists") },
                actions = {
                    IconButton(onClick = onAddPlaylist) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add playlist",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No playlists yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Your synced collections will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                items(items, key = { it.id }) { p ->
                    PlaylistListRow(
                        playlist = p,
                        onClick = { onOpenPlaylist(p.id) },
                        onPlay = { onPlayPlaylist(p.id) },
                        onLongPress = { menuTarget = p },
                    )
                }
            }
        }
    }

    menuTarget?.let { target ->
        PlaylistActionsSheet(
            target = target,
            onOpen = {
                val id = target.id
                menuTarget = null
                onOpenPlaylist(id)
            },
            onShuffle = {
                val id = target.id
                menuTarget = null
                onShufflePlaylist(id)
            },
            onPublish = {
                val id = target.id
                menuTarget = null
                onPublishPlaylist(id)
            },
            onToggleAutoPublish = {
                val id = target.id
                val enabled = !target.autoPublish
                menuTarget = null
                viewModel.setAutoPublish(id, enabled)
            },
            onEdit = {
                val id = target.id
                menuTarget = null
                onEditPlaylist(id)
            },
            onDelete = {
                deleteTarget = target
                menuTarget = null
            },
            onDismiss = { menuTarget = null },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete playlist?") },
            text = { Text("Remove \"${target.name}\"? This deletes it from your account.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target.id)
                    deleteTarget = null
                }) { Text("Delete", color = Color(0xFFE2202D)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PlaylistActionsSheet(
    target: com.odysee.app.core.data.collections.PlaylistSummary,
    onOpen: () -> Unit,
    onShuffle: () -> Unit,
    onPublish: () -> Unit,
    onToggleAutoPublish: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hasItems = target.itemUrls.isNotEmpty()
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
                Text(
                    text = target.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PlaylistActionRow(Icons.Outlined.Visibility, "Open", onClick = onOpen)
                if (hasItems) {
                    PlaylistActionRow(Icons.Outlined.Shuffle, "Shuffle Play", onClick = onShuffle)
                }
                if (hasItems) {
                    PlaylistActionRow(
                        icon = Icons.Outlined.Publish,
                        label = if (target.isPublic) "Update" else "Publish",
                        iconTint = Color(0xFFE50054),
                        onClick = onPublish,
                    )
                }
                if (target.isPublic) {
                    PlaylistActionRow(
                        icon = if (target.autoPublish) Icons.Outlined.Sync else Icons.Outlined.SyncDisabled,
                        label = if (target.autoPublish) "Disable Auto-publish" else "Enable Auto-publish",
                        onClick = onToggleAutoPublish,
                    )
                }
                PlaylistActionRow(Icons.Outlined.Edit, "Edit", onClick = onEdit)
                PlaylistActionRow(
                    icon = Icons.Outlined.Delete,
                    label = "Delete",
                    iconTint = Color(0xFFE2202D),
                    labelColor = Color(0xFFE2202D),
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun PlaylistActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.onBackground,
    labelColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = labelColor)
    }
}

@Composable
fun PlaylistsTabContent(
    topBar: @Composable () -> Unit,
    onWatchHistory: () -> Unit = {},
    onWatchLater: () -> Unit = {},
    onFavorites: () -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
    onPlayPlaylist: (String) -> Unit = {},
    onShufflePlaylist: (String) -> Unit = {},
    onPublishPlaylist: (String) -> Unit = {},
    onAddPlaylist: () -> Unit = {},
    onEditPlaylist: (String) -> Unit = {},
    hazeState: HazeState? = null,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }
    PlaylistsScreenInternal(
        topBar = topBar,
        hazeState = hazeState,
        onWatchHistory = onWatchHistory,
        onWatchLater = onWatchLater,
        onFavorites = onFavorites,
        playlists = playlists,
        onOpenPlaylist = onOpenPlaylist,
        onPlayPlaylist = onPlayPlaylist,
        onShufflePlaylist = onShufflePlaylist,
        onPublishPlaylist = onPublishPlaylist,
        onAddPlaylist = onAddPlaylist,
        onEditPlaylist = onEditPlaylist,
        onDeletePlaylist = viewModel::delete,
        onToggleAutoPublish = viewModel::setAutoPublish,
    )
}

@Composable
private fun PlaylistsScreenInternal(
    topBar: @Composable () -> Unit,
    hazeState: HazeState? = null,
    onWatchHistory: () -> Unit = {},
    onWatchLater: () -> Unit = {},
    onFavorites: () -> Unit = {},
    playlists: List<com.odysee.app.core.data.collections.PlaylistSummary> = emptyList(),
    onOpenPlaylist: (String) -> Unit = {},
    onPlayPlaylist: (String) -> Unit = {},
    onShufflePlaylist: (String) -> Unit = {},
    onPublishPlaylist: (String) -> Unit = {},
    onAddPlaylist: () -> Unit = {},
    onEditPlaylist: (String) -> Unit = {},
    onDeletePlaylist: (String) -> Unit = {},
    onToggleAutoPublish: (String, Boolean) -> Unit = { _, _ -> },
) {
    var menuTarget by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<com.odysee.app.core.data.collections.PlaylistSummary?>(null)
    }
    var deleteTarget by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<com.odysee.app.core.data.collections.PlaylistSummary?>(null)
    }
    Scaffold(
        topBar = topBar,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .let { m -> if (hazeState != null) m.hazeSource(state = hazeState) else m }
                .padding(padding),
        ) {
            item("history") {
                LibraryRow(
                    iconRes = com.odysee.app.core.designsystem.R.drawable.ic_menu_watch_history,
                    label = "Watch History",
                    onClick = onWatchHistory,
                )
            }
            item("watch_later") {
                LibraryRow(
                    iconRes = com.odysee.app.core.designsystem.R.drawable.ic_menu_time,
                    label = "Watch Later",
                    onClick = onWatchLater,
                )
            }
            item("favorites") {
                LibraryRow(
                    iconRes = com.odysee.app.core.designsystem.R.drawable.ic_menu_star,
                    label = "Favorites",
                    onClick = onFavorites,
                )
            }
            item("divider") {
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            item("my_playlists_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            id = com.odysee.app.core.designsystem.R.drawable.ic_menu_playlist,
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "My Playlists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onAddPlaylist) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add playlist",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (playlists.isEmpty()) {
                item("playlists_empty") {
                    Text(
                        text = "No playlists yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(playlists, key = { it.id }) { p ->
                    PlaylistListRow(
                        playlist = p,
                        onClick = { onOpenPlaylist(p.id) },
                        onPlay = { onPlayPlaylist(p.id) },
                        onLongPress = { menuTarget = p },
                    )
                }
            }
            item("bottom_spacer") {
                Spacer(modifier = Modifier.height(140.dp))
            }
        }
    }

    menuTarget?.let { target ->
        PlaylistActionsSheet(
            target = target,
            onOpen = {
                val id = target.id
                menuTarget = null
                onOpenPlaylist(id)
            },
            onShuffle = {
                val id = target.id
                menuTarget = null
                onShufflePlaylist(id)
            },
            onPublish = {
                val id = target.id
                menuTarget = null
                onPublishPlaylist(id)
            },
            onToggleAutoPublish = {
                val id = target.id
                val enabled = !target.autoPublish
                menuTarget = null
                onToggleAutoPublish(id, enabled)
            },
            onEdit = {
                val id = target.id
                menuTarget = null
                onEditPlaylist(id)
            },
            onDelete = {
                deleteTarget = target
                menuTarget = null
            },
            onDismiss = { menuTarget = null },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete playlist?") },
            text = { Text("Remove \"${target.name}\"? This only deletes it on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePlaylist(target.id)
                    deleteTarget = null
                }) { Text("Delete", color = Color(0xFFE2202D)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PlaylistListRow(
    playlist: com.odysee.app.core.data.collections.PlaylistSummary,
    onClick: () -> Unit,
    onPlay: () -> Unit = {},
    onLongPress: () -> Unit = {},
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(
                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                    )
                    onLongPress()
                },
            )
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!playlist.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (playlist.isPublic) Icons.Outlined.Public else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (playlist.isPublic) "Public" else "Private",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${playlist.itemUrls.size} ${if (playlist.itemUrls.size == 1) "video" else "videos"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(
            onClick = onPlay,
            enabled = playlist.itemUrls.isNotEmpty(),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = if (playlist.itemUrls.isEmpty())
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LibraryRow(iconRes: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
