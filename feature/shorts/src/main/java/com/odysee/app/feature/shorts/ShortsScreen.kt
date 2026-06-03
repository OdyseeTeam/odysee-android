package com.odysee.app.feature.shorts

import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.odysee.app.core.data.reactions.MyReaction
import com.odysee.app.core.designsystem.R as DesignR
import kotlinx.coroutines.launch

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ShortsScreen(
    viewModel: ShortsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onChannelClick: (String, String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)

    val pagerState = rememberPagerState(pageCount = { state.items.size })
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onPageChanged(page)
        }
    }
    // Wire autoplay-next: when the current short ends and autoplay is on, advance the pager.
    LaunchedEffect(pagerState, state.items.size) {
        viewModel.setAdvanceCallback {
            val next = pagerState.currentPage + 1
            if (next < state.items.size) {
                scope.launch { pagerState.animateScrollToPage(next) }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.pauseExternal()
                Lifecycle.Event.ON_RESUME -> viewModel.resumeExternal()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentItem = state.items.getOrNull(pagerState.currentPage)
    val hasPrevious = pagerState.currentPage > 0
    val hasNext = pagerState.currentPage < state.items.size - 1

    var repostTarget by remember { mutableStateOf<ShortUi?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (state.items.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val item = state.items[page]
                val reactions = state.reactionsByClaim[item.claimId]
                val isSubscribed = item.channelClaimId?.let { it in state.subscribedChannelIds } ?: false
                ShortPage(
                    item = item,
                    isCurrent = page == pagerState.currentPage,
                    isPlaying = state.isPlaying,
                    isMuted = state.isMuted,
                    viewModel = viewModel,
                    onRepost = { repostTarget = item },
                    likes = reactions?.likes ?: 0,
                    dislikes = reactions?.dislikes ?: 0,
                    myReaction = reactions?.myReaction ?: MyReaction.NONE,
                    isSubscribed = isSubscribed,
                    autoplayNextShort = state.autoplayNextShort,
                    onPrevious = if (hasPrevious) {
                        { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
                    } else null,
                    onNext = if (hasNext) {
                        { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
                    } else null,
                    onChannelClick = {
                        item.channelClaimId?.let { id -> onChannelClick(id, item.channelName) }
                    },
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .zIndex(10f)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close",
                tint = Color.White,
            )
        }
        // Top-center: Related / From [Channel] view-mode toggle.
        val currentChannelName = currentItem?.channelTitle?.takeIf { it.isNotBlank() }
            ?: currentItem?.channelName.orEmpty()
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
                .zIndex(10f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ViewModePill(
                label = "Related",
                active = state.viewMode == ShortsViewMode.RELATED,
                onClick = { viewModel.setViewMode(ShortsViewMode.RELATED) },
            )
            if (currentChannelName.isNotBlank()) {
                val short = if (currentChannelName.length > 15) currentChannelName.take(15) + "…" else currentChannelName
                ViewModePill(
                    label = "From $short",
                    active = state.viewMode == ShortsViewMode.CHANNEL,
                    onClick = { viewModel.setViewMode(ShortsViewMode.CHANNEL) },
                )
            }
        }
        if (state.showInfo && currentItem != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setShowInfo(false) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                ShortsInfoSheet(
                    short = currentItem,
                    viewModel = viewModel,
                    onChannelClick = onChannelClick,
                )
            }
        }
        if (state.showComments && currentItem != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setShowComments(false) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                ShortsCommentsSheet(claimId = currentItem.claimId, viewModel = viewModel)
            }
        }
        repostTarget?.let { target ->
            com.odysee.app.feature.library.RepostSheet(
                claimId = target.claimId,
                claimName = target.title,
                onDismiss = { repostTarget = null },
                onPosted = { repostTarget = null },
            )
        }
    }
}

@Composable
private fun ShortsInfoSheet(
    short: ShortUi,
    viewModel: ShortsViewModel,
    onChannelClick: (String, String) -> Unit,
) {
    val followerCountState = androidx.compose.runtime.remember(short.channelClaimId) {
        androidx.compose.runtime.mutableStateOf<Long?>(null)
    }
    androidx.compose.runtime.LaunchedEffect(short.channelClaimId) {
        val cid = short.channelClaimId ?: return@LaunchedEffect
        followerCountState.value = viewModel.fetchFollowerCount(cid)
    }
    val followerCount = followerCountState.value
    val isSubscribed = short.channelClaimId?.let { id ->
        viewModel.state.collectAsStateWithLifecycle().value.subscribedChannelIds.contains(id)
    } ?: false
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = short.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        short.releaseTime?.let { rt ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatRelativeTime(rt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        short.channelClaimId?.let { onChannelClick(it, short.channelName) }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!short.channelAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = short.channelAvatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = short.channelInitial.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = short.channelTitle?.takeIf { it.isNotBlank() } ?: short.channelName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (short.channelTitle != null && short.channelTitle != short.channelName) {
                        Text(
                            text = short.channelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    followerCount?.let { count ->
                        Text(
                            text = "${formatFollowerCountShort(count)} followers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            short.channelClaimId?.let { cid ->
                Spacer(Modifier.width(12.dp))
                val canSubscribe = viewModel.state.collectAsStateWithLifecycle().value.isSignedIn
                val bg = when {
                    !canSubscribe -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    isSubscribed -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.primary
                }
                val fg = when {
                    !canSubscribe -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    isSubscribed -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> Color.White
                }
                var showUnfollowConfirm by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(bg)
                        .clickable(enabled = canSubscribe) {
                            if (isSubscribed) showUnfollowConfirm = true
                            else viewModel.toggleSubscribe(cid, short.channelName)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = if (isSubscribed) "Following" else "Follow",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = fg,
                    )
                }
                if (showUnfollowConfirm) {
                    val name = short.channelTitle?.takeIf { it.isNotBlank() } ?: short.channelName
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showUnfollowConfirm = false },
                        title = { Text("Unfollow $name?") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showUnfollowConfirm = false
                                viewModel.toggleSubscribe(cid, short.channelName)
                            }) { Text("Unfollow") }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showUnfollowConfirm = false
                            }) { Text("Cancel") }
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val reactions = viewModel.state.collectAsStateWithLifecycle().value
            .reactionsByClaim[short.claimId]
        val likes = reactions?.likes ?: 0L
        val dislikes = reactions?.dislikes ?: 0L
        val myReaction = reactions?.myReaction ?: MyReaction.NONE
        ShortsInfoReactionRow(
            likes = likes,
            dislikes = dislikes,
            myReaction = myReaction,
            onLike = { viewModel.toggleLike(short.claimId) },
            onDislike = { viewModel.toggleDislike(short.claimId) },
        )
        Spacer(Modifier.height(12.dp))
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val inWatchLater = short.claimId in viewModel.watchLaterIds.collectAsStateWithLifecycle().value
        val inFavorites = short.claimId in viewModel.favoriteIds.collectAsStateWithLifecycle().value
        ShortsInfoActionRow(
            inWatchLater = inWatchLater,
            inFavorites = inFavorites,
            onWatchLater = { viewModel.toggleWatchLater(short) },
            onFavorite = { viewModel.toggleFavorite(short) },
            onShare = {
                val stripped = short.permanentUrl.removePrefix("lbry://")
                val url = "https://odysee.com/$stripped"
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, short.title)
                    putExtra(android.content.Intent.EXTRA_TEXT, url)
                }
                ctx.startActivity(android.content.Intent.createChooser(intent, "Share via"))
            },
        )
        if (!short.description.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Description",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = short.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (short.tags.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Tags",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = short.tags.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Claim ID",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = short.claimId,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun formatRelativeTime(unixSeconds: Long): String {
    if (unixSeconds <= 0) return ""
    val now = System.currentTimeMillis() / 1000
    val delta = (now - unixSeconds).coerceAtLeast(0)
    return when {
        delta < 60 -> "Just now"
        delta < 3600 -> "${delta / 60}m ago"
        delta < 86_400 -> "${delta / 3600}h ago"
        delta < 30 * 86_400 -> "${delta / 86_400}d ago"
        delta < 365 * 86_400L -> "${delta / (30 * 86_400)}mo ago"
        else -> "${delta / (365 * 86_400L)}y ago"
    }
}

private fun formatFollowerCountShort(count: Long): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0).removeSuffix(".0M") + "M"
    count >= 1_000 -> "%.1fK".format(count / 1_000.0).removeSuffix(".0K") + "K"
    else -> count.toString()
}.replace(Regex("MM\$"), "M").replace(Regex("KK\$"), "K")

@Composable
private fun ShortsCommentsSheet(claimId: String, viewModel: ShortsViewModel) {
    val commentsFlow = remember(claimId) { viewModel.commentsFor(claimId) }
    val comments by commentsFlow.collectAsStateWithLifecycle(initialValue = null)
    var commentText by remember(claimId) { mutableStateOf("") }
    var posting by remember(claimId) { mutableStateOf(false) }
    val canPost by viewModel.canComment.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxWidth().height(560.dp).padding(16.dp)) {
        Text(
            text = "Comments",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        val list = comments
        when {
            list == null -> Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) { androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            list.isEmpty() -> Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No comments yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(modifier = Modifier.weight(1f)) {
                items(items = list, key = { it.commentId }) { c ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = c.authorName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = c.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        com.odysee.app.core.designsystem.comments.OdyseeCommentComposer(
            draft = commentText,
            onDraftChange = { commentText = it },
            onSubmit = {
                if (canPost && commentText.isNotBlank()) {
                    posting = true
                    viewModel.postComment(claimId, commentText.trim()) { ok ->
                        posting = false
                        if (ok) commentText = ""
                    }
                }
            },
            placeholder = if (canPost) "Add a comment…" else "Sign in to comment",
            enabled = canPost && !posting,
            maxLength = com.odysee.app.core.designsystem.comments.ODYSEE_MAX_CHARS_COMMENT,
        )
    }
}

@Composable
private fun ShortsInfoReactionRow(
    likes: Long,
    dislikes: Long,
    myReaction: MyReaction,
    onLike: () -> Unit,
    onDislike: () -> Unit,
) {
    val liked = myReaction == MyReaction.LIKE
    val disliked = myReaction == MyReaction.DISLIKE
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onLike)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    id = if (liked) DesignR.drawable.ic_reaction_fire_active
                    else DesignR.drawable.ic_reaction_fire,
                ),
                contentDescription = "Like",
                modifier = Modifier.size(20.dp),
                colorFilter = if (liked) null
                else androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatFollowerCountShort(likes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .width(1.dp)
                .height(24.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onDislike)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    id = if (disliked) DesignR.drawable.ic_reaction_slime_active
                    else DesignR.drawable.ic_reaction_slime,
                ),
                contentDescription = "Dislike",
                modifier = Modifier.size(20.dp),
                colorFilter = if (disliked) null
                else androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatFollowerCountShort(dislikes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun ShortsInfoActionRow(
    inWatchLater: Boolean,
    inFavorites: Boolean,
    onWatchLater: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShortsActionButton(
            label = if (inWatchLater) "Saved" else "Later",
            iconVector = Icons.Outlined.Schedule,
            highlight = inWatchLater,
            onClick = onWatchLater,
            modifier = Modifier.weight(1f),
        )
        ShortsActionButton(
            label = if (inFavorites) "Faved" else "Fave",
            iconVector = Icons.Outlined.Star,
            highlight = inFavorites,
            onClick = onFavorite,
            modifier = Modifier.weight(1f),
        )
        ShortsActionButton(
            label = "Share",
            iconVector = Icons.Outlined.Share,
            onClick = onShare,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ShortsActionButton(
    label: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(
                if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.material3.Icon(
            imageVector = iconVector,
            contentDescription = label,
            tint = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShortPage(
    item: ShortUi,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isMuted: Boolean,
    viewModel: ShortsViewModel,
    onRepost: () -> Unit,
    likes: Long,
    dislikes: Long,
    myReaction: MyReaction,
    isSubscribed: Boolean,
    autoplayNextShort: Boolean,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onChannelClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (isCurrent) {
            // SurfaceView doesn't intercept the pager's swipe like PlayerView does.
            // We size it to the actual video aspect so portrait shorts fill the
            // screen but 16:9 / square clips letterbox cleanly instead of stretching.
            var aspect by remember { mutableStateOf(9f / 16f) }
            androidx.compose.runtime.DisposableEffect(viewModel.exoPlayer) {
                val listener = object : androidx.media3.common.Player.Listener {
                    override fun onVideoSizeChanged(size: androidx.media3.common.VideoSize) {
                        if (size.width > 0 && size.height > 0) {
                            val pixelRatio = size.pixelWidthHeightRatio.takeIf { it > 0f } ?: 1f
                            aspect = (size.width * pixelRatio) / size.height
                        }
                    }
                }
                viewModel.exoPlayer.addListener(listener)
                onDispose { viewModel.exoPlayer.removeListener(listener) }
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect),
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        viewModel.exoPlayer.setVideoSurfaceView(this)
                    }
                },
                update = { view -> viewModel.exoPlayer.setVideoSurfaceView(view) },
            )
        }
        // Thumbnail sits on TOP of the surface until playback actually starts, so the
        // last frame of the previous short can't leak through. Cropped to fill the
        // portrait screen — matches how the video itself renders.
        if (!item.thumbnailUrl.isNullOrBlank() && (!isCurrent || !isPlaying)) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (isCurrent) {
            // Tap anywhere on the video area to toggle play/pause.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.togglePlayPause() },
                    ),
            )
            // Big centered play indicator when paused.
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
        // Readability gradient at the bottom.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                    ),
                ),
        )
        // Minimal progress bar at the very bottom.
        if (isCurrent) {
            val progress = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
            val positionMs = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }
            val durationMs = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }
            androidx.compose.runtime.LaunchedEffect(item.claimId) {
                while (true) {
                    val dur = viewModel.exoPlayer.duration
                    val pos = viewModel.exoPlayer.currentPosition
                    positionMs.value = pos.coerceAtLeast(0)
                    durationMs.value = dur.coerceAtLeast(0)
                    progress.value = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                    kotlinx.coroutines.delay(120)
                }
            }
            // Video controls bar at the bottom: current time, progress, duration, mute.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTime(positionMs.value),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.value)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatTime(durationMs.value),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { viewModel.toggleMute() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        // Web's mobile shorts has no prev/next chevrons — navigation is swipe-only.
        // Right-side floating actions (Fire, Slime, Avatar+ring, Info, Autoplay).
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 96.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShortsActionImageButton(
                iconRes = if (myReaction == MyReaction.LIKE) DesignR.drawable.ic_reaction_fire_active
                else DesignR.drawable.ic_reaction_fire,
                tint = if (myReaction == MyReaction.LIKE) null else Color.White,
                label = formatCount(likes),
                contentDescription = "Like",
                onClick = { viewModel.toggleLike(item.claimId) },
            )
            Spacer(Modifier.height(14.dp))
            ShortsActionImageButton(
                iconRes = if (myReaction == MyReaction.DISLIKE) DesignR.drawable.ic_reaction_slime_active
                else DesignR.drawable.ic_reaction_slime,
                tint = if (myReaction == MyReaction.DISLIKE) null else Color.White,
                label = formatCount(dislikes),
                contentDescription = "Dislike",
                onClick = { viewModel.toggleDislike(item.claimId) },
            )
            Spacer(Modifier.height(14.dp))
            val canSubscribe = viewModel.state.collectAsStateWithLifecycle().value.isSignedIn
            var showUnfollowConfirm by remember { mutableStateOf(false) }
            val onSubscribeTap: () -> Unit = {
                val cid = item.channelClaimId
                if (cid != null) {
                    if (isSubscribed) showUnfollowConfirm = true
                    else viewModel.toggleSubscribe(cid, item.channelName)
                }
            }
            Box(modifier = Modifier.size(SHORTS_BUTTON_SIZE + 10.dp)) {
                Box(
                    modifier = Modifier
                        .size(SHORTS_BUTTON_SIZE)
                        .align(Alignment.TopCenter)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(enabled = canSubscribe, onClick = onSubscribeTap),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!item.channelAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.channelAvatarUrl,
                            contentDescription = item.channelName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = item.channelInitial.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canSubscribe, onClick = onSubscribeTap),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(
                            id = if (isSubscribed) DesignR.drawable.ic_heart_filled
                            else DesignR.drawable.ic_heart_outline,
                        ),
                        contentDescription = if (isSubscribed) "Unsubscribe" else "Subscribe",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (showUnfollowConfirm) {
                val name = item.channelTitle?.takeIf { it.isNotBlank() } ?: item.channelName
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showUnfollowConfirm = false },
                    title = { Text("Unfollow $name?") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            showUnfollowConfirm = false
                            item.channelClaimId?.let {
                                viewModel.toggleSubscribe(it, item.channelName)
                            }
                        }) { Text("Unfollow") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            showUnfollowConfirm = false
                        }) { Text("Cancel") }
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
            ShortsRoundedIcon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Info",
                enabled = true,
                onClick = { viewModel.setShowInfo(true) },
            )
            Spacer(Modifier.height(14.dp))
            ShortsRoundedIcon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Comments",
                enabled = true,
                onClick = { viewModel.setShowComments(true) },
            )
            Spacer(Modifier.height(14.dp))
            val context = androidx.compose.ui.platform.LocalContext.current
            ShortsRoundedIcon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Share",
                enabled = true,
                onClick = {
                    val stripped = item.permanentUrl.removePrefix("lbry://")
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, item.title)
                        putExtra(android.content.Intent.EXTRA_TEXT, "https://odysee.com/$stripped")
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
                },
            )
            Spacer(Modifier.height(14.dp))
            ShortsRoundedImageButton(
                iconRes = DesignR.drawable.ic_repost,
                contentDescription = "Repost",
                onClick = onRepost,
            )
            Spacer(Modifier.height(14.dp))
            ShortsRoundedIcon(
                imageVector = Icons.Outlined.PlaylistPlay,
                contentDescription = "Autoplay next short",
                enabled = true,
                active = autoplayNextShort,
                onClick = { viewModel.toggleAutoplayNextShort() },
            )
        }
        // Bottom-left text overlay — sits above the video controls bar.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, end = 16.dp, bottom = 44.dp),
        ) {
            val displayChannel = item.channelTitle?.takeIf { it.isNotBlank() } ?: item.channelName
            Text(
                text = displayChannel,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onChannelClick),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val SHORTS_BUTTON_SIZE = 44.dp
private val SHORTS_ICON_SIZE = 24.dp

@Composable
private fun ShortsRoundedImageButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SHORTS_BUTTON_SIZE)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(SHORTS_ICON_SIZE),
            colorFilter = ColorFilter.tint(Color.White),
        )
    }
}

@Composable
private fun ShortsRoundedIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = when {
        active -> MaterialTheme.colorScheme.primary
        else -> Color.Black.copy(alpha = 0.35f)
    }
    Box(
        modifier = Modifier
            .size(SHORTS_BUTTON_SIZE)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(SHORTS_ICON_SIZE),
        )
    }
}

@Composable
private fun ShortsActionImageButton(
    iconRes: Int,
    tint: Color?,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(SHORTS_BUTTON_SIZE)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(SHORTS_ICON_SIZE),
                colorFilter = tint?.let { ColorFilter.tint(it) },
            )
        }
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun formatCount(count: Long): String = when {
    count <= 0 -> ""
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0).removeSuffix(".0M") + "M"
    count >= 1_000 -> "%.1fK".format(count / 1_000.0).removeSuffix(".0K") + "K"
    else -> count.toString()
}.replace(Regex("MM\$"), "M").replace(Regex("KK\$"), "K")

@Composable
private fun ViewModePill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(
                if (active) MaterialTheme.colorScheme.primary
                else Color.Black.copy(alpha = 0.45f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
