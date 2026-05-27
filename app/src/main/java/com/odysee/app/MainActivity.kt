package com.odysee.app

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.odysee.app.auth.AccountMenuSheet
import com.odysee.app.auth.LocalAuthState
import com.odysee.app.auth.SignInRoute
import com.odysee.app.core.data.auth.AuthRepository
import com.odysee.app.core.data.player.PlayerController
import com.odysee.app.core.designsystem.theme.OdyseeTheme
import com.odysee.app.feature.channel.ChannelRoute
import com.odysee.app.feature.library.FavoritesRoute
import com.odysee.app.feature.library.FollowingListRoute
import com.odysee.app.feature.library.PlaylistsRoute
import com.odysee.app.feature.library.WatchHistoryRoute
import com.odysee.app.feature.library.WatchLaterRoute
import com.odysee.app.feature.notifications.NotificationsRoute
import com.odysee.app.feature.search.SearchRoute
import com.odysee.app.feature.settings.SettingsRoute
import com.odysee.app.feature.wallet.WalletRoute
import com.odysee.app.navigation.OdyseeNavHost
import com.odysee.app.player.LocalPlayerController
import com.odysee.app.player.PlayerSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var playerController: PlayerController
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var appUpdater: com.odysee.app.core.data.updater.AppUpdater

    private var mediaControllerFuture:
        com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController>? = null

    private val pendingIntent = kotlinx.coroutines.flow.MutableSharedFlow<android.content.Intent>(
        replay = 1,
        extraBufferCapacity = 4,
    )

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntent.tryEmit(intent)
    }

    override fun onStart() {
        super.onStart()
        val token = androidx.media3.session.SessionToken(
            this,
            android.content.ComponentName(this, com.odysee.app.player.OdyseePlaybackService::class.java),
        )
        mediaControllerFuture = androidx.media3.session.MediaController.Builder(this, token).buildAsync()
    }

    override fun onStop() {
        mediaControllerFuture?.let { androidx.media3.session.MediaController.releaseFuture(it) }
        mediaControllerFuture = null
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        intent?.let { pendingIntent.tryEmit(it) }
        enableEdgeToEdge()
        // Don't let app content extend under the display cutout in landscape.
        // Default mode = the system pads the cutout area in landscape and lets
        // the status bar cover it in portrait, which is what we want app-wide.
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        if (appUpdater.isSupported) {
            lifecycleScope.launch { appUpdater.checkForUpdates(silent = true) }
        }
        setContent {
            OdyseeTheme {
                val authState by authRepository.state.collectAsStateWithLifecycle()
                CompositionLocalProvider(
                    LocalPlayerController provides playerController,
                    LocalAuthState provides authState,
                ) {
                    val navController = rememberNavController()
                    val onChannelClick: (String, String) -> Unit = { id, name ->
                        navController.navigate(ChannelRoute(claimId = id, name = name))
                    }
                    var showAccountSheet by remember { mutableStateOf(false) }
                    val onAccountClick: () -> Unit = { showAccountSheet = true }
                    val openWallet: () -> Unit = {
                        showAccountSheet = false
                        navController.navigate(WalletRoute)
                    }
                    val openNotifications: () -> Unit = {
                        showAccountSheet = false
                        navController.navigate(NotificationsRoute)
                    }
                    val openSettings: () -> Unit = {
                        showAccountSheet = false
                        navController.navigate(SettingsRoute)
                    }
                    val openWatchHistory: () -> Unit = {
                        showAccountSheet = false
                        navController.navigate(WatchHistoryRoute)
                    }
                    val openWatchLater: () -> Unit = {
                        showAccountSheet = false
                        navController.navigate(WatchLaterRoute)
                    }
                    val openFavorites: () -> Unit = {
                        showAccountSheet = false
                        navController.navigate(FavoritesRoute)
                    }
                    val openPlaylists: () -> Unit = {
                        showAccountSheet = false
                        navController.navigate(PlaylistsRoute)
                    }
                    val openFollowingList: () -> Unit = {
                        showAccountSheet = false
                        navController.navigate(FollowingListRoute)
                    }
                    val openSearch: () -> Unit = { navController.navigate(SearchRoute) }
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        pendingIntent.collect { intent ->
                            if (intent.getBooleanExtra(
                                    com.odysee.app.notifications.OdyseeNotificationPoller.EXTRA_OPEN_NOTIFICATIONS,
                                    false,
                                )
                            ) {
                                intent.removeExtra(
                                    com.odysee.app.notifications.OdyseeNotificationPoller.EXTRA_OPEN_NOTIFICATIONS,
                                )
                                val target = intent.getStringExtra(
                                    com.odysee.app.notifications.OdyseeNotificationPoller.EXTRA_NOTIFICATION_TARGET,
                                )
                                intent.removeExtra(
                                    com.odysee.app.notifications.OdyseeNotificationPoller.EXTRA_NOTIFICATION_TARGET,
                                )
                                val opened = target?.let {
                                    com.odysee.app.navigation.openOdyseeLinkFromMain(
                                        it,
                                        navController,
                                        playerController,
                                        onChannelClick,
                                    )
                                } ?: false
                                if (!opened) navController.navigate(NotificationsRoute)
                            } else if (intent.action == android.content.Intent.ACTION_VIEW) {
                                val data = intent.data
                                if (data != null && data.host == "odysee.com") {
                                    val path = data.path.orEmpty()
                                    if (path == "/\$/verify" || path == "/$/verify") {
                                        val email = data.getQueryParameter("email")
                                        val verToken = data.getQueryParameter("verification_token")
                                        val newAuth = data.getQueryParameter("auth_token")
                                        if (!email.isNullOrBlank() &&
                                            !verToken.isNullOrBlank() &&
                                            !newAuth.isNullOrBlank()
                                        ) {
                                            lifecycleScope.launch {
                                                val result = authRepository.confirmEmail(email, verToken, newAuth)
                                                val msg = when (result) {
                                                    is com.odysee.app.core.data.auth.SignInResult.Success ->
                                                        "Signed in."
                                                    is com.odysee.app.core.data.auth.SignInResult.Failure ->
                                                        "Verification failed: ${result.message}"
                                                    else -> "Verification pending — check your email."
                                                }
                                                android.widget.Toast.makeText(
                                                    this@MainActivity, msg, android.widget.Toast.LENGTH_LONG,
                                                ).show()
                                            }
                                        }
                                        intent.data = null
                                    } else {
                                        val handled = com.odysee.app.navigation.openOdyseeLinkFromMain(
                                            data.toString(),
                                            navController,
                                            playerController,
                                            onChannelClick,
                                        )
                                        if (handled) intent.data = null
                                    }
                                }
                            }
                        }
                    }
                    PlayerSheet(onChannelClick = onChannelClick) {
                        androidx.compose.foundation.layout.Column {
                            com.odysee.app.updater.UpdateBannerHost(appUpdater = appUpdater)
                            OdyseeNavHost(
                                navController = navController,
                                onChannelClick = onChannelClick,
                                onAccountClick = onAccountClick,
                                onSearchClick = openSearch,
                                onNotificationsClick = openNotifications,
                            )
                        }
                    }
                    if (showAccountSheet) {
                        AccountMenuSheet(
                            authState = authState,
                            onDismiss = { showAccountSheet = false },
                            onSignIn = {
                                showAccountSheet = false
                                navController.navigate(SignInRoute)
                            },
                            onSignUp = {
                                showAccountSheet = false
                                navController.navigate(SignInRoute)
                            },
                            onSignOut = {
                                lifecycleScope.launch { authRepository.signOut() }
                            },
                            onSelectChannel = { claimId ->
                                lifecycleScope.launch { authRepository.selectActiveChannel(claimId) }
                            },
                            onChannelClick = onChannelClick,
                            onWallet = openWallet,
                            onFollowing = openFollowingList,
                            onNewChannel = {
                                showAccountSheet = false
                                navController.navigate(com.odysee.app.account.NewChannelRoute)
                            },
                            onUploads = {
                                showAccountSheet = false
                                navController.navigate(com.odysee.app.account.UploadsRoute)
                            },
                            onChannels = {
                                showAccountSheet = false
                                navController.navigate(com.odysee.app.account.ChannelsRoute)
                            },
                            onAnalytics = {
                                showAccountSheet = false
                                val signedIn = authState as? com.odysee.app.core.data.auth.AuthState.SignedIn
                                val target = signedIn?.activeChannel
                                    ?: signedIn?.channels?.firstOrNull()
                                if (target != null) {
                                    navController.navigate(
                                        com.odysee.app.feature.channel.analytics.AnalyticsRoute(
                                            claimId = target.claimId,
                                            name = target.name,
                                        ),
                                    )
                                } else {
                                    navController.navigate(com.odysee.app.account.NewChannelRoute)
                                }
                            },
                            onYoutubeSync = {
                                showAccountSheet = false
                                openExternalUrl(this, "https://odysee.com/\$/youtube")
                            },
                            onCredits = openWallet,
                            onRewards = {
                                showAccountSheet = false
                                navController.navigate(com.odysee.app.account.RewardsRoute)
                            },
                            onInvites = {
                                showAccountSheet = false
                                navController.navigate(com.odysee.app.account.InvitesRoute)
                            },
                            onMemberships = {
                                showAccountSheet = false
                                navController.navigate(com.odysee.app.account.MembershipsRoute)
                            },
                            onPremium = {
                                showAccountSheet = false
                                openExternalUrl(this, "https://odysee.com/\$/odyseepremium")
                            },
                            onSettings = openSettings,
                            onHelp = {
                                showAccountSheet = false
                                openExternalUrl(this, "https://help.odysee.tv/")
                            },
                        )
                    }
                }
            }
        }
    }
}

