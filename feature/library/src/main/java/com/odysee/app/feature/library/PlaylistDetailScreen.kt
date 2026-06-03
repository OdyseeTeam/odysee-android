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
import com.odysee.app.core.designsystem.layout.feedColumns
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.SwapVert
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

    fun applySortAndPersist(sort: PlaylistSort) {
        val cur = _state.value
        if (cur.claims.isEmpty()) return
        val sortedClaims = when (sort) {
            PlaylistSort.Manual -> return
            PlaylistSort.Newest -> cur.claims.sortedByDescending { it.releaseTime ?: 0L }
            PlaylistSort.Oldest -> cur.claims.sortedBy { it.releaseTime ?: Long.MAX_VALUE }
            PlaylistSort.TitleAsc -> cur.claims.sortedBy { it.title.lowercase() }
            PlaylistSort.TitleDesc -> cur.claims.sortedByDescending { it.title.lowercase() }
        }
        val byCid = cur.itemUrls.associateBy { url -> extractClaimId(url) ?: "" }
        val orderedUrls = sortedClaims.mapNotNull { c -> byCid[c.claimId] }
        if (orderedUrls.size != cur.itemUrls.size) return
        _state.value = cur.copy(claims = sortedClaims, itemUrls = orderedUrls)
        viewModelScope.launch {
            playlistsRepository.reorderItems(playlistId, orderedUrls)
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val cur = _state.value
        if (fromIndex == toIndex) return
        if (fromIndex !in cur.claims.indices) return
        val target = toIndex.coerceIn(0, cur.claims.lastIndex)
        val claims = cur.claims.toMutableList().apply { add(target, removeAt(fromIndex)) }
        val urls = cur.itemUrls.toMutableList().apply { add(target, removeAt(fromIndex)) }
        _state.value = cur.copy(claims = claims, itemUrls = urls)
        viewModelScope.launch {
            playlistsRepository.reorderItems(playlistId, urls)
        }
    }
}

enum class PlaylistSort { Manual, Newest, Oldest, TitleAsc, TitleDesc }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onWatch: (PlaylistWatchTarget) -> Unit,
    onEdit: (String) -> Unit = {},
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
    var sortMenuOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var reorderMode by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
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
                actions = {
                    IconButton(onClick = { reorderMode = !reorderMode }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.SwapVert,
                            contentDescription = if (reorderMode) "Done reordering" else "Reorder",
                            tint = if (reorderMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.Sort,
                                contentDescription = "Sort",
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false },
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Newest first") },
                                onClick = {
                                    viewModel.applySortAndPersist(PlaylistSort.Newest)
                                    sortMenuOpen = false
                                },
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Oldest first") },
                                onClick = {
                                    viewModel.applySortAndPersist(PlaylistSort.Oldest)
                                    sortMenuOpen = false
                                },
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Title (A–Z)") },
                                onClick = {
                                    viewModel.applySortAndPersist(PlaylistSort.TitleAsc)
                                    sortMenuOpen = false
                                },
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Title (Z–A)") },
                                onClick = {
                                    viewModel.applySortAndPersist(PlaylistSort.TitleDesc)
                                    sortMenuOpen = false
                                },
                            )
                        }
                    }
                    IconButton(onClick = { onEdit(viewModel.playlistId) }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.Edit,
                            contentDescription = "Edit playlist",
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
        if (state.isLoading && state.claims.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }
        val columns = if (reorderMode) 1 else (com.odysee.app.core.designsystem.layout.rememberWindowSize()
            .feedColumns() / 2).coerceAtLeast(1)
        val rows = state.claims.withIndex().toList().chunked(columns)
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
            items(rows, key = { it.first().value.claimId }) { chunk ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    chunk.forEach { (idx, claim) ->
                        Box(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ClaimRow(
                                        claim = claim,
                                        onClick = {
                                            buildTarget(viewModel.playlistId, state.title, state.claims, idx)?.let(onWatch)
                                        },
                                        onLongPress = { claimMenuTarget = claim },
                                    )
                                }
                                if (reorderMode) {
                                    Column {
                                        IconButton(
                                            enabled = idx > 0,
                                            onClick = { viewModel.moveItem(idx, idx - 1) },
                                        ) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Outlined.KeyboardArrowUp,
                                                contentDescription = "Move up",
                                            )
                                        }
                                        IconButton(
                                            enabled = idx < state.claims.lastIndex,
                                            onClick = { viewModel.moveItem(idx, idx + 1) },
                                        ) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Outlined.KeyboardArrowDown,
                                                contentDescription = "Move down",
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    repeat(columns - chunk.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
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
