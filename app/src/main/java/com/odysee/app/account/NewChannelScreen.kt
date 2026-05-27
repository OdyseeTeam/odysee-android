package com.odysee.app.account

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChannelScreen(
    onBack: () -> Unit,
    viewModel: NewChannelViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    LaunchedEffect(state.created) { if (state.created) onBack() }

    val thumbPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.pickThumbnail(uri)
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.pickCover(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("New channel") },
                actions = {
                    TextButton(onClick = viewModel::submit, enabled = state.canSubmit) {
                        if (state.isCreating) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Create", fontWeight = FontWeight.SemiBold)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 5f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { coverPicker.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (!state.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = state.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (state.isUploadingCover) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                else if (state.coverUrl.isNullOrBlank()) Text(
                    text = "Tap to add banner",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { thumbPicker.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!state.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = state.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (!state.isUploadingThumbnail) {
                        Text(
                            text = state.handle.firstOrNull()?.uppercase() ?: "@",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (state.isUploadingThumbnail) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (state.handle.isBlank()) "@your-handle" else "@${state.handle}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Tap avatar or banner to set",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FieldLabel("Channel handle *")
            FlatField(value = state.handle, onValueChange = viewModel::onHandle, placeholder = "your-handle")
            Text(
                text = "Letters, digits, dashes, or underscores. Used in @${state.handle.ifBlank { "your-handle" }} URLs.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FieldLabel("Display name")
            FlatField(value = state.title, onValueChange = viewModel::onTitle, placeholder = "Display name")

            FieldLabel("About")
            FlatField(
                value = state.description,
                onValueChange = viewModel::onDescription,
                placeholder = "Tell people about your channel",
                minLines = 4,
                maxLines = 8,
            )

            FieldLabel("Website")
            FlatField(value = state.websiteUrl, onValueChange = viewModel::onWebsite, placeholder = "https://")

            FieldLabel("Contact email")
            FlatField(value = state.email, onValueChange = viewModel::onEmail, placeholder = "you@example.com")

            FieldLabel("Tags")
            TagsEditor(state.tags, viewModel::addTag, viewModel::removeTag, "Add a tag")

            FieldLabel("Languages")
            TagsEditor(state.languages, viewModel::addLanguage, viewModel::removeLanguage, "en, es, …")

            FieldLabel("Initial deposit (LBC) *")
            FlatField(
                value = state.bidLbc,
                onValueChange = viewModel::onBid,
                placeholder = "0.01",
                keyboardType = KeyboardType.Decimal,
            )
            Text(
                text = "This LBC stays in the channel's name; you can remove it later.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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

            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Create channel", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun FlatField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
        ),
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(8.dp),
    )
}

@Composable
private fun TagsEditor(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    placeholder: String,
) {
    var draft by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.weight(1f),
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
            Button(
                onClick = {
                    if (draft.isNotBlank()) {
                        onAdd(draft.trim())
                        draft = ""
                    }
                },
                enabled = draft.isNotBlank(),
            ) { Text("Add") }
        }
        if (tags.isNotEmpty()) {
            TagWrap(tags, onRemove)
        }
    }
}

@Composable
private fun TagWrap(tags: List<String>, onRemove: (String) -> Unit) {
    val spacing = 6.dp
    androidx.compose.ui.layout.Layout(
        content = {
            tags.forEach { tag ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { onRemove(tag) }
                            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = tag,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val spacingPx = spacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        var current = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var w = 0
        placeables.forEach { p ->
            val n = w + (if (current.isEmpty()) 0 else spacingPx) + p.width
            if (n > maxWidth && current.isNotEmpty()) {
                rows.add(current); current = mutableListOf(p); w = p.width
            } else { current.add(p); w = n }
        }
        if (current.isNotEmpty()) rows.add(current)
        val height = rows.sumOf { it.maxOf { p -> p.height } } +
            (rows.size - 1).coerceAtLeast(0) * spacingPx
        layout(maxWidth, height) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                row.forEach { p ->
                    p.placeRelative(x, y)
                    x += p.width + spacingPx
                }
                y += row.maxOf { it.height } + spacingPx
            }
        }
    }
}
