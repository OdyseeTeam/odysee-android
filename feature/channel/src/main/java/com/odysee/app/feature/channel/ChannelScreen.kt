package com.odysee.app.feature.channel

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.odysee.app.core.data.player.CurrentMedia
import com.odysee.app.core.designsystem.comments.OdyseeComment
import com.odysee.app.core.designsystem.comments.OdyseeCommentActions
import com.odysee.app.core.designsystem.comments.OdyseeCommentActionsSheet
import com.odysee.app.core.designsystem.comments.OdyseeCommentRow
import com.odysee.app.core.designsystem.comments.OdyseeCommentThread
import com.odysee.app.core.designsystem.comments.OdyseeReaction
import com.odysee.app.core.designsystem.comments.RichCommentBody
import com.odysee.app.core.model.Channel
import androidx.compose.foundation.shape.CircleShape as ComposeCircleShape
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private enum class ChannelTab(val label: String, val ownerOnly: Boolean = false) {
    Content("Content"),
    Shorts("Shorts"),
    Playlists("Playlists"),
    Channels("Channels"),
    Discussion("Discussion"),
    About("About"),
    Settings("Settings", ownerOnly = true),
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChannelScreen(
    viewModel: ChannelViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onWatch: (CurrentMedia) -> Unit,
    onVisitChannel: (claimId: String, name: String) -> Unit = { _, _ -> },
    onEditChannel: (claimId: String, name: String) -> Unit = { _, _ -> },
    onOpenModeration: (claimId: String, name: String) -> Unit = { _, _ -> },
    onOpenFeaturedChannels: (claimId: String, name: String) -> Unit = { _, _ -> },
    onOpenDiscussionSettings: (claimId: String, name: String) -> Unit = { _, _ -> },
    onOpenFeaturedContent: (claimId: String, name: String) -> Unit = { _, _ -> },
    onOpenCreatorMemberships: (claimId: String, name: String) -> Unit = { _, _ -> },
    onOpenAnalytics: (claimId: String, name: String) -> Unit = { _, _ -> },
    onPlayClaimBackground: (CurrentMedia) -> Unit = {},
    onPlayClaimPip: (CurrentMedia) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)

    var selectedTab by rememberSaveable { mutableStateOf(ChannelTab.Content) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(selectedTab, listState) {
        when (selectedTab) {
            ChannelTab.Shorts -> if (state.shorts.isEmpty() && !state.isLoadingShorts) viewModel.loadShorts()
            ChannelTab.Playlists -> if (state.playlists.isEmpty() && !state.isLoadingPlaylists) viewModel.loadChannelPlaylists()
            ChannelTab.Channels -> if (!state.featuredChannelsLoaded && !state.isLoadingFeaturedChannels) viewModel.loadFeaturedChannels()
            else -> Unit
        }
    }

    androidx.compose.runtime.LaunchedEffect(listState, selectedTab) {
        kotlinx.coroutines.flow.combine(
            androidx.compose.runtime.snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 },
            androidx.compose.runtime.snapshotFlow { listState.layoutInfo.totalItemsCount },
        ) { last, total -> last to total }
            .collect { (last, total) ->
                if (total > 0 && last >= total - 4) {
                    when (selectedTab) {
                        ChannelTab.Content -> viewModel.loadMoreVideos()
                        ChannelTab.Shorts -> viewModel.loadMoreShorts()
                        else -> Unit
                    }
                }
            }
    }
    val visibleTabs = ChannelTab.entries.filter { !it.ownerOnly || state.isOwnChannel }

    val videoToMedia: (ChannelVideoUi) -> CurrentMedia = { video ->
        CurrentMedia(
            claimId = video.id,
            permanentUrl = video.permanentUrl,
            title = video.title,
            description = video.description,
            channelClaimId = state.claimId,
            channelName = state.displayName,
            channelInitial = (state.displayName.firstOrNull { it.isLetterOrDigit() } ?: 'O')
                .uppercaseChar(),
            channelAvatarUrl = state.channel?.thumbnailUrl,
            thumbnailUrl = video.thumbnailUrl,
            ageLabel = video.ageLabel.takeIf { it.isNotEmpty() },
        )
    }
    val onVideoClick: (ChannelVideoUi) -> Unit = { video -> onWatch(videoToMedia(video)) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var menuOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var menuTarget by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<OdyseeComment?>(null) }
    var claimMenuTarget by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ChannelVideoUi?>(null) }
    var addToPlaylistTarget by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ChannelVideoUi?>(null) }
    var repostTarget by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ChannelVideoUi?>(null) }
    var repostChannelTarget by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Pair<String, String>?>(null) }
    val clipboardMgr = context.getSystemService(android.content.ClipboardManager::class.java)
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = state.channel?.title?.takeIf { it.isNotBlank() }
                            ?: state.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = {
                        val url = "https://odysee.com/${state.displayName}:${state.claimId}"
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, state.channel?.title ?: state.displayName)
                            putExtra(android.content.Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        val channelUrl = "https://odysee.com/${state.displayName}:${state.claimId}"
                        DropdownMenuItem(
                            text = { Text("Copy link") },
                            onClick = {
                                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                                clipboard?.setPrimaryClip(
                                    android.content.ClipData.newPlainText("Channel URL", channelUrl),
                                )
                                menuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Open in browser") },
                            onClick = {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(channelUrl),
                                )
                                context.startActivity(intent)
                                menuOpen = false
                            },
                        )
                        if (state.isOwnChannel) {
                            DropdownMenuItem(
                                text = { Text("Edit channel") },
                                onClick = {
                                    state.channel?.let { onEditChannel(it.claimId, it.name) }
                                    menuOpen = false
                                },
                            )
                        }
                        if (!state.isOwnChannel) {
                            DropdownMenuItem(
                                text = { Text("Repost channel") },
                                onClick = {
                                    repostChannelTarget = state.claimId to state.displayName
                                    menuOpen = false
                                },
                            )
                        }
                        if (!state.isOwnChannel) {
                            DropdownMenuItem(
                                text = { Text(if (state.isBlocked) "Unblock channel" else "Block channel") },
                                onClick = {
                                    viewModel.toggleBlock()
                                    menuOpen = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Report content") },
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(
                                            "https://odysee.com/\$/report_content?claimId=${state.claimId}",
                                        ),
                                    )
                                    context.startActivity(intent)
                                    menuOpen = false
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item(key = "header") {
                ChannelHeader(
                    channel = state.channel,
                    displayName = state.displayName,
                    isOwnChannel = state.isOwnChannel,
                    isSubscribed = state.isSubscribed,
                    followerCount = state.followerCount,
                    onToggleSubscription = viewModel::toggleSubscription,
                )
            }
            stickyHeader(key = "tabs") {
                val safeSelected = if (selectedTab in visibleTabs) selectedTab else ChannelTab.Content
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column {
                        ScrollableTabRow(
                            selectedTabIndex = visibleTabs.indexOf(safeSelected),
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            edgePadding = 12.dp,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(
                                        tabPositions[visibleTabs.indexOf(safeSelected)],
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            },
                            divider = {},
                        ) {
                            visibleTabs.forEach { tab ->
                                Tab(
                                    selected = safeSelected == tab,
                                    onClick = { selectedTab = tab },
                                    text = {
                                        Text(
                                            text = tab.label,
                                            fontWeight = if (safeSelected == tab) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    },
                                    selectedContentColor = MaterialTheme.colorScheme.onBackground,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
            val activeTab = if (selectedTab in visibleTabs) selectedTab else ChannelTab.Content
            when (activeTab) {
                ChannelTab.Content -> videosTab(
                    state = state,
                    onRetry = viewModel::loadVideos,
                    onVideoClick = onVideoClick,
                    onVideoLongPress = { claimMenuTarget = it },
                )
                ChannelTab.Shorts -> shortsTab(
                    state = state,
                    onRetry = viewModel::loadShorts,
                    onVideoClick = onVideoClick,
                    onVideoLongPress = { claimMenuTarget = it },
                )
                ChannelTab.Playlists -> playlistsTab(state = state, onRetry = viewModel::loadChannelPlaylists)
                ChannelTab.Channels -> channelsTab(
                    state = state,
                    onChannelClick = onVisitChannel,
                )
                ChannelTab.Discussion -> {
                    if (state.discussion is DiscussionState.Idle) viewModel.loadDiscussion()
                    discussionTab(
                        state = state.discussion,
                        isClaimOwner = state.isOwnChannel,
                        onRetry = viewModel::loadDiscussion,
                        onLongPress = { menuTarget = it },
                        onLike = { viewModel.reactToDiscussionComment(it, OdyseeReaction.Like) },
                        onDislike = { viewModel.reactToDiscussionComment(it, OdyseeReaction.Dislike) },
                        onLoadReplies = viewModel::loadDiscussionReplies,
                    )
                }
                ChannelTab.About -> aboutTab(
                    channel = state.channel,
                    isLoading = state.isLoadingChannel,
                    followedTags = state.followedTags,
                    onToggleTag = viewModel::toggleTag,
                )
                ChannelTab.Settings -> settingsTab(
                    channel = state.channel,
                    onEditChannel = {
                        state.channel?.let { onEditChannel(it.claimId, it.name) }
                    },
                    onOpenModeration = {
                        state.channel?.let { onOpenModeration(it.claimId, it.name) }
                    },
                    onOpenFeaturedChannels = {
                        state.channel?.let { onOpenFeaturedChannels(it.claimId, it.name) }
                    },
                    onOpenDiscussionSettings = {
                        state.channel?.let { onOpenDiscussionSettings(it.claimId, it.name) }
                    },
                    onOpenFeaturedContent = {
                        state.channel?.let { onOpenFeaturedContent(it.claimId, it.name) }
                    },
                    onOpenCreatorMemberships = {
                        state.channel?.let { onOpenCreatorMemberships(it.claimId, it.name) }
                    },
                    onOpenAnalytics = {
                        state.channel?.let { onOpenAnalytics(it.claimId, it.name) }
                    },
                )
            }
        }
    }

    claimMenuTarget?.let { target ->
        val media = videoToMedia(target)
        com.odysee.app.core.designsystem.claims.OdyseeClaimMenuSheet(
            target = com.odysee.app.core.designsystem.claims.OdyseeClaimMenuTarget(
                claimId = target.id,
                name = target.title,
                title = target.title,
                permanentUrl = target.permanentUrl,
                channelClaimId = state.claimId,
                channelName = state.displayName,
            ),
            actions = com.odysee.app.core.designsystem.claims.OdyseeClaimMenuActions(
                onPlayBackground = { onPlayClaimBackground(media) },
                onPlayPip = { onPlayClaimPip(media) },
                onSaveWatchLater = { viewModel.saveVideoToWatchLater(target) },
                onSaveFavorite = { viewModel.saveVideoToFavorites(target) },
                onAddToPlaylist = { addToPlaylistTarget = target },
                onRepost = { repostTarget = target },
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
                    val link = "https://odysee.com/\$/report-content?claimId=${target.id}"
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

    repostTarget?.let { target ->
        com.odysee.app.feature.library.RepostSheet(
            claimId = target.id,
            claimName = target.title,
            onDismiss = { repostTarget = null },
            onPosted = { repostTarget = null },
        )
    }

    repostChannelTarget?.let { (cid, name) ->
        com.odysee.app.feature.library.RepostSheet(
            claimId = cid,
            claimName = name,
            onDismiss = { repostChannelTarget = null },
            onPosted = { repostChannelTarget = null },
        )
    }

    addToPlaylistTarget?.let { target ->
        com.odysee.app.feature.library.AddToPlaylistSheet(
            title = target.title,
            permanentUrl = target.permanentUrl,
            onDismiss = { addToPlaylistTarget = null },
            onCreateNew = { addToPlaylistTarget = null },
            quickTarget = com.odysee.app.feature.library.QuickTargetClaim(
                claimId = target.id,
                permanentUrl = target.permanentUrl,
                title = target.title,
                channelName = state.displayName,
                channelClaimId = state.claimId,
                thumbnailUrl = target.thumbnailUrl,
            ),
        )
    }

    menuTarget?.let { target ->
        OdyseeCommentActionsSheet(
            comment = target,
            isClaimOwner = state.isOwnChannel,
            canReply = false,
            onDismiss = { menuTarget = null },
            actions = OdyseeCommentActions(
                onVisitChannel = {
                    target.authorChannelId?.let { onVisitChannel(it, target.authorHandle) }
                    menuTarget = null
                },
                onCopyText = {
                    clipboardMgr?.setPrimaryClip(
                        android.content.ClipData.newPlainText("Comment", target.body),
                    )
                    menuTarget = null
                },
                onCopyLink = {
                    clipboardMgr?.setPrimaryClip(
                        android.content.ClipData.newPlainText(
                            "Comment link",
                            "https://odysee.com/?lc=${target.id}",
                        ),
                    )
                    menuTarget = null
                },
                onBlock = {
                    viewModel.blockDiscussionCommentAuthor(target.id)
                    menuTarget = null
                },
                onReport = {
                    val link = "https://odysee.com/\$/report-content?commentId=${target.id}"
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(link),
                            ),
                        )
                    }
                    menuTarget = null
                },
            ),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.videosTab(
    state: ChannelUiState,
    onRetry: () -> Unit,
    onVideoClick: (ChannelVideoUi) -> Unit,
    onVideoLongPress: (ChannelVideoUi) -> Unit,
) {
    when {
        state.isLoadingVideos && state.videos.isEmpty() ->
            item(key = "videos_loading") { CenteredLoadingItem() }
        state.videosError != null && state.videos.isEmpty() ->
            item(key = "videos_error") { CenteredErrorItem(message = state.videosError, onRetry = onRetry) }
        state.videos.isEmpty() ->
            item(key = "videos_empty") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No videos yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        else -> {
            item { Spacer(Modifier.height(16.dp)) }
            items(state.videos, key = { it.id }) { video ->
                ChannelVideoRow(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onLongPress = { onVideoLongPress(video) },
                )
                Spacer(Modifier.height(20.dp))
            }
            if (state.isLoadingMoreVideos) {
                item(key = "videos_more_loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.shortsTab(
    state: ChannelUiState,
    onRetry: () -> Unit,
    onVideoClick: (ChannelVideoUi) -> Unit,
    onVideoLongPress: (ChannelVideoUi) -> Unit,
) {
    when {
        state.isLoadingShorts && state.shorts.isEmpty() ->
            item(key = "shorts_loading") { CenteredLoadingItem() }
        state.shortsError != null && state.shorts.isEmpty() ->
            item(key = "shorts_error") { CenteredErrorItem(message = state.shortsError, onRetry = onRetry) }
        state.shorts.isEmpty() ->
            item(key = "shorts_empty") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No shorts yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        else -> {
            item { Spacer(Modifier.height(16.dp)) }
            items(state.shorts, key = { it.id }) { video ->
                ChannelVideoRow(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onLongPress = { onVideoLongPress(video) },
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.channelsTab(
    state: ChannelUiState,
    onChannelClick: (String, String) -> Unit,
) {
    when {
        state.isLoadingFeaturedChannels && state.featuredChannels.isEmpty() ->
            item(key = "channels_loading") { CenteredLoadingItem() }
        state.featuredChannels.isEmpty() ->
            item(key = "channels_empty") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No featured channels.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        else -> {
            item { Spacer(Modifier.height(12.dp)) }
            items(state.featuredChannels, key = { it.claimId }) { ch ->
                FeaturedChannelRow(channel = ch, onClick = { onChannelClick(ch.claimId, ch.name) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FeaturedChannelRow(channel: Channel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(ComposeCircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!channel.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = (channel.title ?: channel.name).firstOrNull()?.uppercase() ?: "O",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.title?.takeIf { it.isNotBlank() } ?: channel.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = channel.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.playlistsTab(
    state: ChannelUiState,
    onRetry: () -> Unit,
) {
    when {
        state.isLoadingPlaylists && state.playlists.isEmpty() ->
            item(key = "playlists_loading") { CenteredLoadingItem() }
        state.playlistsError != null && state.playlists.isEmpty() ->
            item(key = "playlists_error") {
                CenteredErrorItem(message = state.playlistsError, onRetry = onRetry)
            }
        state.playlists.isEmpty() ->
            item(key = "playlists_empty") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No playlists yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        else -> {
            item { Spacer(Modifier.height(12.dp)) }
            items(state.playlists, key = { it.claimId }) { p ->
                ChannelPlaylistRow(playlist = p)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ChannelPlaylistRow(playlist: ChannelPlaylistUi) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!playlist.thumbnailUrl.isNullOrBlank()) {
                coil3.compose.AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = "${playlist.itemCount} videos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.discussionTab(
    state: DiscussionState,
    isClaimOwner: Boolean,
    onRetry: () -> Unit,
    onLongPress: (OdyseeComment) -> Unit,
    onLike: (String) -> Unit,
    onDislike: (String) -> Unit,
    onLoadReplies: (String) -> Unit,
) {
    when (state) {
        DiscussionState.Idle, DiscussionState.Loading -> item("discussion_loading") { CenteredLoadingItem() }
        is DiscussionState.Error -> item("discussion_error") {
            CenteredErrorItem(message = state.message, onRetry = onRetry)
        }
        is DiscussionState.Success -> {
            if (state.comments.isEmpty()) {
                item("discussion_empty") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No discussion yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(state.comments, key = { it.id }) { comment ->
                    OdyseeCommentThread(
                        comment = comment,
                        replies = state.repliesByParent[comment.id],
                        actions = OdyseeCommentActions(
                            onLike = { onLike(comment.id) },
                            onDislike = { onDislike(comment.id) },
                        ),
                        replyActionsFor = { reply ->
                            OdyseeCommentActions(
                                onLike = { onLike(reply.id) },
                                onDislike = { onDislike(reply.id) },
                            )
                        },
                        canReply = false,
                        onLoadReplies = { onLoadReplies(comment.id) },
                        onLongPress = onLongPress,
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.settingsTab(
    channel: Channel?,
    onEditChannel: () -> Unit,
    onOpenModeration: () -> Unit,
    onOpenFeaturedChannels: () -> Unit,
    onOpenDiscussionSettings: () -> Unit,
    onOpenFeaturedContent: () -> Unit,
    onOpenCreatorMemberships: () -> Unit,
    onOpenAnalytics: () -> Unit,
) {
    item(key = "settings_content") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Creator settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            SettingsAction(
                label = "Edit channel",
                subtitle = "Title, description, avatar, banner",
                onClick = onEditChannel,
            )
            SettingsAction(
                label = "Featured content",
                subtitle = "Pin claims to your channel",
                onClick = onOpenFeaturedContent,
            )
            SettingsAction(
                label = "Featured channels",
                subtitle = "Showcase channels you support",
                onClick = onOpenFeaturedChannels,
            )
            SettingsAction(
                label = "Memberships",
                subtitle = "Configure tiers and perks",
                onClick = onOpenCreatorMemberships,
            )
            SettingsAction(
                label = "Moderation",
                subtitle = "Manage blocked viewers and mods",
                onClick = onOpenModeration,
            )
            SettingsAction(
                label = "Discussion",
                subtitle = "Comment and reply preferences",
                onClick = onOpenDiscussionSettings,
            )
            SettingsAction(
                label = "Analytics",
                subtitle = "Views, watch time, audience",
                onClick = onOpenAnalytics,
            )
        }
    }
}

@Composable
private fun SettingsAction(label: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.placeholderTab(key: String, message: String) {
    item(key = "tab_placeholder_$key") {
        Box(
            modifier = Modifier.fillMaxWidth().padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.aboutTab(
    channel: Channel?,
    isLoading: Boolean,
    followedTags: Set<String> = emptySet(),
    onToggleTag: (String) -> Unit = {},
) {
    when {
        isLoading -> item(key = "about_loading") { CenteredLoadingItem() }
        channel == null -> item(key = "about_empty") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Channel info unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> item(key = "about_content") {
            ChannelAboutContent(
                channel = channel,
                followedTagsForAbout = followedTags,
                onToggleTag = onToggleTag,
            )
        }
    }
}

@Composable
private fun ChannelAboutContent(
    channel: Channel,
    followedTagsForAbout: Set<String> = emptySet(),
    onToggleTag: (String) -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        channel.description?.takeIf { it.isNotBlank() }?.let { desc ->
            AboutSection(label = "Description") {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        channel.email?.let { email ->
            AboutSection(label = "Contact") {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        runCatching { uriHandler.openUri(if (email.startsWith("mailto:", true)) email else "mailto:$email") }
                    },
                )
            }
        }
        channel.websiteUrl?.let { site ->
            AboutSection(label = "Site") {
                Text(
                    text = site,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        val normalized = if (site.matches(Regex("^(https?:)?//.*", RegexOption.IGNORE_CASE))) site
                        else "https://$site"
                        runCatching { uriHandler.openUri(normalized) }
                    },
                )
            }
        }
        if (channel.tags.isNotEmpty()) {
            AboutSection(label = "Tags") {
                TagFlowRow(tags = channel.tags, followed = followedTagsForAbout, onToggleTag = onToggleTag)
            }
        }
        if (channel.languages.isNotEmpty()) {
            AboutSection(label = "Languages") {
                Text(
                    text = channel.languages.joinToString(" ") { supportedLanguages[it] ?: it },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        channel.claimsInChannel?.takeIf { it > 0 }?.let { uploads ->
            AboutSection(label = "Total Uploads") {
                Text(
                    text = uploads.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        val updatedTs = channel.modifiedAt ?: channel.creationTimestamp
        updatedTs?.let { ts ->
            AboutSection(label = "Last Updated") {
                Text(
                    text = timeAgo(ts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        (channel.canonicalUrl ?: channel.permanentUrl).takeIf { it.isNotBlank() }?.let { url ->
            AboutSection(label = "URL") {
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        AboutSection(label = "Claim ID") {
            Text(
                text = channel.claimId,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        AboutSection(label = "Staked Credits") {
            Text(
                text = formatCredits(channel.stakedAmount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun AboutSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun TagFlowRow(
    tags: List<String>,
    followed: Set<String> = emptySet(),
    onToggleTag: (String) -> Unit = {},
) {
    val spacing = 8.dp
    androidx.compose.ui.layout.Layout(
        content = {
            tags.forEach { tag ->
                val isFollowed = followed.contains(tag.lowercase())
                Surface(
                    color = if (isFollowed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { onToggleTag(tag) },
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isFollowed) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val spacingPx = spacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        var current = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentWidth = 0
        placeables.forEach { p ->
            val next = currentWidth + (if (current.isEmpty()) 0 else spacingPx) + p.width
            if (next > maxWidth && current.isNotEmpty()) {
                rows.add(current)
                current = mutableListOf(p)
                currentWidth = p.width
            } else {
                current.add(p)
                currentWidth = next
            }
        }
        if (current.isNotEmpty()) rows.add(current)
        val height = rows.sumOf { row -> row.maxOf { it.height } } +
            (rows.size - 1).coerceAtLeast(0) * spacingPx
        layout(maxWidth, height) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                row.forEach { p ->
                    p.placeRelative(x, y)
                    x += p.width + spacingPx
                }
                y += row.maxOf { it.height } + spacingPx
            }
        }
    }
}

private fun timeAgo(timestampSeconds: Long): String {
    val nowMs = System.currentTimeMillis()
    val tsMs = if (timestampSeconds > 9_999_999_999L) timestampSeconds else timestampSeconds * 1000L
    val diffMs = (nowMs - tsMs).coerceAtLeast(0L)
    val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "$mins minute${if (mins == 1L) "" else "s"} ago"
        hours < 24 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        days < 30 -> "$days day${if (days == 1L) "" else "s"} ago"
        days < 365 -> {
            val months = days / 30
            "$months month${if (months == 1L) "" else "s"} ago"
        }
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(tsMs))
    }
}

private fun formatCredits(amount: Double): String {
    val fmt = "%.4f".format(amount).trimEnd('0').trimEnd('.')
    return "$fmt LBC"
}

@Composable
private fun ChannelHeader(
    channel: Channel?,
    displayName: String,
    isOwnChannel: Boolean,
    isSubscribed: Boolean,
    followerCount: Long?,
    onToggleSubscription: () -> Unit,
) {
    val coverHeight = 160.dp
    val avatarSize = 88.dp
    val overlap = avatarSize / 3
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(coverHeight),
            ) {
                if (!channel?.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFE50054),
                                        Color(0xFFF77937),
                                    ),
                                ),
                            ),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp + avatarSize + 12.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 16.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel?.title?.takeIf { it.isNotBlank() } ?: displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    followerCount?.let { count ->
                        Text(
                            text = "${formatFollowerCount(count)} followers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (!isOwnChannel) {
                    Button(
                        onClick = onToggleSubscription,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary,
                            contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant
                            else Color.White,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = if (isSubscribed) "Following" else "Follow",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .offset(y = coverHeight - overlap)
                .size(avatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            if (!channel?.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = (displayName.firstOrNull { it.isLetterOrDigit() } ?: 'O')
                        .uppercaseChar()
                        .toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ChannelVideoRow(
    video: ChannelVideoUi,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
) {
    com.odysee.app.core.designsystem.claims.OdyseeClaimCard(
        claim = com.odysee.app.core.designsystem.claims.OdyseeClaimCardModel(
            claimId = video.id,
            title = video.title,
            channelName = "",
            thumbnailUrl = video.thumbnailUrl,
            durationLabel = video.durationLabel,
            ageLabel = video.ageLabel,
            thumbnailTintIndex = video.thumbnailTintIndex,
            paywall = com.odysee.app.core.designsystem.claims.toCardPaywall(video.paywall),
            isPurchased = video.isPurchased,
            isMembersOnly = video.isMembersOnly,
        ),
        onClick = onClick,
        onLongPress = onLongPress,
        showChannelAvatar = false,
    )
}

@Composable
private fun CenteredLoadingItem() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CenteredErrorItem(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Couldn't load",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(8.dp))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

private fun formatFollowerCount(count: Long): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0).removeSuffix(".0M") + "M"
    count >= 1_000 -> "%.1fK".format(count / 1_000.0).removeSuffix(".0K") + "K"
    else -> count.toString()
}.replace(Regex("MM\$"), "M").replace(Regex("KK\$"), "K")

private val thumbnailPalette = listOf(
    Color(0xFF7B2942),
    Color(0xFF2C5364),
    Color(0xFF4A4E69),
    Color(0xFF3D405B),
    Color(0xFF52489C),
    Color(0xFF2A4858),
)

private val supportedLanguages: Map<String, String> = mapOf(
    "en" to "English",
    "es" to "Spanish",
    "fr" to "French",
    "de" to "German",
    "it" to "Italian",
    "pt" to "Portuguese",
    "ru" to "Russian",
    "pl" to "Polish",
    "nl" to "Dutch",
    "sv" to "Swedish",
    "no" to "Norwegian",
    "da" to "Danish",
    "fi" to "Finnish",
    "tr" to "Turkish",
    "ja" to "Japanese",
    "zh" to "Chinese",
    "ko" to "Korean",
    "ar" to "Arabic",
    "hi" to "Hindi",
    "id" to "Indonesian",
    "vi" to "Vietnamese",
    "uk" to "Ukrainian",
    "cs" to "Czech",
    "el" to "Greek",
    "he" to "Hebrew",
    "hu" to "Hungarian",
    "ro" to "Romanian",
    "th" to "Thai",
)
