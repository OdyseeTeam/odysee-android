package com.odysee.app.core.designsystem.comments

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OdyseeCommentThread(
    comment: OdyseeComment,
    replies: List<OdyseeComment>?,
    actions: OdyseeCommentActions,
    replyActionsFor: (OdyseeComment) -> OdyseeCommentActions,
    canReply: Boolean,
    replyOpen: Boolean = false,
    onReplyOpenChange: (Boolean) -> Unit = {},
    onLoadReplies: () -> Unit = {},
    onLongPress: (OdyseeComment) -> Unit = {},
) {
    var expanded by remember(comment.id) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        OdyseeCommentRow(
            comment = comment,
            actions = actions,
            canReply = canReply,
            replyOpen = replyOpen,
            onReplyOpenChange = onReplyOpenChange,
            onLongPress = { onLongPress(comment) },
        )
        if (comment.replyCount > 0) {
            TextButton(
                onClick = {
                    if (!expanded && replies == null) onLoadReplies()
                    expanded = !expanded
                },
                contentPadding = PaddingValues(start = 56.dp, end = 16.dp, top = 0.dp, bottom = 4.dp),
            ) {
                Text(
                    text = if (expanded) "Hide replies"
                    else "View ${comment.replyCount} ${if (comment.replyCount == 1) "reply" else "replies"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (expanded) {
                if (replies == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, top = 4.dp, bottom = 8.dp),
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    Column(modifier = Modifier.padding(start = 40.dp)) {
                        replies.forEach { reply ->
                            OdyseeCommentRow(
                                comment = reply,
                                actions = replyActionsFor(reply),
                                canReply = false,
                                onLongPress = { onLongPress(reply) },
                            )
                        }
                    }
                }
            }
        }
    }
}
