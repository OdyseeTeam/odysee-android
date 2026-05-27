package com.odysee.app.auth

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.model.Channel
import com.odysee.app.core.designsystem.R as DesignR
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountMenuSheet(
    authState: AuthState,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onSignOut: () -> Unit,
    onSelectChannel: (String) -> Unit,
    onChannelClick: (String, String) -> Unit,
    onWallet: () -> Unit,
    onFollowing: () -> Unit,
    onNewChannel: () -> Unit,
    onUploads: () -> Unit,
    onChannels: () -> Unit,
    onAnalytics: () -> Unit,
    onYoutubeSync: () -> Unit,
    onCredits: () -> Unit,
    onRewards: () -> Unit,
    onInvites: () -> Unit,
    onMemberships: () -> Unit,
    onPremium: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun close(after: () -> Unit = {}) {
        scope.launch {
            sheetState.hide()
            onDismiss()
            after()
        }
    }

    var channelsExpanded by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            AccountHeader(
                authState = authState,
                expandable = authState is AuthState.SignedIn && authState.channels.size > 1,
                isExpanded = channelsExpanded,
                onToggleExpand = { channelsExpanded = !channelsExpanded },
                onOpenChannel = {
                    val active = (authState as? AuthState.SignedIn)?.activeChannel ?: return@AccountHeader
                    close { onChannelClick(active.claimId, active.name) }
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            if (authState is AuthState.SignedIn) {
                AnimatedVisibility(visible = channelsExpanded && authState.channels.size > 1) {
                    Column {
                        ChannelList(
                            channels = authState.channels.filter { it.claimId != authState.activeChannel?.claimId },
                            onSelect = { claimId ->
                                onSelectChannel(claimId)
                                channelsExpanded = false
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Library / activity
                    MenuItem(DesignR.drawable.ic_menu_channel, "Following") { close(onFollowing) }
                    MenuItem(DesignR.drawable.ic_menu_wallet, "Wallet") { close(onWallet) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Channels / creator
                    MenuItem(DesignR.drawable.ic_menu_channel, "New Channel") { close(onNewChannel) }
                    MenuItem(DesignR.drawable.ic_menu_channel, "Channels") { close(onChannels) }
                    MenuItem(DesignR.drawable.ic_menu_publish, "Uploads") { close(onUploads) }
                    MenuItem(DesignR.drawable.ic_menu_analytics, "Creator Analytics") { close(onAnalytics) }
                    MenuItem(DesignR.drawable.ic_menu_youtube, "Sync YouTube Channel") { close(onYoutubeSync) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Rewards
                    MenuItem(DesignR.drawable.ic_menu_rewards, "Credits") { close(onCredits) }
                    MenuItem(DesignR.drawable.ic_menu_rewards, "Rewards") { close(onRewards) }
                    MenuItem(DesignR.drawable.ic_menu_invite, "Invites") { close(onInvites) }
                    MenuItem(DesignR.drawable.ic_menu_membership, "Memberships") { close(onMemberships) }
                    MenuItem(DesignR.drawable.ic_menu_upgrade, "Odysee Premium") { close(onPremium) }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                MenuItem(DesignR.drawable.ic_menu_settings, "Settings") { close(onSettings) }
                MenuItem(DesignR.drawable.ic_menu_help, "Help") { close(onHelp) }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                MenuItem(
                    iconRes = DesignR.drawable.ic_menu_signout,
                    label = "Sign Out",
                    destructive = true,
                ) { close(onSignOut) }
            } else {
                MenuItem(DesignR.drawable.ic_menu_signin, "Log In") { close(onSignIn) }
                MenuItem(DesignR.drawable.ic_menu_signup, "Sign Up") { close(onSignUp) }
                MenuItem(DesignR.drawable.ic_menu_settings, "Settings") { close(onSettings) }
                MenuItem(DesignR.drawable.ic_menu_help, "Help") { close(onHelp) }
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun AccountHeader(
    authState: AuthState,
    expandable: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenChannel: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "chevron")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (authState) {
            is AuthState.SignedIn -> {
                val hasActiveChannel = authState.activeChannel != null
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (hasActiveChannel) Modifier.clickable(onClick = onOpenChannel) else Modifier)
                        .padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccountAvatar(
                        thumbnailUrl = authState.activeChannel?.thumbnailUrl,
                        fallback = authState.user.email?.firstOrNull()?.uppercaseChar() ?: 'O',
                        size = 40,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val displayName = authState.activeChannel?.let {
                            it.title?.takeIf { t -> t.isNotBlank() } ?: it.name
                        } ?: authState.user.email ?: "Account"
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        authState.user.email?.let { email ->
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (expandable) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onToggleExpand)
                            .padding(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Hide channels" else "Switch channel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.rotate(chevronRotation),
                        )
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Not signed in",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelList(
    channels: List<Channel>,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        channels.forEach { channel ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(channel.claimId) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountAvatar(
                    thumbnailUrl = channel.thumbnailUrl,
                    fallback = (channel.name.firstOrNull { it.isLetterOrDigit() } ?: 'O').uppercaseChar(),
                    size = 32,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.title?.takeIf { it.isNotBlank() } ?: channel.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountAvatar(thumbnailUrl: String?, fallback: Char, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = fallback.toString(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MenuItem(
    @DrawableRes iconRes: Int,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            fontWeight = FontWeight.Medium,
        )
    }
}
