package com.odysee.app.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.odysee.app.core.data.ContentRepository
import com.odysee.app.core.data.auth.AuthRepository
import com.odysee.app.core.data.auth.AuthState
import com.odysee.app.core.model.Channel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepostUiState(
    val channels: List<Channel> = emptyList(),
    val selectedChannelClaimId: String? = null,
    val name: String = "",
    val bid: String = "0.001",
    val isPosting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

@HiltViewModel
class RepostViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RepostUiState())
    val state: StateFlow<RepostUiState> = _state.asStateFlow()

    init {
        val signed = authRepository.state.value as? AuthState.SignedIn
        _state.update {
            it.copy(
                channels = signed?.channels.orEmpty(),
                selectedChannelClaimId = signed?.activeChannel?.claimId,
            )
        }
    }

    fun seedFromClaim(claimName: String) {
        if (_state.value.name.isBlank() && claimName.isNotBlank()) {
            _state.update { it.copy(name = claimName) }
        }
    }

    fun onChannelChange(claimId: String) {
        _state.update { it.copy(selectedChannelClaimId = claimId) }
    }

    fun onNameChange(v: String) {
        _state.update { it.copy(name = v.take(100)) }
    }

    fun onBidChange(v: String) {
        _state.update { it.copy(bid = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    }

    fun repost(claimId: String) {
        val st = _state.value
        val channel = st.selectedChannelClaimId ?: run {
            _state.update { it.copy(error = "Pick a channel first.") }
            return
        }
        val bid = st.bid.toDoubleOrNull() ?: 0.0
        if (bid <= 0.0) {
            _state.update { it.copy(error = "Bid must be greater than 0.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isPosting = true, error = null) }
            val result = runCatching {
                contentRepository.repost(
                    name = st.name.ifBlank { "repost-" + claimId.take(8) },
                    claimId = claimId,
                    channelId = channel,
                    bidLbc = bid,
                )
            }
            result.fold(
                onSuccess = { _state.update { it.copy(isPosting = false, success = true) } },
                onFailure = { e ->
                    _state.update {
                        it.copy(isPosting = false, error = e.message ?: "Repost failed.")
                    }
                },
            )
        }
    }
}

@Composable
fun RepostSheet(
    claimId: String,
    claimName: String,
    onDismiss: () -> Unit,
    onPosted: () -> Unit = {},
    viewModel: RepostViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(claimName) { viewModel.seedFromClaim(claimName) }
    LaunchedEffect(state.success) { if (state.success) onPosted() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Repost") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Repost \"$claimName\" to one of your channels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                RepostChannelDropdown(
                    channels = state.channels,
                    selectedClaimId = state.selectedChannelClaimId,
                    onSelect = viewModel::onChannelChange,
                )

                TextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            "URL name (lowercase, no spaces)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )

                TextField(
                    value = state.bid,
                    onValueChange = viewModel::onBidChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Bid (LBC)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    suffix = { Text("LBC") },
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                state.error?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.repost(claimId) },
                enabled = !state.isPosting,
            ) { Text(if (state.isPosting) "Reposting…" else "Repost") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RepostChannelDropdown(
    channels: List<Channel>,
    selectedClaimId: String?,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val selected = channels.firstOrNull { it.claimId == selectedClaimId }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                val avatar = selected?.thumbnailUrl
                if (!avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = avatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = (selected?.name?.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: '?').toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = selected?.title?.takeIf { it.isNotBlank() } ?: selected?.name ?: "Select a channel",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
