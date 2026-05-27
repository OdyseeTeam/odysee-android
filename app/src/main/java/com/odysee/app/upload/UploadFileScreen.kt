package com.odysee.app.upload

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.graphics.asImageBitmap
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun UploadFileScreen(
    viewModel: UploadFileViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPublished: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val auth = LocalAuthState.current
    val channels = (auth as? AuthState.SignedIn)?.channels ?: emptyList()
    val activeChannel = (auth as? AuthState.SignedIn)?.activeChannel
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coScope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "video"
            val mime = context.contentResolver.getType(uri)
            viewModel.onPickFile(uri.toString(), name, mime)
        }
    }

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
                        Text("Upload", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stepLabel(state.step),
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
            UploadBottomBar(
                state = state,
                channels = channels,
                onSelectChannel = viewModel::onSelectChannel,
                onPrev = { viewModel.goToStep(prevStep(state.step)) },
                onNext = { viewModel.goToStep(nextStep(state.step)) },
                onPublish = {
                    coScope.launch {
                        val ok = viewModel.publish()
                        if (ok) onPublished()
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            StepIndicator(current = state.step)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            if (state.step == UploadStep.File) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FileStep(
                        state = state,
                        onPick = { picker.launch(arrayOf("*/*")) },
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f),
                    )
                    state.errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    when (state.step) {
                        UploadStep.Details -> DetailsStep(state = state, viewModel = viewModel)
                        UploadStep.Visibility -> VisibilityStep(state = state, viewModel = viewModel)
                        UploadStep.Publish -> PublishStep(state = state)
                        else -> Unit
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
}

private fun stepLabel(step: UploadStep): String = when (step) {
    UploadStep.File -> "Step 1 of 4 — Upload a file"
    UploadStep.Details -> "Step 2 of 4 — Details"
    UploadStep.Visibility -> "Step 3 of 4 — Visibility & price"
    UploadStep.Publish -> "Step 4 of 4 — Review & publish"
}

private fun prevStep(step: UploadStep): UploadStep = when (step) {
    UploadStep.File -> UploadStep.File
    UploadStep.Details -> UploadStep.File
    UploadStep.Visibility -> UploadStep.Details
    UploadStep.Publish -> UploadStep.Visibility
}

private fun nextStep(step: UploadStep): UploadStep = when (step) {
    UploadStep.File -> UploadStep.Details
    UploadStep.Details -> UploadStep.Visibility
    UploadStep.Visibility -> UploadStep.Publish
    UploadStep.Publish -> UploadStep.Publish
}

@Composable
private fun StepIndicator(current: UploadStep) {
    val steps = listOf(UploadStep.File, UploadStep.Details, UploadStep.Visibility, UploadStep.Publish)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = step == current
            val isDone = steps.indexOf(current) > index
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun FlatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helper: String? = null,
    prefix: String? = null,
    suffix: String? = null,
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
                {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            prefix = prefix?.let {
                {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            suffix = suffix?.let {
                {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
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
private fun FileStep(
    state: UploadFormState,
    onPick: () -> Unit,
    viewModel: UploadFileViewModel,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onPick)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.CloudUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            if (state.fileName.isNullOrBlank()) {
                Text(
                    text = "Tap to select a file",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Text(
                    text = state.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap to change",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailsStep(state: UploadFormState, viewModel: UploadFileViewModel) {
    SectionLabel("Title")
    FlatTextField(
        value = state.title,
        onValueChange = viewModel::onTitleChange,
        placeholder = "Descriptive titles work best",
        helper = "${state.title.length} / 200",
    )

    SectionLabel("URL")
    FlatTextField(
        value = state.urlSlug,
        onValueChange = viewModel::onUrlSlugChange,
        placeholder = "my-content",
        prefix = "odysee.com/",
        helper = "Permanent link to your content. Lowercase letters, numbers and dashes only.",
    )

    SectionLabel("Description")
    FlatTextField(
        value = state.description,
        onValueChange = viewModel::onDescriptionChange,
        modifier = Modifier.height(180.dp),
        placeholder = "Tell viewers about your content. Supports Markdown.",
        singleLine = false,
        minLines = 6,
    )

    SectionLabel("Thumbnail")
    ThumbnailSection(state = state, viewModel = viewModel)

    SectionLabel("Tags")
    TagsSection(state = state, viewModel = viewModel)

    SectionLabel("Language")
    LanguageDropdown(
        value = state.language,
        onChange = viewModel::onLanguageChange,
    )

    SectionLabel("License")
    LicenseDropdown(
        value = state.license,
        onChange = viewModel::onLicenseChange,
    )
    if (state.license == UploadLicense.Copyrighted) {
        SectionLabel("Copyright notice")
        FlatTextField(
            value = state.licenseDescription,
            onValueChange = viewModel::onLicenseDescriptionChange,
            placeholder = "© 2026 Your Name",
        )
    } else if (state.license == UploadLicense.Other) {
        SectionLabel("License description")
        FlatTextField(
            value = state.licenseDescription,
            onValueChange = viewModel::onLicenseDescriptionChange,
            placeholder = "The 'cool' license — TM",
        )
        SectionLabel("License URL")
        FlatTextField(
            value = state.licenseUrl,
            onValueChange = viewModel::onLicenseUrlChange,
            placeholder = "https://...",
        )
    }

}

@Composable
private fun VisibilityStep(state: UploadFormState, viewModel: UploadFileViewModel) {
    SectionLabel("Visibility")
    VisibilityOption(
        icon = Icons.Outlined.Public,
        title = "Public",
        description = "Content is visible to everyone.",
        selected = state.visibility == UploadVisibility.Public,
        onClick = { viewModel.onVisibilityChange(UploadVisibility.Public) },
    )
    VisibilityOption(
        icon = Icons.Outlined.VisibilityOff,
        title = "Unlisted",
        description = "The content cannot be viewed without a special link.",
        selected = state.visibility == UploadVisibility.Unlisted,
        onClick = { viewModel.onVisibilityChange(UploadVisibility.Unlisted) },
    )
    VisibilityOption(
        icon = Icons.Outlined.Schedule,
        title = "Scheduled",
        description = "Set a date to make the content public.",
        selected = state.visibility == UploadVisibility.Scheduled,
        onClick = { viewModel.onVisibilityChange(UploadVisibility.Scheduled) },
    )
    if (state.visibility == UploadVisibility.Scheduled) {
        ScheduledPicker(
            timestamp = state.scheduledTimestamp ?: (System.currentTimeMillis() + 30 * 60 * 1000L),
            showOnUpcoming = state.showOnUpcoming,
            onTimestampChange = viewModel::onScheduledTimestampChange,
            onShowOnUpcomingChange = viewModel::onShowOnUpcomingChange,
        )
    }
    VisibilityOption(
        icon = Icons.Outlined.Lock,
        title = "Private",
        description = "Only you can see this content.",
        selected = state.visibility == UploadVisibility.Private,
        onClick = { viewModel.onVisibilityChange(UploadVisibility.Private) },
    )

    Spacer(Modifier.height(8.dp))
    SectionLabel("Members only")
    InlineToggleRow(
        title = if (state.membersOnly) "Restricted to channel members" else "Open to everyone",
        description = "Limit access to paying members of your channel.",
        checked = state.membersOnly,
        onChange = viewModel::onMembersOnlyChange,
    )

    if (state.visibility != UploadVisibility.Public) {
        Spacer(Modifier.height(8.dp))
        SectionLabel("Price")
        Text(
            text = "Payment options are not available for unlisted, scheduled or private content.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Spacer(Modifier.height(8.dp))
        SectionLabel("Price")
        PaywallOption(
            title = "Free",
            description = "Anyone can view this content for free.",
            selected = state.paywall == UploadPaywall.Free,
            onClick = { viewModel.onPaywallChange(UploadPaywall.Free) },
        )
        PaywallOption(
            title = "Paid with Credits (LBC)",
            description = "Charge in LBC via the network.",
            selected = state.paywall == UploadPaywall.Lbc,
            onClick = { viewModel.onPaywallChange(UploadPaywall.Lbc) },
        )
        if (state.paywall == UploadPaywall.Lbc) {
            SectionLabel("Credits (LBC)")
            FlatTextField(
                value = state.lbcAmount,
                onValueChange = viewModel::onLbcAmountChange,
                suffix = "LBC",
            )
        }
        PaywallOption(
            title = "Paid content (Purchase / Rent)",
            description = "Sell access in USD via a one-off purchase or rental.",
            selected = state.paywall == UploadPaywall.Fiat,
            onClick = { viewModel.onPaywallChange(UploadPaywall.Fiat) },
        )
        if (state.paywall == UploadPaywall.Fiat) {
            InlineToggleRow(
                title = "Purchase",
                description = "One-time purchase grants permanent access.",
                checked = state.fiatPurchaseEnabled,
                onChange = viewModel::onFiatPurchaseEnabledChange,
            )
            if (state.fiatPurchaseEnabled) {
                SectionLabel("Purchase price")
                FlatTextField(
                    value = state.fiatPurchaseAmount,
                    onValueChange = viewModel::onFiatPurchaseAmountChange,
                    suffix = "USD",
                )
            }
            InlineToggleRow(
                title = "Rent",
                description = "Time-limited access that expires.",
                checked = state.fiatRentalEnabled,
                onChange = viewModel::onFiatRentalEnabledChange,
            )
            if (state.fiatRentalEnabled) {
                SectionLabel("Rental price")
                FlatTextField(
                    value = state.fiatRentalAmount,
                    onValueChange = viewModel::onFiatRentalAmountChange,
                    suffix = "USD",
                )
                SectionLabel("Rental duration")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        FlatTextField(
                            value = state.fiatRentalDuration,
                            onValueChange = viewModel::onFiatRentalDurationChange,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            ),
                        )
                    }
                    Box(modifier = Modifier.weight(1.2f)) {
                        RentalUnitDropdown(
                            value = state.fiatRentalDurationUnit,
                            onChange = viewModel::onFiatRentalDurationUnitChange,
                        )
                    }
                }
            }
            Text(
                text = "By continuing, you accept Odysee's paid-content terms and conditions.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InlineToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PaywallOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White),
            )
        }
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduledPicker(
    timestamp: Long,
    showOnUpcoming: Boolean,
    onTimestampChange: (Long) -> Unit,
    onShowOnUpcomingChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val fmt = remember {
        java.text.SimpleDateFormat("EEE d MMM yyyy, HH:mm", java.util.Locale.getDefault())
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 36.dp, top = 4.dp, bottom = 8.dp),
    ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RentalUnitDropdown(
    value: RentalDurationUnit,
    onChange: (RentalDurationUnit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(value.label.replaceFirstChar { it.uppercase() })
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            RentalDurationUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.label.replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onChange(unit)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PublishStep(state: UploadFormState) {
    SectionLabel("Review")
    SummaryRow("File", state.fileName ?: "—")
    SummaryRow("Title", state.title.ifBlank { "—" })
    SummaryRow("URL", "odysee.com/${state.urlSlug.ifBlank { "—" }}")
    SummaryRow(
        "Description",
        if (state.description.isBlank()) "—"
        else state.description.take(120) + if (state.description.length > 120) "…" else "",
    )
    SummaryRow("Thumbnail", state.thumbnailUrl.ifBlank { "—" })
    SummaryRow("Tags", if (state.tags.isEmpty()) "—" else state.tags.joinToString(", "))
    SummaryRow("Language", SupportedLanguages.list.firstOrNull { it.first == state.language }?.second ?: state.language)
    SummaryRow("License", state.license.display)
    SummaryRow(
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
    SummaryRow(
        "Price",
        when (state.paywall) {
            UploadPaywall.Free -> "Free"
            UploadPaywall.Lbc -> "${state.lbcAmount} LBC"
            UploadPaywall.Fiat -> buildString {
                if (state.fiatPurchaseEnabled) append("Purchase ${state.fiatPurchaseAmount} USD")
                if (state.fiatRentalEnabled) {
                    if (isNotEmpty()) append(" · ")
                    append("Rent ${state.fiatRentalAmount} USD / ${state.fiatRentalDuration} ${state.fiatRentalDurationUnit.label}")
                }
                if (isEmpty()) append("Paid (no options selected)")
            }
        },
    )
    if (state.membersOnly) SummaryRow("Members only", "Yes")
}

@Composable
private fun SummaryRow(label: String, value: String) {
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
private fun VisibilityOption(
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
private fun TagsSection(state: UploadFormState, viewModel: UploadFileViewModel) {
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
            WrapChips(spacing = 6.dp) {
                state.tags.forEach { tag ->
                    TagChip(
                        name = tag,
                        type = TagChipType.Remove,
                        onClick = { viewModel.removeTag(tag) },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FlatTextField(
            value = state.tagSearch,
            onValueChange = viewModel::onTagSearchChange,
            placeholder = "Search or add tags...",
            singleLine = true,
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
        val trimmed = state.tagSearch.trim().lowercase()
        val suggestions = remember(state.tags, trimmed) {
            POPULAR_TAGS
                .filter { !state.tags.contains(it) }
                .filter { if (trimmed.isBlank()) true else it.contains(trimmed) }
                .take(20)
        }
        Spacer(Modifier.height(8.dp))
        WrapChips(spacing = 6.dp) {
            if (trimmed.isNotEmpty() && !suggestions.contains(trimmed) && state.tags.size < 5) {
                TagChip(
                    name = trimmed,
                    type = TagChipType.Add,
                    onClick = { viewModel.addTag(trimmed) },
                )
            }
            suggestions.forEach { name ->
                TagChip(
                    name = name,
                    type = TagChipType.Add,
                    disabled = state.tags.size >= 5,
                    onClick = { viewModel.addTag(name) },
                )
            }
        }
    }
}

@Composable
private fun WrapChips(
    spacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.layout.Layout(content = content) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val spacingPx = spacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentWidth = 0
        placeables.forEach { p ->
            val nextWidth = currentWidth + (if (currentRow.isEmpty()) 0 else spacingPx) + p.width
            if (nextWidth > maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf(p)
                currentWidth = p.width
            } else {
                currentRow.add(p)
                currentWidth = nextWidth
            }
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)
        val height = rows.sumOf { row -> row.maxOf { it.height } } +
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

private enum class TagChipType { Add, Remove }

@Composable
private fun TagChip(
    name: String,
    type: TagChipType,
    disabled: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = when {
        type == TagChipType.Remove -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        type == TagChipType.Remove -> Color.White
        else -> MaterialTheme.colorScheme.onBackground
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg.copy(alpha = if (disabled) 0.5f else 1f))
            .clickable(enabled = !disabled, onClick = onClick)
            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#$name",
            style = MaterialTheme.typography.labelMedium,
            color = fg.copy(alpha = if (disabled) 0.5f else 1f),
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = if (type == TagChipType.Remove) Icons.Outlined.Close
            else Icons.Filled.Add,
            contentDescription = null,
            tint = fg.copy(alpha = if (disabled) 0.5f else 1f),
            modifier = Modifier.size(14.dp),
        )
    }
}

private val POPULAR_TAGS = listOf(
    "art", "automotive", "blockchain", "comedy", "cooking", "crypto", "diy",
    "education", "finance", "fitness", "gaming", "history", "kids", "lifestyle",
    "movie", "music", "nature", "news", "podcast", "politics", "reaction",
    "review", "science", "spirituality", "sports", "tech", "travel", "tutorial",
    "vlog", "weapons",
)

@Composable
private fun ThumbnailSection(state: UploadFormState, viewModel: UploadFileViewModel) {
    val context = LocalContext.current
    val isVideo = state.fileMime?.startsWith("video/") == true
    val isImage = state.fileMime?.startsWith("image/") == true
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.uploadThumbnail(uri.toString())
    }
    var frames by remember(state.fileUri, isVideo) {
        mutableStateOf<List<FrameOption>>(emptyList())
    }
    var framesLoading by remember(state.fileUri, isVideo) { mutableStateOf(isVideo) }
    val fileUri = state.fileUri
    if (isVideo && fileUri != null) {
        LaunchedEffect(fileUri) {
            framesLoading = true
            val parsed = runCatching { android.net.Uri.parse(fileUri) }.getOrNull()
            if (parsed != null) {
                val extracted = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    extractFrameOptions(context, parsed)
                }
                frames = extracted
            }
            framesLoading = false
        }
    }

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

        val tiles = mutableListOf<ThumbnailTile>()
        if (isImage && fileUri != null) {
            tiles += ThumbnailTile.UseUploadedImage(fileUri)
        }
        if (isVideo) {
            if (framesLoading) {
                tiles += ThumbnailTile.LoadingFrames
            } else {
                frames.forEach { f -> tiles += ThumbnailTile.Frame(f) }
            }
        }
        tiles += ThumbnailTile.PickImage
        tiles += ThumbnailTile.PasteUrl

        SectionLabel("Suggested thumbnails")
        ThumbnailTileGrid(
            tiles = tiles,
            disabled = state.isUploadingThumbnail,
            onPick = { picked ->
                when (picked) {
                    is ThumbnailTile.UseUploadedImage -> viewModel.uploadThumbnail(picked.uri)
                    is ThumbnailTile.Frame -> viewModel.uploadThumbnailBytes(
                        picked.frame.jpegBytes,
                        "frame-${picked.frame.timestampMs}.jpg",
                    )
                    ThumbnailTile.PickImage -> imagePicker.launch(arrayOf("image/*"))
                    ThumbnailTile.PasteUrl -> Unit
                    ThumbnailTile.LoadingFrames -> Unit
                }
            },
        )

        SectionLabel("Or paste an image URL")
        FlatTextField(
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

private sealed interface ThumbnailTile {
    data class UseUploadedImage(val uri: String) : ThumbnailTile
    data class Frame(val frame: FrameOption) : ThumbnailTile
    data object PickImage : ThumbnailTile
    data object PasteUrl : ThumbnailTile
    data object LoadingFrames : ThumbnailTile
}

private data class FrameOption(
    val bitmap: android.graphics.Bitmap,
    val jpegBytes: ByteArray,
    val timestampMs: Long,
    val label: String,
)

@Composable
private fun ThumbnailTileGrid(
    tiles: List<ThumbnailTile>,
    disabled: Boolean,
    onPick: (ThumbnailTile) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tiles.size) { idx ->
            val tile = tiles[idx]
            ThumbnailTileCard(tile = tile, disabled = disabled, onClick = { onPick(tile) })
        }
    }
}

@Composable
private fun ThumbnailTileCard(
    tile: ThumbnailTile,
    disabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !disabled && tile !is ThumbnailTile.LoadingFrames, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (tile) {
            is ThumbnailTile.UseUploadedImage -> {
                AsyncImage(
                    model = tile.uri,
                    contentDescription = "Uploaded image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
                TileBadge(text = "Image")
            }
            is ThumbnailTile.Frame -> {
                androidx.compose.foundation.Image(
                    bitmap = tile.frame.bitmap.asImageBitmap(),
                    contentDescription = "Frame at ${tile.frame.label}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
                TileBadge(text = tile.frame.label)
            }
            ThumbnailTile.LoadingFrames -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            ThumbnailTile.PickImage -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Upload",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ThumbnailTile.PasteUrl -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "URL",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.TileBadge(text: String) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

private fun extractFrameOptions(
    context: android.content.Context,
    uri: android.net.Uri,
): List<FrameOption> {
    val retriever = android.media.MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: return emptyList()
        if (durationMs <= 0) return emptyList()
        val percentages = listOf(0.10, 0.25, 0.50, 0.75, 0.90)
        percentages.mapNotNull { pct ->
            val tsMs = (durationMs * pct).toLong()
            val bitmap = retriever.getFrameAtTime(
                tsMs * 1000L,
                android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            ) ?: return@mapNotNull null
            val out = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            FrameOption(
                bitmap = bitmap,
                jpegBytes = out.toByteArray(),
                timestampMs = tsMs,
                label = formatMs(tsMs),
            )
        }
    } catch (t: Throwable) {
        emptyList()
    } finally {
        runCatching { retriever.release() }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(value: String, onChange: (String) -> Unit) {
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
private fun LicenseDropdown(value: UploadLicense, onChange: (UploadLicense) -> Unit) {
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
private fun UploadBottomBar(
    state: UploadFormState,
    channels: List<Channel>,
    onSelectChannel: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPublish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        if (state.step == UploadStep.Publish) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChannelChip(
                    channels = channels,
                    selectedClaimId = state.channelClaimId,
                    onSelect = onSelectChannel,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.step != UploadStep.File) {
                OutlinedButton(
                    onClick = onPrev,
                    modifier = Modifier.weight(1f),
                ) { Text("Back") }
            }
            if (state.step != UploadStep.Publish) {
                val enabled = when (state.step) {
                    UploadStep.File -> state.isFileStepValid
                    UploadStep.Details -> state.isDetailsStepValid
                    UploadStep.Visibility -> state.isVisibilityStepValid
                    else -> false
                }
                Button(
                    onClick = onNext,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { Text("Next", color = Color.White, fontWeight = FontWeight.SemiBold) }
            } else {
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
                        Text("Publishing…", color = Color.White, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("Publish", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelChip(
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
