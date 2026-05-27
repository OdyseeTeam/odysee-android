package com.odysee.app.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odysee.app.core.designsystem.claims.OdyseeClaimCard
import com.odysee.app.core.model.Claim

private fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    onBack: () -> Unit,
    onClaimClick: (Claim) -> Unit,
    onChannelClick: (String, String) -> Unit = { _, _ -> },
    viewModel: PurchasesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Purchases") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            state.items.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(inner).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "You haven't purchased anything yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.padding(4.dp))
                Text(
                    text = "When you buy or rent content, it'll show up here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(inner),
            ) {
                items(state.items, key = { it.claimId }) { claim ->
                    val channelInitial = (claim.signingChannel?.name?.firstOrNull { it.isLetterOrDigit() }
                        ?: 'O').uppercaseChar()
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                        OdyseeClaimCard(
                            claim = com.odysee.app.core.designsystem.claims.OdyseeClaimCardModel(
                                claimId = claim.claimId,
                                title = claim.title,
                                channelName = claim.signingChannel?.name.orEmpty(),
                                channelClaimId = claim.signingChannel?.claimId,
                                channelAvatarUrl = claim.signingChannel?.thumbnailUrl,
                                channelInitial = channelInitial,
                                thumbnailUrl = claim.thumbnailUrl,
                                durationLabel = formatDuration(claim.durationSeconds),
                                ageLabel = "",
                                viewCount = null,
                                isLivestream = claim.isLivestream,
                                isUpcoming = claim.isUpcoming,
                                isShort = claim.isShort,
                            ),
                            onClick = { onClaimClick(claim) },
                            onChannelClick = {
                                val cid = claim.signingChannel?.claimId
                                val cname = claim.signingChannel?.name
                                if (!cid.isNullOrBlank() && !cname.isNullOrBlank()) onChannelClick(cid, cname)
                            },
                        )
                    }
                }
                if (state.isLoadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        state.error?.let { err ->
            Surface(
                color = Color(0x33E2202D),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = err,
                    color = Color(0xFFE2202D),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}
