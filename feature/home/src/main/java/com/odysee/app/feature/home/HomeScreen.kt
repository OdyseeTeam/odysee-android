package com.odysee.app.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.odysee.app.core.designsystem.layout.feedColumns
import com.odysee.app.core.designsystem.layout.rememberWindowSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import com.odysee.app.core.designsystem.R as DesignR
import com.odysee.app.core.designsystem.theme.OdyseeTheme

val LocalHazeState = androidx.compose.runtime.compositionLocalOf<HazeState?> { null }

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onWatchClick: (VideoUiModel) -> Unit,
    onPlayBackground: (VideoUiModel) -> Unit = {},
    onPlayPip: (VideoUiModel) -> Unit = {},
    onChannelClick: (String, String) -> Unit,
    onAccountClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    signedInEmail: String? = null,
    signedInAvatarUrl: String? = null,
    premiumTier: com.odysee.app.core.data.auth.PremiumTier = com.odysee.app.core.data.auth.PremiumTier.None,
    showOdyseeTopBar: Boolean = true,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreenContent(
        state = state,
        onRetryHomepage = viewModel::loadHomepage,
        onSelectCategory = viewModel::selectCategory,
        onRetryFeed = viewModel::retry,
        onRetryFollowing = viewModel::retryFollowing,
        onLoadMore = viewModel::loadMoreCurrent,
        onWatchClick = onWatchClick,
        onPlayBackground = onPlayBackground,
        onPlayPip = onPlayPip,
        onChannelClick = onChannelClick,
        onAccountClick = onAccountClick,
        onSearchClick = onSearchClick,
        onNotificationsClick = onNotificationsClick,
        onSaveWatchLater = viewModel::saveToWatchLater,
        onSaveFavorite = viewModel::saveToFavorites,
        onSelectContentTypes = viewModel::setContentTypes,
        signedInEmail = signedInEmail,
        signedInAvatarUrl = signedInAvatarUrl,
        premiumTier = premiumTier,
        showOdyseeTopBar = showOdyseeTopBar,
    )
}

