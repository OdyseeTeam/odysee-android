package com.odysee.app.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Tag
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.designsystem.claims.OdyseeClaimCard
import com.odysee.app.core.designsystem.claims.OdyseeClaimCardModel
import com.odysee.app.core.designsystem.claims.toCardPaywall
import com.odysee.app.core.designsystem.layout.feedColumns
import com.odysee.app.core.designsystem.layout.rememberWindowSize
import com.odysee.app.core.model.Claim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject
import kotlin.math.absoluteValue

@Serializable
data class HashtagRoute(val tag: String)

data class HashtagUiState(
    val tag: String = "",
    val claims: List<Claim> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val exhausted: Boolean = false,
    val page: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class HashtagViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
) : ViewModel() {

    val tag: String = (savedStateHandle.get<String>("tag") ?: "")
        .removePrefix("#").trim().lowercase()

    private val _state = MutableStateFlow(HashtagUiState(tag = tag, isLoading = true))
    val state: StateFlow<HashtagUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (tag.isEmpty()) {
            _state.update { it.copy(isLoading = false, error = "No tag", exhausted = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, claims = emptyList(), page = 0, exhausted = false) }
            runCatching { contentRepository.getClaimsByTags(listOf(tag), page = 1, pageSize = PAGE_SIZE) }
                .onSuccess { claims ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            claims = claims,
                            page = 1,
                            exhausted = claims.size < PAGE_SIZE,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: e::class.simpleName ?: "Couldn't load",
                        )
                    }
                }
        }
    }

    fun loadMore() {
        val cur = _state.value
        if (cur.isLoading || cur.isLoadingMore || cur.exhausted) return
        val next = cur.page + 1
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            runCatching { contentRepository.getClaimsByTags(listOf(tag), page = next, pageSize = PAGE_SIZE) }
                .onSuccess { claims ->
                    val existing = cur.claims.mapTo(mutableSetOf()) { it.claimId }
                    val newOnes = claims.filterNot { existing.contains(it.claimId) }
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            claims = it.claims + newOnes,
                            page = next,
                            exhausted = claims.size < PAGE_SIZE,
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashtagScreen(
    viewModel: HashtagViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onWatch: (Claim) -> Unit,
    onChannelClick: (String, String) -> Unit,
) {
    BackHandler(onBack = onBack)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    LaunchedEffect(gridState, state.claims.size) {
        androidx.compose.runtime.snapshotFlow {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last to info.totalItemsCount
        }.collect { (lastVisible, total) ->
            if (total > 0 && lastVisible >= total - 4) viewModel.loadMore()
        }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Tag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.fillMaxWidth(0f))
                        Text(
                            text = state.tag,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp),
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
        when {
            state.isLoading && state.claims.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            state.error != null && state.claims.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.error ?: "Couldn't load",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.claims.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No videos found for #${state.tag}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                val columns = rememberWindowSize().feedColumns()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (columns > 1) 12.dp else 0.dp,
                        end = if (columns > 1) 12.dp else 0.dp,
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + 16.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(if (columns > 1) 12.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(state.claims, key = { it.claimId }) { claim ->
                        OdyseeClaimCard(
                            forceColumnLayout = columns > 1,
                            claim = OdyseeClaimCardModel(
                                claimId = claim.claimId,
                                title = claim.title.ifBlank { claim.name },
                                channelName = claim.signingChannel?.name.orEmpty(),
                                channelClaimId = claim.signingChannel?.claimId,
                                channelTitle = claim.signingChannel?.title,
                                channelAvatarUrl = claim.signingChannel?.thumbnailUrl,
                                channelInitial = (claim.signingChannel?.name?.firstOrNull { it.isLetterOrDigit() }
                                    ?: 'O').uppercaseChar(),
                                thumbnailUrl = claim.thumbnailUrl,
                                ageLabel = formatAgeShort(claim.releaseTime),
                                durationLabel = formatDurationShort(claim.durationSeconds),
                                thumbnailTintIndex = claim.claimId.hashCode().absoluteValue,
                                paywall = toCardPaywall(claim.paywall),
                                isPurchased = claim.isPurchased,
                                isMembersOnly = claim.isMembersOnly,
                            ),
                            onClick = { onWatch(claim) },
                            onChannelClick = {
                                val cid = claim.signingChannel?.claimId
                                val cname = claim.signingChannel?.name
                                if (cid != null && cname != null) onChannelClick(cid, cname)
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun formatDurationShort(seconds: Long?): String {
    if (seconds == null || seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatAgeShort(releaseTimeSeconds: Long?): String {
    if (releaseTimeSeconds == null || releaseTimeSeconds <= 0) return ""
    val nowSec = System.currentTimeMillis() / 1000
    val delta = (nowSec - releaseTimeSeconds).coerceAtLeast(0)
    return when {
        delta < 60 -> "Just now"
        delta < 3600 -> "${delta / 60}m ago"
        delta < 86_400 -> "${delta / 3600}h ago"
        delta < 7 * 86_400 -> "${delta / 86_400}d ago"
        delta < 30 * 86_400 -> "${delta / (7 * 86_400)}w ago"
        delta < 365 * 86_400 -> "${delta / (30 * 86_400)}mo ago"
        else -> "${delta / (365 * 86_400)}y ago"
    }
}
