package com.odysee.app.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import android.view.LayoutInflater
import androidx.media3.ui.PlayerView
import com.odysee.app.R
import coil3.compose.AsyncImage
import androidx.compose.ui.text.withStyle
import com.odysee.app.core.data.player.CommentUiModel
import com.odysee.app.core.data.player.CommentsState
import com.odysee.app.core.data.player.PlayerController
import com.odysee.app.core.data.player.PlayerState
import com.odysee.app.core.data.player.RelatedItemUi
import com.odysee.app.core.data.player.RelatedState
import com.odysee.app.core.designsystem.R as DesignR
import com.odysee.app.core.designsystem.comments.EMOTE_CATEGORIES
import com.odysee.app.core.designsystem.comments.EmoteDef
import com.odysee.app.core.designsystem.comments.FREE_GLOBAL_STICKERS
import com.odysee.app.core.designsystem.comments.OdyseeChannelAvatar
import com.odysee.app.core.designsystem.comments.OdyseeComment
import com.odysee.app.core.designsystem.comments.OdyseeCommentActions
import com.odysee.app.core.designsystem.comments.OdyseeCommentActionsSheet
import com.odysee.app.core.designsystem.comments.OdyseeCommentRow
import com.odysee.app.core.designsystem.comments.OdyseeCommentThread
import com.odysee.app.core.designsystem.comments.OdyseeMembershipTier
import com.odysee.app.core.designsystem.comments.OdyseeReaction
import com.odysee.app.core.designsystem.comments.PAID_GLOBAL_STICKERS
import com.odysee.app.core.designsystem.comments.RichCommentBody
import com.odysee.app.core.designsystem.comments.StickerDef
import com.odysee.app.core.designsystem.comments.toCommentToken
import kotlinx.coroutines.launch

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> this.baseContext.findActivity()
    else -> null
}

private fun buildDescriptionAnnotated(
    text: String,
    linkColor: androidx.compose.ui.graphics.Color,
): androidx.compose.ui.text.AnnotatedString {
    val pattern = android.util.Patterns.WEB_URL
    val matcher = pattern.matcher(text)
    val linkStyle = androidx.compose.ui.text.TextLinkStyles(
        style = androidx.compose.ui.text.SpanStyle(
            color = linkColor,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
        ),
    )
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    var lastEnd = 0
    while (matcher.find()) {
        val start = matcher.start()
        val end = matcher.end()
        if (start > lastEnd) builder.append(text.substring(lastEnd, start))
        val raw = text.substring(start, end)
        val href = if (raw.startsWith("http://", ignoreCase = true) ||
            raw.startsWith("https://", ignoreCase = true)) raw else "https://$raw"
        val linkIdx = builder.pushLink(
            androidx.compose.ui.text.LinkAnnotation.Url(url = href, styles = linkStyle),
        )
        builder.append(raw)
        builder.pop(linkIdx)
        lastEnd = end
    }
    if (lastEnd < text.length) builder.append(text.substring(lastEnd))
    return builder.toAnnotatedString()
}

private enum class WatchTab(val label: String, val iconRes: Int) {
    Info("Info", DesignR.drawable.ic_tab_info),
    Comments("Comments", DesignR.drawable.ic_tab_comments),
    Playlist("Playlist", DesignR.drawable.ic_menu_playlist),
    Related("Related", DesignR.drawable.ic_tab_discover),
}