/** Reusable header for the home/playlists tabs — wordmark on the left, search/notifications/account on the right. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdyseeHomeHeader(
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAccountClick: () -> Unit,
    signedInEmail: String?,
    signedInAvatarUrl: String?,
    unseenNotifications: Int = 0,
    premiumTier: com.odysee.app.core.data.auth.PremiumTier = com.odysee.app.core.data.auth.PremiumTier.None,
    isSignedIn: Boolean = signedInEmail != null,
) {
    val hazeState = LocalHazeState.current
    Box(
        modifier = Modifier.let { m ->
            if (hazeState != null) m.hazeEffect(state = hazeState, style = HazeMaterials.thick()) else m
        },
    ) {
    TopAppBar(
        title = { OdyseeWordmark() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (hazeState != null) androidx.compose.ui.graphics.Color.Transparent
            else MaterialTheme.colorScheme.background,
        ),
        actions = {
            HeaderCircleIconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            }
            if (isSignedIn) {
                Spacer(Modifier.width(6.dp))
                HeaderCircleIconButton(onClick = onNotificationsClick) {
                    androidx.compose.material3.BadgedBox(
                        badge = {
                            if (unseenNotifications > 0) {
                                androidx.compose.material3.Badge(
                                    containerColor = Color(0xFFE2202D),
                                    contentColor = Color.White,
                                ) {
                                    Text(
                                        text = if (unseenNotifications > 99) "99+"
                                        else unseenNotifications.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            AccountButton(
                email = signedInEmail,
                avatarUrl = signedInAvatarUrl,
                onClick = onAccountClick,
                premiumTier = premiumTier,
            )
            Spacer(Modifier.width(8.dp))
        },
    )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    state: HomeUiState,
    onRetryHomepage: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onRetryFeed: () -> Unit,
    onRetryFollowing: () -> Unit,
    onLoadMore: () -> Unit = {},
    onWatchClick: (VideoUiModel) -> Unit,
    onPlayBackground: (VideoUiModel) -> Unit,
    onPlayPip: (VideoUiModel) -> Unit,
    onChannelClick: (String, String) -> Unit,
    onAccountClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSaveWatchLater: (VideoUiModel) -> Unit = {},
    onSaveFavorite: (VideoUiModel) -> Unit = {},
    onSelectContentTypes: (Set<String>) -> Unit = {},
    signedInEmail: String?,
    signedInAvatarUrl: String?,
    premiumTier: com.odysee.app.core.data.auth.PremiumTier = com.odysee.app.core.data.auth.PremiumTier.None,
    showOdyseeTopBar: Boolean = true,
) {
    val hazeState = LocalHazeState.current ?: remember { HazeState() }
    var scrollToTopTick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.compose.runtime.CompositionLocalProvider(LocalHazeState provides hazeState) {
    Scaffold(
        topBar = {
            Column {
                if (!showOdyseeTopBar) {
                    Spacer(
                        modifier = Modifier
                            .statusBarsPadding()
                            .height(64.dp),
                    )
                }
                if (showOdyseeTopBar) {
                    Box(modifier = Modifier.hazeEffect(state = hazeState, style = HazeMaterials.thick())) {
                        TopAppBar(
                            modifier = Modifier.displayCutoutPadding(),
                            title = {
                                OdyseeWordmark(onClick = { scrollToTopTick += 1 })
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                            actions = {
                                HeaderCircleIconButton(onClick = onSearchClick) {
                                    Icon(Icons.Outlined.Search, contentDescription = "Search")
                                }
                                if (state.isSignedIn) {
                                    Spacer(Modifier.width(6.dp))
                                    HeaderCircleIconButton(onClick = onNotificationsClick) {
                                        androidx.compose.material3.BadgedBox(
                                            badge = {
                                                if (state.unseenNotifications > 0) {
                                                    androidx.compose.material3.Badge(
                                                        containerColor = Color(0xFFE2202D),
                                                        contentColor = Color.White,
                                                    ) {
                                                        Text(
                                                            text = if (state.unseenNotifications > 99) "99+"
                                                            else state.unseenNotifications.toString(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                        )
                                                    }
                                                }
                                            },
                                        ) {
                                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                                        }
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                AccountButton(
                                    email = signedInEmail,
                                    avatarUrl = signedInAvatarUrl,
                                    onClick = onAccountClick,
                                    premiumTier = premiumTier,
                                )
                            },
                        )
                    }
                }
                Column(
                    modifier = Modifier.hazeEffect(state = hazeState, style = HazeMaterials.thin()),
                ) {
                    ContentTypeFilterRow(
                        selected = state.selectedContentTypes,
                        onChange = onSelectContentTypes,
                    )
                    if (state.categories.isNotEmpty()) {
                        CategoryChipRow(
                            categories = state.categories,
                            selectedId = state.selectedCategoryId,
                            onSelect = onSelectCategory,
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val isFollowing = state.selectedCategoryId == HomeViewModel.FOLLOWING_ID
        when {
            state.isLoadingHomepage && state.categories.isEmpty() -> CenteredLoading(innerPadding)
            state.homepageError != null && state.categories.isEmpty() -> CenteredError(
                message = state.homepageError,
                onRetry = onRetryHomepage,
                padding = innerPadding,
            )
            isFollowing -> {
                val wantsLive = "live" in state.selectedContentTypes || "upcoming" in state.selectedContentTypes
                val pending = state.isLivestreamFetching && wantsLive
                val filteredFollowing = if (pending) FeedState.Loading
                    else state.followingFeed
                        .filterByTypes(state.selectedContentTypes)
                        .mergeLive(state)
                val empty = filteredFollowing is FeedState.Success && filteredFollowing.videos.isEmpty()
                if (empty) {
                    EmptyState(
                        title = "Follow your favorites",
                        message = "Tap Follow on any channel page and their newest videos show up here.",
                        padding = innerPadding,
                    )
                } else {
                    FeedPane(
                        feed = filteredFollowing,
                        padding = innerPadding,
                        onRetry = onRetryFollowing,
                        onWatchClick = onWatchClick,
                        onPlayBackground = onPlayBackground,
                        onPlayPip = onPlayPip,
                        onChannelClick = onChannelClick,
                        onSaveWatchLater = onSaveWatchLater,
                        onSaveFavorite = onSaveFavorite,
                        onRefresh = onRetryFollowing,
                        onLoadMore = onLoadMore,
                        scrollToTopTick = scrollToTopTick,
                    )
                }
            }
            else -> {
                val wantsLive = "live" in state.selectedContentTypes || "upcoming" in state.selectedContentTypes
                val pending = state.isLivestreamFetching && wantsLive
                val feed = if (pending) FeedState.Loading
                    else state.feed.filterByTypes(state.selectedContentTypes).mergeLive(state)
                FeedPane(
                    feed = feed,
                    padding = innerPadding,
                onRetry = onRetryFeed,
                onWatchClick = onWatchClick,
                onPlayBackground = onPlayBackground,
                onPlayPip = onPlayPip,
                onChannelClick = onChannelClick,
                onSaveWatchLater = onSaveWatchLater,
                onSaveFavorite = onSaveFavorite,
                onRefresh = onRetryFeed,
                onLoadMore = onLoadMore,
                scrollToTopTick = scrollToTopTick,
            )
            }
        }
    }
    }
}

@Composable
private fun HeaderCircleIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun FeedState.filterByTypes(types: Set<String>): FeedState {
    if (this !is FeedState.Success) return this
    val mediaOn = "videos" in types
    val shortsOn = "shorts" in types
    if (mediaOn && shortsOn) return this
    val filtered = videos.filter { v ->
        if (v.isShort) shortsOn else mediaOn
    }
    return FeedState.Success(filtered)
}

private fun FeedState.mergeLive(state: HomeUiState): FeedState {
    if (this !is FeedState.Success) return this
    // A live or upcoming claim can also appear in the regular following feed
    // (it's the same claim ID either way). Dedup by id; the live/upcoming
    // copies are preferred because they carry the live decoration.
    val list = mutableListOf<VideoUiModel>()
    if ("live" in state.selectedContentTypes) list.addAll(state.livestreamFeed)
    if ("upcoming" in state.selectedContentTypes) list.addAll(state.upcomingFeed)
    list.addAll(videos)
    return FeedState.Success(list.distinctBy { it.id })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentTypeFilterRow(
    selected: Set<String>,
    onChange: (Set<String>) -> Unit,
) {
    val options = remember {
        listOf(
            "videos" to "Media",
            "shorts" to "Shorts",
            "live" to "Live",
            "upcoming" to "Upcoming",
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 0.dp,
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(options, key = { it.first }) { option ->
                val value = option.first
                val label = option.second
                val isSelected = value in selected
                val border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val next = if (isSelected) selected - value else selected + value
                        onChange(next)
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                    ),
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = border,
                )
            }
        }
    }
}

@Composable
private fun CategoryChipRow(
    categories: List<CategoryChip>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val firstId = categories.firstOrNull()?.id
    androidx.compose.runtime.LaunchedEffect(firstId) {
        if (firstId != null) listState.scrollToItem(0)
    }
    val selectedIndex = categories.indexOfFirst { it.id == selectedId }
    androidx.compose.runtime.LaunchedEffect(selectedIndex, categories.size) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }
    Box {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories, key = { it.id }) { chip ->
                val iconRes = categoryIconRes(chip.iconName)
                FilterChip(
                    selected = chip.id == selectedId,
                    onClick = { onSelect(chip.id) },
                    label = { Text(chip.label) },
                    leadingIcon = iconRes?.let { resId ->
                        {
                            val tint = if (chip.id == selectedId) MaterialTheme.colorScheme.background
                            else MaterialTheme.colorScheme.onSurfaceVariant
                            Icon(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                        selectedLabelColor = MaterialTheme.colorScheme.background,
                    ),
                    border = null,
                )
            }
        }
    }
}

@androidx.annotation.DrawableRes
private fun categoryIconRes(iconName: String?): Int? = when (iconName) {
    "Speaker" -> DesignR.drawable.ic_category_speaker
    "Aperature" -> DesignR.drawable.ic_category_aperature
    "DollarSign" -> DesignR.drawable.ic_category_dollarsign
    "MusicCategory" -> DesignR.drawable.ic_category_music
    "WildWest" -> DesignR.drawable.ic_category_wildwest
    "Universe" -> DesignR.drawable.ic_category_universe
    "MindBlown" -> DesignR.drawable.ic_category_mindblown
    "Gaming" -> DesignR.drawable.ic_category_gaming
    "Sports" -> DesignR.drawable.ic_category_sports
    "Artists" -> DesignR.drawable.ic_category_artists
    "Education" -> DesignR.drawable.ic_category_education
    "Pop Culture" -> DesignR.drawable.ic_category_pop_culture
    "Comedy" -> DesignR.drawable.ic_category_comedy
    "Lifestyle" -> DesignR.drawable.ic_category_lifestyle
    "Spirituality" -> DesignR.drawable.ic_category_spirituality
    "Horror" -> DesignR.drawable.ic_category_horror
    "Featured" -> DesignR.drawable.ic_category_featured
    "Life" -> DesignR.drawable.ic_category_life
    "Peace" -> DesignR.drawable.ic_category_peace
    else -> null
}

@Composable
private fun FeedPane(
    feed: FeedState,
    padding: PaddingValues,
    onRetry: () -> Unit,
    onWatchClick: (VideoUiModel) -> Unit,
    onPlayBackground: (VideoUiModel) -> Unit,
    onPlayPip: (VideoUiModel) -> Unit,
    onChannelClick: (String, String) -> Unit,
    onSaveWatchLater: (VideoUiModel) -> Unit,
    onSaveFavorite: (VideoUiModel) -> Unit,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    scrollToTopTick: Int = 0,
) {
    when (feed) {
        FeedState.Loading -> CenteredLoading(padding)
        is FeedState.Error -> CenteredError(
            message = feed.message,
            onRetry = onRetry,
            padding = padding,
        )
        is FeedState.Success -> VerticalVideoFeed(
            videos = feed.videos,
            padding = padding,
            onWatchClick = onWatchClick,
            onPlayBackground = onPlayBackground,
            onPlayPip = onPlayPip,
            onChannelClick = onChannelClick,
            onSaveWatchLater = onSaveWatchLater,
            onSaveFavorite = onSaveFavorite,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            scrollToTopTick = scrollToTopTick,
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun VerticalVideoFeed(
    videos: List<VideoUiModel>,
    padding: PaddingValues,
    onWatchClick: (VideoUiModel) -> Unit,
    onPlayBackground: (VideoUiModel) -> Unit,
    onPlayPip: (VideoUiModel) -> Unit,
    onChannelClick: (String, String) -> Unit,
    onSaveWatchLater: (VideoUiModel) -> Unit,
    onSaveFavorite: (VideoUiModel) -> Unit,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    isRefreshing: Boolean = false,
    scrollToTopTick: Int = 0,
) {
    var addToPlaylistVideo by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<VideoUiModel?>(null)
    }
    var moreSheetVideo by androidx.compose.runtime.saveable.rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.Saver<VideoUiModel?, String>(
            save = { it?.id ?: "" },
            restore = { null },
        ),
    ) { androidx.compose.runtime.mutableStateOf<VideoUiModel?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val columns = rememberWindowSize().feedColumns()
    val gridState = rememberLazyGridState()

    // Belt-and-braces dedup: LazyVerticalGrid crashes hard if any key repeats.
    // Every assembly path upstream is meant to be unique, but a single new
    // merge that forgets to dedup would crash for every user. Cheap to keep.
    val videos = remember(videos) { videos.distinctBy { it.id } }

    val topKey = videos.firstOrNull()?.id
    androidx.compose.runtime.LaunchedEffect(topKey) {
        if (gridState.firstVisibleItemIndex > 0) {
            gridState.animateScrollToItem(0)
        }
    }

    androidx.compose.runtime.LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) gridState.animateScrollToItem(0)
    }

    androidx.compose.runtime.LaunchedEffect(gridState, videos.size) {
        androidx.compose.runtime.snapshotFlow {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last to info.totalItemsCount
        }.collect { (lastVisible, total) ->
            if (total > 0 && lastVisible >= total - 4) {
                onLoadMore()
            }
        }
    }

    val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = padding.calculateTopPadding()),
                state = pullState,
                isRefreshing = isRefreshing,
            )
        },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .let { m -> LocalHazeState.current?.let { m.hazeSource(state = it) } ?: m }
                .displayCutoutPadding(),
            contentPadding = PaddingValues(
                start = if (columns > 1) 12.dp else 0.dp,
                end = if (columns > 1) 12.dp else 0.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(if (columns > 1) 12.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(videos, key = { it.id }) { video ->
                VideoCard(
                    video = video,
                    onClick = { onWatchClick(video) },
                    onChannelClick = {
                        video.channelClaimId?.let { id -> onChannelClick(id, video.channelName) }
                    },
                    onMoreClick = { moreSheetVideo = video },
                    forceColumnLayout = columns > 1,
                )
            }
        }
    }

    moreSheetVideo?.let { video ->
        Dialog(
            onDismissRequest = { moreSheetVideo = null },
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
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    SheetRow(label = "Play in background") {
                        onPlayBackground(video)
                        moreSheetVideo = null
                    }
                    SheetRow(label = "Play in pop-up player") {
                        onPlayPip(video)
                        moreSheetVideo = null
                    }
                    SheetRow(label = "Save to Watch Later") {
                        onSaveWatchLater(video)
                        moreSheetVideo = null
                    }
                    SheetRow(label = "Add to Favorites") {
                        onSaveFavorite(video)
                        moreSheetVideo = null
                    }
                    SheetRow(label = "Add to playlist…") {
                        addToPlaylistVideo = video
                        moreSheetVideo = null
                    }
                    SheetRow(label = "Share") {
                        val stripped = video.permanentUrl.removePrefix("lbry://")
                        val url = "https://odysee.com/$stripped"
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, video.title)
                            putExtra(android.content.Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
                        moreSheetVideo = null
                    }
                    video.channelClaimId?.let { channelId ->
                        SheetRow(label = "Go to ${video.channelName}") {
                            moreSheetVideo = null
                            onChannelClick(channelId, video.channelName)
                        }
                    }
                }
            }
        }
    }

    addToPlaylistVideo?.let { video ->
        com.odysee.app.feature.library.AddToPlaylistSheet(
            title = video.title,
            permanentUrl = video.permanentUrl,
            onDismiss = { addToPlaylistVideo = null },
            onCreateNew = { addToPlaylistVideo = null },
            quickTarget = com.odysee.app.feature.library.QuickTargetClaim(
                claimId = video.id,
                permanentUrl = video.permanentUrl,
                title = video.title,
                channelName = video.channelName,
                channelClaimId = video.channelClaimId,
                thumbnailUrl = video.thumbnailUrl,
            ),
        )
    }
}

@Composable
private fun SheetRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun CenteredLoading(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState(title: String, message: String, padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun YouPane(
    padding: PaddingValues,
    signedInEmail: String?,
    signedInAvatarUrl: String?,
    onAccountClick: () -> Unit,
    subscriptions: List<com.odysee.app.core.data.subscriptions.Subscription>,
    onChannelClick: (String, String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 16.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
            start = 16.dp,
            end = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onAccountClick),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!signedInAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = signedInAvatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = (signedInEmail?.firstOrNull() ?: '?').uppercaseChar().toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = signedInEmail ?: "Not signed in",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    TextButton(onClick = onAccountClick, contentPadding = PaddingValues(0.dp)) {
                        Text(if (signedInEmail != null) "Account menu" else "Sign in")
                    }
                }
            }
        }
        if (subscriptions.isNotEmpty()) {
            item {
                Text(
                    text = "Following",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            items(subscriptions, key = { it.claimId }) { sub ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChannelClick(sub.claimId, sub.name) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (sub.name.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar().toString(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = sub.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredError(message: String, onRetry: () -> Unit, padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Couldn't load the feed",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(16.dp))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun AccountButton(
    email: String?,
    avatarUrl: String?,
    onClick: () -> Unit,
    premiumTier: com.odysee.app.core.data.auth.PremiumTier = com.odysee.app.core.data.auth.PremiumTier.None,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
        when {
            !avatarUrl.isNullOrBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = email ?: "Account",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            !email.isNullOrBlank() -> {
                val initial = email.first().uppercaseChar()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = "Sign in")
            }
        }
        }
        if (premiumTier != com.odysee.app.core.data.auth.PremiumTier.None) {
            val res = if (premiumTier == com.odysee.app.core.data.auth.PremiumTier.PremiumPlus)
                DesignR.drawable.badge_premium_plus
            else DesignR.drawable.badge_premium
            Image(
                painter = painterResource(id = res),
                contentDescription = "Premium",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 8.dp)
                    .size(16.dp),
            )
        }
    }
}

@Composable
private fun OdyseeWordmark(onClick: (() -> Unit)? = null) {
    Image(
        painter = painterResource(id = DesignR.drawable.odysee_wordmark),
        contentDescription = "Odysee",
        modifier = Modifier
            .height(38.dp)
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m },
    )
}

@Composable
private fun VideoCard(
    video: VideoUiModel,
    onClick: () -> Unit,
    onChannelClick: () -> Unit,
    onMoreClick: () -> Unit,
    forceColumnLayout: Boolean = false,
) {
    com.odysee.app.core.designsystem.claims.OdyseeClaimCard(
        forceColumnLayout = forceColumnLayout,
        claim = com.odysee.app.core.designsystem.claims.OdyseeClaimCardModel(
            claimId = video.id,
            title = video.title,
            channelName = video.channelName,
            channelClaimId = video.channelClaimId,
            channelTitle = video.channelTitle,
            channelAvatarUrl = video.channelAvatarUrl,
            channelInitial = video.channelInitial,
            thumbnailUrl = video.thumbnailUrl,
            durationLabel = video.durationLabel,
            ageLabel = video.ageLabel,
            viewCount = video.viewCount,
            isLivestream = video.isLivestream,
            isUpcoming = video.isUpcoming,
            isShort = video.isShort,
            thumbnailTintIndex = video.thumbnailTintIndex,
            channelAvatarTintIndex = video.channelAvatarTintIndex,
            paywall = com.odysee.app.core.designsystem.claims.toCardPaywall(video.paywall),
            isPurchased = video.isPurchased,
            isMembersOnly = video.isMembersOnly,
        ),
        onClick = onClick,
        onChannelClick = onChannelClick,
        onLongPress = onMoreClick,
    )
}

@Composable
private fun BoxScope.ThumbnailOverlay(video: VideoUiModel) {
    if (!video.thumbnailUrl.isNullOrBlank()) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
    when {
        video.isLivestream -> {
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
                        .clip(androidx.compose.foundation.shape.CircleShape)
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
        video.isUpcoming -> {
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
        video.durationLabel.isNotEmpty() -> {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = video.durationLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun VideoMeta(
    video: VideoUiModel,
    onChannelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = buildString {
                append(video.channelName)
                video.viewCount?.let {
                    append(" • ")
                    append(formatViewCount(it))
                    append(" views")
                }
                if (video.ageLabel.isNotEmpty()) {
                    append(" • ")
                    append(video.ageLabel)
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClick = onChannelClick),
        )
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

private fun formatViewCount(count: Long): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0).removeSuffix(".0")
    count >= 1_000 -> "%.1fK".format(count / 1_000.0).removeSuffix(".0")
    else -> count.toString()
}

private val thumbnailPalette = listOf(
    Color(0xFF7B2942),
    Color(0xFF2C5364),
    Color(0xFF4A4E69),
    Color(0xFF3D405B),
    Color(0xFF52489C),
    Color(0xFF2A4858),
)

private val avatarPalette = listOf(
    Color(0xFFE50054),
    Color(0xFF2EC4B6),
    Color(0xFFFF9F1C),
    Color(0xFF5E60CE),
    Color(0xFF06D6A0),
    Color(0xFFEF476F),
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    OdyseeTheme {
        HomeScreenContent(
            state = HomeUiState(
                isLoadingHomepage = false,
                baseCategories = listOf(
                    CategoryChip("FEATURED", "Featured", "Featured"),
                    CategoryChip("TECH", "Tech", "Speaker"),
                    CategoryChip("MUSIC", "Music", "MusicCategory"),
                ),
                selectedCategoryId = "FEATURED",
                feed = FeedState.Success(
                    listOf(
                        VideoUiModel(
                            id = "1",
                            permanentUrl = "lbry://example#1",
                            title = "A long video title that wraps to two lines",
                            description = null,
                            channelClaimId = null,
                            channelName = "@SomeChannel",
                            channelInitial = 'S',
                            channelAvatarUrl = null,
                            channelAvatarTintIndex = 0,
                            thumbnailUrl = null,
                            thumbnailTintIndex = 0,
                            ageLabel = "1d ago",
                            durationLabel = "12:47",
                        ),
                    ),
                ),
            ),
            onRetryHomepage = {},
            onSelectCategory = {},
            onRetryFeed = {},
            onRetryFollowing = {},
            onWatchClick = {},
            onPlayBackground = {},
            onPlayPip = {},
            onChannelClick = { _, _ -> },
            onAccountClick = {},
            signedInEmail = null,
            signedInAvatarUrl = null,
        )
    }
}
