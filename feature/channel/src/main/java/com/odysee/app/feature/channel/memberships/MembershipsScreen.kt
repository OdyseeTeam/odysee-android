package com.odysee.app.feature.channel.memberships

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odysee.app.core.data.memberships.MembershipTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipsScreen(
    onBack: () -> Unit,
    viewModel: MembershipsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    var deleteTarget by remember { mutableStateOf<MembershipTier?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Memberships") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::startNew,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add tier", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            state.error?.let { err ->
                Surface(
                    color = Color(0x33E2202D),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { viewModel.clearError() },
                ) {
                    Text(
                        text = err,
                        color = Color(0xFFE2202D),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            if (state.tiers.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No membership tiers yet. Tap + to create one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.tiers, key = { it.id }) { tier ->
                        TierRow(
                            tier = tier,
                            onEdit = { viewModel.startEdit(tier) },
                            onDelete = { deleteTarget = tier },
                        )
                    }
                }
            }
        }
    }

    state.editing?.let { draft ->
        AlertDialog(
            onDismissRequest = viewModel::closeEdit,
            title = { Text(if (draft.id == null) "New tier" else "Edit tier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = draft.name,
                        onValueChange = viewModel::onName,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tier name") },
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 1,
                    )
                    TextField(
                        value = draft.description,
                        onValueChange = viewModel::onDescription,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Description") },
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        minLines = 2,
                        maxLines = 5,
                    )
                    TextField(
                        value = draft.priceText,
                        onValueChange = viewModel::onPrice,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Monthly price (USD)") },
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        maxLines = 1,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::submit,
                    enabled = draft.canSubmit && !state.isMutating,
                ) {
                    if (state.isMutating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(if (draft.id == null) "Create" else "Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeEdit) { Text("Cancel") }
            },
        )
    }

    deleteTarget?.let { tier ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete tier?") },
            text = { Text("Remove “${tier.name}”? Active subscribers will keep access until their period ends.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(tier)
                    deleteTarget = null
                }) { Text("Delete", color = Color(0xFFE2202D)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun TierRow(tier: MembershipTier, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tier.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                tier.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "$${"%.2f".format(tier.priceUsd)} / month",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFE2202D))
            }
        }
    }
}

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    unfocusedIndicatorColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
)
