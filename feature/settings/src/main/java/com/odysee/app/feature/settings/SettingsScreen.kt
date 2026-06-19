package com.odysee.app.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odysee.app.core.designsystem.i18n.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenBlockedChannels: () -> Unit,
    onOpenPassword: () -> Unit,
    onClearCache: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenNotificationSettings: () -> Unit = {},
    onOpenComments: () -> Unit = {},
    onOpenPurchases: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    val ctx = LocalContext.current

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showHomepageLanguageDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showPlaylistActionDialog by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = tr("Back"))
                    }
                },
                title = { Text(tr("Settings")) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val wide = com.odysee.app.core.designsystem.layout.rememberWindowSize()
            .ordinal >= com.odysee.app.core.designsystem.layout.WindowSize.Medium.ordinal
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .let { if (wide) it.widthIn(max = 720.dp).padding(horizontal = 24.dp) else it },
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item { SectionHeader(tr("Appearance")) }
            item {
                ActionRow(
                    title = tr("Homepage"),
                    subtitle = homepageLanguageLabel(state.homepageLanguage),
                    onClick = { showHomepageLanguageDialog = true },
                )
            }
            item {
                ActionRow(
                    title = tr("Language"),
                    subtitle = languageLabel(state.language),
                    onClick = { showLanguageDialog = true },
                )
            }
            item {
                ToggleRow(
                    title = tr("Search only in the selected language by default"),
                    checked = state.searchInLanguage,
                    onCheckedChange = viewModel::setSearchInLanguage,
                )
            }
            item {
                ChoiceChipsRow(
                    title = tr("Theme"),
                    options = themeOptions,
                    selected = state.theme,
                    onSelect = viewModel::setTheme,
                )
            }
            item {
                ToggleRow(
                    title = "24-hour clock",
                    checked = state.clock24h,
                    onCheckedChange = viewModel::setClock24h,
                )
            }
            item {
                ToggleRow(
                    title = tr("Hide unread notification count"),
                    checked = state.hideNotificationCount,
                    onCheckedChange = viewModel::setHideNotificationCount,
                )
            }
            item { Divider() }

            item { SectionHeader(tr("Account")) }
            item {
                ActionRow(
                    title = tr("Password"),
                    subtitle = tr("Sign in to change your password."),
                    onClick = onOpenPassword,
                )
            }
            if (state.hasChannels) {
                item {
                    ActionRow(
                        title = tr("Comments"),
                        subtitle = tr("View your past comments."),
                        onClick = onOpenComments,
                    )
                }
            }
            item {
                ActionRow(
                    title = tr("Purchases"),
                    subtitle = tr("View your purchased content."),
                    onClick = onOpenPurchases,
                )
            }
            item { Divider() }

            item { SectionHeader(tr("Content")) }
            item {
                ToggleRow(
                    title = tr("Hide members-only content"),
                    subtitle = tr("You will not see content that requires a membership subscription."),
                    checked = state.hideMembersOnly,
                    onCheckedChange = viewModel::setHideMembersOnly,
                )
            }
            item {
                ToggleRow(
                    title = tr("Hide reposts"),
                    subtitle = tr("You will not see reposts by people you follow."),
                    checked = state.hideReposts,
                    onCheckedChange = viewModel::setHideReposts,
                )
            }
            item {
                ActionRow(
                    title = tr("Default playlist action"),
                    subtitle = playlistActionLabel(state.defaultPlaylistAction),
                    onClick = { showPlaylistActionDialog = true },
                )
            }
            item {
                ActionRow(
                    title = tr("Notifications"),
                    subtitle = tr("Push or background fetch — your choice"),
                    onClick = onOpenNotificationSettings,
                )
            }
            item {
                ActionRow(
                    title = tr("Blocked and hidden channels"),
                    subtitle = null,
                    onClick = onOpenBlockedChannels,
                )
            }
            item {
                ToggleRow(
                    title = tr("Publish confirmation"),
                    subtitle = tr("Show preview and confirmation dialog before publishing content."),
                    checked = state.publishConfirmation,
                    onCheckedChange = viewModel::setPublishConfirmation,
                )
            }
            item {
                ToggleRow(
                    title = tr("Purchase and tip confirmations"),
                    subtitle = tr("Show a confirmation dialog before purchases and tips."),
                    checked = state.purchaseTipConfirmation,
                    onCheckedChange = viewModel::setPurchaseTipConfirmation,
                )
            }
            item { Divider() }

            item { SectionHeader(tr("Player")) }
            item {
                ToggleRow(
                    title = tr("Autoplay media files"),
                    subtitle = tr("Autoplay video and audio when opening a file."),
                    checked = state.autoplayMedia,
                    onCheckedChange = viewModel::setAutoplayMedia,
                )
            }
            item {
                ToggleRow(
                    title = tr("Autoplay next recommended content"),
                    subtitle = tr("Autoplay the next related item when a file finishes."),
                    checked = state.autoplay,
                    onCheckedChange = viewModel::setAutoplay,
                )
            }
            item {
                ActionRow(
                    title = tr("Default video quality"),
                    subtitle = qualityLabel(state.defaultVideoQuality),
                    onClick = { showQualityDialog = true },
                )
            }
            item {
                ToggleRow(
                    title = tr("P2P stream delivery"),
                    subtitle = tr("Help other viewers by sharing video segments peer-to-peer."),
                    checked = state.p2pDelivery,
                    onCheckedChange = viewModel::setP2pDelivery,
                )
            }
            item { Divider() }

            item { SectionHeader(tr("System")) }
            item {
                ActionRow(
                    title = tr("Clear application cache"),
                    subtitle = tr("This might fix issues you are having."),
                    onClick = {
                        onClearCache()
                        toast(ctx, com.odysee.app.core.model.i18n.Translations.tr("Cache cleared"))
                    },
                )
            }
            item {
                ActionRow(
                    title = tr("Request account deletion"),
                    subtitle = tr("Send account deletion request to Odysee."),
                    onClick = { showDeleteAccount = true },
                )
            }
            item { Divider() }

            item { SectionHeader(tr("About")) }
            item {
                val versionLabel = remember(ctx) {
                    runCatching {
                        val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                        val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
                            info.longVersionCode else info.versionCode.toLong()
                        "${info.versionName} ($code)"
                    }.getOrDefault("unknown")
                }
                InfoRow(title = tr("Version"), subtitle = versionLabel)
            }
            item {
                ActionRow(
                    title = tr("Privacy policy"),
                    onClick = { openUrl(ctx, "https://odysee.com/\$/privacypolicy") },
                )
            }
            item {
                ActionRow(
                    title = tr("Terms of service"),
                    onClick = { openUrl(ctx, "https://odysee.com/\$/tos") },
                )
            }
            if (viewModel.updaterSupported) {
                item {
                    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
                    val subtitle = when (val s = updateState) {
                        is com.odysee.app.core.data.updater.UpdateState.Checking -> tr("Checking…")
                        is com.odysee.app.core.data.updater.UpdateState.UpToDate ->
                            "Up to date (${s.installedVersion})"
                        is com.odysee.app.core.data.updater.UpdateState.Available ->
                            "Update available: ${s.info.displayVersion}"
                        is com.odysee.app.core.data.updater.UpdateState.Downloading ->
                            "Downloading… ${(s.progress * 100).toInt()}%"
                        is com.odysee.app.core.data.updater.UpdateState.ReadyToInstall ->
                            tr("Ready to install")
                        is com.odysee.app.core.data.updater.UpdateState.Failed ->
                            "Failed: ${s.message}"
                        else -> tr("Tap to check")
                    }
                    ActionRow(
                        title = if (updateState is com.odysee.app.core.data.updater.UpdateState.Available ||
                            updateState is com.odysee.app.core.data.updater.UpdateState.ReadyToInstall) {
                            tr("Install update")
                        } else tr("Check for updates"),
                        subtitle = subtitle,
                        onClick = {
                            when (updateState) {
                                is com.odysee.app.core.data.updater.UpdateState.Available,
                                is com.odysee.app.core.data.updater.UpdateState.ReadyToInstall ->
                                    viewModel.downloadUpdate()
                                else -> viewModel.checkForUpdates()
                            }
                        },
                    )
                }
            }
        }
    }

    if (showLanguageDialog) {
        ChoiceDialog(
            title = tr("Language"),
            options = languageOptions,
            selected = state.language ?: "system",
            onSelect = {
                viewModel.setLanguage(if (it == "system") null else it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    if (showHomepageLanguageDialog) {
        ChoiceDialog(
            title = tr("Homepage language"),
            options = homepageLanguageOptions,
            selected = state.homepageLanguage ?: "en",
            onSelect = {
                viewModel.setHomepageLanguage(it)
                showHomepageLanguageDialog = false
            },
            onDismiss = { showHomepageLanguageDialog = false },
        )
    }
    if (showQualityDialog) {
        ChoiceDialog(
            title = tr("Default video quality"),
            options = qualityOptions,
            selected = state.defaultVideoQuality,
            onSelect = {
                viewModel.setDefaultVideoQuality(it)
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false },
        )
    }
    if (showPlaylistActionDialog) {
        ChoiceDialog(
            title = tr("Default playlist action"),
            options = playlistActionOptions,
            selected = state.defaultPlaylistAction,
            onSelect = {
                viewModel.setDefaultPlaylistAction(it)
                showPlaylistActionDialog = false
            },
            onDismiss = { showPlaylistActionDialog = false },
        )
    }
    if (showDeleteAccount) {
        AlertDialog(
            onDismissRequest = { showDeleteAccount = false },
            title = { Text(tr("Request account deletion?")) },
            text = { Text(tr("This sends a deletion request to Odysee. You'll need to delete all uploads first.")) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccount = false
                    viewModel.requestAccountDeletion { ok, msg ->
                        toast(ctx, if (ok) com.odysee.app.core.model.i18n.Translations.tr("Deletion request sent") else (msg ?: com.odysee.app.core.model.i18n.Translations.tr("Couldn't send request")))
                    }
                }) { Text(tr("Send request")) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccount = false }) { Text(tr("Cancel")) }
            },
        )
    }
}

