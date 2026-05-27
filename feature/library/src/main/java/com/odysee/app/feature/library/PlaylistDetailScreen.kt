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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.collections.PlaylistsRepository
import com.odysee.app.core.model.Claim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistWatchTarget(
    val playlistId: String,
    val playlistName: String,
    val items: List<PlaylistWatchItem>,
    val startIndex: Int,
)

data class PlaylistWatchItem(
    val claimId: String,
    val permanentUrl: String,
    val title: String,
    val description: String? = null,
    val channelName: String,
    val channelClaimId: String?,
    val channelAvatarUrl: String?,
    val thumbnailUrl: String?,
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistsRepository: PlaylistsRepository,
    private val contentRepository: ContentRepository,
) : ViewModel() {

    val playlistId: String = checkNotNull(savedStateHandle["playlistId"]) {
        "playlistId required"
    }
    val autoplay: Boolean = savedStateHandle.get<Boolean>("autoplay") ?: false
    val shuffle: Boolean = savedStateHandle.get<Boolean>("shuffle") ?: false

    data class State(
        val title: String = "",
        val itemUrls: List<String> = emptyList(),
        val claims: List<Claim> = emptyList(),
        val isLoading: Boolean = true,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val p = playlistsRepository.playlists.first().firstOrNull { it.id == playlistId }
            if (p == null) {
                _state.value = State(isLoading = false)
                return@launch
            }
            _state.value = State(
                title = p.name,
                itemUrls = p.itemUrls,
                claims = emptyList(),
                isLoading = true,
            )
            val claimIds = p.itemUrls.mapNotNull { extractClaimId(it) }.distinct()
            val claims = runCatching { contentRepository.getClaimsByIds(claimIds) }
                .getOrDefault(emptyList())
            val byId = claims.associateBy { it.claimId }
            val ordered = p.itemUrls.mapNotNull { url ->
                val cid = extractClaimId(url) ?: return@mapNotNull null
                byId[cid]
            }
            _state.value = _state.value.copy(claims = ordered, isLoading = false)
        }
    }

    private fun extractClaimId(url: String): String? {
        val stripped = url.removePrefix("lbry://").trim()
        val sepIdx = stripped.lastIndexOfAny(charArrayOf('#', ':'))
        if (sepIdx <= 0) return null
        val tail = stripped.substring(sepIdx + 1).takeWhile { it != '/' && it != '?' }
        return tail.takeIf { id -> id.length in 8..40 && id.all { it.isLetterOrDigit() } }
    }

    fun removeFromPlaylist(claim: Claim) {
        viewModelScope.launch {
            val url = state.value.itemUrls.firstOrNull { extractClaimId(it) == claim.claimId }
                ?: claim.permanentUrl
            playlistsRepository.removeItem(playlistId, url)
            val updated = playlistsRepository.playlists.first().firstOrNull { it.id == playlistId }
                ?: return@launch
            val newClaims = state.value.claims.filterNot { it.claimId == claim.claimId }
            _state.value = _state.value.copy(itemUrls = updated.itemUrls, claims = newClaims)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onWatch: (PlaylistWatchTarget) -> Unit,
) {
    BackHandler(onBack = onBack)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardMgr = context.getSystemService(android.content.ClipboardManager::class.java)
    var autoplayConsumed by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(state.claims) {
        if (!autoplayConsumed && viewModel.autoplay && state.claims.isNotEmpty()) {
            autoplayConsumed = true
            val ordered = if (viewModel.shuffle) state.claims.shuffled() else state.claims
            buildTarget(viewModel.playlistId, state.title, ordered, 0)?.let(onWatch)
        }
    }
    var claimMenuTarget by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<Claim?>(null)
    }
    var addToPlaylistTarget by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<Claim?>(null)
    }
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
                        text = state.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading && state.claims.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }
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
                        text = "${state.claims.size} ${if (state.claims.size == 1) "video" else "videos"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val target = buildTarget(viewModel.playlistId, state.title, state.claims, 0)
                                ?: return@Button
                            onWatch(target)
                        },
                        enabled = state.claims.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Play all", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            items(state.claims.size, key = { state.claims[it].claimId }) { idx ->
                val claim = state.claims[idx]
                ClaimRow(
                    claim = claim,
                    onClick = {
                        buildTarget(viewModel.playlistId, state.title, state.claims, idx)?.let(onWatch)
                    },
                    onLongPress = { claimMenuTarget = claim },
                )
            }
        }
    }

    claimMenuTarget?.let { target ->
        com.odysee.app.core.designsystem.claims.OdyseeClaimMenuSheet(
            target = com.odysee.app.core.designsystem.claims.OdyseeClaimMenuTarget(
                claimId = target.claimId,
                name = target.name,
                title = target.title.ifBlank { target.name },
                permanentUrl = target.permanentUrl,
                channelClaimId = target.signingChannel?.claimId,
                channelName = target.signingChannel?.name,
            ),
            actions = com.odysee.app.core.designsystem.claims.OdyseeClaimMenuActions(
                onAddToPlaylist = { addToPlaylistTarget = target },
                onRemoveFromPlaylist = { viewModel.removeFromPlaylist(target) },
                onShare = {
                    val stripped = target.permanentUrl.removePrefix("lbry://")
                    val url = "https://odysee.com/$stripped"
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, target.title.ifBlank { target.name })
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
            title = target.title.ifBlank { target.name },
            permanentUrl = target.permanentUrl,
            onDismiss = { addToPlaylistTarget = null },
            onCreateNew = { addToPlaylistTarget = null },
            quickTarget = QuickTargetClaim(
                claimId = target.claimId,
                permanentUrl = target.permanentUrl,
                title = target.title.ifBlank { target.name },
                channelName = target.signingChannel?.name.orEmpty(),
                channelClaimId = target.signingChannel?.claimId,
                thumbnailUrl = target.thumbnailUrl,
            ),
        )
    }
}

private fun buildTarget(
    playlistId: String,
    playlistName: String,
    claims: List<Claim>,
    startIndex: Int,
): PlaylistWatchTarget? {
    if (claims.isEmpty()) return null
    val items = claims.map { c ->
        PlaylistWatchItem(
            claimId = c.claimId,
            permanentUrl = c.permanentUrl,
            title = c.title,
            description = c.description,
            channelName = c.signingChannel?.name.orEmpty(),
            channelClaimId = c.signingChannel?.claimId,
            channelAvatarUrl = c.signingChannel?.thumbnailUrl,
            thumbnailUrl = c.thumbnailUrl,
        )
    }
    return PlaylistWatchTarget(
        playlistId = playlistId,
        playlistName = playlistName,
        items = items,
        startIndex = startIndex.coerceIn(0, items.size - 1),
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ClaimRow(
    claim: Claim,
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
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!claim.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = claim.thumbnailUrl,
                    contentDescription = claim.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = claim.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = claim.signingChannel?.title ?: claim.signingChannel?.name.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
