package com.odysee.app.account

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.odysee.app.auth.LocalAuthState
import com.odysee.app.core.data.auth.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitesScreen(
    onBack: () -> Unit,
    viewModel: InvitesViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    BackHandler(onBack = onBack)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Invites") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            return@Scaffold
        }
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("header") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Earn credits for inviting friends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = if (state.invitesRemaining > 0)
                            "You have ${state.invitesRemaining} invites left to send."
                        else "Send your referral link or invite by email.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            run {
                item("link") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Your invite link",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = state.inviteUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                            )
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(state.inviteUrl))
                            }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
                            }
                        }
                        if (state.channels.isNotEmpty()) {
                            InviteCustomizeDropdown(
                                channels = state.channels,
                                referralCode = state.referralCode,
                                selectedKey = state.selectedReferralKey,
                                onSelect = viewModel::selectReferralKey,
                            )
                        }
                        androidx.compose.material3.Button(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "Join me on Odysee: ${state.inviteUrl}",
                                    )
                                }
                                runCatching {
                                    context.startActivity(
                                        android.content.Intent.createChooser(intent, "Share"),
                                    )
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Share link", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item("send") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Invite by email",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    androidx.compose.material3.TextField(
                        value = state.emailDraft,
                        onValueChange = viewModel::onEmailDraftChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        placeholder = {
                            Text("friend@example.com", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                        ),
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                    androidx.compose.material3.Button(
                        onClick = viewModel::sendInvite,
                        enabled = !state.isSending && state.emailDraft.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            text = if (state.isSending) "Sending…" else "Send invite",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    state.statusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.statusIsError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (state.invitees.isNotEmpty()) {
                item("invite_history_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Invite History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        val reward = state.claimableReward
                        if (reward != null) {
                            androidx.compose.material3.Button(
                                onClick = viewModel::claimReward,
                                enabled = !state.isClaiming,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Text(
                                    text = if (state.isClaiming) "Claiming…"
                                    else "Receive ${reward.rewardAmount ?: 0.0} LBC",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
                item("invite_history_columns") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    ) {
                        Text(
                            text = "Email",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.4f),
                        )
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.9f),
                        )
                        Text(
                            text = "Credit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.9f),
                        )
                    }
                }
                items(state.invitees.size) { idx ->
                    val invitee = state.invitees[idx]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = invitee.email?.takeIf { it.isNotBlank() } ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1.4f),
                            maxLines = 1,
                        )
                        Text(
                            text = if (invitee.inviteAccepted == true) "Accepted" else "Not Accepted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.9f),
                        )
                        Text(
                            text = when {
                                invitee.inviteRewardClaimed == true -> "Claimed"
                                invitee.inviteRewardClaimable == true -> "Claimable"
                                else -> "Unclaimable"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (invitee.inviteRewardClaimed == true) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.9f),
                        )
                    }
                }
            }

            state.error?.let { err ->
                item("error") {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteCustomizeDropdown(
    channels: List<com.odysee.app.core.model.Channel>,
    referralCode: String,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    val openState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var open by openState
    val selectedLabel = channels.firstOrNull { it.name == selectedKey }?.name ?: referralCode
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Customize link",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            androidx.compose.material3.OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { open = true },
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.ExpandMore,
                        contentDescription = null,
                    )
                },
            )
            androidx.compose.material3.DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
            ) {
                channels.forEach { ch ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(ch.name) },
                        onClick = {
                            onSelect(ch.name)
                            open = false
                        },
                    )
                }
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(referralCode) },
                    onClick = {
                        onSelect(referralCode)
                        open = false
                    },
                )
            }
        }
    }
}