private fun toast(ctx: android.content.Context, msg: String) {
    android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
}

private fun openUrl(ctx: android.content.Context, url: String) {
    runCatching {
        ctx.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = tr(text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tr(title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = tr(subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tr(title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = tr(subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChoiceChipsRow(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = tr(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .clickable { onSelect(value) }
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = tr(label),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // 56 languages overflow a non-scrolling AlertDialog body; make
            // the content scrollable and cap its height so the dialog
            // doesn't push its title/buttons off-screen.
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 0.dp,
            ) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
            ) {
                items(options, key = { it.first }) { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            label,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Done")) } },
    )
}

@Composable
private fun InfoRow(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val themeOptions = listOf(
    "system" to "System",
    "light" to "Light",
    "dark" to "Dark",
)

private val languageOptions =
    com.odysee.app.core.data.i18n.TranslationStore.SUPPORTED

// Endonyms — these stay in their native script regardless of UI language.
private val homepageLanguageOptions = listOf(
    "en" to "English",
    "fr" to "Français",
    "es" to "Español",
    "de" to "Deutsch",
    "zh" to "中文",
    "ru" to "Русский",
    "it" to "Italiano",
    "pt-BR" to "Português (Brasil)",
    "hi" to "हिन्दी",
)

private val qualityOptions = listOf(
    "auto" to "Auto",
    "1080" to "1080p",
    "720" to "720p",
    "480" to "480p",
    "360" to "360p",
    "240" to "240p",
)

private val playlistActionOptions = listOf(
    "view" to "View",
    "play" to "Play",
)

private fun languageLabel(value: String?): String =
    languageOptions.firstOrNull { it.first == (value ?: "system") }?.second
        ?: com.odysee.app.core.model.i18n.Translations.tr("System default")

private fun homepageLanguageLabel(value: String?): String =
    homepageLanguageOptions.firstOrNull { it.first == value }?.second ?: "English"

private fun qualityLabel(value: String): String {
    val raw = qualityOptions.firstOrNull { it.first == value }?.second ?: "Auto"
    return if (raw == "Auto") com.odysee.app.core.model.i18n.Translations.tr("Auto") else raw
}

private fun playlistActionLabel(value: String): String =
    com.odysee.app.core.model.i18n.Translations.tr(
        playlistActionOptions.firstOrNull { it.first == value }?.second ?: "View"
    )
