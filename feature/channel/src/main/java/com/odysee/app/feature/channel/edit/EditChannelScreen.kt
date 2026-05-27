package com.odysee.app.feature.channel.edit

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChannelScreen(
    onBack: () -> Unit,
    viewModel: EditChannelViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)

    LaunchedEffect(state.savedTxId) {
        if (state.savedTxId != null) onBack()
    }

    val thumbnailPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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
                title = { Text("Edit channel") },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = !state.isSaving && !state.isLoading,
                    ) {
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Banner with camera-icon overlay (web parity)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2048f / 320f)
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
                if (state.isUploadingCover) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC000000))
                        .clickable { coverPicker.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "Edit cover",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(80.dp)) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { thumbnailPicker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!state.thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = state.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (state.isUploadingThumbnail) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { thumbnailPicker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "Edit avatar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.handle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Tap the camera icons to replace",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FieldLabel("Title")
            FlatField(value = state.title, onValueChange = viewModel::onTitle, placeholder = "Display name")

            FieldLabel("Description")
            FlatField(
                value = state.description,
                onValueChange = viewModel::onDescription,
                placeholder = "Tell people about your channel",
                minLines = 4,
                maxLines = 8,
            )

            FieldLabel("Website")
            FlatField(value = state.websiteUrl, onValueChange = viewModel::onWebsite, placeholder = "https://")

            FieldLabel("Email")
            FlatField(value = state.email, onValueChange = viewModel::onEmail, placeholder = "you@example.com")

            FieldLabel("Tags")
            TagsEditor(
                tags = state.tags,
                onAdd = viewModel::addTag,
                onRemove = viewModel::removeTag,
                placeholder = "Add a tag",
            )

            FieldLabel("Languages")
            TagsEditor(
                tags = state.languages,
                onAdd = viewModel::addLanguage,
                onRemove = viewModel::removeLanguage,
                placeholder = "en, es, …",
            )

            state.pendingCrop?.let { pending ->
                ImageCropDialog(
                    uri = pending.uri,
                    aspectRatio = if (pending.isCover) 2048f / 320f else 1f,
                    circular = !pending.isCover,
                    onCancel = viewModel::cancelCrop,
                    onConfirm = { bytes -> viewModel.applyCroppedImage(bytes, pending.isCover) },
                )
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
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
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
            TagWrap(
                tags = tags,
                onRemove = onRemove,
            )
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