enum class BottomTab { Home, Playlists }

@Composable
fun HomeBottomTabs(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit,
    createMenuExpanded: Boolean = false,
    onCreateMenuToggle: () -> Unit = {},
    onUploadFile: () -> Unit = {},
    onGoLive: () -> Unit = {},
    onPost: () -> Unit = {},
    hazeState: HazeState? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 56.dp, end = 56.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                .let { m ->
                    if (hazeState != null) m.hazeEffect(state = hazeState, style = HazeMaterials.ultraThin())
                    else m.background(MaterialTheme.colorScheme.background)
                },
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = createMenuExpanded,
                enter = androidx.compose.animation.expandVertically(
                    expandFrom = Alignment.Bottom,
                ) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically(
                    shrinkTowards = Alignment.Bottom,
                ) + androidx.compose.animation.fadeOut(),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    CreateMenuItem(icon = Icons.Outlined.CloudUpload, label = "Upload file", onClick = onUploadFile)
                    CreateMenuItem(icon = Icons.Outlined.Videocam, label = "Go live", onClick = onGoLive)
                    CreateMenuItem(icon = Icons.Outlined.Edit, label = "Post", onClick = onPost)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomTabItem(
                    label = "Home",
                    icon = Icons.Outlined.Home,
                    selected = selected == BottomTab.Home,
                    onClick = { onSelect(BottomTab.Home) },
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onCreateMenuToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    val rotation by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (createMenuExpanded) 45f else 0f,
                        label = "createPlusRotation",
                    )
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = if (createMenuExpanded) "Close" else "New",
                        tint = Color.White,
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer { rotationZ = rotation },
                    )
                }
                BottomTabItem(
                    label = "Playlists",
                    icon = Icons.Outlined.PlaylistPlay,
                    selected = selected == BottomTab.Playlists,
                    onClick = { onSelect(BottomTab.Playlists) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CreateMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
internal fun BottomTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) MaterialTheme.colorScheme.onBackground
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}


private fun openExternalUrl(context: android.content.Context, url: String) {
    val intent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse(url),
    )
    runCatching { context.startActivity(intent) }
}
