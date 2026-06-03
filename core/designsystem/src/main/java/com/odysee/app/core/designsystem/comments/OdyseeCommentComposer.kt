package com.odysee.app.core.designsystem.comments

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

const val ODYSEE_MAX_CHARS_COMMENT = 2000
const val ODYSEE_MAX_CHARS_LIVESTREAM_COMMENT = 300

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdyseeCommentComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Add a comment…",
    maxLength: Int = ODYSEE_MAX_CHARS_COMMENT,
    maxLines: Int = 4,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    showEmojis: Boolean = true,
    showStickers: Boolean = true,
    showHyperchat: Boolean = false,
    onInsertSticker: (StickerDef) -> Unit = {},
    onOpenHyperchat: () -> Unit = {},
) {
    var showStickerSheet by remember { mutableStateOf(false) }
    var showEmojiSheet by remember { mutableStateOf(false) }
    val trimmed = draft.trim()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                if (draft.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { v ->
                        if (v.length <= maxLength) onDraftChange(v)
                        else onDraftChange(v.take(maxLength))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = if (singleLine) 1 else maxLines,
                )
            }
            if (showEmojis) {
                IconButton(onClick = { showEmojiSheet = true }, modifier = Modifier.size(36.dp), enabled = enabled) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEmotions,
                        contentDescription = "Emoji",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            if (showStickers) {
                IconButton(onClick = { showStickerSheet = true }, modifier = Modifier.size(36.dp), enabled = enabled) {
                    Icon(
                        imageVector = Icons.Outlined.AddReaction,
                        contentDescription = "Sticker",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            if (showHyperchat) {
                IconButton(onClick = onOpenHyperchat, modifier = Modifier.size(36.dp), enabled = enabled) {
                    Icon(
                        imageVector = Icons.Outlined.AttachMoney,
                        contentDescription = "Hyperchat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = onSubmit,
            enabled = enabled && trimmed.isNotEmpty(),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Post",
                tint = if (enabled && trimmed.isNotEmpty()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
    if (showStickerSheet) {
        OdyseeStickerPickerSheet(
            onDismiss = { showStickerSheet = false },
            onPick = { sticker ->
                showStickerSheet = false
                onInsertSticker(sticker)
            },
        )
    }
    if (showEmojiSheet) {
        OdyseeEmojiPickerSheet(
            onDismiss = { showEmojiSheet = false },
            onPick = { token ->
                val next = draft + token
                if (next.length <= maxLength) onDraftChange(next)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdyseeStickerPickerSheet(onDismiss: () -> Unit, onPick: (StickerDef) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(80.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Free",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
            gridItems(items = FREE_GLOBAL_STICKERS, key = { "free-${it.name}" }) { sticker ->
                StickerCell(sticker = sticker, onPick = onPick)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Tips",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
            gridItems(items = PAID_GLOBAL_STICKERS, key = { "paid-${it.name}" }) { sticker ->
                StickerCell(sticker = sticker, onPick = onPick)
            }
        }
    }
}

@Composable
private fun StickerCell(sticker: StickerDef, onPick: (StickerDef) -> Unit) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onPick(sticker) },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = sticker.url,
            contentDescription = sticker.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(4.dp),
        )
        val price = sticker.priceLbc
        if (price != null && price > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE2202D))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(
                    text = "$$price",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdyseeEmojiPickerSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(items = EMOTE_CATEGORIES, key = { _, c -> c.key }) { idx, cat ->
                val selected = idx == selectedCategoryIndex
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.surfaceVariant
                            else Color.Transparent,
                        )
                        .clickable { selectedCategoryIndex = idx }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = cat.mainImg,
                        contentDescription = cat.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        val category = EMOTE_CATEGORIES[selectedCategoryIndex]
        LazyVerticalGrid(
            columns = GridCells.Adaptive(48.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
        ) {
            gridItems(items = category.items, key = { "${category.key}-${it.name}" }) { emote ->
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onPick(emote.name) }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = emote.url,
                        contentDescription = emote.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
fun OdyseeHyperchatStickerDialog(
    sticker: StickerDef,
    onDismiss: () -> Unit,
    onConfirm: suspend (Double) -> Result<Unit>,
) {
    val price = (sticker.priceLbc ?: 0).toDouble()
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coScope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text("Send sticker hyperchat") },
        text = {
            Column {
                AsyncImage(
                    model = sticker.url,
                    contentDescription = sticker.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(96.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Tip $${price.toInt()} (LBC) and post this sticker as a hyperchat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !sending,
                onClick = {
                    sending = true
                    error = null
                    coScope.launch {
                        val result = onConfirm(price)
                        sending = false
                        if (result.isFailure) error = result.exceptionOrNull()?.message ?: "Failed"
                        else onDismiss()
                    }
                },
            ) { Text(if (sending) "Sending…" else "Send") }
        },
        dismissButton = { TextButton(enabled = !sending, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun OdyseeHyperchatTextDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: suspend (String, Double) -> Result<Unit>,
) {
    var text by remember { mutableStateOf(initialText) }
    var amount by remember { mutableStateOf("1") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coScope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text("Hyperchat") },
        text = {
            Column {
                Text(
                    text = "Highlight your comment by tipping the creator. Paid in LBC from your wallet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message") },
                    maxLines = 4,
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { v -> amount = v.filter { it.isDigit() || it == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount (LBC)") },
                    singleLine = true,
                )
                error?.let {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            val amt = amount.toDoubleOrNull() ?: 0.0
            TextButton(
                enabled = !sending && text.isNotBlank() && amt > 0,
                onClick = {
                    sending = true
                    error = null
                    coScope.launch {
                        val result = onConfirm(text.trim(), amt)
                        sending = false
                        if (result.isFailure) error = result.exceptionOrNull()?.message ?: "Failed"
                        else onDismiss()
                    }
                },
            ) { Text(if (sending) "Sending…" else "Send") }
        },
        dismissButton = { TextButton(enabled = !sending, onClick = onDismiss) { Text("Cancel") } },
    )
}
