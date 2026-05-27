package com.odysee.app.core.network.mapper

import com.odysee.app.core.model.Comment
import com.odysee.app.core.network.dto.CommentDto

fun CommentDto.toDomain(): Comment = Comment(
    commentId = commentId,
    parentId = parentId,
    claimId = claimId,
    authorName = channelName ?: "@anonymous",
    authorChannelId = channelId,
    authorAvatarUrl = null,
    body = comment,
    timestamp = timestamp,
    signingTimestamp = signingTs?.toLongOrNull(),
    isPinned = isPinned,
    isCreator = isCreator,
    isModerator = isModerator,
    isGlobalMod = isGlobalMod,
    replyCount = replies ?: 0,
    supportAmount = supportAmount ?: 0.0,
)
