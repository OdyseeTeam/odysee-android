package com.odysee.app.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.odysee.app.auth.LocalAuthState
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.model.Claim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadsViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val claims: List<Claim> = emptyList(),
        val errorMessage: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load(channelIds: List<String>) {
        if (channelIds.isEmpty()) {
            _state.value = UiState(isLoading = false, claims = emptyList())
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = runCatching { contentRepository.getChannelVideos(channelIds.first(), page = 1, pageSize = 30) }
            result.fold(
                onSuccess = { items ->
                    _state.update { it.copy(isLoading = false, claims = items, errorMessage = null) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't load uploads") }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadsScreen(
    viewModel: UploadsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onWatch: (Claim) -> Unit,
    onPlayBackground: (Claim) -> Unit = {},
    onPlayPip: (Claim) -> Unit = {},
    onSaveWatchLater: (Claim) -> Unit = {},
    onSaveFavorite: (Claim) -> Unit = {},
) {
    BackHandler(onBack = onBack)
    val auth = LocalAuthState.current
    val activeChannelId = (auth as? AuthState.SignedIn)?.activeChannel?.claimId
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardMgr = context.getSystemService(android.content.ClipboardManager::class.java)
    var claimMenuTarget by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<Claim?>(null)
    }
    var addToPlaylistTarget by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<Claim?>(null)
    }
    LaunchedEffect(activeChannelId) {
        if (!activeChannelId.isNullOrBlank()) viewModel.load(listOf(activeChannelId))
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Your uploads") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            state.errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.claims.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No uploads yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Tap the + button in the bottom tabs to upload your first file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                items(state.claims, key = { it.claimId }) { claim ->
                    UploadRow(
                        claim = claim,
                        onClick = { onWatch(claim) },
                        onLongPress = { claimMenuTarget = claim },
                    )
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
                onPlayBackground = { onPlayBackground(target) },
                onPlayPip = { onPlayPip(target) },
                onSaveWatchLater = { onSaveWatchLater(target) },
                onSaveFavorite = { onSaveFavorite(target) },
                onAddToPlaylist = { addToPlaylistTarget = target },
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
                onEdit = {
                    val stripped = target.permanentUrl.removePrefix("lbry://")
                    val link = "https://odysee.com/$stripped?view=publish"
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
        com.odysee.app.feature.library.AddToPlaylistSheet(
            title = target.title.ifBlank { target.name },
            permanentUrl = target.permanentUrl,
            onDismiss = { addToPlaylistTarget = null },
            onCreateNew = { addToPlaylistTarget = null },
            quickTarget = com.odysee.app.feature.library.QuickTargetClaim(
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun UploadRow(
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
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
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
        }
    }
}

