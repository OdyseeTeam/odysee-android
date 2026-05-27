package com.odysee.app.upload

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.odysee.app.auth.LocalAuthState
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.model.Channel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivestreamFormScreen(
    viewModel: LivestreamFormViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPublished: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val auth = LocalAuthState.current
    val channels = (auth as? AuthState.SignedIn)?.channels ?: emptyList()
    val activeChannel = (auth as? AuthState.SignedIn)?.activeChannel
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coScope = rememberCoroutineScope()

    LaunchedEffect(activeChannel) {
        if (state.channelClaimId == null && activeChannel != null) {
            viewModel.onSelectChannel(activeChannel.claimId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("New livestream", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = liveStepLabel(state.step),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            LiveBottomBar(
                state = state,
                channels = channels,
                onSelectChannel = viewModel::onSelectChannel,
                onPrev = { viewModel.goToStep(prevLiveStep(state.step)) },
                onNext = { viewModel.goToStep(nextLiveStep(state.step)) },
                onPublish = {
                    coScope.launch { viewModel.publish() }
                },
                onDone = onPublished,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LiveStepIndicator(current = state.step)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (state.step) {
                    LivestreamStep.Details -> LiveDetailsStep(state = state, viewModel = viewModel)
                    LivestreamStep.Visibility -> LiveVisibilityStep(state = state, viewModel = viewModel)
                    LivestreamStep.Publish -> LivePublishStep(state = state)
                    LivestreamStep.Ready -> LiveReadyStep(state = state)
                }
                state.errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun liveStepLabel(step: LivestreamStep): String = when (step) {
    LivestreamStep.Details -> "Step 1 of 3 — Details"
    LivestreamStep.Visibility -> "Step 2 of 3 — Visibility"
    LivestreamStep.Publish -> "Step 3 of 3 — Review & create"
    LivestreamStep.Ready -> "You're live!"
}

private fun prevLiveStep(step: LivestreamStep): LivestreamStep = when (step) {
    LivestreamStep.Details -> LivestreamStep.Details
    LivestreamStep.Visibility -> LivestreamStep.Details
    LivestreamStep.Publish -> LivestreamStep.Visibility
    LivestreamStep.Ready -> LivestreamStep.Ready
}

private fun nextLiveStep(step: LivestreamStep): LivestreamStep = when (step) {
    LivestreamStep.Details -> LivestreamStep.Visibility
    LivestreamStep.Visibility -> LivestreamStep.Publish
    LivestreamStep.Publish -> LivestreamStep.Publish
    LivestreamStep.Ready -> LivestreamStep.Ready
}

@Composable
private fun LiveStepIndicator(current: LivestreamStep) {
    val steps = listOf(LivestreamStep.Details, LivestreamStep.Visibility, LivestreamStep.Publish)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = step == current
            val isDone = current == LivestreamStep.Ready || steps.indexOf(current) > index
            val color = if (isActive) MaterialTheme.colorScheme.primary
            else if (isDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun LSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun LFlatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helper: String? = null,
    prefix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        androidx.compose.material3.TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(10.dp),
            placeholder = placeholder?.let {
                { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
            },
            prefix = prefix?.let {
                { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
            },
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
        if (helper != null) {
            Text(
                text = helper,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        }
    }
}

@Composable
private fun LiveDetailsStep(state: LivestreamFormState, viewModel: LivestreamFormViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Videocam,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Create a livestream",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "After publishing, point OBS at Odysee's RTMP server with your stream key.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    LSectionLabel("Title")
    LFlatTextField(
        value = state.title,
        onValueChange = viewModel::onTitleChange,
        placeholder = "What's the stream about?",
        helper = "${state.title.length} / 200",
    )

    LSectionLabel("URL")
    LFlatTextField(
        value = state.urlSlug,
        onValueChange = viewModel::onUrlSlugChange,
        placeholder = "my-stream",
        prefix = "odysee.com/",
        helper = "Permanent link. Lowercase letters, numbers and dashes only.",
    )

    LSectionLabel("Description")
    LFlatTextField(
        value = state.description,
        onValueChange = viewModel::onDescriptionChange,
        modifier = Modifier.height(140.dp),
        placeholder = "Tell viewers what to expect. Markdown supported.",
        singleLine = false,
        minLines = 5,
    )

    LSectionLabel("Thumbnail")
    LiveThumbnailSection(state = state, viewModel = viewModel)

    LSectionLabel("Tags")
    LiveTagsSection(state = state, viewModel = viewModel)

    LSectionLabel("Language")
    LiveLanguageDropdown(value = state.language, onChange = viewModel::onLanguageChange)

    LSectionLabel("License")
    LiveLicenseDropdown(value = state.license, onChange = viewModel::onLicenseChange)
    if (state.license == UploadLicense.Copyrighted) {
        LSectionLabel("Copyright notice")
        LFlatTextField(
            value = state.licenseDescription,
            onValueChange = viewModel::onLicenseDescriptionChange,
            placeholder = "© 2026 Your Name",
        )
    } else if (state.license == UploadLicense.Other) {
        LSectionLabel("License description")
        LFlatTextField(
            value = state.licenseDescription,
            onValueChange = viewModel::onLicenseDescriptionChange,
            placeholder = "The 'cool' license — TM",
        )
        LSectionLabel("License URL")
        LFlatTextField(
            value = state.licenseUrl,
            onValueChange = viewModel::onLicenseUrlChange,
            placeholder = "https://...",
        )
    }
}

@Composable
private fun LiveVisibilityStep(state: LivestreamFormState, viewModel: LivestreamFormViewModel) {
    LSectionLabel("Visibility")
    LiveVisibilityOption(
        icon = Icons.Outlined.Public,
        title = "Public",
        description = "Anyone can find and watch your stream.",
        selected = state.visibility == UploadVisibility.Public,
        onClick = { viewModel.onVisibilityChange(UploadVisibility.Public) },
    )
    LiveVisibilityOption(
        icon = Icons.Outlined.VisibilityOff,
        title = "Unlisted",
        description = "Only people with the link can watch.",
        selected = state.visibility == UploadVisibility.Unlisted,
        onClick = { viewModel.onVisibilityChange(UploadVisibility.Unlisted) },
    )
    LiveVisibilityOption(
        icon = Icons.Outlined.Schedule,
        title = "Scheduled",
        description = "Show as upcoming until you go live.",
        selected = state.visibility == UploadVisibility.Scheduled,
        onClick = { viewModel.onVisibilityChange(UploadVisibility.Scheduled) },
    )
    if (state.visibility == UploadVisibility.Scheduled) {
        LiveScheduledPicker(
            timestamp = state.scheduledTimestamp ?: (System.currentTimeMillis() + 30 * 60 * 1000L),
            showOnUpcoming = state.showOnUpcoming,
            onTimestampChange = viewModel::onScheduledTimestampChange,
            onShowOnUpcomingChange = viewModel::onShowOnUpcomingChange,
        )
    }
    LiveVisibilityOption(
        icon = Icons.Outlined.Lock,
        title = "Private",
        description = "Only you can watch this stream.",
        selected = state.visibility == UploadVisibility.Private,
        onClick = { viewModel.onVisibilityChange(UploadVisibility.Private) },
    )

    Spacer(Modifier.height(8.dp))
    LSectionLabel("Members only")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.onMembersOnlyChange(!state.membersOnly) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (state.membersOnly) "Restricted to channel members" else "Open to everyone",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Limit access to paying members of your channel.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = state.membersOnly, onCheckedChange = viewModel::onMembersOnlyChange)
    }
}

@Composable
private fun LivePublishStep(state: LivestreamFormState) {
    LSectionLabel("Review")
    LSummaryRow("Title", state.title.ifBlank { "—" })
    LSummaryRow("URL", "odysee.com/${state.urlSlug.ifBlank { "—" }}")
    LSummaryRow(
        "Description",
        if (state.description.isBlank()) "—"
        else state.description.take(120).let { if (state.description.length > 120) "$it…" else it },
    )
    LSummaryRow("Thumbnail", state.thumbnailUrl.ifBlank { "—" })
    LSummaryRow("Tags", if (state.tags.isEmpty()) "—" else state.tags.joinToString(", "))
    LSummaryRow(
        "Language",
        SupportedLanguages.list.firstOrNull { it.first == state.language }?.second ?: state.language,
    )
    LSummaryRow("License", state.license.display)
    LSummaryRow(
        "Visibility",
        when (state.visibility) {
            UploadVisibility.Public -> "Public"
            UploadVisibility.Unlisted -> "Unlisted"
            UploadVisibility.Scheduled -> {
                val fmt = java.text.SimpleDateFormat("EEE d MMM yyyy, HH:mm", java.util.Locale.getDefault())
                val ts = state.scheduledTimestamp ?: 0L
                "Scheduled — ${fmt.format(java.util.Date(ts))}"
            }
            UploadVisibility.Private -> "Private"
        },
    )
    if (state.membersOnly) LSummaryRow("Members only", "Yes")
}

@Composable
private fun LiveReadyStep(state: LivestreamFormState) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = "Stream created",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Open OBS (or your encoder) and use the values below to start streaming. Your stream will appear at odysee.com/${state.urlSlug}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    LSectionLabel("RTMP server")
    CopyRow(value = state.rtmpUrl) {
        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("RTMP server", state.rtmpUrl))
    }
    LSectionLabel("Stream key")
    val key = state.streamKey ?: "—"
    CopyRow(value = key, masked = true) {
        if (state.streamKey != null) {
            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Stream key", state.streamKey))
        }
    }
    Text(
        text = "Treat the stream key like a password. Anyone with it can stream to your channel.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CopyRow(value: String, masked: Boolean = false, onCopy: () -> Unit) {
    var revealed by remember { mutableStateOf(!masked) }
    val display = if (masked && !revealed) "•".repeat(value.length.coerceAtMost(32)) else value
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = display,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (masked) {
            IconButton(onClick = { revealed = !revealed }) {
                Icon(
                    imageVector = if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Public,
                    contentDescription = if (revealed) "Hide" else "Show",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun LSummaryRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun LiveVisibilityOption(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = borderColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(borderColor),
            )
        }
    }
}

@Composable
private fun LiveScheduledPicker(
    timestamp: Long,
    showOnUpcoming: Boolean,
    onTimestampChange: (Long) -> Unit,
    onShowOnUpcomingChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val fmt = remember {
        java.text.SimpleDateFormat("EEE d MMM yyyy, HH:mm", java.util.Locale.getDefault())
    }
    Column(modifier = Modifier.fillMaxWidth().padding(start = 36.dp, top = 4.dp, bottom = 8.dp)) {
        OutlinedButton(
            onClick = {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                android.app.DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val newCal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                        newCal.set(java.util.Calendar.YEAR, y)
                        newCal.set(java.util.Calendar.MONTH, m)
                        newCal.set(java.util.Calendar.DAY_OF_MONTH, d)
                        android.app.TimePickerDialog(
                            context,
                            { _, hh, mm ->
                                newCal.set(java.util.Calendar.HOUR_OF_DAY, hh)
                                newCal.set(java.util.Calendar.MINUTE, mm)
                                onTimestampChange(newCal.timeInMillis)
                            },
                            newCal.get(java.util.Calendar.HOUR_OF_DAY),
                            newCal.get(java.util.Calendar.MINUTE),
                            true,
                        ).show()
                    },
                    cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.MONTH),
                    cal.get(java.util.Calendar.DAY_OF_MONTH),
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(fmt.format(java.util.Date(timestamp)))
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowOnUpcomingChange(!showOnUpcoming) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Checkbox(
                checked = showOnUpcoming,
                onCheckedChange = onShowOnUpcomingChange,
            )
            Text(
                text = "Show this on my channel's Upcoming section",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun LiveTagsSection(state: LivestreamFormState, viewModel: LivestreamFormViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Selected (${state.tags.size}/5)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        if (state.tags.isEmpty()) {
            Text(
                text = "No tags added",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        } else {
            LiveWrapChips {
                state.tags.forEach { tag ->
                    LiveTagChip(name = tag, remove = true) { viewModel.removeTag(tag) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        LFlatTextField(
            value = state.tagSearch,
            onValueChange = viewModel::onTagSearchChange,
            placeholder = "Add a tag…",
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    val v = state.tagSearch.trim()
                    if (v.isNotBlank()) viewModel.addTag(v)
                },
            ),
        )
    }
}

@Composable
private fun LiveWrapChips(content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content = content) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val spacingPx = 6.dp.roundToPx()
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

@Composable
private fun LiveTagChip(name: String, remove: Boolean, onClick: () -> Unit) {
    val bg = if (remove) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (remove) Color.White else MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "#$name", style = MaterialTheme.typography.labelMedium, color = fg)
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = if (remove) Icons.Outlined.Close else Icons.Filled.Add,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun LiveThumbnailSection(state: LivestreamFormState, viewModel: LivestreamFormViewModel) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.uploadThumbnail(uri.toString()) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.thumbnailUrl.isNotBlank() || state.isUploadingThumbnail) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (state.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = state.thumbnailUrl,
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                }
                if (state.isUploadingThumbnail) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = Color.White) }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = !state.isUploadingThumbnail) {
                    imagePicker.launch(arrayOf("image/*"))
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Upload thumbnail",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LSectionLabel("Or paste an image URL")
        LFlatTextField(
            value = state.thumbnailUrl,
            onValueChange = viewModel::onThumbnailChange,
            placeholder = "https://...",
        )
        state.thumbnailError?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveLanguageDropdown(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val labelText = SupportedLanguages.list.firstOrNull { it.first == value }?.second ?: value
    Box {
        OutlinedTextField(
            value = labelText,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = true },
            readOnly = true,
            enabled = false,
            trailingIcon = { Icon(Icons.Outlined.ExpandMore, contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SupportedLanguages.list.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onChange(code)
                        open = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveLicenseDropdown(value: UploadLicense, onChange: (UploadLicense) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value.display,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = true },
            readOnly = true,
            enabled = false,
            trailingIcon = { Icon(Icons.Outlined.ExpandMore, contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            UploadLicense.entries.forEach { lic ->
                DropdownMenuItem(
                    text = { Text(lic.display) },
                    onClick = {
                        onChange(lic)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun LiveBottomBar(
    state: LivestreamFormState,
    channels: List<Channel>,
    onSelectChannel: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPublish: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        if (state.step == LivestreamStep.Publish) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveChannelChip(
                    channels = channels,
                    selectedClaimId = state.channelClaimId,
                    onSelect = onSelectChannel,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state.step) {
                LivestreamStep.Details -> {
                    Button(
                        onClick = onNext,
                        enabled = state.isDetailsStepValid,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) { Text("Next", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
                LivestreamStep.Visibility -> {
                    OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text("Back") }
                    Button(
                        onClick = onNext,
                        enabled = state.isVisibilityStepValid,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) { Text("Next", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
                LivestreamStep.Publish -> {
                    OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text("Back") }
                    Button(
                        onClick = onPublish,
                        enabled = state.canPublish && !state.isPublishing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        if (state.isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Creating…", color = Color.White, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Create", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                LivestreamStep.Ready -> {
                    Button(
                        onClick = onDone,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) { Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveChannelChip(
    channels: List<Channel>,
    selectedClaimId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val selected = channels.firstOrNull { it.claimId == selectedClaimId }
    val name = selected?.title?.takeIf { it.isNotBlank() } ?: selected?.name ?: "Select channel"
    val avatar = selected?.thumbnailUrl
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { open = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (!avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = avatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = (selected?.name?.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: '?').toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            channels.forEach { ch ->
                DropdownMenuItem(
                    text = { Text(ch.title?.takeIf { it.isNotBlank() } ?: ch.name) },
                    onClick = {
                        onSelect(ch.claimId)
                        open = false
                    },
                )
            }
        }
    }
}
