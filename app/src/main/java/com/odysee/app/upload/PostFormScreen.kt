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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Article
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
fun PostFormScreen(
    viewModel: PostFormViewModel = hiltViewModel(),
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
                        Text("New post", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = postStepLabel(state.step),
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
            PostBottomBar(
                state = state,
                channels = channels,
                onSelectChannel = viewModel::onSelectChannel,
                onPrev = { viewModel.goToStep(prevPostStep(state.step)) },
                onNext = { viewModel.goToStep(nextPostStep(state.step)) },
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
            PostStepIndicator(current = state.step)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (state.step) {
                    PostStep.Post -> PostPostStep(state = state, viewModel = viewModel)
                    PostStep.Content -> PostContentStep(state = state, viewModel = viewModel)
                    PostStep.Visibility -> PostVisibilityStep(state = state, viewModel = viewModel)
                    PostStep.Publish -> PostPublishStep(state = state)
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

private fun postStepLabel(step: PostStep): String = when (step) {
    PostStep.Post -> "Step 1 of 4 — Post"
    PostStep.Content -> "Step 2 of 4 — Content"
    PostStep.Visibility -> "Step 3 of 4 — Visibility & price"
    PostStep.Publish -> "Step 4 of 4 — Review & publish"
}

private fun prevPostStep(step: PostStep): PostStep = when (step) {
    PostStep.Post -> PostStep.Post
    PostStep.Content -> PostStep.Post
    PostStep.Visibility -> PostStep.Content
    PostStep.Publish -> PostStep.Visibility
}

private fun nextPostStep(step: PostStep): PostStep = when (step) {
    PostStep.Post -> PostStep.Content
    PostStep.Content -> PostStep.Visibility
    PostStep.Visibility -> PostStep.Publish
    PostStep.Publish -> PostStep.Publish
}

@Composable
private fun PostStepIndicator(current: PostStep) {
    val steps = listOf(PostStep.Post, PostStep.Content, PostStep.Visibility, PostStep.Publish)
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
private fun PSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun PFlatTextField(
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
private fun PostPostStep(state: PostFormState, viewModel: PostFormViewModel) {
    PSectionLabel("Title")
    PFlatTextField(
        value = state.title,
        onValueChange = viewModel::onTitleChange,
        placeholder = "Descriptive titles work best",
        helper = "${state.title.length} / 200",
    )

    PSectionLabel("URL")
    PFlatTextField(
        value = state.urlSlug,
        onValueChange = viewModel::onUrlSlugChange,
        placeholder = "my-post",
        prefix = "odysee.com/",
        helper = "Permanent link. Lowercase letters, numbers and dashes only.",
    )

    PSectionLabel("Post")
    MarkdownEditor(
        value = state.body,
        onValueChange = viewModel::onBodyChange,
        placeholder = "What do you want to say? Markdown supported.",
        helper = "${state.body.length} characters",
        minLines = 10,
    )
}

@Composable
private fun PostContentStep(state: PostFormState, viewModel: PostFormViewModel) {
    PSectionLabel("Thumbnail")
    PostThumbnailSection(state = state, viewModel = viewModel)

    PSectionLabel("Tags")
    PostTagsSection(state = state, viewModel = viewModel)

    PSectionLabel("Language")
    PostLanguageDropdown(value = state.language, onChange = viewModel::onLanguageChange)

    PSectionLabel("License")
    PostLicenseDropdown(value = state.license, onChange = viewModel::onLicenseChange)
    if (state.license == UploadLicense.Copyrighted) {
        PSectionLabel("Copyright notice")
        PFlatTextField(
            value = state.licenseDescription,
            onValueChange = viewModel::onLicenseDescriptionChange,
            placeholder = "© 2026 Your Name",
        )
    } else if (state.license == UploadLicense.Other) {
        PSectionLabel("License description")
        PFlatTextField(
            value = state.licenseDescription,
            onValueChange = viewModel::onLicenseDescriptionChange,
            placeholder = "The 'cool' license — TM",
        )
        PSectionLabel("License URL")
        PFlatTextField(
            value = state.licenseUrl,
            onValueChange = viewModel::onLicenseUrlChange,
            placeholder = "https://...",
        )
    }
}

@Composable
private fun MarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    helper: String,
    minLines: Int,
) {
    var preview by remember { mutableStateOf(false) }
    var tfv by remember(value) {
        // Re-sync to incoming value if it changes externally (e.g., draft restore).
        // Cursor goes to end of new content.
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = value,
                selection = androidx.compose.ui.text.TextRange(value.length),
            ),
        )
    }
    fun apply(transform: (text: String, start: Int, end: Int) -> Pair<String, androidx.compose.ui.text.TextRange>) {
        val sel = tfv.selection
        val (newText, newRange) = transform(tfv.text, sel.start.coerceAtMost(tfv.text.length), sel.end.coerceAtMost(tfv.text.length))
        tfv = androidx.compose.ui.text.input.TextFieldValue(text = newText, selection = newRange)
        onValueChange(newText)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MdButton(label = "B", aria = "Bold", bold = true, enabled = !preview) {
                apply { t, s, e -> wrapSelection(t, s, e, "**", "**", "bold text") }
            }
            MdButton(label = "I", aria = "Italic", italic = true, enabled = !preview) {
                apply { t, s, e -> wrapSelection(t, s, e, "*", "*", "italic text") }
            }
            MdButton(label = "S", aria = "Strikethrough", strikethrough = true, enabled = !preview) {
                apply { t, s, e -> wrapSelection(t, s, e, "~~", "~~", "struck text") }
            }
            MdButton(label = "\"", aria = "Quote", enabled = !preview) {
                apply { t, s, e -> prefixSelectedLines(t, s, e) { "> " } }
            }
            MdButton(label = "UL", aria = "Bulleted list", enabled = !preview) {
                apply { t, s, e -> prefixSelectedLines(t, s, e) { "- " } }
            }
            MdButton(label = "OL", aria = "Numbered list", enabled = !preview) {
                apply { t, s, e -> prefixSelectedLines(t, s, e) { idx -> "${idx + 1}. " } }
            }
            MdButton(label = "Link", aria = "Link", enabled = !preview) {
                apply { t, s, e -> insertLink(t, s, e) }
            }
            MdButton(label = "</>", aria = "Code block", enabled = !preview) {
                apply { t, s, e -> wrapSelection(t, s, e, "```\n", "\n```", "code") }
            }
            Spacer(Modifier.weight(1f))
            MdButton(label = if (preview) "Edit" else "Preview", aria = "Toggle preview", enabled = true) {
                preview = !preview
            }
        }
        if (preview) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = (minLines * 22).dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            ) {
                if (tfv.text.isBlank()) {
                    Text(
                        text = "Nothing to preview yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        text = tfv.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        } else {
            androidx.compose.material3.TextField(
                value = tfv,
                onValueChange = {
                    tfv = it
                    onValueChange(it.text)
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = (minLines * 22).dp),
                singleLine = false,
                minLines = minLines,
                shape = RoundedCornerShape(10.dp),
                placeholder = {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
            )
        }
        Text(
            text = helper,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
        )
    }
}

@Composable
private fun MdButton(
    label: String,
    aria: String,
    bold: Boolean = false,
    italic: Boolean = false,
    strikethrough: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic
        else androidx.compose.ui.text.font.FontStyle.Normal,
        textDecoration = if (strikethrough) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = style,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

private fun wrapSelection(
    content: String,
    start: Int,
    end: Int,
    open: String,
    close: String,
    placeholder: String,
): Pair<String, androidx.compose.ui.text.TextRange> {
    val before = content.substring(0, start)
    val selected = content.substring(start, end)
    val after = content.substring(end)
    val inner = selected.ifBlank { placeholder }
    val newText = before + open + inner + close + after
    val cursorStart = start + open.length
    val cursorEnd = cursorStart + inner.length
    return newText to androidx.compose.ui.text.TextRange(cursorStart, cursorEnd)
}

private fun prefixSelectedLines(
    content: String,
    start: Int,
    end: Int,
    prefix: (lineIndex: Int) -> String,
): Pair<String, androidx.compose.ui.text.TextRange> {
    val lineStart = content.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
    val lineEnd = content.indexOf('\n', end).let { if (it < 0) content.length else it }
    val before = content.substring(0, lineStart)
    val selected = content.substring(lineStart, lineEnd)
    val after = content.substring(lineEnd)
    val newSelected = selected.split('\n').mapIndexed { i, line -> prefix(i) + line }.joinToString("\n")
    val newText = before + newSelected + after
    val cursorEnd = lineStart + newSelected.length
    return newText to androidx.compose.ui.text.TextRange(cursorEnd, cursorEnd)
}

private fun insertLink(
    content: String,
    start: Int,
    end: Int,
): Pair<String, androidx.compose.ui.text.TextRange> {
    val before = content.substring(0, start)
    val selected = content.substring(start, end)
    val after = content.substring(end)
    val label = selected.ifBlank { "link" }
    val inserted = "[$label](https://)"
    val newText = before + inserted + after
    // Place cursor inside the URL ('https://' is 8 chars after '](').
    val urlStart = start + "[".length + label.length + "](".length
    val urlEnd = urlStart + "https://".length
    return newText to androidx.compose.ui.text.TextRange(urlStart, urlEnd)
}

@Composable
private fun PostVisibilityStep(state: PostFormState, viewModel: PostFormViewModel) {
    PSectionLabel("Visibility")
    PostVisibilityOption(
        icon = Icons.Outlined.Public,
        title = "Public",
        description = "Anyone can read this post.",
        selected = state.visibility == UploadVisibility.Public,
        onClick = { viewModel.onVisibilityChange(UploadVisibility.Public) },
    )
    PostVisibilityOption(
        icon = Icons.Outlined.VisibilityOff,
        title = "Unlisted",
        description = "Only people with the link can read it.",
        selected = state.visibility == UploadVisibility.Unlisted,
        onClick = { viewModel.onVisibilityChange(UploadVisibility.Unlisted) },
    )
    PostVisibilityOption(
        icon = Icons.Outlined.Schedule,
        title = "Scheduled",
        description = "Make it public at a chosen date.",
        selected = state.visibility == UploadVisibility.Scheduled,
        onClick = { viewModel.onVisibilityChange(UploadVisibility.Scheduled) },
    )
    if (state.visibility == UploadVisibility.Scheduled) {
        PostScheduledPicker(
            timestamp = state.scheduledTimestamp ?: (System.currentTimeMillis() + 30 * 60 * 1000L),
            showOnUpcoming = state.showOnUpcoming,
            onTimestampChange = viewModel::onScheduledTimestampChange,
            onShowOnUpcomingChange = viewModel::onShowOnUpcomingChange,
        )
    }
    PostVisibilityOption(
        icon = Icons.Outlined.Lock,
        title = "Private",
        description = "Only you can see this post.",
        selected = state.visibility == UploadVisibility.Private,
        onClick = { viewModel.onVisibilityChange(UploadVisibility.Private) },
    )

    Spacer(Modifier.height(8.dp))
    PSectionLabel("Members only")
    PostInlineToggleRow(
        title = if (state.membersOnly) "Restricted to channel members" else "Open to everyone",
        description = "Limit access to paying members of your channel.",
        checked = state.membersOnly,
        onChange = viewModel::onMembersOnlyChange,
    )

    if (state.visibility != UploadVisibility.Public) {
        Spacer(Modifier.height(8.dp))
        PSectionLabel("Price")
        Text(
            text = "Payment options aren't available for unlisted, scheduled or private posts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Spacer(Modifier.height(8.dp))
        PSectionLabel("Price")
        PostPaywallOption(
            title = "Free",
            description = "Anyone can view this post for free.",
            selected = state.paywall == UploadPaywall.Free,
            onClick = { viewModel.onPaywallChange(UploadPaywall.Free) },
        )
        PostPaywallOption(
            title = "Paid with Credits (LBC)",
            description = "Charge in LBC via the network.",
            selected = state.paywall == UploadPaywall.Lbc,
            onClick = { viewModel.onPaywallChange(UploadPaywall.Lbc) },
        )
        if (state.paywall == UploadPaywall.Lbc) {
            PSectionLabel("Credits (LBC)")
            PFlatTextField(
                value = state.lbcAmount,
                onValueChange = viewModel::onLbcAmountChange,
                suffix = "LBC",
            )
        }
        PostPaywallOption(
            title = "Paid (Purchase / Rent)",
            description = "Sell access in USD via a one-off purchase or rental.",
            selected = state.paywall == UploadPaywall.Fiat,
            onClick = { viewModel.onPaywallChange(UploadPaywall.Fiat) },
        )
        if (state.paywall == UploadPaywall.Fiat) {
            PostInlineToggleRow(
                title = "Purchase",
                description = "One-time purchase grants permanent access.",
                checked = state.fiatPurchaseEnabled,
                onChange = viewModel::onFiatPurchaseEnabledChange,
            )
            if (state.fiatPurchaseEnabled) {
                PSectionLabel("Purchase price")
                PFlatTextField(
                    value = state.fiatPurchaseAmount,
                    onValueChange = viewModel::onFiatPurchaseAmountChange,
                    suffix = "USD",
                )
            }
            PostInlineToggleRow(
                title = "Rent",
                description = "Time-limited access that expires.",
                checked = state.fiatRentalEnabled,
                onChange = viewModel::onFiatRentalEnabledChange,
            )
            if (state.fiatRentalEnabled) {
                PSectionLabel("Rental price")
                PFlatTextField(
                    value = state.fiatRentalAmount,
                    onValueChange = viewModel::onFiatRentalAmountChange,
                    suffix = "USD",
                )
                PSectionLabel("Rental duration")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PFlatTextField(
                            value = state.fiatRentalDuration,
                            onValueChange = viewModel::onFiatRentalDurationChange,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            ),
                        )
                    }
                    Box(modifier = Modifier.weight(1.2f)) {
                        PostRentalUnitDropdown(
                            value = state.fiatRentalDurationUnit,
                            onChange = viewModel::onFiatRentalDurationUnitChange,
                        )
                    }
                }
            }
            Text(
                text = "By continuing, you accept Odysee's paid-content terms.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PostInlineToggleRow(
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
private fun PostPaywallOption(
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

@Composable
private fun PostScheduledPicker(
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
private fun PostRentalUnitDropdown(
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
private fun PostPublishStep(state: PostFormState) {
    PSectionLabel("Review")
    PSummaryRow("Title", state.title.ifBlank { "—" })
    PSummaryRow("URL", "odysee.com/${state.urlSlug.ifBlank { "—" }}")
    PSummaryRow(
        "Content",
        state.body.take(160).let { if (state.body.length > 160) "$it…" else it }.ifBlank { "—" },
    )
    PSummaryRow("Thumbnail", state.thumbnailUrl.ifBlank { "—" })
    PSummaryRow("Tags", if (state.tags.isEmpty()) "—" else state.tags.joinToString(", "))
    PSummaryRow(
        "Language",
        SupportedLanguages.list.firstOrNull { it.first == state.language }?.second ?: state.language,
    )
    PSummaryRow("License", state.license.display)
    PSummaryRow(
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
    PSummaryRow(
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
    if (state.membersOnly) PSummaryRow("Members only", "Yes")
}

@Composable
private fun PSummaryRow(label: String, value: String) {
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
private fun PostVisibilityOption(
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
private fun PostTagsSection(state: PostFormState, viewModel: PostFormViewModel) {
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
            PostWrapChips(spacing = 6.dp) {
                state.tags.forEach { tag ->
                    PostTagChip(name = tag, type = PostTagChipType.Remove) { viewModel.removeTag(tag) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PFlatTextField(
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
            POPULAR_POST_TAGS
                .filter { !state.tags.contains(it) }
                .filter { if (trimmed.isBlank()) true else it.contains(trimmed) }
                .take(20)
        }
        Spacer(Modifier.height(8.dp))
        PostWrapChips(spacing = 6.dp) {
            if (trimmed.isNotEmpty() && !suggestions.contains(trimmed) && state.tags.size < 5) {
                PostTagChip(name = trimmed, type = PostTagChipType.Add) { viewModel.addTag(trimmed) }
            }
            suggestions.forEach { name ->
                PostTagChip(
                    name = name,
                    type = PostTagChipType.Add,
                    disabled = state.tags.size >= 5,
                ) { viewModel.addTag(name) }
            }
        }
    }
}

@Composable
private fun PostWrapChips(
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

private enum class PostTagChipType { Add, Remove }

@Composable
private fun PostTagChip(
    name: String,
    type: PostTagChipType,
    disabled: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = when (type) {
        PostTagChipType.Remove -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when (type) {
        PostTagChipType.Remove -> Color.White
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
            imageVector = if (type == PostTagChipType.Remove) Icons.Outlined.Close else Icons.Filled.Add,
            contentDescription = null,
            tint = fg.copy(alpha = if (disabled) 0.5f else 1f),
            modifier = Modifier.size(14.dp),
        )
    }
}

private val POPULAR_POST_TAGS = listOf(
    "article", "blog", "commentary", "education", "explainer", "finance", "gaming",
    "health", "journal", "news", "opinion", "philosophy", "politics", "review",
    "science", "spirituality", "tech", "tutorial", "world",
)

@Composable
private fun PostThumbnailSection(state: PostFormState, viewModel: PostFormViewModel) {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
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
                        text = "Upload",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        PSectionLabel("Or paste an image URL")
        PFlatTextField(
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
private fun PostLanguageDropdown(value: String, onChange: (String) -> Unit) {
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
private fun PostLicenseDropdown(value: UploadLicense, onChange: (UploadLicense) -> Unit) {
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
private fun PostBottomBar(
    state: PostFormState,
    channels: List<Channel>,
    onSelectChannel: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPublish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        if (state.step == PostStep.Publish) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PostChannelChip(
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
            if (state.step != PostStep.Post) {
                OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text("Back") }
            }
            if (state.step != PostStep.Publish) {
                val enabled = when (state.step) {
                    PostStep.Post -> state.isPostStepValid
                    PostStep.Content -> state.isContentStepValid
                    PostStep.Visibility -> state.isVisibilityStepValid
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
private fun PostChannelChip(
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
