package com.odysee.app.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.odysee.app.auth.LocalAuthState
import com.odysee.app.auth.SignInRoute
import com.odysee.app.auth.SignInScreen
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.data.player.CurrentMedia
import com.odysee.app.feature.channel.ChannelRoute
import com.odysee.app.feature.channel.ChannelScreen
import com.odysee.app.feature.channel.edit.EditChannelRoute
import com.odysee.app.feature.channel.edit.EditChannelScreen
import com.odysee.app.feature.channel.moderation.ModerationRoute
import com.odysee.app.feature.channel.moderation.ModerationScreen
import com.odysee.app.feature.channel.featuredchannels.FeaturedChannelsEditRoute
import com.odysee.app.feature.channel.featuredchannels.FeaturedChannelsEditScreen
import com.odysee.app.feature.channel.discussion.DiscussionSettingsRoute
import com.odysee.app.feature.channel.discussion.DiscussionSettingsScreen
import com.odysee.app.feature.channel.featuredcontent.FeaturedContentRoute
import com.odysee.app.feature.channel.featuredcontent.FeaturedContentScreen
import com.odysee.app.feature.channel.memberships.CreatorMembershipsRoute
import com.odysee.app.feature.channel.memberships.MembershipsScreen as CreatorMembershipsScreen
import com.odysee.app.feature.channel.analytics.AnalyticsRoute as ChannelAnalyticsRoute
import com.odysee.app.feature.channel.analytics.AnalyticsScreen as ChannelAnalyticsScreen
import com.odysee.app.feature.home.HomeRoute
import com.odysee.app.feature.home.HomeScreen
import com.odysee.app.feature.library.FavoritesRoute
import com.odysee.app.feature.library.FavoritesScreen
import com.odysee.app.feature.library.FollowingListRoute
import com.odysee.app.feature.library.FollowingListScreen
import com.odysee.app.feature.library.PlaylistsRoute
import com.odysee.app.feature.library.PlaylistsScreen
import com.odysee.app.feature.library.WatchHistoryRoute
import com.odysee.app.feature.library.WatchHistoryScreen
import com.odysee.app.feature.library.WatchLaterRoute
import com.odysee.app.feature.library.WatchLaterScreen
import com.odysee.app.feature.notifications.NotificationsRoute
import com.odysee.app.feature.notifications.NotificationsScreen
import com.odysee.app.feature.search.SearchRoute
import com.odysee.app.feature.search.SearchScreen
import com.odysee.app.feature.shorts.ShortsRoute
import com.odysee.app.feature.shorts.ShortsScreen
import com.odysee.app.feature.settings.SettingsRoute
import com.odysee.app.feature.settings.SettingsScreen
import com.odysee.app.feature.wallet.WalletRoute
import com.odysee.app.feature.wallet.WalletScreen
import com.odysee.app.core.data.player.PlayerOpenMode
import com.odysee.app.player.LocalPlayerController

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OdyseeNavHost(
    navController: NavHostController,
    onChannelClick: (String, String) -> Unit,
    onAccountClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    val playerController = LocalPlayerController.current
    androidx.compose.runtime.LaunchedEffect(playerController) {
        // Centralized short → shorts-player routing. PlayerController emits to
        // this whenever `play(media)` is invoked with `isShort = true`,
        // regardless of which surface fired the call.
        playerController.openShortsEvents.collect { media ->
            navController.navigate(
                ShortsRoute(
                    initialClaimId = media.claimId,
                    initialPermanentUrl = media.permanentUrl,
                    initialTitle = media.title,
                    initialChannelName = media.channelName,
                    initialChannelClaimId = media.channelClaimId,
                    initialChannelAvatarUrl = media.channelAvatarUrl,
                    initialThumbnailUrl = media.thumbnailUrl,
                ),
            )
        }
    }
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
    ) {
        composable<HomeRoute> {
            val authState = LocalAuthState.current
            val signedIn = authState as? AuthState.SignedIn
            fun videoToMedia(video: com.odysee.app.feature.home.VideoUiModel) = CurrentMedia(
                claimId = video.id,
                permanentUrl = video.permanentUrl,
                title = video.title,
                description = video.description,
                channelClaimId = video.channelClaimId,
                channelName = video.channelName,
                channelTitle = video.channelTitle,
                channelInitial = video.channelInitial,
                channelAvatarUrl = video.channelAvatarUrl,
                thumbnailUrl = video.thumbnailUrl,
                ageLabel = video.ageLabel.takeIf { it.isNotEmpty() },
                liveStreamUrl = video.liveStreamUrl,
            )
            // Guests don't have playlists, so the home pager collapses to just Home.
            val pagerState = rememberPagerState(pageCount = { if (signedIn != null) 2 else 1 })
            val scope = rememberCoroutineScope()
            val hazeState = androidx.compose.runtime.remember { HazeState() }
            val showCreateMenuState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            var showCreateMenu = showCreateMenuState.value
            androidx.compose.runtime.CompositionLocalProvider(
                com.odysee.app.feature.home.LocalHazeState provides hazeState,
            ) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 -> HomeScreen(
                            onWatchClick = { video ->
                                if (video.isShort) {
                                    playerController.close()
                                    navController.navigate(
                                        ShortsRoute(
                                            initialClaimId = video.id,
                                            initialPermanentUrl = video.permanentUrl,
                                            initialTitle = video.title,
                                            initialChannelName = video.channelName,
                                            initialChannelClaimId = video.channelClaimId,
                                            initialChannelAvatarUrl = video.channelAvatarUrl,
                                            initialThumbnailUrl = video.thumbnailUrl,
                                        ),
                                    )
                                } else {
                                    playerController.play(videoToMedia(video))
                                }
                            },
                            onPlayBackground = { video ->
                                playerController.play(videoToMedia(video), openMode = PlayerOpenMode.Minimized)
                            },
                            onPlayPip = { video ->
                                playerController.play(videoToMedia(video), openMode = PlayerOpenMode.Pip)
                            },
                            onChannelClick = onChannelClick,
                            onAccountClick = onAccountClick,
                            onSearchClick = onSearchClick,
                            onNotificationsClick = onNotificationsClick,
                            signedInEmail = signedIn?.user?.email,
                            signedInAvatarUrl = signedIn?.activeChannel?.thumbnailUrl,
                            showOdyseeTopBar = false,
                        )
                        else -> com.odysee.app.feature.library.PlaylistsTabContent(
                            topBar = {
                                Spacer(
                                    modifier = Modifier
                                        .statusBarsPadding()
                                        .height(64.dp),
                                )
                            },
                            onWatchHistory = { navController.navigate(WatchHistoryRoute) },
                            onWatchLater = { navController.navigate(WatchLaterRoute) },
                            onFavorites = { navController.navigate(FavoritesRoute) },
                            onOpenPlaylist = { id ->
                                navController.navigate(com.odysee.app.feature.library.PlaylistDetailRoute(id))
                            },
                            onPlayPlaylist = { id ->
                                navController.navigate(
                                    com.odysee.app.feature.library.PlaylistDetailRoute(id, autoplay = true),
                                )
                            },
                            onShufflePlaylist = { id ->
                                navController.navigate(
                                    com.odysee.app.feature.library.PlaylistDetailRoute(id, autoplay = true, shuffle = true),
                                )
                            },
                            onPublishPlaylist = { id ->
                                navController.navigate(com.odysee.app.feature.library.PlaylistEditRoute(id = id))
                            },
                            onAddPlaylist = {
                                navController.navigate(com.odysee.app.feature.library.PlaylistEditRoute(id = null))
                            },
                            onEditPlaylist = { id ->
                                navController.navigate(com.odysee.app.feature.library.PlaylistEditRoute(id = id))
                            },
                            hazeState = hazeState,
                        )
                    }
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(androidx.compose.ui.Alignment.TopCenter),
                ) {
                    com.odysee.app.feature.home.OdyseeHomeHeader(
                        onSearchClick = onSearchClick,
                        onNotificationsClick = onNotificationsClick,
                        onAccountClick = onAccountClick,
                        signedInEmail = signedIn?.user?.email,
                        signedInAvatarUrl = signedIn?.activeChannel?.thumbnailUrl,
                        premiumTier = signedIn?.premiumTier ?: com.odysee.app.core.data.auth.PremiumTier.None,
                    )
                }
                if (signedIn != null) androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(androidx.compose.ui.Alignment.BottomCenter),
                ) {
                    com.odysee.app.HomeBottomTabs(
                        selected = if (pagerState.currentPage == 0) com.odysee.app.BottomTab.Home
                        else com.odysee.app.BottomTab.Playlists,
                        onSelect = { tab ->
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    if (tab == com.odysee.app.BottomTab.Home) 0 else 1,
                                )
                            }
                        },
                        createMenuExpanded = showCreateMenuState.value,
                        onCreateMenuToggle = { showCreateMenuState.value = !showCreateMenuState.value },
                        onUploadFile = {
                            showCreateMenuState.value = false
                            navController.navigate(com.odysee.app.upload.UploadFileRoute)
                        },
                        onGoLive = {
                            showCreateMenuState.value = false
                            navController.navigate(com.odysee.app.upload.LivestreamFormRoute)
                        },
                        onPost = {
                            showCreateMenuState.value = false
                            navController.navigate(com.odysee.app.upload.PostFormRoute)
                        },
                        hazeState = hazeState,
                    )
                }
            }
            }
        }
        composable<ShortsRoute> {
            ShortsScreen(
                onBack = { navController.popBackStack() },
                onChannelClick = onChannelClick,
            )
        }
        composable<ChannelRoute> {
            ChannelScreen(
                onBack = { navController.popBackStack() },
                onWatch = { media -> playerController.play(media) },
                onVisitChannel = onChannelClick,
                onEditChannel = { claimId, name ->
                    navController.navigate(EditChannelRoute(claimId, name))
                },
                onOpenModeration = { claimId, name ->
                    navController.navigate(ModerationRoute(claimId, name))
                },
                onOpenFeaturedChannels = { claimId, name ->
                    navController.navigate(FeaturedChannelsEditRoute(claimId, name))
                },
                onOpenDiscussionSettings = { claimId, name ->
                    navController.navigate(DiscussionSettingsRoute(claimId, name))
                },
                onOpenFeaturedContent = { claimId, name ->
                    navController.navigate(FeaturedContentRoute(claimId, name))
                },
                onOpenCreatorMemberships = { claimId, name ->
                    navController.navigate(CreatorMembershipsRoute(claimId, name))
                },
                onOpenAnalytics = { claimId, name ->
                    navController.navigate(ChannelAnalyticsRoute(claimId, name))
                },
                onPlayClaimBackground = { media -> playerController.play(media, openMode = PlayerOpenMode.Minimized) },
                onPlayClaimPip = { media -> playerController.play(media, openMode = PlayerOpenMode.Pip) },
                onWatchShort = { target ->
                    playerController.close()
                    navController.navigate(
                        ShortsRoute(
                            initialClaimId = target.claimId,
                            initialPermanentUrl = target.permanentUrl,
                            initialTitle = target.title,
                            initialChannelName = target.channelName,
                            initialChannelClaimId = target.channelClaimId,
                            initialChannelAvatarUrl = target.channelAvatarUrl,
                            initialThumbnailUrl = target.thumbnailUrl,
                        ),
                    )
                },
            )
        }
        composable<CreatorMembershipsRoute> {
            CreatorMembershipsScreen(onBack = { navController.popBackStack() })
        }
        composable<ChannelAnalyticsRoute> {
            ChannelAnalyticsScreen(onBack = { navController.popBackStack() })
        }
        composable<EditChannelRoute> {
            EditChannelScreen(onBack = { navController.popBackStack() })
        }
        composable<ModerationRoute> {
            ModerationScreen(onBack = { navController.popBackStack() })
        }
        composable<FeaturedChannelsEditRoute> {
            FeaturedChannelsEditScreen(onBack = { navController.popBackStack() })
        }
        composable<DiscussionSettingsRoute> {
            DiscussionSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<FeaturedContentRoute> {
            FeaturedContentScreen(onBack = { navController.popBackStack() })
        }
        composable<SignInRoute> {
            SignInScreen(
                onBack = { navController.popBackStack(SignInRoute, inclusive = true) },
            )
        }
        composable<SearchRoute> {
            fun searchResultToMedia(result: com.odysee.app.feature.search.SearchResultUi) = CurrentMedia(
                claimId = result.claimId,
                permanentUrl = result.permanentUrl,
                title = result.title,
                description = result.description,
                channelClaimId = result.channelClaimId,
                channelName = result.channelName ?: "",
                channelInitial = (result.channelName?.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
                channelAvatarUrl = result.channelAvatarUrl,
                thumbnailUrl = result.thumbnailUrl,
                ageLabel = result.ageLabel.takeIf { it.isNotEmpty() },
            )
            SearchScreen(
                onBack = { navController.popBackStack() },
                onChannelClick = onChannelClick,
                onWatch = { result -> playerController.play(searchResultToMedia(result)) },
                onPlayBackground = { result ->
                    playerController.play(searchResultToMedia(result), openMode = PlayerOpenMode.Minimized)
                },
                onPlayPip = { result ->
                    playerController.play(searchResultToMedia(result), openMode = PlayerOpenMode.Pip)
                },
                onSaveWatchLater = { result ->
                    playerController.saveToWatchLater(searchResultToMedia(result))
                },
                onSaveFavorite = { result ->
                    playerController.saveToFavorites(searchResultToMedia(result))
                },
            )
        }
        composable<WalletRoute> {
            WalletScreen(onBack = { navController.popBackStack() })
        }
        composable<NotificationsRoute> {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onOpenTarget = { url ->
                    val handled = openOdyseeLink(url, navController, playerController, onChannelClick)
                    if (handled) navController.popBackStack()
                },
            )
        }
        composable<SettingsRoute> {
            val cacheContext = androidx.compose.ui.platform.LocalContext.current
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenNotificationSettings = {
                    navController.navigate(com.odysee.app.feature.settings.NotificationSettingsRoute)
                },
                onOpenBlockedChannels = {
                    navController.navigate(com.odysee.app.account.BlockedChannelsRoute)
                },
                onOpenComments = {
                    navController.navigate(com.odysee.app.account.OwnCommentsRoute)
                },
                onOpenPurchases = {
                    navController.navigate(com.odysee.app.account.PurchasesRoute)
                },
                onOpenPassword = { navController.navigate(SignInRoute) },
                onClearCache = {
                    runCatching {
                        cacheContext.cacheDir.deleteRecursively()
                        cacheContext.cacheDir.mkdirs()
                    }
                    coil3.SingletonImageLoader.get(cacheContext).also {
                        runCatching { it.memoryCache?.clear() }
                        runCatching { it.diskCache?.clear() }
                    }
                },
            )
        }
        composable<com.odysee.app.feature.settings.NotificationSettingsRoute> {
            com.odysee.app.feature.settings.NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<WatchHistoryRoute> {
            WatchHistoryScreen(
                onBack = { navController.popBackStack() },
                onWatch = { entry -> playerController.play(entry.toCurrentMedia()) },
            )
        }
        composable<WatchLaterRoute> {
            WatchLaterScreen(
                onBack = { navController.popBackStack() },
                onWatchPlaylist = { target -> playFromPlaylistTarget(playerController, target) },
            )
        }
        composable<FavoritesRoute> {
            FavoritesScreen(
                onBack = { navController.popBackStack() },
                onWatchPlaylist = { target -> playFromPlaylistTarget(playerController, target) },
            )
        }
        composable<PlaylistsRoute> {
            PlaylistsScreen(
                onBack = { navController.popBackStack() },
                onOpenPlaylist = { id ->
                    navController.navigate(com.odysee.app.feature.library.PlaylistDetailRoute(id))
                },
                onPlayPlaylist = { id ->
                    navController.navigate(
                        com.odysee.app.feature.library.PlaylistDetailRoute(id, autoplay = true),
                    )
                },
                onShufflePlaylist = { id ->
                    navController.navigate(
                        com.odysee.app.feature.library.PlaylistDetailRoute(id, autoplay = true, shuffle = true),
                    )
                },
                onPublishPlaylist = { id ->
                    navController.navigate(com.odysee.app.feature.library.PlaylistEditRoute(id = id))
                },
                onAddPlaylist = {
                    navController.navigate(com.odysee.app.feature.library.PlaylistEditRoute(id = null))
                },
                onEditPlaylist = { id ->
                    navController.navigate(com.odysee.app.feature.library.PlaylistEditRoute(id = id))
                },
            )
        }
        composable<com.odysee.app.upload.UploadFileRoute> {
            com.odysee.app.upload.UploadFileScreen(
                onBack = { navController.popBackStack() },
                onPublished = { navController.popBackStack() },
            )
        }
        composable<com.odysee.app.upload.PostFormRoute> {
            com.odysee.app.upload.PostFormScreen(
                onBack = { navController.popBackStack() },
                onPublished = { navController.popBackStack() },
            )
        }
        composable<com.odysee.app.upload.LivestreamFormRoute> {
            com.odysee.app.upload.LivestreamFormScreen(
                onBack = { navController.popBackStack() },
                onPublished = { navController.popBackStack() },
            )
        }
        composable<com.odysee.app.feature.library.PlaylistDetailRoute> {
            com.odysee.app.feature.library.PlaylistDetailScreen(
                onBack = { navController.popBackStack() },
                onWatch = { target -> playFromPlaylistTarget(playerController, target) },
            )
        }
        composable<com.odysee.app.feature.library.PlaylistEditRoute> {
            com.odysee.app.feature.library.PlaylistEditScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<FollowingListRoute> {
            FollowingListScreen(
                onBack = { navController.popBackStack() },
                onChannelClick = onChannelClick,
            )
        }
        composable<com.odysee.app.account.ChannelsRoute> {
            com.odysee.app.account.ChannelsScreen(
                onBack = { navController.popBackStack() },
                onNewChannel = { navController.navigate(com.odysee.app.account.NewChannelRoute) },
                onChannelClick = onChannelClick,
            )
        }
        composable<com.odysee.app.account.NewChannelRoute> {
            com.odysee.app.account.NewChannelScreen(onBack = { navController.popBackStack() })
        }
        composable<com.odysee.app.account.UploadsRoute> {
            com.odysee.app.account.UploadsScreen(
                onBack = { navController.popBackStack() },
                onWatch = { claim -> playerController.play(claimToMedia(claim)) },
                onPlayBackground = { claim ->
                    playerController.play(claimToMedia(claim), openMode = PlayerOpenMode.Minimized)
                },
                onPlayPip = { claim ->
                    playerController.play(claimToMedia(claim), openMode = PlayerOpenMode.Pip)
                },
                onSaveWatchLater = { claim ->
                    playerController.saveToWatchLater(claimToMedia(claim))
                },
                onSaveFavorite = { claim ->
                    playerController.saveToFavorites(claimToMedia(claim))
                },
            )
        }
        composable<com.odysee.app.account.InvitesRoute> {
            com.odysee.app.account.InvitesScreen(onBack = { navController.popBackStack() })
        }
        composable<com.odysee.app.account.MembershipsRoute> {
            com.odysee.app.account.MembershipsScreen(
                onBack = { navController.popBackStack() },
                onChannelClick = onChannelClick,
            )
        }
        composable<com.odysee.app.account.OwnCommentsRoute> {
            com.odysee.app.account.OwnCommentsScreen(
                onBack = { navController.popBackStack() },
                onOpenContent = { claim, commentId ->
                    playerController.play(claimToMedia(claim).copy(linkedCommentId = commentId))
                },
            )
        }
        composable<com.odysee.app.account.PurchasesRoute> {
            com.odysee.app.account.PurchasesScreen(
                onBack = { navController.popBackStack() },
                onClaimClick = { claim -> playerController.play(claimToMedia(claim)) },
                onChannelClick = onChannelClick,
            )
        }
        composable<com.odysee.app.account.BlockedChannelsRoute> {
            com.odysee.app.account.BlockedChannelsScreen(
                onBack = { navController.popBackStack() },
                onChannelClick = onChannelClick,
            )
        }
        composable<com.odysee.app.account.RewardsRoute> {
            com.odysee.app.account.RewardsScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun playFromPlaylistTarget(
    playerController: com.odysee.app.core.data.player.PlayerController,
    target: com.odysee.app.feature.library.PlaylistWatchTarget,
) {
    val item = target.items.getOrNull(target.startIndex) ?: return
    playerController.play(
        media = CurrentMedia(
            claimId = item.claimId,
            permanentUrl = item.permanentUrl,
            title = item.title,
            description = item.description,
            channelClaimId = item.channelClaimId,
            channelName = item.channelName,
            channelInitial = (item.channelName.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
            channelAvatarUrl = item.channelAvatarUrl,
            thumbnailUrl = item.thumbnailUrl,
            ageLabel = null,
        ),
        playlist = com.odysee.app.core.data.player.PlaylistContext(
            id = target.playlistId,
            name = target.playlistName,
            currentIndex = target.startIndex,
            items = target.items.map { i ->
                com.odysee.app.core.data.player.PlaylistContextItem(
                    claimId = i.claimId,
                    permanentUrl = i.permanentUrl,
                    title = i.title,
                    description = i.description,
                    channelName = i.channelName,
                    channelClaimId = i.channelClaimId,
                    channelAvatarUrl = i.channelAvatarUrl,
                    thumbnailUrl = i.thumbnailUrl,
                )
            },
        ),
    )
}

/** Internal-facing alias usable from MainActivity (which is in the parent package). */
internal fun openOdyseeLinkFromMain(
    rawUrl: String,
    navController: NavHostController,
    playerController: com.odysee.app.core.data.player.PlayerController,
    onChannelClick: (String, String) -> Unit,
): Boolean = openOdyseeLink(rawUrl, navController, playerController, onChannelClick)

/**
 * Routes an Odysee URL (https://odysee.com/... or lbry://...) to the right
 * destination. Channels go to ChannelRoute; streams ask the player to play.
 * Supports `?lc=<commentId>` for linked comments. Returns true if dispatched.
 */
private fun openOdyseeLink(
    rawUrl: String,
    navController: NavHostController,
    playerController: com.odysee.app.core.data.player.PlayerController,
    onChannelClick: (String, String) -> Unit,
): Boolean {
    if (rawUrl.isBlank()) return false
    val (pathPart, queryPart) = rawUrl.substringBefore('#').let { noFrag ->
        val q = noFrag.indexOf('?')
        if (q < 0) noFrag to ""
        else noFrag.substring(0, q) to noFrag.substring(q + 1)
    }
    val linkedCommentId: String? = queryPart
        .split('&')
        .firstOrNull { it.startsWith("lc=") }
        ?.removePrefix("lc=")
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
    val stripped = pathPart
        .removePrefix("https://odysee.com/")
        .removePrefix("http://odysee.com/")
        .removePrefix("odysee.com/")
        .removePrefix("lbry://")
        .trim('/')
    if (stripped.isBlank()) return false

    val parts = stripped.split('/')
    // Each segment looks like "name#claimId" or "name:claimId".
    fun extract(seg: String): Pair<String, String>? {
        val sep = seg.indexOfAny(charArrayOf('#', ':'))
        if (sep <= 0 || sep >= seg.length - 1) return null
        val name = seg.substring(0, sep)
        val id = seg.substring(sep + 1).takeWhile { it.isLetterOrDigit() }
        if (id.length !in 8..40) return null
        return java.net.URLDecoder.decode(name, "UTF-8") to id
    }

    val firstSeg = parts.getOrNull(0) ?: return false
    val secondSeg = parts.getOrNull(1)

    return when {
        secondSeg != null -> {
            // channel/stream form — play the stream as a top-level item.
            val (streamName, streamId) = extract(secondSeg) ?: return false
            val (channelName, channelId) = extract(firstSeg) ?: (firstSeg to null)
            playerController.play(
                media = CurrentMedia(
                    claimId = streamId,
                    permanentUrl = "lbry://$channelName${if (channelId != null) "#$channelId" else ""}/$streamName#$streamId",
                    title = streamName,
                    description = null,
                    channelClaimId = channelId,
                    channelName = channelName.removePrefix("@"),
                    channelInitial = (channelName.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
                    channelAvatarUrl = null,
                    thumbnailUrl = null,
                    ageLabel = null,
                    linkedCommentId = linkedCommentId,
                ),
            )
            true
        }
        firstSeg.startsWith("@") -> {
            val (name, id) = extract(firstSeg) ?: return false
            onChannelClick(id, name)
            true
        }
        else -> {
            // Bare stream (no channel) — play it.
            val (name, id) = extract(firstSeg) ?: return false
            playerController.play(
                media = CurrentMedia(
                    claimId = id,
                    permanentUrl = "lbry://$name#$id",
                    title = name,
                    description = null,
                    channelClaimId = null,
                    channelName = "",
                    channelInitial = 'O',
                    channelAvatarUrl = null,
                    thumbnailUrl = null,
                    ageLabel = null,
                    linkedCommentId = linkedCommentId,
                ),
            )
            true
        }
    }
}

private fun claimToMedia(claim: com.odysee.app.core.model.Claim): CurrentMedia {
    val initial = (claim.signingChannel?.name?.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar()
    return CurrentMedia(
        claimId = claim.claimId,
        permanentUrl = claim.permanentUrl,
        title = claim.title,
        description = claim.description,
        channelClaimId = claim.signingChannel?.claimId,
        channelName = claim.signingChannel?.name.orEmpty(),
        channelTitle = claim.signingChannel?.title,
        channelInitial = initial,
        channelAvatarUrl = claim.signingChannel?.thumbnailUrl,
        thumbnailUrl = claim.thumbnailUrl,
        ageLabel = null,
        mediaType = claim.mediaType,
    )
}

private fun com.odysee.app.feature.library.HistoryWatchTarget.toCurrentMedia(): CurrentMedia =
    CurrentMedia(
        claimId = claimId,
        permanentUrl = permanentUrl,
        title = title,
        description = null,
        channelClaimId = channelClaimId,
        channelName = channelName,
        channelInitial = (channelName.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
        channelAvatarUrl = null,
        thumbnailUrl = thumbnailUrl,
        ageLabel = null,
    )
