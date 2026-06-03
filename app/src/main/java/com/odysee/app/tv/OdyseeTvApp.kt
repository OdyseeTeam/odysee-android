package com.odysee.app.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.odysee.app.core.data.player.CurrentMedia
import com.odysee.app.core.data.player.PlayerController
import com.odysee.app.core.data.player.PlayerOpenMode
import com.odysee.app.feature.home.FeedState
import com.odysee.app.feature.home.HomeViewModel
import com.odysee.app.feature.home.VideoUiModel

private sealed interface TvDestination {
    data class Section(val section: TvSection) : TvDestination
    data class Player(val media: CurrentMedia, val queue: List<CurrentMedia> = emptyList()) : TvDestination
    data class Channel(val claimId: String, val name: String) : TvDestination
}

private enum class TvSection(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Home),
    Library("Library", Icons.Outlined.LibraryBooks),
    Search("Search", Icons.Outlined.Search),
    SignIn("Sign in", Icons.Outlined.AccountCircle),
    Settings("Settings", Icons.Outlined.Settings),
}

@Composable
fun OdyseeTvApp(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var destination by remember { mutableStateOf<TvDestination>(TvDestination.Section(TvSection.Home)) }
    val playerController = com.odysee.app.player.LocalPlayerController.current
    val autoplay by playerController.autoplayNext.collectAsStateWithLifecycle(initialValue = true)

    when (val d = destination) {
        is TvDestination.Section -> TvShell(
            current = d.section,
            onSelect = { destination = TvDestination.Section(it) },
        ) {
            when (d.section) {
                TvSection.Home -> TvHomeSection(
                    state = state,
                    viewModel = viewModel,
                    onVideoClick = { v, row ->
                        destination = TvDestination.Player(
                            media = v.toCurrentMedia(),
                            queue = row.map { it.toCurrentMedia() },
                        )
                    },
                    onLongClickVideo = { v ->
                        val id = v.channelClaimId
                        if (id != null) destination = TvDestination.Channel(id, v.channelName)
                    },
                )
                TvSection.Library -> TvLibrarySection(
                    onVideoClick = { v -> destination = TvDestination.Player(v) },
                    onChannelClick = { cid, name -> destination = TvDestination.Channel(cid, name) },
                )
                TvSection.Search -> TvSearchSection(
                    onVideoClick = { v -> destination = TvDestination.Player(v) },
                    onChannelClick = { cid, name -> destination = TvDestination.Channel(cid, name) },
                )
                TvSection.Settings -> TvSettingsSection()
                TvSection.SignIn -> TvSignInSection()
            }
        }
        is TvDestination.Player -> TvPlayer(
            media = d.media,
            controller = playerController,
            queue = d.queue,
            autoplayNext = autoplay,
            onAdvance = { next -> destination = TvDestination.Player(next, d.queue) },
            onOpenChannel = { cid, name ->
                playerController.close()
                destination = TvDestination.Channel(cid, name)
            },
            onBack = {
                playerController.close()
                destination = TvDestination.Section(TvSection.Home)
            },
        )
        is TvDestination.Channel -> TvChannelSection(
            claimId = d.claimId,
            name = d.name,
            onBack = { destination = TvDestination.Section(TvSection.Home) },
            onVideoClick = { v -> destination = TvDestination.Player(v) },
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvShell(
    current: TvSection,
    onSelect: (TvSection) -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        colors = SurfaceDefaults.colors(containerColor = Color(0xFF0F0F0F)),
    ) {
        NavigationDrawer(
            drawerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Odysee",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    TvSection.entries.forEach { section ->
                        NavigationDrawerItem(
                            selected = section == current,
                            onClick = { onSelect(section) },
                            leadingContent = {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = section.label,
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(),
                        ) {
                            Text(section.label)
                        }
                    }
                }
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvHomeSection(
    state: com.odysee.app.feature.home.HomeUiState,
    viewModel: HomeViewModel,
    onVideoClick: (VideoUiModel, List<VideoUiModel>) -> Unit,
    onLongClickVideo: ((VideoUiModel) -> Unit)? = null,
) {
    val live = state.livestreamFeed
    val following = (state.followingFeed as? FeedState.Success)?.videos.orEmpty()
    val recommended = (state.feed as? FeedState.Success)?.videos.orEmpty()
    val chips = state.categories.filterNot { it.id == com.odysee.app.feature.home.HomeViewModel.FOLLOWING_ID }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        if (chips.isNotEmpty()) {
            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(chips, key = { it.id }) { chip ->
                    val selected = chip.id == state.selectedCategoryId
                    androidx.tv.material3.Surface(
                        onClick = { viewModel.selectCategory(chip.id) },
                        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
                        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                            containerColor = if (selected) com.odysee.app.core.designsystem.theme.OdyseePink else Color(0xFF2A2A2A),
                            focusedContainerColor = if (selected) com.odysee.app.core.designsystem.theme.OdyseePinkDark else Color(0xFF3A3A3A),
                        ),
                    ) {
                        Text(
                            text = chip.label,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
        if (live.isNotEmpty()) {
            TvVideoRow(
                label = "Live now",
                videos = live,
                onVideoClick = { v -> onVideoClick(v, live) },
                onLongClickVideo = onLongClickVideo,
            )
        }
        if (following.isNotEmpty()) {
            TvVideoRow(
                label = "From channels you follow",
                videos = following,
                onVideoClick = { v -> onVideoClick(v, following) },
                onLongClickVideo = onLongClickVideo,
            )
        }
        val rowLabel = chips.firstOrNull { it.id == state.selectedCategoryId }?.label ?: "Recommended"
        TvVideoRow(
            label = rowLabel,
            videos = recommended,
            onVideoClick = { v -> onVideoClick(v, recommended) },
            onLongClickVideo = onLongClickVideo,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSettingsSection(viewModel: TvSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val languages = listOf(
        "system" to "Follow device",
        "en" to "English",
        "fr" to "Français",
        "es" to "Español",
        "de" to "Deutsch",
        "it" to "Italiano",
        "hi" to "हिन्दी",
        "zh" to "中文",
        "ru" to "Русский",
        "pt-BR" to "Português",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Homepage language",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(languages, key = { it.first }) { (code, label) ->
                val selected = (state.homepageLanguage ?: "system") == code
                androidx.tv.material3.Surface(
                    onClick = { viewModel.setHomepageLanguage(if (code == "system") null else code) },
                    shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
                    colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                        containerColor = if (selected) com.odysee.app.core.designsystem.theme.OdyseePink else Color(0xFF2A2A2A),
                        focusedContainerColor = if (selected) com.odysee.app.core.designsystem.theme.OdyseePinkDark else Color(0xFF3A3A3A),
                    ),
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        androidx.tv.material3.Surface(
            onClick = { viewModel.setAutoplay(!state.autoplay) },
            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
            colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF1A1A1A),
                focusedContainerColor = Color(0xFF2A2A2A),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Autoplay next video",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (state.autoplay) "On" else "Off",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 28.dp)
                        .background(
                            if (state.autoplay) com.odysee.app.core.designsystem.theme.OdyseePink
                            else Color(0xFF555555),
                            RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = if (state.autoplay) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(24.dp)
                            .background(Color.White, RoundedCornerShape(12.dp)),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSignInSection(viewModel: TvSignInViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = if (state.isSignedIn) "Signed in" else "Sign in",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        if (state.isSignedIn) {
            Text(
                text = state.signedInEmail ?: "—",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleMedium,
            )
            androidx.tv.material3.Surface(
                onClick = { viewModel.signOut() },
                shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
                colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                    containerColor = Color(0xFF2A2A2A),
                    focusedContainerColor = com.odysee.app.core.designsystem.theme.OdyseePink,
                ),
            ) {
                Text(
                    text = "Sign out",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            return@Column
        }
        if (state.emailSent) {
            Text(
                text = "Check your email",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                text = "We sent a link to ${state.email}. Open it on any device — your phone, your laptop, anywhere. " +
                    "This TV will sign in automatically within a few seconds of you tapping the link.",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(0.7f),
            )
            androidx.tv.material3.Surface(
                onClick = { viewModel.reset() },
                shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
                colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                    containerColor = Color(0xFF2A2A2A),
                    focusedContainerColor = Color(0xFF3A3A3A),
                ),
            ) {
                Text(
                    text = "Use a different email",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
            return@Column
        }
        Text(
            text = "Enter your email. We'll send a one-tap sign-in link.",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.titleMedium,
        )
        androidx.compose.material3.OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::setEmail,
            modifier = Modifier.fillMaxWidth(0.5f),
            placeholder = {
                androidx.compose.material3.Text(
                    text = "you@example.com",
                    color = Color.White.copy(alpha = 0.4f),
                )
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
            ),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = Color.White,
            ),
        )
        state.errorMessage?.let {
            Text(text = it, color = com.odysee.app.core.designsystem.theme.OdyseePink, style = MaterialTheme.typography.bodyMedium)
        }
        androidx.tv.material3.Surface(
            onClick = { viewModel.submit() },
            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
            colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                containerColor = com.odysee.app.core.designsystem.theme.OdyseePink,
                focusedContainerColor = com.odysee.app.core.designsystem.theme.OdyseePinkDark,
            ),
        ) {
            Text(
                text = if (state.isSending) "Sending…" else "Send sign-in link",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvPlaceholderSection(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 36.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvVideoRow(
    label: String,
    videos: List<VideoUiModel>,
    onVideoClick: (VideoUiModel) -> Unit,
    onLongClickVideo: ((VideoUiModel) -> Unit)? = null,
) {
    if (videos.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            items(videos, key = { it.id }) { video ->
                TvVideoCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onLongClick = onLongClickVideo?.let { f -> { f(video) } },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvVideoCard(
    video: VideoUiModel,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    androidx.tv.material3.Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.width(280.dp),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(158.dp)
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center,
            ) {
                if (!video.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = video.channelTitle?.takeIf { it.isNotBlank() } ?: video.channelName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvPlayer(
    media: CurrentMedia,
    controller: PlayerController,
    onBack: () -> Unit,
    queue: List<CurrentMedia> = emptyList(),
    autoplayNext: Boolean = true,
    onAdvance: (CurrentMedia) -> Unit = {},
    onOpenChannel: ((String, String) -> Unit)? = null,
) {
    BackHandler(onBack = onBack)
    androidx.compose.runtime.LaunchedEffect(media.claimId) {
        controller.play(media, openMode = PlayerOpenMode.Expanded)
    }
    if (autoplayNext) {
        androidx.compose.runtime.DisposableEffect(media.claimId, queue) {
            val listener = object : androidx.media3.common.Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == androidx.media3.common.Player.STATE_ENDED) {
                        val idx = queue.indexOfFirst { it.claimId == media.claimId }
                        val next = queue.getOrNull(idx + 1)
                        if (next != null) onAdvance(next)
                    }
                }
            }
            controller.exoPlayer.addListener(listener)
            onDispose { controller.exoPlayer.removeListener(listener) }
        }
    }
    var showOverlay by androidx.compose.runtime.remember(media.claimId) {
        androidx.compose.runtime.mutableStateOf(true)
    }
    androidx.compose.runtime.LaunchedEffect(media.claimId) {
        kotlinx.coroutines.delay(5_000)
        showOverlay = false
    }
    val nextInQueue = remember(media.claimId, queue) {
        val idx = queue.indexOfFirst { it.claimId == media.claimId }
        queue.getOrNull(idx + 1)
    }
    var timeRemainingMs by remember(media.claimId) { androidx.compose.runtime.mutableLongStateOf(Long.MAX_VALUE) }
    androidx.compose.runtime.LaunchedEffect(media.claimId, autoplayNext, nextInQueue) {
        while (autoplayNext && nextInQueue != null) {
            val dur = controller.exoPlayer.duration
            val pos = controller.exoPlayer.currentPosition
            timeRemainingMs = if (dur > 0) (dur - pos).coerceAtLeast(0) else Long.MAX_VALUE
            kotlinx.coroutines.delay(500)
        }
    }
    val showUpNext = nextInQueue != null && autoplayNext && timeRemainingMs in 0..10_000 &&
        media.liveStreamUrl == null
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val view = android.view.LayoutInflater.from(ctx).inflate(
                    com.odysee.app.R.layout.odysee_player_view, null,
                ) as PlayerView
                view.useController = true
                view.controllerShowTimeoutMs = 4000
                view.controllerAutoShow = true
                view.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                view.player = controller.exoPlayer
                view
            },
            update = { view ->
                view.player = controller.exoPlayer
                val isLive = media.liveStreamUrl != null
                val V = android.view.View.VISIBLE
                val G = android.view.View.GONE
                view.findViewById<android.view.View?>(androidx.media3.ui.R.id.exo_position)?.visibility =
                    if (isLive) G else V
                view.findViewById<android.view.View?>(androidx.media3.ui.R.id.exo_duration)?.visibility =
                    if (isLive) G else V
                view.findViewById<android.view.View?>(androidx.media3.ui.R.id.exo_progress)?.visibility =
                    if (isLive) G else V
                view.findViewById<android.view.View?>(com.odysee.app.R.id.odysee_live_button)?.visibility =
                    if (isLive) V else G
                if (isLive) {
                    val atEdge = run {
                        val dur = controller.exoPlayer.duration
                        val pos = controller.exoPlayer.currentPosition
                        dur > 0 && (dur - pos) < 5_000L
                    }
                    view.findViewById<android.view.View?>(com.odysee.app.R.id.odysee_live_dot)?.alpha =
                        if (atEdge) 1f else 0.45f
                    view.findViewById<android.widget.TextView?>(com.odysee.app.R.id.odysee_live_label)?.alpha =
                        if (atEdge) 1f else 0.7f
                    view.findViewById<android.view.View?>(com.odysee.app.R.id.odysee_live_button)
                        ?.setOnClickListener {
                            val dur = controller.exoPlayer.duration
                            if (dur > 0) controller.exoPlayer.seekTo(dur)
                            else controller.exoPlayer.seekToDefaultPosition()
                        }
                }
            },
        )
        if (showOverlay) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                        ),
                    )
                    .padding(horizontal = 48.dp, vertical = 32.dp),
            ) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = media.channelTitle?.takeIf { it.isNotBlank() } ?: media.channelName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        val channelClaimId = media.channelClaimId
        if (showOverlay && onOpenChannel != null && channelClaimId != null) {
            androidx.tv.material3.Surface(
                onClick = { onOpenChannel(channelClaimId, media.channelName) },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 48.dp, bottom = 36.dp),
                shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
                colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    focusedContainerColor = com.odysee.app.core.designsystem.theme.OdyseePink,
                ),
            ) {
                Text(
                    text = "Go to channel",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
        if (showUpNext && nextInQueue != null) {
            UpNextCard(
                next = nextInQueue,
                secondsLeft = (timeRemainingMs / 1000).toInt(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 48.dp, bottom = 36.dp),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UpNextCard(
    next: CurrentMedia,
    secondsLeft: Int,
    modifier: Modifier = Modifier,
) {
    androidx.tv.material3.Surface(
        modifier = modifier.width(360.dp),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.85f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Up next in ${secondsLeft.coerceAtLeast(0)}s",
                style = MaterialTheme.typography.labelMedium,
                color = com.odysee.app.core.designsystem.theme.OdyseePink,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 68.dp)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp)),
                ) {
                    if (!next.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = next.thumbnailUrl,
                            contentDescription = next.title,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = next.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = next.channelTitle?.takeIf { it.isNotBlank() } ?: next.channelName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvChannelSection(
    claimId: String,
    name: String,
    onBack: () -> Unit,
    onVideoClick: (CurrentMedia) -> Unit,
    viewModel: TvChannelViewModel = hiltViewModel(),
) {
    BackHandler(onBack = onBack)
    androidx.compose.runtime.LaunchedEffect(claimId) { viewModel.load(claimId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val displayName = state.channel?.title?.takeIf { it.isNotBlank() }
        ?: state.channel?.name?.takeIf { it.isNotBlank() }
        ?: name
    val videos = remember(state.claims) {
        state.claims.map { claim ->
            VideoUiModel(
                id = claim.claimId,
                permanentUrl = claim.permanentUrl ?: "",
                title = claim.title ?: claim.name.orEmpty(),
                description = claim.description,
                channelClaimId = claim.signingChannel?.claimId,
                channelName = claim.signingChannel?.name.orEmpty(),
                channelTitle = claim.signingChannel?.title,
                channelInitial = (claim.signingChannel?.name?.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
                channelAvatarUrl = claim.signingChannel?.thumbnailUrl,
                channelAvatarTintIndex = 0,
                thumbnailUrl = claim.thumbnailUrl,
                thumbnailTintIndex = 0,
                ageLabel = "",
                durationLabel = "",
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(horizontal = 48.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(48.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val avatar = state.channel?.thumbnailUrl
                if (!avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = avatar,
                        contentDescription = displayName,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = (displayName.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar().toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                state.followerCount?.let { fc ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${formatTvCount(fc)} followers",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
            androidx.tv.material3.Surface(
                onClick = { viewModel.toggleSubscribe(state.channel?.name ?: name) },
                shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
                colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                    containerColor = if (state.isSubscribed) Color(0xFF2A2A2A) else com.odysee.app.core.designsystem.theme.OdyseePink,
                    focusedContainerColor = if (state.isSubscribed) Color(0xFF3A3A3A) else com.odysee.app.core.designsystem.theme.OdyseePinkDark,
                ),
            ) {
                Text(
                    text = if (state.isSubscribed) "Following" else "Follow",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
        when {
            state.isLoading -> Text("Loading…", color = Color.White.copy(alpha = 0.7f))
            videos.isEmpty() && state.playlists.isEmpty() ->
                Text("Nothing here yet.", color = Color.White.copy(alpha = 0.6f))
            else -> {
                if (videos.isNotEmpty()) {
                    TvVideoRow(
                        label = "Latest",
                        videos = videos,
                        onVideoClick = { v -> onVideoClick(v.toCurrentMedia()) },
                    )
                }
                if (state.playlists.isNotEmpty()) {
                    TvChannelPlaylistsRow(playlists = state.playlists)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvChannelPlaylistsRow(playlists: List<TvChannelPlaylist>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Playlists",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        TvLazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(playlists, key = { it.claimId }) { pl ->
                androidx.tv.material3.Surface(
                    onClick = { /* TODO: open playlist on TV */ },
                    shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
                    modifier = Modifier.width(280.dp),
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(158.dp)
                                .background(Color(0xFF1A1A1A)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!pl.thumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = pl.thumbnailUrl,
                                    contentDescription = pl.title,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Text(
                                    text = "▶",
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.displayMedium,
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = pl.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = "${pl.itemCount} videos",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTvCount(count: Long): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0).removeSuffix(".0M") + "M"
    count >= 1_000 -> "%.1fK".format(count / 1_000.0).removeSuffix(".0K") + "K"
    else -> count.toString()
}.replace(Regex("MM\$"), "M").replace(Regex("KK\$"), "K")

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSearchSection(
    onVideoClick: (CurrentMedia) -> Unit,
    onChannelClick: (String, String) -> Unit,
    viewModel: com.odysee.app.feature.search.SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val voiceLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val data = result.data
            val spoken = data
                ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) viewModel.onQueryChange(spoken)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.weight(1f, fill = false).fillMaxWidth(0.55f),
                placeholder = {
                    androidx.compose.material3.Text(
                        text = "Search Odysee…",
                        color = Color.White.copy(alpha = 0.5f),
                    )
                },
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color.White,
                ),
            )
            Spacer(Modifier.width(16.dp))
            androidx.tv.material3.Surface(
                onClick = {
                    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                        )
                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak to search")
                    }
                    runCatching { voiceLauncher.launch(intent) }
                },
                shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
                colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                    containerColor = Color(0xFF2A2A2A),
                    focusedContainerColor = com.odysee.app.core.designsystem.theme.OdyseePink,
                ),
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Mic,
                    contentDescription = "Voice search",
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp).size(28.dp),
                )
            }
        }
        when (val s = state.state) {
            com.odysee.app.feature.search.SearchState.Idle -> Text(
                text = "Type to search.",
                color = Color.White.copy(alpha = 0.6f),
            )
            com.odysee.app.feature.search.SearchState.Loading -> Text(
                text = "Searching…",
                color = Color.White.copy(alpha = 0.6f),
            )
            is com.odysee.app.feature.search.SearchState.Error -> Text(
                text = s.message,
                color = com.odysee.app.core.designsystem.theme.OdyseePink,
            )
            is com.odysee.app.feature.search.SearchState.Success -> {
                if (s.results.isEmpty()) {
                    Text(
                        text = "No results.",
                        color = Color.White.copy(alpha = 0.6f),
                    )
                } else {
                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(s.results, key = { it.claimId }) { result ->
                            TvVideoCard(
                                video = result.toVideoUiModelMinimal(),
                                onClick = {
                                    if (result.isChannel) onChannelClick(result.claimId, result.name)
                                    else onVideoClick(result.toCurrentMedia())
                                },
                                onLongClick = result.channelClaimId?.let { cid ->
                                    { onChannelClick(cid, result.channelName.orEmpty()) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvLibrarySection(
    onVideoClick: (CurrentMedia) -> Unit,
    onChannelClick: (String, String) -> Unit,
    viewModel: TvLibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val historyAsVideos = remember(state.history) {
        state.history.map { it.toVideoUiModelMinimal() }
    }
    val watchLaterAsVideos = remember(state.watchLater) {
        state.watchLater.map { it.toVideoUiModelMinimal() }
    }
    val favoritesAsVideos = remember(state.favorites) {
        state.favorites.map { it.toVideoUiModelMinimal() }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        if (historyAsVideos.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Continue watching",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    androidx.tv.material3.Surface(
                        onClick = { viewModel.clearHistory() },
                        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
                        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                            containerColor = Color(0xFF2A2A2A),
                            focusedContainerColor = Color(0xFF3A3A3A),
                        ),
                    ) {
                        Text(
                            text = "Clear",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                TvLazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(historyAsVideos, key = { it.id }) { v ->
                        TvVideoCard(video = v, onClick = { onVideoClick(v.toCurrentMedia()) })
                    }
                }
            }
        }
        if (watchLaterAsVideos.isNotEmpty()) {
            TvVideoRow(
                label = "Watch later",
                videos = watchLaterAsVideos,
                onVideoClick = { v -> onVideoClick(v.toCurrentMedia()) },
            )
        }
        if (favoritesAsVideos.isNotEmpty()) {
            TvVideoRow(
                label = "Favorites",
                videos = favoritesAsVideos,
                onVideoClick = { v -> onVideoClick(v.toCurrentMedia()) },
            )
        }
        if (state.subscriptions.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Following",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                TvLazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(state.subscriptions, key = { it.claimId }) { sub ->
                        androidx.tv.material3.Surface(
                            onClick = { onChannelClick(sub.claimId, sub.name) },
                            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
                            modifier = Modifier.width(220.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(Color(0xFF1A1A1A), RoundedCornerShape(36.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = (sub.name.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar().toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = sub.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (historyAsVideos.isEmpty() && watchLaterAsVideos.isEmpty() && favoritesAsVideos.isEmpty() && state.subscriptions.isEmpty()) {
            Text(
                text = "Nothing here yet. Sign in and save videos, follow channels, or build playlists on the phone/tablet app and they'll show up here.",
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

private fun com.odysee.app.feature.search.SearchResultUi.toVideoUiModelMinimal(): VideoUiModel = VideoUiModel(
    id = claimId,
    permanentUrl = permanentUrl,
    title = title.takeIf { it.isNotBlank() } ?: name,
    description = description,
    channelClaimId = channelClaimId,
    channelName = channelName.orEmpty(),
    channelTitle = null,
    channelInitial = (channelName?.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
    channelAvatarUrl = channelAvatarUrl,
    channelAvatarTintIndex = 0,
    thumbnailUrl = thumbnailUrl,
    thumbnailTintIndex = tintIndex,
    ageLabel = ageLabel,
    durationLabel = durationLabel,
)

private fun com.odysee.app.feature.search.SearchResultUi.toCurrentMedia(): CurrentMedia = CurrentMedia(
    claimId = claimId,
    permanentUrl = permanentUrl,
    title = title.takeIf { it.isNotBlank() } ?: name,
    description = description,
    channelClaimId = channelClaimId,
    channelName = channelName.orEmpty(),
    channelTitle = null,
    channelInitial = (channelName?.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
    channelAvatarUrl = channelAvatarUrl,
    thumbnailUrl = thumbnailUrl,
    ageLabel = ageLabel.takeIf { it.isNotEmpty() },
    liveStreamUrl = null,
    isShort = false,
)

private fun com.odysee.app.core.data.collections.CollectionEntry.toVideoUiModelMinimal(): VideoUiModel = VideoUiModel(
    id = claimId,
    permanentUrl = permanentUrl,
    title = title,
    description = null,
    channelClaimId = channelClaimId,
    channelName = channelName,
    channelTitle = null,
    channelInitial = (channelName.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
    channelAvatarUrl = null,
    channelAvatarTintIndex = 0,
    thumbnailUrl = thumbnailUrl,
    thumbnailTintIndex = 0,
    ageLabel = "",
    durationLabel = "",
)

private fun com.odysee.app.core.data.history.WatchHistoryEntry.toVideoUiModelMinimal(): VideoUiModel = VideoUiModel(
    id = claimId,
    permanentUrl = permanentUrl,
    title = title,
    description = null,
    channelClaimId = channelClaimId,
    channelName = channelName,
    channelTitle = null,
    channelInitial = (channelName.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
    channelAvatarUrl = null,
    channelAvatarTintIndex = 0,
    thumbnailUrl = thumbnailUrl,
    thumbnailTintIndex = 0,
    ageLabel = "",
    durationLabel = "",
)

private fun VideoUiModel.toCurrentMedia(): CurrentMedia = CurrentMedia(
    claimId = id,
    permanentUrl = permanentUrl,
    title = title,
    description = description,
    channelClaimId = channelClaimId,
    channelName = channelName,
    channelTitle = channelTitle,
    channelInitial = channelInitial,
    channelAvatarUrl = channelAvatarUrl,
    thumbnailUrl = thumbnailUrl,
    ageLabel = ageLabel.takeIf { it.isNotEmpty() },
    liveStreamUrl = liveStreamUrl,
    isShort = false,
)
