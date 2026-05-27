package com.odysee.app.feature.channel.featuredchannels

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.odysee.app.core.data.featured.FeaturedChannelSection
import com.odysee.app.core.model.Channel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturedChannelsEditScreen(
    onBack: () -> Unit,
    viewModel: FeaturedChannelsEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    var renameTarget by remember { mutableStateOf<FeaturedChannelSection?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Featured channels") },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = !state.isSaving && !state.isLoading) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Save", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.sections, key = { it.id }) { section ->
                    FilterChip(
                        selected = state.activeSectionId == section.id,
                        onClick = { viewModel.setActiveSection(section.id) },
                        label = { Text(section.title.ifBlank { "Untitled" }) },
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = viewModel::addSection,
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New section")
                            }
                        },
                    )
                }
            }

            val active = state.sections.firstOrNull { it.id == state.activeSectionId }
            if (active != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = active.title,
                        onValueChange = { viewModel.renameSection(active.id, it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Section title") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { renameTarget = active }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete section",
                            tint = Color(0xFFE2202D),
                        )
                    }
                }

                Text(
                    text = "Channels in this section",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                if (active.uris.isEmpty()) {
                    Text(
                        text = "No channels yet. Search below to add one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        active.uris.forEach { uri ->
                            val claimId = uri.substringAfterLast('#').takeWhile { it.isLetterOrDigit() }
                            val ch = state.resolved[claimId]
                            ChannelListRow(
                                avatarUrl = ch?.thumbnailUrl,
                                title = ch?.title?.takeIf { it.isNotBlank() } ?: ch?.name ?: uri,
                                subtitle = ch?.name ?: "",
                                trailing = {
                                    IconButton(onClick = { viewModel.removeChannelFromActiveSection(uri) }) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Remove")
                                    }
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Add channels",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                TextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::search,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by name") },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 1,
                )
                if (state.isSearching) {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    items(state.searchResults, key = { it.claimId }) { ch ->
                        ChannelListRow(
                            avatarUrl = ch.thumbnailUrl,
                            title = ch.title?.takeIf { it.isNotBlank() } ?: ch.name,
                            subtitle = ch.name,
                            trailing = {
                                val active2 = state.sections.firstOrNull { it.id == state.activeSectionId }
                                val alreadyAdded = active2?.uris?.any { it.endsWith("#${ch.claimId}") } == true
                                if (alreadyAdded) {
                                    Text(
                                        "Added",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(end = 12.dp),
                                    )
                                } else {
                                    TextButton(onClick = { viewModel.addChannelToActiveSection(ch) }) {
                                        Text("Add")
                                    }
                                }
                            },
                        )
                    }
                }
            }

            state.error?.let { err ->
                Surface(
                    color = Color(0x33E2202D),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.clearError() },
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

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Delete section?") },
            text = { Text("Remove “${target.title}” and its channels?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSection(target.id)
                    renameTarget = null
                }) { Text("Delete", color = Color(0xFFE2202D)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ChannelListRow(
    avatarUrl: String?,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = (title.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        trailing()
    }
}
