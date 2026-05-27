package com.odysee.app.feature.wallet

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odysee.app.core.data.wallet.WalletBalance
import com.odysee.app.core.data.wallet.WalletTransaction
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    var showReceive by remember { mutableStateOf(false) }
    var showSend by remember { mutableStateOf(false) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Wallet") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            state.error != null && state.balance == null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Wallet unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = viewModel::load) { Text("Retry") }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    state.balance?.let { BalanceCard(it) }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { showSend = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(
                                Icons.Outlined.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Send", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.ensureReceiveAddress()
                                showReceive = true
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Outlined.CallReceived,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Receive")
                        }
                    }
                }
                item {
                    Text(
                        text = "Recent transactions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (state.transactions.isEmpty()) {
                    item {
                        Text(
                            text = "No transactions yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(state.transactions, key = { it.txid }) { tx ->
                        TransactionRow(tx)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    if (showReceive) {
        AlertDialog(
            onDismissRequest = { showReceive = false },
            title = { Text("Receive LBC") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Send LBC to this address from any LBRY wallet. A new unused address is generated each time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val addr = state.receiveAddress
                    if (addr == null || state.isLoadingAddress) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = addr,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            IconButton(onClick = {
                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(addr))
                            }) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReceive = false }) { Text("Done") }
            },
        )
    }

    if (showSend) {
        AlertDialog(
            onDismissRequest = {
                showSend = false
                viewModel.clearSendStatus()
            },
            title = { Text("Send LBC") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = state.sendDraftAddress,
                        onValueChange = viewModel::onSendDraftAddressChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        placeholder = {
                            Text(
                                "Destination address",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    )
                    TextField(
                        value = state.sendDraftAmount,
                        onValueChange = viewModel::onSendDraftAmountChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        placeholder = {
                            Text(
                                "Amount in LBC",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                        ),
                        suffix = { Text("LBC") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    )
                    state.sendStatus?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.sendIsError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.send() },
                    enabled = !state.isSending,
                ) { Text(if (state.isSending) "Sending…" else "Send") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSend = false
                    viewModel.clearSendStatus()
                }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun BalanceCard(balance: WalletBalance) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Available",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${formatLbc(balance.available)} LBC",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                BalanceStat("Total", balance.total, modifier = Modifier.weight(1f))
                BalanceStat("Reserved", balance.reserved, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                BalanceStat("Claims", balance.reservedForClaims, modifier = Modifier.weight(1f))
                BalanceStat("Supports", balance.reservedForSupports, modifier = Modifier.weight(1f))
                BalanceStat("Tips", balance.reservedForTips, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BalanceStat(label: String, value: Double, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatLbc(value),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun TransactionRow(tx: WalletTransaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description.ifBlank { "Transaction" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tx.timestamp?.let { formatDate(it) } ?: "Pending",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = (if (tx.valueLbc >= 0) "+" else "") + formatLbc(tx.valueLbc),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (tx.valueLbc >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

private fun formatLbc(amount: Double): String {
    return "%.4f".format(amount).trimEnd('0').trimEnd('.').let {
        if (it.isEmpty() || it == "-") "0" else it
    }
}

private fun formatDate(seconds: Long): String {
    val ms = if (seconds > 9_999_999_999L) seconds else seconds * 1000L
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))
}