@Composable
fun ExpandedPlayerContent(
    state: PlayerState,
    controller: PlayerController,
    onCollapse: () -> Unit,
    onChannelClick: (String, String) -> Unit,
) {
    val media = state.media ?: return
    val tabs = remember(state.playlist) {
        if (state.playlist != null) WatchTab.entries
        else WatchTab.entries.filter { it != WatchTab.Playlist }
    }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val isFullscreen by controller.isFullscreen.collectAsStateWithLifecycle()
    val isPipActive by controller.isPipActive.collectAsStateWithLifecycle()
    val isCasting by controller.castController.isSessionActive.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findActivity() }
    var showPlayerSettings by remember { mutableStateOf(false) }
    val activeVideoHeight by controller.activeVideoHeight.collectAsStateWithLifecycle()

    var controlsVisible by remember { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var relatedMenuTarget by remember { mutableStateOf<RelatedItemUi?>(null) }
    var relatedAddToPlaylistTarget by remember { mutableStateOf<RelatedItemUi?>(null) }
    var relatedRepostTarget by remember { mutableStateOf<RelatedItemUi?>(null) }

    // Auto-show when the user paused — but NOT when the player is briefly not playing
    // because of buffering or auto-transition between videos (autoplay-next).
    // `playWhenReady` distinguishes the two: user-paused → false, transition → true.
    LaunchedEffect(state.isPlaying) {
        if (!state.isPlaying && !controller.exoPlayer.playWhenReady) {
            controlsVisible = true
            playerViewRef?.showController()
        }
    }

    // Auto-hide 3s after controls become visible while playing.
    LaunchedEffect(controlsVisible, state.isPlaying) {
        if (controlsVisible && state.isPlaying) {
            kotlinx.coroutines.delay(3000)
            controlsVisible = false
            playerViewRef?.hideController()
        }
    }

    DisposableEffect(activity, isFullscreen) {
        val act = activity
        if (act != null) {
            val controllerCompat = androidx.core.view.WindowCompat.getInsetsController(act.window, act.window.decorView)
            if (isFullscreen) {
                controllerCompat.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controllerCompat.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                controllerCompat.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val a = activity ?: return@onDispose
            val controllerCompat = androidx.core.view.WindowCompat.getInsetsController(a.window, a.window.decorView)
            controllerCompat.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    val wide = !isFullscreen && com.odysee.app.core.designsystem.layout.rememberWindowSize()
        .ordinal >= com.odysee.app.core.designsystem.layout.WindowSize.Expanded.ordinal
    val outerModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .then(
            if (isFullscreen) Modifier
            else Modifier.statusBarsPadding().navigationBarsPadding(),
        )
    val videoSurface: @Composable (Modifier) -> Unit = { videoModifier ->
        Box(modifier = videoModifier) {
            val renderMode = media.renderMode
            when (renderMode) {
                com.odysee.app.core.data.player.MediaRenderMode.Image ->
                    ImageMediaSurface(state = state, fillScreen = isFullscreen)
                com.odysee.app.core.data.player.MediaRenderMode.Pdf ->
                    PdfMediaSurface(state = state, fillScreen = isFullscreen)
                com.odysee.app.core.data.player.MediaRenderMode.Text ->
                    TextMediaSurface(state = state, fillScreen = isFullscreen)
                com.odysee.app.core.data.player.MediaRenderMode.Download ->
                    DownloadMediaSurface(state = state, media = media, fillScreen = isFullscreen)
                com.odysee.app.core.data.player.MediaRenderMode.Audio,
                com.odysee.app.core.data.player.MediaRenderMode.Video -> Unit
            }
            if (renderMode == com.odysee.app.core.data.player.MediaRenderMode.Video ||
                renderMode == com.odysee.app.core.data.player.MediaRenderMode.Audio
            ) {
            val needsPurchase = state.paywall !is com.odysee.app.core.model.Paywall.Free && !state.isPurchased
            val needsMembership = state.isMembersOnly && !state.isMemberOfChannel
            if (needsPurchase) {
                PaywallGateSurface(
                    media = media,
                    fillScreen = isFullscreen,
                    paywall = state.paywall,
                    purchaseStatus = state.purchaseStatus,
                    onBuyLbc = controller::purchaseWithLbc,
                    onFiatComplete = controller::onFiatPurchaseCompleted,
                    onClearError = controller::clearPurchaseError,
                )
            } else if (needsMembership) {
                MembersOnlyGateSurface(
                    media = media,
                    fillScreen = isFullscreen,
                    onJoinComplete = controller::onFiatPurchaseCompleted,
                )
            } else if (isCasting) {
                CastPlaceholderSurface(
                    thumbnailUrl = media.thumbnailUrl,
                    fillScreen = isFullscreen,
                    onPlay = {
                        val url = state.streamingUrl
                        if (url != null) {
                            controller.castController.loadMedia(
                                streamUrl = url,
                                title = media.title,
                                channelName = media.channelName,
                                thumbnailUrl = media.thumbnailUrl,
                            )
                        }
                    },
                )
            } else if (isPipActive) {
                PipPlaceholderSurface(
                    thumbnailUrl = media.thumbnailUrl,
                    fillScreen = isFullscreen,
                    onResume = { controller.requestExpand() },
                )
            } else {
            PlayerSurface(
                streamingUrl = state.streamingUrl,
                isResolving = state.isResolving,
                isBuffering = state.isBuffering,
                errorMessage = state.errorMessage,
                thumbnailUrl = media.thumbnailUrl,
                onRetry = controller::retryResolve,
                fillScreen = isFullscreen,
                exoPlayerFactory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(R.layout.odysee_player_view, null) as PlayerView).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        this.player = controller.exoPlayer
                        useController = true
                        controllerShowTimeoutMs = 0
                        controllerAutoShow = false
                        setFullscreenButtonClickListener { fullscreenRequested ->
                            controller.setFullscreen(fullscreenRequested)
                        }
                        findViewById<android.widget.ImageButton?>(R.id.odysee_player_settings_btn)
                            ?.setOnClickListener { showPlayerSettings = true }
                        findViewById<android.widget.ImageButton?>(R.id.odysee_player_pip_btn)
                            ?.setOnClickListener {
                                // Hand off to the existing FloatingPlayerService-backed
                                // pop-up player (same path as the menu's "Play in pop-up
                                // player"), not the system activity-wide PiP.
                                controller.requestPip()
                            }
                        setControllerVisibilityListener(
                            androidx.media3.ui.PlayerView.ControllerVisibilityListener { visibility ->
                                controlsVisible = visibility == android.view.View.VISIBLE
                            },
                        )
                        playerViewRef = this
                    }
                },
                onUpdate = { view ->
                    view.findViewById<android.widget.TextView?>(R.id.odysee_hd_label)?.visibility =
                        if (activeVideoHeight >= 720) android.view.View.VISIBLE else android.view.View.GONE
                    view.findViewById<android.widget.ImageButton?>(R.id.odysee_player_settings_btn)?.let { cog ->
                        cog.animate().cancel()
                        cog.animate().rotation(if (showPlayerSettings) 90f else 0f).setDuration(220).start()
                    }
                },
            )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (!controlsVisible) Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { playerViewRef?.showController() },
                                onDoubleTap = { offset ->
                                    val player = controller.exoPlayer
                                    val delta = if (offset.x < size.width / 2f) -10_000L else 10_000L
                                    player.seekTo((player.currentPosition + delta).coerceAtLeast(0L))
                                },
                            )
                        } else Modifier,
                    ),
            )
            if (controlsVisible) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .align(Alignment.Center)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color(0xFFE50054))
                        .clickable {
                            val wasPaused = !state.isPlaying
                            controller.togglePlayPause()
                            if (wasPaused) {
                                scope.launch {
                                    kotlinx.coroutines.delay(150)
                                    controlsVisible = false
                                    playerViewRef?.hideController()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            if (!isFullscreen) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.align(Alignment.TopStart),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = "Collapse",
                        tint = Color.White,
                    )
                }
            }
            if (controlsVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 48.dp, top = 6.dp)
                        .size(36.dp),
                ) {
                    com.odysee.app.cast.OdyseeCastButton(modifier = Modifier.size(36.dp))
                }
            }
            if (media.renderMode == com.odysee.app.core.data.player.MediaRenderMode.Audio) {
                AudioCoverOverlay(thumbnailUrl = media.thumbnailUrl)
            }
            }
        }
    }
    val sidePane: @Composable ColumnScope.() -> Unit = {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxSize(),
        ) { page ->
            when (tabs[page]) {
                WatchTab.Info -> InfoPage(
                    state = state,
                    media = media,
                    onChannelClick = {
                        media.channelClaimId?.let { id ->
                            onCollapse()
                            onChannelClick(id, media.channelName)
                        }
                    },
                    onToggleWatchLater = controller::toggleWatchLater,
                    onToggleFavorite = controller::toggleFavorite,
                    onLike = controller::like,
                    onDislike = controller::dislike,
                )
                WatchTab.Comments -> {
                    val auth = com.odysee.app.auth.LocalAuthState.current
                    val signedIn = auth as? com.odysee.app.core.data.auth.AuthState.SignedIn
                    val canPost = signedIn?.activeChannel != null
                    if (media.liveStreamUrl != null) {
                        ChatPage(
                            comments = state.comments,
                            onRetry = controller::retryComments,
                            onPost = { controller.postComment(it) },
                            canPost = canPost,
                        )
                    } else {
                        CommentsPage(
                            comments = state.comments,
                            repliesByParent = state.repliesByParent,
                            currentSort = state.commentSort,
                            onSortChange = controller::setCommentSort,
                            onRetry = controller::retryComments,
                            linkedCommentId = media.linkedCommentId,
                            onPost = { controller.postComment(it) },
                            onReply = { parentId, text -> controller.postComment(text, parentId) },
                            onLike = { id -> controller.toggleCommentReaction(id, true) },
                            onDislike = { id -> controller.toggleCommentReaction(id, false) },
                            onLoadReplies = { id -> controller.loadReplies(id) },
                            onPinToggle = controller::togglePinComment,
                            onEdit = controller::editCommentText,
                            onDelete = controller::deleteCommentById,
                            onVisitChannel = { id, name ->
                                onCollapse()
                                onChannelClick(id, name)
                            },
                            onBlock = { id ->
                                val scope = if (media.channelClaimId != null && signedIn?.activeChannel?.claimId == media.channelClaimId)
                                    com.odysee.app.core.data.player.BlockScope.Creator
                                else com.odysee.app.core.data.player.BlockScope.Self
                                controller.blockCommenter(id, scope)
                            },
                            onAddModerator = controller::addCommentModerator,
                            onRemoveModerator = controller::removeCommentModerator,
                            onHyperchat = { text, amount -> controller.postHyperchat(text, amount) },
                            canPost = canPost,
                            isClaimOwner = media.channelClaimId != null && signedIn?.activeChannel?.claimId == media.channelClaimId,
                        )
                    }
                }
                WatchTab.Playlist -> PlaylistTabContent(
                    playlist = state.playlist,
                    currentClaimId = media.claimId,
                    onSelect = { index -> controller.playPlaylistItem(index) },
                )
                WatchTab.Related -> RelatedPage(
                    state = state.related,
                    moreFromChannelState = state.moreFromChannel,
                    channelName = media.channelName.takeIf { it.isNotBlank() },
                    onRetry = controller::retryRelated,
                    onWatch = { item ->
                        controller.play(
                            com.odysee.app.core.data.player.CurrentMedia(
                                claimId = item.claimId,
                                permanentUrl = item.permanentUrl,
                                title = item.title,
                                description = item.description,
                                channelClaimId = item.channelClaimId,
                                channelName = item.channelName,
                                channelInitial = item.channelInitial,
                                channelAvatarUrl = item.channelAvatarUrl,
                                thumbnailUrl = item.thumbnailUrl,
                                ageLabel = item.ageLabel.takeIf { it.isNotEmpty() },
                            ),
                        )
                    },
                    onLongPressRelated = { item -> relatedMenuTarget = item },
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    icon = {
                        val iconRes = if (tab == WatchTab.Comments && media.liveStreamUrl != null) {
                            DesignR.drawable.ic_tab_chat
                        } else tab.iconRes
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    text = {
                        val label = if (tab == WatchTab.Comments && media.liveStreamUrl != null) "Chat"
                            else tab.label
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.onBackground,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (wide) {
        Row(modifier = outerModifier) {
            Box(
                modifier = Modifier
                    .weight(1.6f)
                    .fillMaxHeight()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                videoSurface(Modifier.fillMaxWidth())
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                sidePane()
            }
        }
    } else {
        Column(modifier = outerModifier) {
            videoSurface(
                if (isFullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
            )
            if (!isFullscreen) sidePane()
        }
    }

    if (showPlayerSettings) {
        PlayerSettingsSheet(
            controller = controller,
            onDismiss = { showPlayerSettings = false },
        )
    }

    relatedMenuTarget?.let { item ->
        val clipboardMgr = ctx.getSystemService(android.content.ClipboardManager::class.java)
        com.odysee.app.core.designsystem.claims.OdyseeClaimMenuSheet(
            target = com.odysee.app.core.designsystem.claims.OdyseeClaimMenuTarget(
                claimId = item.claimId,
                name = item.title,
                title = item.title,
                permanentUrl = item.permanentUrl,
                channelClaimId = item.channelClaimId,
                channelName = item.channelName,
            ),
            actions = com.odysee.app.core.designsystem.claims.OdyseeClaimMenuActions(
                onPlayBackground = {
                    controller.play(
                        com.odysee.app.core.data.player.CurrentMedia(
                            claimId = item.claimId,
                            permanentUrl = item.permanentUrl,
                            title = item.title,
                            description = item.description,
                            channelClaimId = item.channelClaimId,
                            channelName = item.channelName,
                            channelInitial = item.channelInitial,
                            channelAvatarUrl = item.channelAvatarUrl,
                            thumbnailUrl = item.thumbnailUrl,
                            ageLabel = item.ageLabel.takeIf { it.isNotEmpty() },
                        ),
                        openMode = com.odysee.app.core.data.player.PlayerOpenMode.Minimized,
                    )
                },
                onPlayPip = {
                    controller.play(
                        com.odysee.app.core.data.player.CurrentMedia(
                            claimId = item.claimId,
                            permanentUrl = item.permanentUrl,
                            title = item.title,
                            description = item.description,
                            channelClaimId = item.channelClaimId,
                            channelName = item.channelName,
                            channelInitial = item.channelInitial,
                            channelAvatarUrl = item.channelAvatarUrl,
                            thumbnailUrl = item.thumbnailUrl,
                            ageLabel = item.ageLabel.takeIf { it.isNotEmpty() },
                        ),
                        openMode = com.odysee.app.core.data.player.PlayerOpenMode.Pip,
                    )
                },
                onSaveWatchLater = {
                    controller.saveToWatchLater(
                        com.odysee.app.core.data.player.CurrentMedia(
                            claimId = item.claimId,
                            permanentUrl = item.permanentUrl,
                            title = item.title,
                            description = item.description,
                            channelClaimId = item.channelClaimId,
                            channelName = item.channelName,
                            channelInitial = item.channelInitial,
                            channelAvatarUrl = item.channelAvatarUrl,
                            thumbnailUrl = item.thumbnailUrl,
                            ageLabel = item.ageLabel.takeIf { it.isNotEmpty() },
                        ),
                    )
                },
                onSaveFavorite = {
                    controller.saveToFavorites(
                        com.odysee.app.core.data.player.CurrentMedia(
                            claimId = item.claimId,
                            permanentUrl = item.permanentUrl,
                            title = item.title,
                            description = item.description,
                            channelClaimId = item.channelClaimId,
                            channelName = item.channelName,
                            channelInitial = item.channelInitial,
                            channelAvatarUrl = item.channelAvatarUrl,
                            thumbnailUrl = item.thumbnailUrl,
                            ageLabel = item.ageLabel.takeIf { it.isNotEmpty() },
                        ),
                    )
                },
                onAddToPlaylist = { relatedAddToPlaylistTarget = item },
                onRepost = { relatedRepostTarget = item },
                onShare = {
                    val stripped = item.permanentUrl.removePrefix("lbry://")
                    val url = "https://odysee.com/$stripped"
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, item.title)
                        putExtra(android.content.Intent.EXTRA_TEXT, url)
                    }
                    ctx.startActivity(android.content.Intent.createChooser(intent, "Share via"))
                },
                onCopyLink = {
                    val stripped = item.permanentUrl.removePrefix("lbry://")
                    clipboardMgr?.setPrimaryClip(
                        android.content.ClipData.newPlainText("Video link", "https://odysee.com/$stripped"),
                    )
                },
                onGoToChannel = item.channelClaimId?.takeIf { item.channelName.isNotBlank() }?.let { cid ->
                    { onChannelClick(cid, item.channelName) }
                },
                onReport = {
                    val link = "https://odysee.com/\$/report-content?claimId=${item.claimId}"
                    runCatching {
                        ctx.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link)),
                        )
                    }
                },
            ),
            onDismiss = { relatedMenuTarget = null },
        )
    }

    relatedAddToPlaylistTarget?.let { item ->
        com.odysee.app.feature.library.AddToPlaylistSheet(
            title = item.title,
            permanentUrl = item.permanentUrl,
            onDismiss = { relatedAddToPlaylistTarget = null },
            onCreateNew = { relatedAddToPlaylistTarget = null },
            quickTarget = com.odysee.app.feature.library.QuickTargetClaim(
                claimId = item.claimId,
                permanentUrl = item.permanentUrl,
                title = item.title,
                channelName = item.channelName,
                channelClaimId = item.channelClaimId,
                thumbnailUrl = item.thumbnailUrl,
            ),
        )
    }

    relatedRepostTarget?.let { item ->
        com.odysee.app.feature.library.RepostSheet(
            claimId = item.claimId,
            claimName = item.title,
            onDismiss = { relatedRepostTarget = null },
            onPosted = { relatedRepostTarget = null },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSettingsSheet(
    controller: PlayerController,
    onDismiss: () -> Unit,
) {
    val exoPlayer = controller.exoPlayer
    var currentSpeed by remember { mutableStateOf(exoPlayer.playbackParameters.speed) }
    var isLooping by remember { mutableStateOf(exoPlayer.repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) }
    var view by remember { mutableStateOf("main") }
    val autoplayNext by controller.autoplayNext.collectAsStateWithLifecycle(initialValue = true)
    val autoplayMedia by controller.autoplayMedia.collectAsStateWithLifecycle(initialValue = true)

    val qualityOptions = remember(exoPlayer.currentTracks) { collectQualityOptions(exoPlayer) }
    val activeHeight by controller.activeVideoHeight.collectAsStateWithLifecycle()
    var selectedQualityHeight by remember(qualityOptions) {
        mutableStateOf(currentSelectedQualityHeight(exoPlayer))
    }
    val currentQualityLabel = qualityLabelFor(selectedQualityHeight, activeHeight)

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            when (view) {
                "speed" -> {
                    SettingsBackHeader(title = "Playback speed", onBack = { view = "main" })
                    val speeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
                    speeds.forEach { speed ->
                        SettingsOptionRow(
                            label = if (speed == 1f) "Normal" else "${speed}x",
                            selected = currentSpeed == speed,
                            onClick = {
                                exoPlayer.setPlaybackSpeed(speed)
                                currentSpeed = speed
                                view = "main"
                            },
                        )
                    }
                }
                "quality" -> {
                    SettingsBackHeader(title = "Quality", onBack = { view = "main" })
                    qualityOptions.forEach { option ->
                        val isSelected =
                            (option.isAuto && selectedQualityHeight == null) ||
                                (!option.isAuto && option.height == selectedQualityHeight)
                        SettingsOptionRow(
                            label = option.label,
                            selected = isSelected,
                            onClick = {
                                applyQualityOption(exoPlayer, option)
                                selectedQualityHeight = if (option.isAuto) null else option.height
                                view = "main"
                            },
                        )
                    }
                }
                else -> {
                    SettingsToggleRow(
                        label = "Autoplay",
                        checked = autoplayMedia,
                        onToggle = { controller.setAutoplayMedia(!autoplayMedia) },
                    )
                    SettingsToggleRow(
                        label = "Autoplay next",
                        checked = autoplayNext,
                        onToggle = { controller.setAutoplayNext(!autoplayNext) },
                    )
                    SettingsToggleRow(
                        label = "Loop",
                        checked = isLooping,
                        onToggle = {
                            isLooping = !isLooping
                            exoPlayer.repeatMode = if (isLooping)
                                androidx.media3.common.Player.REPEAT_MODE_ONE
                            else
                                androidx.media3.common.Player.REPEAT_MODE_OFF
                        },
                    )
                    SettingsArrowRow(
                        label = "Playback speed",
                        value = if (currentSpeed == 1f) "Normal" else "${currentSpeed}x",
                        onClick = { view = "speed" },
                    )
                    SettingsArrowRow(
                        label = "Quality",
                        value = currentQualityLabel,
                        onClick = { view = "quality" },
                    )
                }
            }
        }
    }
}

private data class QualityOption(val label: String, val height: Int?, val isAuto: Boolean)

private fun collectQualityOptions(exoPlayer: androidx.media3.exoplayer.ExoPlayer): List<QualityOption> {
    val heights = mutableSetOf<Int>()
    exoPlayer.currentTracks.groups.forEach { group ->
        if (group.type != androidx.media3.common.C.TRACK_TYPE_VIDEO) return@forEach
        for (i in 0 until group.length) {
            val format = group.getTrackFormat(i)
            val h = format.height
            if (h > 0) heights.add(h)
        }
    }
    val sorted = heights.sortedDescending()
    val list = mutableListOf(QualityOption(label = "Auto", height = null, isAuto = true))
    sorted.forEach { list.add(QualityOption(label = "${it}p", height = it, isAuto = false)) }
    return list
}

private fun currentSelectedQualityHeight(
    exoPlayer: androidx.media3.exoplayer.ExoPlayer,
): Int? {
    val maxHeight = exoPlayer.trackSelectionParameters.maxVideoHeight
    return if (maxHeight == Int.MAX_VALUE) null else maxHeight
}

private fun qualityLabelFor(selectedHeight: Int?, activeHeight: Int): String =
    if (selectedHeight == null) {
        if (activeHeight > 0) "Auto (${activeHeight}p)" else "Auto"
    } else {
        "${selectedHeight}p"
    }

private fun applyQualityOption(
    exoPlayer: androidx.media3.exoplayer.ExoPlayer,
    option: QualityOption,
) {
    val builder = exoPlayer.trackSelectionParameters.buildUpon()
    if (option.isAuto || option.height == null) {
        builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
    } else {
        builder.setMaxVideoSize(Int.MAX_VALUE, option.height)
    }
    exoPlayer.trackSelectionParameters = builder.build()
}

@Composable
private fun SettingsBackHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SettingsArrowRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun InfoPage(
    state: PlayerState,
    media: com.odysee.app.core.data.player.CurrentMedia,
    onChannelClick: () -> Unit,
    onToggleWatchLater: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    var showTipDialog by remember { mutableStateOf(false) }
    var showUnfollowConfirm by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = media.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        run {
            val hasViews = (state.viewCount ?: 0L) > 0L
            val ageText = media.ageLabel?.takeIf { it.isNotBlank() }
            if (hasViews || ageText != null) {
                Spacer(Modifier.size(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val subColor = MaterialTheme.colorScheme.onSurfaceVariant
                    if (hasViews) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = subColor,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = formatViews(state.viewCount!!),
                                style = MaterialTheme.typography.bodySmall,
                                color = subColor,
                            )
                        }
                    }
                    if (ageText != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = subColor,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = ageText,
                                style = MaterialTheme.typography.bodySmall,
                                color = subColor,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.size(16.dp))
        val playerCtrl = com.odysee.app.player.LocalPlayerController.current
        val authStateForFollow = com.odysee.app.auth.LocalAuthState.current
        val canSubscribe = authStateForFollow is com.odysee.app.core.data.auth.AuthState.SignedIn
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar tap → toggle subscribe (NOT navigate). Unfollow needs a confirm.
            Box(
                modifier = Modifier.clickable(enabled = canSubscribe) {
                    if (media.channelClaimId == null) return@clickable
                    if (state.isChannelSubscribed) showUnfollowConfirm = true
                    else playerCtrl.toggleChannelSubscription()
                },
            ) {
                OdyseeChannelAvatar(
                    avatarUrl = media.channelAvatarUrl,
                    initial = media.channelInitial,
                    size = 40,
                )
            }
            Spacer(Modifier.width(12.dp))
            // Text tap → navigate to channel page.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onChannelClick),
            ) {
                val displayTitle = media.channelTitle?.takeIf { it.isNotBlank() } ?: media.channelName
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subText = buildString {
                    if (displayTitle != media.channelName) append(media.channelName)
                    state.channelFollowerCount?.let { count ->
                        if (isNotEmpty()) append(" • ")
                        append(formatFollowerCount(count))
                        append(" followers")
                    }
                }
                if (subText.isNotEmpty()) {
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // Follow button on its own row so it can never be intercepted by the channel row.
        media.channelClaimId?.let {
            Spacer(Modifier.size(12.dp))
            val followBg = when {
                !canSubscribe -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                state.isChannelSubscribed -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.primary
            }
            val followFg = when {
                !canSubscribe -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                state.isChannelSubscribed -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> Color.White
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(followBg)
                    .clickable(enabled = canSubscribe) {
                        if (state.isChannelSubscribed) showUnfollowConfirm = true
                        else playerCtrl.toggleChannelSubscription()
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.isChannelSubscribed) "Following" else "Follow",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = followFg,
                )
            }
        }
        Spacer(Modifier.size(16.dp))
        ReactionRow(
            likes = state.reactions?.likes ?: 0,
            dislikes = state.reactions?.dislikes ?: 0,
            myReaction = state.reactions?.myReaction ?: com.odysee.app.core.data.reactions.MyReaction.NONE,
            onLike = onLike,
            onDislike = onDislike,
        )
        Spacer(Modifier.size(12.dp))
        var showSavePicker by remember { mutableStateOf(false) }
        ActionRow(
            inWatchLater = state.inWatchLater,
            inFavorites = state.inFavorites,
            onWatchLater = onToggleWatchLater,
            onFavorite = onToggleFavorite,
            onSave = { showSavePicker = true },
            onTip = { showTipDialog = true },
            onShare = {
                val url = buildShareUrl(media.permanentUrl)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, media.title)
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                context.startActivity(Intent.createChooser(intent, "Share via"))
            },
        )
        if (showSavePicker) {
            com.odysee.app.feature.library.AddToPlaylistSheet(
                title = media.title,
                permanentUrl = media.permanentUrl,
                onDismiss = { showSavePicker = false },
                onCreateNew = { showSavePicker = false },
                quickTarget = com.odysee.app.feature.library.QuickTargetClaim(
                    claimId = media.claimId,
                    permanentUrl = media.permanentUrl,
                    title = media.title,
                    channelName = media.channelName,
                    channelClaimId = media.channelClaimId,
                    thumbnailUrl = media.thumbnailUrl,
                ),
            )
        }
        if (showTipDialog) {
            TipDialog(
                onDismiss = { showTipDialog = false },
                channelName = media.channelName,
                controller = com.odysee.app.player.LocalPlayerController.current,
            )
        }
        if (showUnfollowConfirm) {
            val name = media.channelTitle?.takeIf { it.isNotBlank() } ?: media.channelName
            AlertDialog(
                onDismissRequest = { showUnfollowConfirm = false },
                title = { Text("Unfollow $name?") },
                confirmButton = {
                    TextButton(onClick = {
                        showUnfollowConfirm = false
                        playerCtrl.toggleChannelSubscription()
                    }) { Text("Unfollow") }
                },
                dismissButton = {
                    TextButton(onClick = { showUnfollowConfirm = false }) { Text("Cancel") }
                },
            )
        }
        media.description?.takeIf { it.isNotBlank() }?.let { desc ->
            Spacer(Modifier.size(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.size(16.dp))
            val linkColor = MaterialTheme.colorScheme.primary
            val annotated = remember(desc, linkColor) { buildDescriptionAnnotated(desc, linkColor) }
            Text(
                text = annotated,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun ChatPage(
    comments: CommentsState,
    onRetry: () -> Unit,
    onPost: (String) -> Unit,
    canPost: Boolean,
) {
    var draft by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val pinned = (comments as? CommentsState.Success)?.comments?.firstOrNull { it.isPinned }
    var dismissedPinId by remember { mutableStateOf<String?>(null) }
    val visiblePinned = pinned?.takeIf { it.id != dismissedPinId }
    Column(modifier = Modifier.fillMaxSize()) {
        if (visiblePinned != null) {
            PinnedChatBanner(visiblePinned, onDismiss = { dismissedPinId = visiblePinned.id })
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
        Box(modifier = Modifier.weight(1f)) {
            when (comments) {
                CommentsState.Idle, CommentsState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                is CommentsState.Error -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Couldn't load chat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = comments.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
                is CommentsState.Success -> {
                    val nonPinned = comments.comments.filterNot { it.isPinned }
                    val items = nonPinned.asReversed()
                    androidx.compose.runtime.LaunchedEffect(items.size) {
                        if (items.isNotEmpty()) {
                            listState.animateScrollToItem(items.lastIndex)
                        }
                    }
                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Say hi to start the chat!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                        ) {
                            items(items, key = { it.id }) { msg -> ChatMessageRow(msg) }
                        }
                    }
                }
            }
        }
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        if (canPost) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Say something...") },
                    singleLine = true,
                )
                Spacer(Modifier.size(8.dp))
                TextButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onPost(draft.trim())
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank(),
                ) {
                    Text("Send")
                }
            }
        } else {
            Text(
                text = "Sign in to chat",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PinnedChatBanner(comment: CommentUiModel, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.PushPin,
            contentDescription = "Pinned",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.author,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = comment.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Dismiss pinned",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ChatMessageRow(comment: CommentUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val avatarUrl = comment.authorAvatarUrl
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = comment.authorInitial.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        val text = androidx.compose.ui.text.buildAnnotatedString {
            withStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                ),
            ) {
                append(comment.author)
            }
            append("  ")
            withStyle(androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                append(comment.body)
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CommentsPage(
    comments: CommentsState,
    repliesByParent: Map<String, List<CommentUiModel>> = emptyMap(),
    currentSort: com.odysee.app.core.data.player.CommentSort = com.odysee.app.core.data.player.CommentSort.Best,
    onSortChange: (com.odysee.app.core.data.player.CommentSort) -> Unit = {},
    onRetry: () -> Unit,
    onPost: (String) -> Unit = {},
    onReply: (String, String) -> Unit = { _, _ -> },
    onLike: (String) -> Unit = {},
    onDislike: (String) -> Unit = {},
    onLoadReplies: (String) -> Unit = {},
    onPinToggle: (String, Boolean) -> Unit = { _, _ -> },
    onEdit: (String, String) -> Unit = { _, _ -> },
    onDelete: (String) -> Unit = {},
    onVisitChannel: (String, String) -> Unit = { _, _ -> },
    onBlock: (String) -> Unit = {},
    onAddModerator: (String) -> Unit = {},
    onRemoveModerator: (String) -> Unit = {},
    onHyperchat: suspend (String, Double) -> Result<Unit> = { _, _ -> Result.success(Unit) },
    canPost: Boolean = false,
    isClaimOwner: Boolean = false,
    linkedCommentId: String? = null,
) {
    var draft by remember { mutableStateOf("") }
    var pendingPaidSticker by remember { mutableStateOf<StickerDef?>(null) }
    var showHyperchatDialog by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        if (canPost) {
            CommentComposer(
                draft = draft,
                onDraftChange = { draft = it },
                onSubmit = {
                    if (draft.isNotBlank()) {
                        onPost(draft.trim())
                        draft = ""
                    }
                },
                onInsertSticker = { sticker ->
                    if ((sticker.priceLbc ?: 0) > 0) {
                        pendingPaidSticker = sticker
                    } else {
                        val trimmed = draft.trimEnd()
                        val sep = if (trimmed.isEmpty()) "" else " "
                        onPost(trimmed + sep + sticker.toCommentToken())
                        draft = ""
                    }
                },
                onOpenHyperchat = { showHyperchatDialog = true },
            )
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
        CommentSortBar(currentSort = currentSort, onSortChange = onSortChange)
        Box(modifier = Modifier.weight(1f)) {
            CommentsBody(
                comments = comments,
                repliesByParent = repliesByParent,
                onRetry = onRetry,
                canReply = canPost,
                onReply = onReply,
                onLike = onLike,
                onDislike = onDislike,
                onLoadReplies = onLoadReplies,
                onPinToggle = onPinToggle,
                onEdit = onEdit,
                onDelete = onDelete,
                onVisitChannel = onVisitChannel,
                onBlock = onBlock,
                onAddModerator = onAddModerator,
                onRemoveModerator = onRemoveModerator,
                isClaimOwner = isClaimOwner,
                linkedCommentId = linkedCommentId,
            )
        }
    }

    pendingPaidSticker?.let { sticker ->
        HyperchatStickerDialog(
            sticker = sticker,
            onDismiss = { pendingPaidSticker = null },
            onConfirm = { amount ->
                val payload = sticker.toCommentToken()
                pendingPaidSticker = null
                onHyperchat(payload, amount)
            },
        )
    }
    if (showHyperchatDialog) {
        HyperchatTextDialog(
            initialText = draft,
            onDismiss = { showHyperchatDialog = false },
            onConfirm = { text, amount ->
                showHyperchatDialog = false
                draft = ""
                onHyperchat(text, amount)
            },
        )
    }
}

@Composable
private fun HyperchatStickerDialog(
    sticker: StickerDef,
    onDismiss: () -> Unit,
    onConfirm: suspend (Double) -> Result<Unit>,
) {
    val price = (sticker.priceLbc ?: 0).toDouble()
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coScope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text("Send sticker hyperchat") },
        text = {
            Column {
                AsyncImage(
                    model = sticker.url,
                    contentDescription = sticker.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(96.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Tip $${price.toInt()} (LBC) and post this sticker as a hyperchat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !sending,
                onClick = {
                    sending = true
                    error = null
                    coScope.launch {
                        val result = onConfirm(price)
                        sending = false
                        if (result.isFailure) error = result.exceptionOrNull()?.message ?: "Failed"
                        else onDismiss()
                    }
                },
            ) { Text(if (sending) "Sending…" else "Send") }
        },
        dismissButton = { TextButton(enabled = !sending, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun HyperchatTextDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: suspend (String, Double) -> Result<Unit>,
) {
    var text by remember { mutableStateOf(initialText) }
    var amount by remember { mutableStateOf("1") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coScope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text("Hyperchat") },
        text = {
            Column {
                Text(
                    text = "Highlight your comment by tipping the creator. Paid in LBC from your wallet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message") },
                    maxLines = 4,
                )
                Spacer(Modifier.size(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = amount,
                    onValueChange = { v -> amount = v.filter { it.isDigit() || it == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount (LBC)") },
                    singleLine = true,
                )
                error?.let {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            val amt = amount.toDoubleOrNull() ?: 0.0
            TextButton(
                enabled = !sending && text.isNotBlank() && amt > 0,
                onClick = {
                    sending = true
                    error = null
                    coScope.launch {
                        val result = onConfirm(text.trim(), amt)
                        sending = false
                        if (result.isFailure) error = result.exceptionOrNull()?.message ?: "Failed"
                        else onDismiss()
                    }
                },
            ) { Text(if (sending) "Sending…" else "Send") }
        },
        dismissButton = { TextButton(enabled = !sending, onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CommentComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onInsertSticker: (StickerDef) -> Unit,
    onOpenHyperchat: () -> Unit = {},
) {
    var showStickers by remember { mutableStateOf(false) }
    var showEmojis by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                if (draft.isEmpty()) {
                    Text(
                        text = "Add a comment...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 4,
                )
            }
            IconButton(onClick = { showEmojis = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEmotions,
                    contentDescription = "Emoji",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = { showStickers = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.AddReaction,
                    contentDescription = "Sticker",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onOpenHyperchat, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.AttachMoney,
                    contentDescription = "Hyperchat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = onSubmit,
            enabled = draft.isNotBlank(),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Post",
                tint = if (draft.isNotBlank()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
    if (showStickers) {
        StickerPickerSheet(
            onDismiss = { showStickers = false },
            onPick = { sticker ->
                showStickers = false
                onInsertSticker(sticker)
            },
        )
    }
    if (showEmojis) {
        EmojiPickerSheet(
            onDismiss = { showEmojis = false },
            onPick = { token ->
                onDraftChange(draft + token)
            },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun StickerPickerSheet(onDismiss: () -> Unit, onPick: (StickerDef) -> Unit) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(80.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Free",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
            gridItems(items = FREE_GLOBAL_STICKERS, key = { "free-${it.name}" }) { sticker ->
                StickerCell(sticker = sticker, onPick = onPick)
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Tips",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
            gridItems(items = PAID_GLOBAL_STICKERS, key = { "paid-${it.name}" }) { sticker ->
                StickerCell(sticker = sticker, onPick = onPick)
            }
        }
    }
}

@Composable
private fun StickerCell(sticker: StickerDef, onPick: (StickerDef) -> Unit) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(72.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable { onPick(sticker) },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = sticker.url,
            contentDescription = sticker.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(4.dp),
        )
        val price = sticker.priceLbc
        if (price != null && price > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(Color(0xFFE2202D))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(
                    text = "$$price",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EmojiPickerSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(items = EMOTE_CATEGORIES, key = { _, c -> c.key }) { idx, cat ->
                val selected = idx == selectedCategoryIndex
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.surfaceVariant
                            else Color.Transparent
                        )
                        .clickable { selectedCategoryIndex = idx }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = cat.mainImg,
                        contentDescription = cat.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        val category = EMOTE_CATEGORIES[selectedCategoryIndex]
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(48.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
        ) {
            gridItems(items = category.items, key = { "${category.key}-${it.name}" }) { emote ->
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(44.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .clickable { onPick(emote.name) }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = emote.url,
                        contentDescription = emote.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentSortBar(
    currentSort: com.odysee.app.core.data.player.CommentSort,
    onSortChange: (com.odysee.app.core.data.player.CommentSort) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Sort by",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Box {
            TextButton(
                onClick = { open = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(currentSort.label, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                com.odysee.app.core.data.player.CommentSort.entries.forEach { sort ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(sort.label) },
                        onClick = {
                            onSortChange(sort)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentsBody(
    comments: CommentsState,
    repliesByParent: Map<String, List<CommentUiModel>> = emptyMap(),
    onRetry: () -> Unit,
    canReply: Boolean = false,
    onReply: (String, String) -> Unit = { _, _ -> },
    onLike: (String) -> Unit = {},
    onDislike: (String) -> Unit = {},
    onLoadReplies: (String) -> Unit = {},
    onPinToggle: (String, Boolean) -> Unit = { _, _ -> },
    onEdit: (String, String) -> Unit = { _, _ -> },
    onDelete: (String) -> Unit = {},
    onVisitChannel: (String, String) -> Unit = { _, _ -> },
    onBlock: (String) -> Unit = {},
    onAddModerator: (String) -> Unit = {},
    onRemoveModerator: (String) -> Unit = {},
    isClaimOwner: Boolean = false,
    linkedCommentId: String? = null,
) {
    var menuTarget by remember { mutableStateOf<CommentUiModel?>(null) }
    var editTarget by remember { mutableStateOf<CommentUiModel?>(null) }
    var deleteTarget by remember { mutableStateOf<CommentUiModel?>(null) }
    var replyOpenFor by remember { mutableStateOf<String?>(null) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var consumedLinkedId by remember { mutableStateOf<String?>(null) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    when (comments) {
        CommentsState.Idle, CommentsState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }
        is CommentsState.Error -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Couldn't load comments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = comments.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(8.dp))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
        is CommentsState.Success -> {
            if (comments.comments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No comments yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                androidx.compose.runtime.LaunchedEffect(linkedCommentId, comments.comments) {
                    val id = linkedCommentId ?: return@LaunchedEffect
                    if (consumedLinkedId == id) return@LaunchedEffect
                    val idx = comments.comments.indexOfFirst { it.id == id }
                    if (idx >= 0) {
                        consumedLinkedId = id
                        runCatching { listState.animateScrollToItem(idx) }
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(comments.comments, key = { it.id }) { comment ->
                        CommentThread(
                            comment = comment,
                            replies = repliesByParent[comment.id],
                            canReply = canReply,
                            replyOpen = replyOpenFor == comment.id,
                            onReplyOpenChange = { open ->
                                replyOpenFor = if (open) comment.id else null
                            },
                            onReply = { text ->
                                onReply(comment.id, text)
                                replyOpenFor = null
                            },
                            onLike = { onLike(comment.id) },
                            onDislike = { onDislike(comment.id) },
                            onLoadReplies = { onLoadReplies(comment.id) },
                            onReplyLike = { id -> onLike(id) },
                            onReplyDislike = { id -> onDislike(id) },
                            onLongPress = { menuTarget = it },
                            onReplyLongPress = { menuTarget = it },
                        )
                    }
                }
            }
        }
    }

    menuTarget?.let { target ->
        OdyseeCommentActionsSheet(
            comment = target.toOdyseeComment(),
            isClaimOwner = isClaimOwner,
            canReply = canReply,
            onDismiss = { menuTarget = null },
            actions = OdyseeCommentActions(
                onVisitChannel = {
                    target.authorChannelId?.let { onVisitChannel(it, target.author) }
                    menuTarget = null
                },
                onReply = {
                    replyOpenFor = target.id
                    menuTarget = null
                },
                onCopyText = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(target.body))
                    menuTarget = null
                },
                onCopyLink = {
                    val link = "https://odysee.com/?lc=${target.id}"
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(link))
                    menuTarget = null
                },
                onPinToggle = {
                    onPinToggle(target.id, target.isPinned)
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
                onBlock = {
                    onBlock(target.id)
                    menuTarget = null
                },
                onAddModerator = {
                    onAddModerator(target.id)
                    menuTarget = null
                },
                onRemoveModerator = {
                    onRemoveModerator(target.id)
                    menuTarget = null
                },
                onReport = {
                    val link = "https://odysee.com/\$/report-content?commentId=${target.id}"
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                    runCatching { context.startActivity(intent) }
                    menuTarget = null
                },
            ),
        )
    }

    editTarget?.let { target ->
        EditCommentDialog(
            initial = target.body,
            onDismiss = { editTarget = null },
            onConfirm = { newText ->
                onEdit(target.id, newText)
                editTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete comment?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) { Text("Delete", color = Color(0xFFE2202D)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
}

private fun CommentUiModel.toOdyseeComment(): OdyseeComment = OdyseeComment(
    id = id,
    parentId = parentId,
    authorDisplayName = authorTitle?.takeIf { it.isNotBlank() } ?: author,
    authorHandle = author,
    authorChannelId = authorChannelId,
    authorAvatarUrl = authorAvatarUrl,
    authorInitial = authorInitial,
    ageLabel = ageLabel,
    body = body,
    isPinned = isPinned,
    pinnedByName = pinnedByName,
    isEdited = isEdited,
    isCreator = isCreator,
    isMine = isMine,
    isModerator = isModerator,
    isGlobalMod = isGlobalMod,
    membership = when (authorPremiumTier) {
        com.odysee.app.core.data.auth.PremiumTier.PremiumPlus -> OdyseeMembershipTier.PremiumPlus
        com.odysee.app.core.data.auth.PremiumTier.Premium -> OdyseeMembershipTier.Premium
        else -> OdyseeMembershipTier.None
    },
    creatorMembership = creatorMembership,
    likes = likes,
    dislikes = dislikes,
    myReaction = when (myReaction) {
        com.odysee.app.core.data.reactions.MyReaction.LIKE -> OdyseeReaction.Like
        com.odysee.app.core.data.reactions.MyReaction.DISLIKE -> OdyseeReaction.Dislike
        else -> OdyseeReaction.None
    },
    supportAmount = supportAmount,
    creatorLiked = creatorLiked,
    creatorAvatarUrl = creatorAvatarUrl,
    replyCount = replyCount,
)

@Composable
private fun EditCommentDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit comment") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 6,
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank() && text != initial,
                onClick = { onConfirm(text.trim()) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ReactionRow(
    likes: Long,
    dislikes: Long,
    myReaction: com.odysee.app.core.data.reactions.MyReaction,
    onLike: () -> Unit,
    onDislike: () -> Unit,
) {
    val liked = myReaction == com.odysee.app.core.data.reactions.MyReaction.LIKE
    val disliked = myReaction == com.odysee.app.core.data.reactions.MyReaction.DISLIKE
    val fireColor = Color(0xFFC91800)
    val slimeColor = Color(0xFF7BC45E)
    Column(modifier = Modifier.fillMaxWidth()) {
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
                    painter = painterResource(
                        id = if (liked) DesignR.drawable.ic_reaction_fire_active
                        else DesignR.drawable.ic_reaction_fire,
                    ),
                    contentDescription = "Like",
                    modifier = Modifier.size(20.dp),
                    colorFilter = if (liked) null else androidx.compose.ui.graphics.ColorFilter.tint(
                        MaterialTheme.colorScheme.onBackground,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatReactionCount(likes),
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
                    painter = painterResource(
                        id = if (disliked) DesignR.drawable.ic_reaction_slime_active
                        else DesignR.drawable.ic_reaction_slime,
                    ),
                    contentDescription = "Dislike",
                    modifier = Modifier.size(20.dp),
                    colorFilter = if (disliked) null else androidx.compose.ui.graphics.ColorFilter.tint(
                        MaterialTheme.colorScheme.onBackground,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatReactionCount(dislikes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        RatioBar(
            likes = likes,
            dislikes = dislikes,
            likeColor = fireColor,
            dislikeColor = slimeColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun RatioBar(
    likes: Long,
    dislikes: Long,
    likeColor: Color,
    dislikeColor: Color,
    trackColor: Color,
) {
    val total = likes + dislikes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            .background(trackColor),
    ) {
        if (total > 0) {
            val likeFrac = likes.toFloat() / total.toFloat()
            if (likeFrac > 0f) {
                Box(
                    modifier = Modifier
                        .weight(likeFrac.coerceAtLeast(0.0001f))
                        .fillMaxHeight()
                        .background(likeColor),
                )
            }
            val dislikeFrac = 1f - likeFrac
            if (dislikeFrac > 0f) {
                Box(
                    modifier = Modifier
                        .weight(dislikeFrac.coerceAtLeast(0.0001f))
                        .fillMaxHeight()
                        .background(dislikeColor),
                )
            }
        }
    }
}

private fun formatReactionCount(count: Long): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0).removeSuffix(".0")
    count >= 1_000 -> "%.1fK".format(count / 1_000.0).removeSuffix(".0")
    else -> count.toString()
}

@Composable
private fun ActionRow(
    inWatchLater: Boolean,
    inFavorites: Boolean,
    onWatchLater: () -> Unit,
    onFavorite: () -> Unit,
    onSave: () -> Unit,
    onTip: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionButton(
            label = if (inWatchLater) "Saved" else "Later",
            icon = if (inWatchLater) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            highlight = inWatchLater,
            onClick = onWatchLater,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            label = if (inFavorites) "Faved" else "Fave",
            icon = if (inFavorites) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            highlight = inFavorites,
            onClick = onFavorite,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            label = "Save",
            icon = Icons.Outlined.Add,
            onClick = onSave,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            label = "Tip",
            icon = Icons.Outlined.AttachMoney,
            onClick = onTip,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            label = "Share",
            icon = Icons.Outlined.Share,
            onClick = onShare,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    val tint = if (highlight) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onBackground
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint)
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}

@Composable
private fun TipDialog(
    onDismiss: () -> Unit,
    channelName: String,
    controller: PlayerController,
) {
    var amount by remember { mutableStateOf("1") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text("Tip $channelName") },
        text = {
            Column {
                Text(
                    text = "Send LBC directly to the creator. Requires a funded wallet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { v -> amount = v.filter { it.isDigit() || it == '.' } },
                    label = { Text("Amount (LBC)") },
                    singleLine = true,
                    enabled = !sending && success == null,
                )
                success?.let {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Tip sent! tx ${it.take(10)}…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                error?.let {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            if (success != null) {
                TextButton(onClick = onDismiss) { Text("Close") }
            } else {
                TextButton(
                    enabled = !sending && amount.toDoubleOrNull()?.let { it > 0 } == true,
                    onClick = {
                        val v = amount.toDoubleOrNull() ?: return@TextButton
                        sending = true
                        error = null
                        scope.launch {
                            val r = controller.sendTip(v)
                            sending = false
                            r.fold(
                                onSuccess = { txid -> success = txid },
                                onFailure = { err -> error = err.message ?: "Tip failed" },
                            )
                        }
                    },
                ) {
                    Text(if (sending) "Sending…" else "Send tip")
                }
            }
        },
        dismissButton = {
            if (success == null) {
                TextButton(onClick = onDismiss, enabled = !sending) { Text("Cancel") }
            }
        },
    )
}

private fun formatFollowerCount(count: Long): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0).removeSuffix(".0M") + "M"
    count >= 1_000 -> "%.1fK".format(count / 1_000.0).removeSuffix(".0K") + "K"
    else -> count.toString()
}.replace(Regex("MM\$"), "M").replace(Regex("KK\$"), "K")

private fun formatViews(count: Long): String = when {
    count >= 1_000_000 -> "%.1f".format(count / 1_000_000.0).removeSuffix(".0") + "M"
    count >= 1_000 -> "%.1f".format(count / 1_000.0).removeSuffix(".0") + "K"
    else -> count.toString()
}

private fun buildShareUrl(permanentUrl: String): String {
    // permanentUrl looks like "lbry://@channel#abcd/name#xyz" — convert to https odysee URL
    val stripped = permanentUrl.removePrefix("lbry://")
    return "https://odysee.com/$stripped"
}

@Composable
private fun RelatedPage(
    state: RelatedState,
    moreFromChannelState: RelatedState = RelatedState.Idle,
    channelName: String? = null,
    onRetry: () -> Unit,
    onWatch: (RelatedItemUi) -> Unit,
    onLongPressRelated: (RelatedItemUi) -> Unit = {},
) {
    val hasChannel = channelName != null && moreFromChannelState !is RelatedState.Idle
    var showingMore by remember(channelName) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        if (hasChannel) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RelatedFilterChip(
                    label = "Related",
                    selected = !showingMore,
                    onClick = { showingMore = false },
                )
                RelatedFilterChip(
                    label = "More from ${channelName ?: "channel"}",
                    selected = showingMore,
                    onClick = { showingMore = true },
                )
            }
        }
        val active = if (showingMore) moreFromChannelState else state
        Box(modifier = Modifier.weight(1f)) {
            RelatedListContent(
                state = active,
                onRetry = onRetry,
                onWatch = onWatch,
                onLongPressRelated = onLongPressRelated,
            )
        }
    }
}

@Composable
private fun RelatedFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) Color.White
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun RelatedListContent(
    state: RelatedState,
    onRetry: () -> Unit,
    onWatch: (RelatedItemUi) -> Unit,
    onLongPressRelated: (RelatedItemUi) -> Unit = {},
) {
    when (state) {
        RelatedState.Idle, RelatedState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }
        is RelatedState.Error -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Couldn't load related",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(8.dp))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
        is RelatedState.Success -> {
            if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No related videos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.items, key = { it.claimId }) { item ->
                        RelatedRow(
                            item = item,
                            onClick = { onWatch(item) },
                            onLongPress = { onLongPressRelated(item) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun RelatedRow(
    item: RelatedItemUi,
    onClick: () -> Unit,
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!item.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (item.durationLabel.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = item.durationLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val channelLabel = item.channelTitle?.takeIf { it.isNotBlank() } ?: item.channelName
            Text(
                text = if (item.ageLabel.isNotEmpty()) "$channelLabel • ${item.ageLabel}" else channelLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CommentThread(
    comment: CommentUiModel,
    replies: List<CommentUiModel>?,
    canReply: Boolean,
    replyOpen: Boolean = false,
    onReplyOpenChange: (Boolean) -> Unit = {},
    onReply: (String) -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onLoadReplies: () -> Unit,
    onReplyLike: (String) -> Unit,
    onReplyDislike: (String) -> Unit,
    onLongPress: (CommentUiModel) -> Unit = {},
    onReplyLongPress: (CommentUiModel) -> Unit = {},
) {
    val rawReplies = replies
    OdyseeCommentThread(
        comment = comment.toOdyseeComment(),
        replies = rawReplies?.map { it.toOdyseeComment() },
        actions = OdyseeCommentActions(
            onLike = onLike,
            onDislike = onDislike,
            onReply = onReply,
        ),
        replyActionsFor = { rDisplay: OdyseeComment ->
            OdyseeCommentActions(
                onLike = { onReplyLike(rDisplay.id) },
                onDislike = { onReplyDislike(rDisplay.id) },
            )
        },
        canReply = canReply,
        replyOpen = replyOpen,
        onReplyOpenChange = onReplyOpenChange,
        onLoadReplies = onLoadReplies,
        onLongPress = { display: OdyseeComment ->
            val src = if (display.id == comment.id) comment
                else rawReplies?.firstOrNull { it.id == display.id }
            if (src != null) {
                if (src.id == comment.id) onLongPress(src) else onReplyLongPress(src)
            }
        },
    )
}

@Composable
private fun PaywallGateSurface(
    media: com.odysee.app.core.data.player.CurrentMedia,
    fillScreen: Boolean,
    paywall: com.odysee.app.core.model.Paywall,
    purchaseStatus: com.odysee.app.core.data.player.PurchaseStatus,
    onBuyLbc: () -> Unit,
    onFiatComplete: () -> Unit,
    onClearError: () -> Unit,
) {
    var showFiatWebView by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box(
        modifier = Modifier
            .let { if (fillScreen) it.fillMaxSize() else it.fillMaxWidth().aspectRatio(16f / 9f) }
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (!media.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = media.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
            )
        }
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = when (paywall) {
                    is com.odysee.app.core.model.Paywall.Lbc ->
                        "${formatLbcAmountForGate(paywall.amount)} LBC to unlock"
                    is com.odysee.app.core.model.Paywall.FiatPurchase ->
                        "$${"%.2f".format(paywall.usd)} to unlock"
                    is com.odysee.app.core.model.Paywall.FiatRental ->
                        "Rent for $${"%.2f".format(paywall.usd)}"
                    com.odysee.app.core.model.Paywall.Free -> ""
                },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (paywall is com.odysee.app.core.model.Paywall.FiatRental) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Access for ${formatRentalDuration(paywall.expirySeconds)}",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(16.dp))
            val processing = purchaseStatus is com.odysee.app.core.data.player.PurchaseStatus.Processing
            androidx.compose.material3.Button(
                onClick = {
                    when (paywall) {
                        is com.odysee.app.core.model.Paywall.Lbc -> onBuyLbc()
                        is com.odysee.app.core.model.Paywall.FiatPurchase,
                        is com.odysee.app.core.model.Paywall.FiatRental -> showFiatWebView = true
                        com.odysee.app.core.model.Paywall.Free -> Unit
                    }
                },
                enabled = !processing,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                if (processing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        text = when (paywall) {
                            is com.odysee.app.core.model.Paywall.Lbc -> "Pay with LBC"
                            is com.odysee.app.core.model.Paywall.FiatPurchase -> "Buy"
                            is com.odysee.app.core.model.Paywall.FiatRental -> "Rent"
                            com.odysee.app.core.model.Paywall.Free -> ""
                        },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            (purchaseStatus as? com.odysee.app.core.data.player.PurchaseStatus.Failed)?.let { failed ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = failed.message,
                    color = Color(0xFFFF7B7B),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clickable(onClick = onClearError)
                        .padding(8.dp),
                )
            }
        }
    }
    if (showFiatWebView) {
        FiatPurchaseWebViewDialog(
            media = media,
            onDismiss = {
                showFiatWebView = false
                onFiatComplete()
            },
        )
    }
}

@Composable
private fun MembersOnlyGateSurface(
    media: com.odysee.app.core.data.player.CurrentMedia,
    fillScreen: Boolean,
    onJoinComplete: () -> Unit,
) {
    var showWebView by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box(
        modifier = Modifier
            .let { if (fillScreen) it.fillMaxSize() else it.fillMaxWidth().aspectRatio(16f / 9f) }
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (!media.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = media.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)))
        }
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Members-only content",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Join ${media.channelTitle ?: media.channelName.removePrefix("@")}'s membership to watch",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = { showWebView = true },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("Join membership", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (showWebView) {
        MembershipJoinWebViewDialog(
            media = media,
            onDismiss = {
                showWebView = false
                onJoinComplete()
            },
        )
    }
}

@Composable
private fun MembershipJoinWebViewDialog(
    media: com.odysee.app.core.data.player.CurrentMedia,
    onDismiss: () -> Unit,
) {
    val channelHandle = media.channelName.trim().removePrefix("@")
    val url = buildString {
        append("https://odysee.com/")
        if (channelHandle.isNotBlank()) {
            append('@').append(channelHandle)
            media.channelClaimId?.let { append(':').append(it) }
        }
        append("?view=memberships")
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                    Text(
                        text = "Join membership",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                }
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = android.webkit.WebViewClient()
                            loadUrl(url)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun FiatPurchaseWebViewDialog(
    media: com.odysee.app.core.data.player.CurrentMedia,
    onDismiss: () -> Unit,
) {
    // Parse the stream name out of permanentUrl: "lbry://<name>#<claimId>".
    val streamName = media.permanentUrl
        .removePrefix("lbry://")
        .substringBefore('#')
        .takeIf { it.isNotBlank() }
        ?: media.claimId
    val canonicalUrl = buildString {
        append("https://odysee.com/")
        val channelHandle = media.channelName.trim().removePrefix("@")
        if (channelHandle.isNotBlank()) {
            append('@')
            append(channelHandle)
            media.channelClaimId?.let { append(':').append(it) }
            append('/')
        }
        append(streamName).append(':').append(media.claimId)
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                    Text(
                        text = "Complete purchase",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                }
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = android.webkit.WebViewClient()
                            loadUrl(canonicalUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun formatLbcAmountForGate(amount: Double): String =
    if (amount == amount.toLong().toDouble()) "${amount.toLong()}"
    else "%.2f".format(amount).trimEnd('0').trimEnd('.')

private fun formatRentalDuration(seconds: Long): String {
    if (seconds <= 0) return ""
    val hours = seconds / 3600
    val days = hours / 24
    return when {
        days >= 30 -> "${days / 30} months"
        days > 1 -> "$days days"
        days == 1L -> "1 day"
        hours > 1 -> "$hours hours"
        else -> "${seconds / 60} minutes"
    }
}

@Composable
private fun CastPlaceholderSurface(
    thumbnailUrl: String?,
    fillScreen: Boolean,
    onPlay: () -> Unit,
) {
    Box(
        modifier = Modifier
            .let { if (fillScreen) it.fillMaxSize() else it.fillMaxWidth().aspectRatio(16f / 9f) }
            .background(Color.Black)
            .clickable(onClick = onPlay),
        contentAlignment = Alignment.Center,
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
            )
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play on cast device",
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
        Text(
            text = "Tap to play on your cast device",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PipPlaceholderSurface(
    thumbnailUrl: String?,
    fillScreen: Boolean,
    onResume: () -> Unit,
) {
    Box(
        modifier = Modifier
            .let { if (fillScreen) it.fillMaxSize() else it.fillMaxWidth().aspectRatio(16f / 9f) }
            .background(Color.Black)
            .clickable(onClick = onResume),
        contentAlignment = Alignment.Center,
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
            )
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Resume inline",
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
        Text(
            text = "Playing in pop-up player • tap to resume here",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PlayerSurface(
    streamingUrl: String?,
    isResolving: Boolean,
    isBuffering: Boolean = false,
    errorMessage: String?,
    thumbnailUrl: String?,
    onRetry: () -> Unit,
    fillScreen: Boolean = false,
    exoPlayerFactory: (android.content.Context) -> PlayerView,
    onUpdate: (PlayerView) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .let { if (fillScreen) it.fillMaxSize() else it.fillMaxWidth().aspectRatio(16f / 9f) }
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (streamingUrl != null) {
            val context = LocalContext.current
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> exoPlayerFactory(ctx) },
                update = { onUpdate(it) },
            )
            if (isBuffering) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (isResolving) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            CircularProgressIndicator(color = Color.White)
        } else if (errorMessage != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Couldn't load this video",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = errorMessage,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(8.dp))
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun PlaylistTabContent(
    playlist: com.odysee.app.core.data.player.PlaylistContext?,
    currentClaimId: String?,
    onSelect: (Int) -> Unit,
) {
    if (playlist == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No playlist active",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val cur = playlist.currentIndex + 1
            Text(
                text = "$cur / ${playlist.items.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(playlist.items.size, key = { playlist.items[it].claimId }) { idx ->
                val item = playlist.items[idx]
                val isCurrent = item.claimId == currentClaimId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.Transparent,
                        )
                        .clickable { onSelect(idx) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${idx + 1}",
                        modifier = Modifier.width(24.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .aspectRatio(16f / 9f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        if (!item.thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.channelName.isNotBlank()) {
                            Text(
                                text = item.channelName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.AudioCoverOverlay(thumbnailUrl: String?) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .aspectRatio(1f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .aspectRatio(1f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "♪",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displayLarge,
                )
            }
        }
    }
}

@Composable
private fun ImageMediaSurface(state: com.odysee.app.core.data.player.PlayerState, fillScreen: Boolean) {
    Box(
        modifier = (if (fillScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val url = state.streamingUrl ?: state.media?.thumbnailUrl
        if (state.isResolving || url == null) {
            CircularProgressIndicator(color = Color.White)
        } else {
            AsyncImage(
                model = url,
                contentDescription = state.media?.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun TextMediaSurface(state: com.odysee.app.core.data.player.PlayerState, fillScreen: Boolean) {
    Box(
        modifier = if (fillScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        contentAlignment = Alignment.Center,
    ) {
        if (state.isResolving) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            val url = state.streamingUrl
            if (url == null) {
                Text(
                    text = "Text content unavailable",
                    color = MaterialTheme.colorScheme.onBackground,
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            loadUrl(url)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PdfMediaSurface(state: com.odysee.app.core.data.player.PlayerState, fillScreen: Boolean) {
    val context = LocalContext.current
    var pages by remember(state.streamingUrl) {
        mutableStateOf<List<android.graphics.Bitmap>>(emptyList())
    }
    var error by remember(state.streamingUrl) { mutableStateOf<String?>(null) }
    val url = state.streamingUrl
    LaunchedEffect(url) {
        if (url.isNullOrBlank()) return@LaunchedEffect
        pages = emptyList()
        error = null
        val result = runCatching {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val tmp = java.io.File.createTempFile("odysee-pdf", ".pdf", context.cacheDir)
                java.net.URL(url).openStream().use { input ->
                    tmp.outputStream().use { input.copyTo(it) }
                }
                val pfd = android.os.ParcelFileDescriptor.open(
                    tmp,
                    android.os.ParcelFileDescriptor.MODE_READ_ONLY,
                )
                val renderer = android.graphics.pdf.PdfRenderer(pfd)
                val list = (0 until renderer.pageCount).map { i ->
                    val page = renderer.openPage(i)
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        android.graphics.Bitmap.Config.ARGB_8888,
                    )
                    page.render(
                        bitmap,
                        null,
                        null,
                        android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                    )
                    page.close()
                    bitmap
                }
                renderer.close()
                pfd.close()
                tmp.delete()
                list
            }
        }
        result.fold(
            onSuccess = { pages = it },
            onFailure = { error = it.message ?: "Couldn't render PDF" },
        )
    }
    Box(
        modifier = if (fillScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(3f / 4f),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.isResolving || (url != null && pages.isEmpty() && error == null) ->
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            error != null -> Text(
                text = "PDF error: $error",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
                items(pages.size, key = { it }) { idx ->
                    androidx.compose.foundation.Image(
                        bitmap = pages[idx].asImageBitmap(),
                        contentDescription = "PDF page ${idx + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadMediaSurface(
    state: com.odysee.app.core.data.player.PlayerState,
    media: com.odysee.app.core.data.player.CurrentMedia,
    fillScreen: Boolean,
) {
    val context = LocalContext.current
    Box(
        modifier = if (fillScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "This file type can't be played in-app",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = media.mediaType ?: "Unknown type",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = {
                    val url = state.streamingUrl ?: return@TextButton
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(url),
                    )
                    runCatching { context.startActivity(intent) }
                },
            ) { Text("Open externally") }
        }
    }
}
