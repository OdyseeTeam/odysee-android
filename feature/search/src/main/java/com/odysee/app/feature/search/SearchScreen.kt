package com.odysee.app.feature.search

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onWatch: (SearchResultUi) -> Unit,
    onChannelClick: (String, String) -> Unit,
    onPlayBackground: (SearchResultUi) -> Unit = {},
    onPlayPip: (SearchResultUi) -> Unit = {},
    onSaveWatchLater: (SearchResultUi) -> Unit = {},
    onSaveFavorite: (SearchResultUi) -> Unit = {},
    onCreateNewPlaylist: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardMgr = context.getSystemService(android.content.ClipboardManager::class.java)
    var claimMenuTarget by remember { mutableStateOf<SearchResultUi?>(null) }
    var addToPlaylistTarget by remember { mutableStateOf<SearchResultUi?>(null) }
    var repostTarget by remember { mutableStateOf<SearchResultUi?>(null) }
    BackHandler(onBack = onBack)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    TextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search Odysee") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.submit() }),
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (val current = state.state) {
            SearchState.Idle -> EmptyState(
                title = "Find creators, channels and videos",
                message = "Type a query to search Odysee.",
                padding = padding,
            )
            SearchState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is SearchState.Error -> EmptyState(
                title = "Couldn't search",
                message = current.message,
                padding = padding,
            )
            is SearchState.Success -> {
                if (current.results.isEmpty()) {
                    EmptyState(
                        title = "No results",
                        message = "Try a different query.",
                        padding = padding,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = padding.calculateTopPadding(),
                            bottom = padding.calculateBottomPadding() + 16.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(current.results, key = { it.claimId }) { result ->
                            ResultRow(
                                result = result,
                                onClick = {
                                    if (result.isChannel) {
                                        onChannelClick(result.claimId, result.name)
                                    } else {
                                        onWatch(result)
                                    }
                                },
                                onChannelClick = {
                                    val id = result.channelClaimId
                                    val n = result.channelName
                                    if (id != null && n != null) onChannelClick(id, n)
                                },
                                onLongPress = {
                                    // Streams get the full claim menu; channels go
                                    // straight to the repost dialog (the only useful
                                    // long-press action for a channel).
                                    if (result.isChannel) repostTarget = result
                                    else claimMenuTarget = result
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    claimMenuTarget?.let { target ->
        com.odysee.app.core.designsystem.claims.OdyseeClaimMenuSheet(
            target = com.odysee.app.core.designsystem.claims.OdyseeClaimMenuTarget(
                claimId = target.claimId,
                name = target.name,
                title = target.title,
                permanentUrl = target.permanentUrl,
                channelClaimId = target.channelClaimId,
                channelName = target.channelName,
            ),
            actions = com.odysee.app.core.designsystem.claims.OdyseeClaimMenuActions(
                onPlayBackground = { onPlayBackground(target) },
                onPlayPip = { onPlayPip(target) },
                onSaveWatchLater = { onSaveWatchLater(target) },
                onSaveFavorite = { onSaveFavorite(target) },
                onAddToPlaylist = { addToPlaylistTarget = target },
                onRepost = { repostTarget = target },
                onShare = {
                    val stripped = target.permanentUrl.removePrefix("lbry://")
                    val url = "https://odysee.com/$stripped"
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, target.title)
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
                onGoToChannel = target.channelClaimId?.let { cid ->
                    target.channelName?.let { cname -> { onChannelClick(cid, cname) } }
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

    repostTarget?.let { target ->
        com.odysee.app.feature.library.RepostSheet(
            claimId = target.claimId,
            claimName = target.title,
            onDismiss = { repostTarget = null },
            onPosted = { repostTarget = null },
        )
    }

    addToPlaylistTarget?.let { target ->
        com.odysee.app.feature.library.AddToPlaylistSheet(
            title = target.title,
            permanentUrl = target.permanentUrl,
            onDismiss = { addToPlaylistTarget = null },
            onCreateNew = {
                addToPlaylistTarget = null
                onCreateNewPlaylist()
            },
            quickTarget = com.odysee.app.feature.library.QuickTargetClaim(
                claimId = target.claimId,
                permanentUrl = target.permanentUrl,
                title = target.title,
                channelName = target.channelName.orEmpty(),
                channelClaimId = target.channelClaimId,
                thumbnailUrl = target.thumbnailUrl,
            ),
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ResultRow(
    result: SearchResultUi,
    onClick: () -> Unit,
    onChannelClick: () -> Unit,
    onLongPress: () -> Unit = {},
) {
    if (result.isChannel) {
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
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(palette[result.tintIndex % palette.size]),
                contentAlignment = Alignment.Center,
            ) {
                if (!result.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = result.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = (result.name.firstOrNull { it.isLetterOrDigit() } ?: '@').uppercaseChar().toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title.takeIf { it.isNotBlank() } ?: result.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onLongPress()
                    },
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(palette[result.tintIndex % palette.size]),
            ) {
                if (!result.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = result.thumbnailUrl,
                        contentDescription = result.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (result.durationLabel.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = result.durationLabel,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                SearchPaywallBadge(
                    paywall = result.paywall,
                    isPurchased = result.isPurchased,
                    isMembersOnly = result.isMembersOnly,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onChannelClick),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!result.channelAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = result.channelAvatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = (result.channelName?.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar().toString(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            result.channelName?.let { append(it) }
                            if (result.ageLabel.isNotEmpty()) {
                                if (isNotEmpty()) append(" • ")
                                append(result.ageLabel)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String, padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val palette = listOf(
    Color(0xFF7B2942),
    Color(0xFF2C5364),
    Color(0xFF4A4E69),
    Color(0xFF3D405B),
    Color(0xFF52489C),
    Color(0xFF2A4858),
)

@Composable
private fun androidx.compose.foundation.layout.BoxScope.SearchPaywallBadge(
    paywall: com.odysee.app.core.model.Paywall,
    isPurchased: Boolean,
    isMembersOnly: Boolean,
) {
    val (label, bg) = when {
        isPurchased && paywall !is com.odysee.app.core.model.Paywall.Free ->
            "Purchased" to Color(0xCC1AC04F)
        paywall is com.odysee.app.core.model.Paywall.Lbc ->
            "${formatLbc(paywall.amount)} LBC" to Color(0xCCE2202D)
        paywall is com.odysee.app.core.model.Paywall.FiatPurchase ->
            "$${"%.2f".format(paywall.usd)}" to Color(0xCCE2202D)
        paywall is com.odysee.app.core.model.Paywall.FiatRental ->
            "Rent $${"%.2f".format(paywall.usd)}" to Color(0xCCE2202D)
        isMembersOnly -> "Members" to Color(0xCC5E60CE)
        else -> return
    }
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formatLbc(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) "${amount.toLong()}"
    else "%.2f".format(amount).trimEnd('0').trimEnd('.')
}
