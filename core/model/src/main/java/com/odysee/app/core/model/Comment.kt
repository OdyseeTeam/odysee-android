package com.odysee.app.core.model

data class Comment(
    val commentId: String,
    val parentId: String?,
    val claimId: String? = null,
    val authorName: String,
    val authorChannelId: String?,
    val authorAvatarUrl: String?,
    val body: String,
    val timestamp: Long,
    val signingTimestamp: Long? = null,
    val isPinned: Boolean,
    val isCreator: Boolean = false,
    val isModerator: Boolean = false,
    val isGlobalMod: Boolean = false,
    val replyCount: Int = 0,
    val supportAmount: Double = 0.0,
)
