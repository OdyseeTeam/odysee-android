package com.odysee.app.feature.channel.moderation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odysee.app.core.data.moderation.BlockScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationScreen(
    onBack: () -> Unit,
    viewModel: ModerationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    var selected by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Blocked viewers", "Moderators")

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Moderation") },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            TabRow(
                selectedTabIndex = selected,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[selected]),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                divider = {},
            ) {
                tabs.forEachIndexed { i, label ->
                    Tab(
                        selected = selected == i,
                        onClick = { selected = i },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (selected == i) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }
            state.error?.let { err ->
                Surface(
                    color = Color(0x33E2202D),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { viewModel.clearError() },
                ) {
                    Text(
                        text = err,
                        color = Color(0xFFE2202D),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            when (selected) {
                0 -> BlockedList(state, onUnblock = viewModel::unblock)
                else -> ModeratorList(state, onRemove = viewModel::removeModerator)
            }
        }
    }
}

@Composable
private fun BlockedList(
    state: ModerationState,
    onUnblock: (com.odysee.app.core.data.moderation.BlockedCommenter) -> Unit,
) {
    if (state.blockedLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    if (state.blocked.isEmpty()) {
        EmptyView("No blocked viewers.")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.blocked, key = { it.claimId + "_" + it.scope.name }) { c ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarPlaceholder(initial = (c.name.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar())
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = c.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    val scopeLabel = when (c.scope) {
                        BlockScope.Channel -> "Blocked on your channel"
                        BlockScope.Global -> "Blocked across all your channels"
                        BlockScope.Delegated -> "Blocked by a delegated moderator"
                    }
                    Text(
                        text = scopeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { onUnblock(c) },
                    enabled = state.mutatingId != c.claimId,
                ) {
                    if (state.mutatingId == c.claimId) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Unblock")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeratorList(
    state: ModerationState,
    onRemove: (com.odysee.app.core.data.moderation.CommentModerator) -> Unit,
) {
    if (state.moderatorsLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    if (state.moderators.isEmpty()) {
        EmptyView("You haven't delegated any moderators yet.")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.moderators, key = { it.claimId }) { m ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarPlaceholder(initial = (m.name.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar())
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = m.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Delegated moderator",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { onRemove(m) },
                    enabled = state.mutatingId != m.claimId,
                ) {
                    if (state.mutatingId == m.claimId) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyView(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AvatarPlaceholder(initial: Char) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial.toString(),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
